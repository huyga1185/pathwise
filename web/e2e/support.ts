import { expect, request, type APIRequestContext, type Page } from '@playwright/test';

/** The E2E stack (compose.e2e.yaml, ports in e2e/e2e.env). */
export const API = process.env.PATHWISE_API_PROXY ?? 'http://localhost:8091';
export const MAILPIT = process.env.PATHWISE_MAILPIT ?? 'http://localhost:8035';

/** Unique per run, so the stack can be reused without cleaning the database. */
export const RUN = Date.now().toString(36);

/** Today in the store's timezone: a new plan's default date and the driver's default day. */
export function todayInVietnam(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date());
}

/** No request leaves for third-party hosts (fonts): the run only talks to the app and its stack. */
export async function blockThirdParties(page: Page) {
  await page.route(/^https?:\/\/(?!localhost)/, (route) => route.abort());
}

/** The text of the newest email sent to `to` by the stack's Mailpit (waits for it). */
async function lastEmail(to: string, part: 'Text' | 'HTML'): Promise<string> {
  const mailpit = await request.newContext({ baseURL: MAILPIT });
  try {
    let body = '';
    await expect
      .poll(
        async () => {
          const search = await mailpit.get(`/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}`);
          const messages = ((await search.json()) as { messages?: Array<{ ID: string }> }).messages ?? [];
          if (messages.length === 0) return false;
          const message = await mailpit.get(`/api/v1/message/${messages[0]?.ID}`);
          body = ((await message.json()) as Record<string, string>)[part] ?? '';
          return body.length > 0;
        },
        { timeout: 30_000, message: `email to ${to}` },
      )
      .toBe(true);
    return body;
  } finally {
    await mailpit.dispose();
  }
}

export async function otpSentTo(email: string): Promise<string> {
  const match = /\b(\d{6})\b/.exec(await lastEmail(email, 'Text'));
  if (!match?.[1]) throw new Error(`no code in the email to ${email}`);
  return match[1];
}

export async function invitationCodeSentTo(email: string): Promise<string> {
  const match = />([A-Za-z0-9_-]{43})</.exec(await lastEmail(email, 'HTML'));
  if (!match?.[1]) throw new Error(`no invitation code in the email to ${email}`);
  return match[1];
}

/** The driver's app, by API (the Flutter app comes with M00–M03): signs in as mobile and reports stop results. */
export async function driverApi(email: string, password: string): Promise<APIRequestContext> {
  const signIn = await request.newContext({ baseURL: API, extraHTTPHeaders: { 'X-Client-Platform': 'mobile' } });
  const response = await signIn.post('/api/v1/auth/log-in', { data: { email, password } });
  expect(response.status(), await response.text()).toBe(200);
  const token = ((await response.json()) as { data: { accessToken: string } }).data.accessToken;
  await signIn.dispose();
  return request.newContext({
    baseURL: API,
    extraHTTPHeaders: { 'X-Client-Platform': 'mobile', Authorization: `Bearer ${token}` },
  });
}
