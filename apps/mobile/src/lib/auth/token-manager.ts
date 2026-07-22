import { type AuthTokens, createFikaliakoClient } from '@fikaliako/api-client';

import { API_URL } from '@/lib/config';

import { type PersistedTokens, saveTokens } from './token-storage';

const REFRESH_MARGIN_MS = 30_000;

const bareClient = createFikaliakoClient(API_URL);

let current: PersistedTokens | null = null;
let refreshing: Promise<string | null> | null = null;
let sessionExpiredListener: (() => void) | null = null;

export function onSessionExpired(listener: () => void): void {
  sessionExpiredListener = listener;
}

export function hydrateTokens(tokens: PersistedTokens | null): void {
  current = tokens;
}

export function dropTokens(): void {
  current = null;
}

export function currentRefreshToken(): string | null {
  return current?.refreshToken ?? null;
}

export async function adoptTokens(tokens: AuthTokens): Promise<void> {
  current = {
    accessToken: tokens.access_token,
    accessTokenExpiresAt: Date.now() + tokens.expires_in * 1000,
    refreshToken: tokens.refresh_token,
  };
  await saveTokens(current);
}

export async function getValidAccessToken(): Promise<string | null> {
  if (!current) return null;
  if (Date.now() < current.accessTokenExpiresAt - REFRESH_MARGIN_MS) {
    return current.accessToken;
  }
  refreshing ??= rotate().finally(() => {
    refreshing = null;
  });
  return refreshing;
}

async function rotate(): Promise<string | null> {
  if (!current) return null;
  try {
    const { data, response } = await bareClient.POST('/v1/auth/refresh', {
      body: { refresh_token: current.refreshToken },
    });
    if (data) {
      await adoptTokens(data);
      return data.access_token;
    }
    if (response.status === 400 || response.status === 401) {
      current = null;
      sessionExpiredListener?.();
      return null;
    }
    return current.accessToken;
  } catch {
    return current?.accessToken ?? null;
  }
}
