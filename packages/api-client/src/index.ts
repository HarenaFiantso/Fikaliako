import createClient, { type ClientOptions, type Middleware } from 'openapi-fetch';

import type { components, operations, paths } from './schema.js';

/**
 * Typed client for the Fikaliako API, generated from the hand-managed
 * OpenAPI contract (apps/api/src/main/resources/static/v1/openapi.yaml).
 *
 * Platform-neutral: relies only on the global `fetch`, so it works in
 * Next.js (server and client components) and React Native alike.
 *
 * Regenerate the types after every contract change:
 *   pnpm --filter @fikaliako/api-client generate
 */
export function createFikaliakoClient(baseUrl: string, options?: Omit<ClientOptions, 'baseUrl'>) {
  return createClient<paths>({ baseUrl, ...options });
}

export type FikaliakoClient = ReturnType<typeof createFikaliakoClient>;

/**
 * Supplies the current access token, or null/undefined when logged out.
 * May be async so callers can refresh a stale token before the request
 * (e.g. POST /v1/auth/refresh once `expires_in` has elapsed).
 */
export type AccessTokenProvider = () =>
  string | null | undefined | Promise<string | null | undefined>;

/**
 * openapi-fetch middleware adding `Authorization: Bearer <token>` to every
 * request. Requests go out untouched while the provider yields no token, so
 * one client instance serves both public browsing and authenticated calls.
 */
export function bearerAuthMiddleware(getAccessToken: AccessTokenProvider): Middleware {
  return {
    async onRequest({ request }) {
      const token = await getAccessToken();
      if (token) request.headers.set('Authorization', `Bearer ${token}`);
      return request;
    },
  };
}

/**
 * A Fikaliako client wired with {@link bearerAuthMiddleware}.
 *
 * ```ts
 * const api = createAuthenticatedFikaliakoClient(baseUrl, () => tokenStore.accessToken);
 * const { data } = await api.GET('/v1/users/me');
 * ```
 */
export function createAuthenticatedFikaliakoClient(
  baseUrl: string,
  getAccessToken: AccessTokenProvider,
  options?: Omit<ClientOptions, 'baseUrl'>
) {
  const client = createFikaliakoClient(baseUrl, options);
  client.use(bearerAuthMiddleware(getAccessToken));
  return client;
}

/** Raw generated types, for advanced use. */
export type { components, operations, paths };

/** Schema shortcuts — one alias per component schema in the contract. */
export type Schemas = components['schemas'];
export type PingResponse = Schemas['PingResponse'];
export type Problem = Schemas['Problem'];
export type GeoPoint = Schemas['GeoPoint'];
export type EstablishmentType = Schemas['EstablishmentType'];
export type EstablishmentSummary = Schemas['EstablishmentSummary'];
export type EstablishmentDetail = Schemas['EstablishmentDetail'];
export type EstablishmentPage = Schemas['EstablishmentPage'];
export type SearchInterpretation = Schemas['SearchInterpretation'];
export type SearchPage = Schemas['SearchPage'];
export type Amenities = Schemas['Amenities'];
export type OpeningInterval = Schemas['OpeningInterval'];
export type RatingSummary = Schemas['RatingSummary'];
export type ReferentialItem = Schemas['ReferentialItem'];
export type ReviewItem = Schemas['ReviewItem'];
export type ReviewPage = Schemas['ReviewPage'];
export type ReviewInput = Schemas['ReviewInput'];
export type PhoneNumber = Schemas['PhoneNumber'];
export type OtpCode = Schemas['OtpCode'];
export type UserRole = Schemas['UserRole'];
export type UserProfile = Schemas['UserProfile'];
export type RegisterRequest = Schemas['RegisterRequest'];
export type VerifyPhoneRequest = Schemas['VerifyPhoneRequest'];
export type ResendOtpRequest = Schemas['ResendOtpRequest'];
export type LoginRequest = Schemas['LoginRequest'];
export type RefreshRequest = Schemas['RefreshRequest'];
export type LogoutRequest = Schemas['LogoutRequest'];
export type ForgotPasswordRequest = Schemas['ForgotPasswordRequest'];
export type ResetPasswordRequest = Schemas['ResetPasswordRequest'];
export type ChangePasswordRequest = Schemas['ChangePasswordRequest'];
export type AuthTokens = Schemas['AuthTokens'];
export type AuthSession = Schemas['AuthSession'];
export type UpdateProfileRequest = Schemas['UpdateProfileRequest'];
export type BusinessEstablishmentUpdate = Schemas['BusinessEstablishmentUpdate'];
export type AmenitiesUpdate = Schemas['AmenitiesUpdate'];
export type OpeningHoursUpdate = Schemas['OpeningHoursUpdate'];
export type ManagerItem = Schemas['ManagerItem'];
export type ManagerPage = Schemas['ManagerPage'];
