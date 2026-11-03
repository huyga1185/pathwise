import type { components } from './schema';

/** Shapes generated from api/openapi.json (P-41). Never written by hand. */
export type Schemas = components['schemas'];
export type AuthTokens = Schemas['AuthTokenResponse'];
export type AuthUser = Schemas['AuthUserResponse'];
export type User = Schemas['GetUserResponse'];
export type Store = Schemas['StoreResponse'];
export type UserRole = NonNullable<User['role']>;
