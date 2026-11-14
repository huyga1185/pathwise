import { describe, expect, it, vi } from 'vitest';
import { MemoryTokenStore } from '@/platform/tokenStore';
import { ApiError } from './apiError';
import { createApi, filenameOf, unwrap } from './http';

const ORIGIN = 'http://localhost';
const json = (status: number, body: unknown, headers: Record<string, string> = {}) =>
  new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json', ...headers } });

/** A fake backend: /users/me needs `Bearer fresh`, /auth/refresh hands it out after a short delay. */
function backend(refreshStatus = 200) {
  const calls: string[] = [];
  const fetchImpl = vi.fn(async (input: RequestInfo | URL) => {
    const request = input instanceof Request ? input : new Request(String(input), { method: 'POST' });
    const path = new URL(request.url).pathname;
    calls.push(path);
    if (path === '/api/v1/auth/refresh') {
      await new Promise((resolve) => setTimeout(resolve, 10));
      return refreshStatus === 200
        ? json(200, { data: { accessToken: 'fresh' } })
        : json(refreshStatus, { code: 'INVALID_TOKEN' });
    }
    if (request.headers.get('Authorization') !== 'Bearer fresh') return json(401, { code: 'INVALID_TOKEN' });
    return json(200, { data: { id: 1, email: 'a@example.com' } });
  });
  return { fetchImpl, calls };
}

describe('createApi', () => {
  it('chỉ gọi refresh một lần khi hai request cùng bị 401, rồi gửi lại mỗi request đúng một lần', async () => {
    const { fetchImpl, calls } = backend();
    const tokenStore = new MemoryTokenStore();
    tokenStore.set('expired');
    const api = createApi({ origin: ORIGIN, tokenStore, fetch: fetchImpl as unknown as typeof fetch });

    const [first, second] = await Promise.all([
      unwrap(api.client.GET('/api/v1/users/me')),
      unwrap(api.client.GET('/api/v1/users/me')),
    ]);

    expect(first).toEqual({ id: 1, email: 'a@example.com' });
    expect(second).toEqual({ id: 1, email: 'a@example.com' });
    expect(calls.filter((path) => path === '/api/v1/auth/refresh')).toHaveLength(1);
    expect(calls.filter((path) => path === '/api/v1/users/me')).toHaveLength(4);
    expect(tokenStore.get()).toBe('fresh');
  });

  it('báo hết phiên một lần và trả lỗi 401 khi refresh thất bại', async () => {
    const { fetchImpl } = backend(401);
    const tokenStore = new MemoryTokenStore();
    tokenStore.set('expired');
    const onSessionExpired = vi.fn();
    const api = createApi({
      origin: ORIGIN,
      tokenStore,
      onSessionExpired,
      fetch: fetchImpl as unknown as typeof fetch,
    });

    await expect(unwrap(api.client.GET('/api/v1/users/me'))).rejects.toMatchObject({
      status: 401,
      code: 'INVALID_TOKEN',
    });
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
    expect(tokenStore.get()).toBeNull();
  });

  it('không refresh khi chính lệnh đăng nhập trả 401', async () => {
    const fetchImpl = vi.fn(async () => json(401, { code: 'INVALID_EMAIL_OR_PASSWORD' }, { 'X-Request-Id': 'r-1' }));
    const api = createApi({
      origin: ORIGIN,
      tokenStore: new MemoryTokenStore(),
      fetch: fetchImpl as unknown as typeof fetch,
    });

    const error = await unwrap(
      api.client.POST('/api/v1/auth/log-in', { body: { email: 'a@example.com', password: 'x' } }),
    ).catch((e) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ code: 'INVALID_EMAIL_OR_PASSWORD', requestId: 'r-1' });
    expect(fetchImpl).toHaveBeenCalledTimes(1);
  });

  it('đọc Retry-After và details của lỗi', async () => {
    const fetchImpl = vi.fn(async () =>
      json(429, { code: 'RATE_LIMITED', details: [{ field: 'email', code: 'X' }] }, { 'Retry-After': '252' }),
    );
    const api = createApi({
      origin: ORIGIN,
      tokenStore: new MemoryTokenStore(),
      fetch: fetchImpl as unknown as typeof fetch,
    });

    const error = (await unwrap(api.client.POST('/api/v1/auth/log-in', { body: { email: 'a', password: 'b' } })).catch(
      (e) => e,
    )) as ApiError;
    expect(error.retryAfter).toBe(252);
    expect(error.details).toEqual([{ field: 'email', code: 'X' }]);
  });

  it('đổi lỗi mạng thành NETWORK_ERROR', async () => {
    const fetchImpl = vi.fn(async () => {
      throw new TypeError('Failed to fetch');
    });
    const api = createApi({
      origin: ORIGIN,
      tokenStore: new MemoryTokenStore(),
      fetch: fetchImpl as unknown as typeof fetch,
    });

    await expect(unwrap(api.client.GET('/api/v1/users/me'))).rejects.toMatchObject({ code: 'NETWORK_ERROR' });
  });
});

describe('filenameOf', () => {
  it('đọc tên file của Content-Disposition, cả dạng filename*', () => {
    expect(filenameOf('attachment; filename="pathwise-20261201-20261207.csv"')).toBe('pathwise-20261201-20261207.csv');
    expect(filenameOf("attachment; filename*=UTF-8''m%E1%BA%ABu.csv")).toBe('mẫu.csv');
    expect(filenameOf(null)).toBeNull();
  });
});
