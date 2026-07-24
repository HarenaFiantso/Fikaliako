import { createFikaliakoClient } from '@fikaliako/api-client';

import { API_BASE_URL } from '../lib/base-url';
import { clearSession, getAccessToken, loadSession, updateTokens } from './session-store';

/** Refresh a little early so a token never expires mid-request. */
const REFRESH_MARGIN_MS = 30_000;

// Bare client: the refresh call itself must not carry an Authorization header.
const bareApi = createFikaliakoClient(API_BASE_URL);

let refreshInFlight: Promise<string | null> | null = null;

/**
 * Returns an access token valid for at least {@link REFRESH_MARGIN_MS},
 * rotating the refresh token when needed. Refresh tokens are single-use
 * (replaying one revokes the whole session), so concurrent callers share a
 * single in-flight rotation.
 */
export async function getValidAccessToken(): Promise<string | null> {
  const { token, expiresAt } = getAccessToken();
  if (token && Date.now() < expiresAt - REFRESH_MARGIN_MS) return token;
  refreshInFlight ??= rotate().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function rotate(): Promise<string | null> {
  const session = loadSession();
  if (!session) return null;
  const { data, error, response } = await bareApi.POST('/v1/auth/refresh', {
    body: { refresh_token: session.refreshToken },
  });
  if (error || !data) {
    // A rejected refresh token means the session is gone for good.
    if (response.status === 400 || response.status === 401) clearSession();
    return null;
  }
  updateTokens(data);
  return data.access_token;
}
