import { createFikaliakoClient } from '@fikaliako/api-client';

import { API_BASE_URL } from '@/lib/api/base-url';
import { useAuthStore } from '@/lib/auth/auth-store';

const EXPIRY_SAFETY_WINDOW_MS = 30_000;

const bareClient = createFikaliakoClient(API_BASE_URL);

let refreshInFlight: Promise<string | null> | null = null;

/**
 * Returns an access token that is still valid for at least 30 seconds,
 * rotating the refresh token if needed. Concurrent callers share one refresh
 * request — the refresh token is single-use, so a duplicate rotation would
 * revoke the whole session family server-side.
 */
export async function getValidAccessToken(): Promise<string | null> {
  const { accessToken, accessTokenExpiresAt, refreshToken } = useAuthStore.getState();

  if (
    accessToken &&
    accessTokenExpiresAt &&
    Date.now() < accessTokenExpiresAt - EXPIRY_SAFETY_WINDOW_MS
  ) {
    return accessToken;
  }
  if (!refreshToken) return null;

  refreshInFlight ??= rotateTokens(refreshToken).finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function rotateTokens(refreshToken: string): Promise<string | null> {
  try {
    const { data, response } = await bareClient.POST('/v1/auth/refresh', {
      body: { refresh_token: refreshToken },
    });
    if (!data) {
      if (response.status === 400 || response.status === 401) {
        useAuthStore.getState().clearSession();
      }
      return null;
    }
    useAuthStore.getState().setTokens(data);
    return data.access_token;
  } catch {
    return null;
  }
}
