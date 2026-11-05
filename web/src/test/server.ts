import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

/** API origin of the test environment (vite.config.ts test.env). */
export const API = 'http://localhost/api/v1';

export const server = setupServer();

export const owner = { id: 449, email: 'chu.cuahang@example.com', role: 'OWNER', storeId: 182, canDrive: false };
export const driver = {
  id: 451,
  email: 'taixe.a@example.com',
  fullName: 'Tài xế A',
  role: 'DRIVER',
  storeId: 182,
  canDrive: true,
};

/** `{data}` envelope of API_SPEC §1. */
export const ok = (data: unknown, init?: ResponseInit) => HttpResponse.json({ data }, init);
/** Error body `{code, details?}` with optional headers. */
export const fail = (status: number, code: string, details?: unknown[], headers?: Record<string, string>) =>
  HttpResponse.json(details ? { code, details } : { code }, {
    status,
    headers: { 'X-Request-Id': 'req-123', ...headers },
  });

/** Handlers of a signed-in OWNER whose session the refresh cookie restores. */
export function signedInHandlers(user = owner) {
  return [
    http.post(`${API}/auth/refresh`, () => ok({ accessToken: 'access-1' })),
    http.get(`${API}/users/me`, () => ok(user)),
    http.get(`${API}/store`, () => ok({ id: 182, name: 'Tiệm bánh Cô Lan', timezone: 'Asia/Ho_Chi_Minh' })),
    http.post(`${API}/auth/log-out`, () => new HttpResponse(null, { status: 204 })),
    http.get(`${API}/plans`, () => ok({ items: [], page: 0, size: 20, totalPages: 0, totalElements: 0 })),
  ];
}

/** Handlers of a visitor without a session. */
export function anonymousHandlers() {
  return [
    http.post(`${API}/auth/refresh`, () => fail(401, 'INVALID_TOKEN')),
    http.get(`${API}/store`, () => ok({ id: 182, name: 'Tiệm bánh Cô Lan', timezone: 'Asia/Ho_Chi_Minh' })),
    http.post(`${API}/auth/log-out`, () => new HttpResponse(null, { status: 204 })),
  ];
}
