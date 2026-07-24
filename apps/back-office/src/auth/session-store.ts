import type { AuthTokens, UserProfile } from '@fikaliako/api-client';

/**
 * Back-office session persistence, mirroring the web app's policy: only the
 * user profile and the revocable refresh token touch localStorage — the
 * short-lived access JWT stays in memory (project book ch. 7.3).
 */
const STORAGE_KEY = 'fikaliako-back-office-auth';

export type PersistedSession = {
  user: UserProfile;
  refreshToken: string;
};

let accessToken: string | null = null;
let accessTokenExpiresAt = 0;

export function loadSession(): PersistedSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as PersistedSession) : null;
  } catch {
    return null;
  }
}

export function saveSession(user: UserProfile, tokens: AuthTokens): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ user, refreshToken: tokens.refresh_token }));
  keepAccessToken(tokens);
}

/** After a refresh rotation: same user, new token pair. */
export function updateTokens(tokens: AuthTokens): void {
  const session = loadSession();
  if (session) saveSession(session.user, tokens);
  else keepAccessToken(tokens);
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY);
  accessToken = null;
  accessTokenExpiresAt = 0;
}

export function getAccessToken(): { token: string | null; expiresAt: number } {
  return { token: accessToken, expiresAt: accessTokenExpiresAt };
}

function keepAccessToken(tokens: AuthTokens): void {
  accessToken = tokens.access_token;
  accessTokenExpiresAt = Date.now() + tokens.expires_in * 1000;
}
