/** One entry of an error body's `details` (API_SPEC §1): the request field to fix, its code, extra fields per code. */
export interface ErrorDetail {
  field?: string;
  code: string;
  [extra: string]: unknown;
}

/**
 * Every failed API call, as the rest of the app sees it (docs/client/ARCHITECTURE.md §3.1). `code` is the server's
 * error code, or NETWORK_ERROR when no response came back.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: ErrorDetail[];
  /** Seconds to wait before retrying, from Retry-After (429). */
  readonly retryAfter: number | null;
  /** X-Request-Id of the response, for "Mã tra cứu". */
  readonly requestId: string | null;

  constructor(init: {
    status: number;
    code: string;
    details?: ErrorDetail[];
    retryAfter?: number | null;
    requestId?: string | null;
  }) {
    super(init.code);
    this.name = 'ApiError';
    this.status = init.status;
    this.code = init.code;
    this.details = init.details ?? [];
    this.retryAfter = init.retryAfter ?? null;
    this.requestId = init.requestId ?? null;
  }

  static network(): ApiError {
    return new ApiError({ status: 0, code: 'NETWORK_ERROR' });
  }

  /** Reads the error body `{code, details?}` and the headers of a failed response. */
  static async fromResponse(response: Response, body?: unknown): Promise<ApiError> {
    let parsed = body;
    if (parsed === undefined) {
      try {
        parsed = await response.clone().json();
      } catch {
        parsed = undefined;
      }
    }
    const record = (parsed && typeof parsed === 'object' ? parsed : {}) as { code?: unknown; details?: unknown };
    const retryAfterHeader = response.headers.get('Retry-After');
    const retryAfter = retryAfterHeader !== null && /^\d+$/.test(retryAfterHeader) ? Number(retryAfterHeader) : null;
    return new ApiError({
      status: response.status,
      code: typeof record.code === 'string' ? record.code : `HTTP_${response.status}`,
      details: Array.isArray(record.details) ? (record.details as ErrorDetail[]) : [],
      retryAfter,
      requestId: response.headers.get('X-Request-Id'),
    });
  }
}
