/**
 * TokenStore port (P-46): where the access token lives. On the web it is memory only, never localStorage; the refresh
 * token is an HttpOnly cookie the server sets, so the app never sees it.
 */
export interface TokenStore {
  get(): string | null;
  set(token: string | null): void;
}

export class MemoryTokenStore implements TokenStore {
  private token: string | null = null;

  get(): string | null {
    return this.token;
  }

  set(token: string | null): void {
    this.token = token;
  }
}
