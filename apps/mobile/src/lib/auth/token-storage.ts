import type { UserProfile } from '@fikaliako/api-client';

import { deleteStoredItem, getStoredItem, setStoredItem } from '@/lib/storage';

const TOKENS_KEY = 'fikaliako.auth.tokens';
const USER_KEY = 'fikaliako.auth.user';

export interface PersistedTokens {
  accessToken: string;
  accessTokenExpiresAt: number;
  refreshToken: string;
}

export interface StoredSession {
  tokens: PersistedTokens;
  user: UserProfile;
}

function parse<T>(raw: string | null): T | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

export async function loadStoredSession(): Promise<StoredSession | null> {
  const [tokens, user] = await Promise.all([
    getStoredItem(TOKENS_KEY).then((raw) => parse<PersistedTokens>(raw)),
    getStoredItem(USER_KEY).then((raw) => parse<UserProfile>(raw)),
  ]);
  if (!tokens || !user) return null;
  return { tokens, user };
}

export async function saveTokens(tokens: PersistedTokens): Promise<void> {
  await setStoredItem(TOKENS_KEY, JSON.stringify(tokens));
}

export async function saveUser(user: UserProfile): Promise<void> {
  await setStoredItem(USER_KEY, JSON.stringify(user));
}

export async function clearStoredSession(): Promise<void> {
  await Promise.all([deleteStoredItem(TOKENS_KEY), deleteStoredItem(USER_KEY)]);
}
