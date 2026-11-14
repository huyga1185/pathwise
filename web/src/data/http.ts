import createClient from 'openapi-fetch';
import type { TokenStore } from '@/platform/tokenStore';
import { ApiError } from './apiError';
import type { paths } from './schema';

export type ApiClient = ReturnType<typeof createClient<paths>>;

export interface Api {
  /** Typed client generated from api/openapi.json (P-41). Use with {@link unwrap}. */
  client: ApiClient;
  /**
   * Gets a new access token from the refresh cookie. Concurrent callers share one request (single flight, F-29).
   * @returns whether the session is still valid
   */
  refresh(): Promise<boolean>;
}

export interface CreateApiOptions {
  origin: string;
  tokenStore: TokenStore;
  /** Called once when a refresh fails while the app thought it was signed in. */
  onSessionExpired?: () => void;
  fetch?: typeof fetch;
}

const AUTH_PATHS = ['/api/v1/auth/log-in', '/api/v1/auth/refresh', '/api/v1/auth/log-out'];

/**
 * The HTTP layer (docs/client/ARCHITECTURE.md §3, §4): adds the access token, and on a 401 refreshes once (shared by
 * every request waiting) then sends the request again exactly once. Auth endpoints are never retried.
 */
export function createApi({
  origin,
  tokenStore,
  onSessionExpired,
  fetch: baseFetch = globalThis.fetch,
}: CreateApiOptions): Api {
  let refreshing: Promise<boolean> | null = null;

  async function doRefresh(): Promise<boolean> {
    let response: Response;
    try {
      response = await baseFetch(`${origin}/api/v1/auth/refresh`, { method: 'POST', credentials: 'include' });
    } catch {
      return false;
    }
    if (!response.ok) {
      tokenStore.set(null);
      return false;
    }
    const body = (await response.json()) as { data?: { accessToken?: string } };
    const token = body.data?.accessToken ?? null;
    tokenStore.set(token);
    return token !== null;
  }

  function refresh(): Promise<boolean> {
    refreshing ??= doRefresh().finally(() => {
      refreshing = null;
    });
    return refreshing;
  }

  function withToken(request: Request): Request {
    const token = tokenStore.get();
    if (!token) return request;
    const headers = new Headers(request.headers);
    headers.set('Authorization', `Bearer ${token}`);
    return new Request(request, { headers });
  }

  const authFetch = async (input: Request): Promise<Response> => {
    const isAuthCall = AUTH_PATHS.some((path) => new URL(input.url, 'http://local').pathname === path);
    const retry = isAuthCall ? null : input.clone();
    const hadToken = tokenStore.get() !== null;
    let response: Response;
    try {
      response = await baseFetch(withToken(input));
    } catch {
      throw ApiError.network();
    }
    if (response.status !== 401 || retry === null) return response;
    if (!(await refresh())) {
      if (hadToken) onSessionExpired?.();
      return response;
    }
    try {
      return await baseFetch(withToken(retry));
    } catch {
      throw ApiError.network();
    }
  };

  const client = createClient<paths>({
    baseUrl: origin,
    credentials: 'include',
    fetch: authFetch,
  });
  return { client, refresh };
}

/**
 * Returns `data` of the `{data}` envelope, or throws an {@link ApiError} for an error response (API_SPEC §1).
 */
export async function unwrap<T>(
  call: Promise<{ data?: { data?: T }; error?: unknown; response: Response }>,
): Promise<T> {
  const { data, error, response } = await call;
  if (error !== undefined || !response.ok) throw await ApiError.fromResponse(response, error);
  return data?.data as T;
}

/** Like {@link unwrap} for responses without a body (204). */
export async function unwrapEmpty(call: Promise<{ error?: unknown; response: Response }>): Promise<void> {
  const { error, response } = await call;
  if (error !== undefined || !response.ok) throw await ApiError.fromResponse(response, error);
}

/** A file the server sends (`Content-Disposition: attachment`), with the name it gives. */
export interface DownloadedFile {
  blob: Blob;
  filename: string;
}

/**
 * Like {@link unwrap} for a file response (call made with `parseAs: 'blob'`): the body and the file name of its
 * `Content-Disposition`, `fallbackName` without one. An error response still throws an {@link ApiError}.
 */
export async function unwrapFile(
  call: Promise<{ data?: unknown; error?: unknown; response: Response }>,
  fallbackName: string,
): Promise<DownloadedFile> {
  const { data, error, response } = await call;
  if (error !== undefined || !response.ok) throw await ApiError.fromResponse(response, error);
  return { blob: data as Blob, filename: filenameOf(response.headers.get('Content-Disposition')) ?? fallbackName };
}

/** `attachment; filename="a.csv"` or `filename*=UTF-8''a.csv` → `a.csv`. */
export function filenameOf(disposition: string | null): string | null {
  if (!disposition) return null;
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition);
  if (encoded?.[1]) return decodeURIComponent(encoded[1]);
  const plain = /filename="?([^";]+)"?/i.exec(disposition);
  return plain?.[1] ?? null;
}
