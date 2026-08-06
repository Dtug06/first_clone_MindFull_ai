/**
 * Shared API client used by every frontend feature module.
 *
 * Rules:
 * - Base URL is read from `import.meta.env.VITE_API_BASE_URL` (set in `.env`).
 *   Never hard-code the backend URL inside feature code.
 * - Adds `Authorization: Bearer <token>` when a token is present.
 * - Normalises errors into a single `ApiError` shape so feature code never
 *   touches raw `Response` objects.
 * - Propagates `X-Request-Id` from the response header so the UI can show
 *   a trace ID to the user when something fails.
 */

export class ApiError extends Error {
  status: number;
  code: string;
  requestId: string | null;
  fieldErrors: ReadonlyArray<{ field: string; message: string }> | null;

  constructor(
    status: number,
    code: string,
    message: string,
    requestId: string | null,
    fieldErrors: ReadonlyArray<{ field: string; message: string }> | null,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.requestId = requestId;
    this.fieldErrors = fieldErrors;
  }
}

export type TokenProvider = () => string | null;
export type OnUnauthorized = () => void;

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Record<string, string | number | boolean | undefined>;
  headers?: Record<string, string>;
}

const REQUEST_ID_HEADER = 'X-Request-Id';
const AUTH_HEADER = 'Authorization';

export class ApiClient {
  private readonly baseUrl: string;
  private readonly getToken: TokenProvider;
  private readonly onUnauthorized: OnUnauthorized;

  constructor(baseUrl: string, getToken: TokenProvider, onUnauthorized: OnUnauthorized) {
    this.baseUrl = baseUrl.replace(/\/+$/, '');
    this.getToken = getToken;
    this.onUnauthorized = onUnauthorized;
  }

  async request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const method = options.method ?? 'GET';
    const url = this.buildUrl(path, options.query);

    const headers: Record<string, string> = {
      Accept: 'application/json',
      ...(options.headers ?? {}),
    };
    if (options.body !== undefined) {
      headers['Content-Type'] = 'application/json';
    }
    const token = this.getToken();
    if (token) {
      headers[AUTH_HEADER] = `Bearer ${token}`;
    }

    let response: Response;
    try {
      response = await fetch(url, {
        method,
        headers,
        body: options.body === undefined ? undefined : JSON.stringify(options.body),
      });
    } catch {
      throw new ApiError(0, 'NETWORK_ERROR', 'Network request failed', null, null);
    }

    const requestId = response.headers.get(REQUEST_ID_HEADER);

    if (response.status === 204) {
      return undefined as T;
    }

    const contentType = response.headers.get('Content-Type') ?? '';
    const isJson = contentType.includes('application/json');
    const payload = isJson ? await response.json().catch(() => null) : null;

    if (!response.ok) {
      if (response.status === 401) {
        this.onUnauthorized();
      }
      const code = (payload && typeof payload.code === 'string' ? payload.code : 'UNEXPECTED_ERROR');
      const message = (payload && typeof payload.message === 'string'
        ? payload.message
        : `Request failed with status ${response.status}`);
      const fieldErrors = Array.isArray(payload && payload.fieldErrors) ? payload.fieldErrors : null;
      throw new ApiError(response.status, code, message, requestId, fieldErrors);
    }

    return payload as T;
  }

  private buildUrl(path: string, query?: Record<string, string | number | boolean | undefined>): string {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    if (!query) {
      return `${this.baseUrl}${normalizedPath}`;
    }
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query)) {
      if (value === undefined || value === null) {
        continue;
      }
      params.append(key, String(value));
    }
    const queryString = params.toString();
    return queryString
      ? `${this.baseUrl}${normalizedPath}?${queryString}`
      : `${this.baseUrl}${normalizedPath}`;
  }
}

export const REQUEST_ID_HEADER_NAME = REQUEST_ID_HEADER;
