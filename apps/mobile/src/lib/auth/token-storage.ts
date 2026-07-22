import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

import type { UserProfile } from '@fikaliako/api-client';

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

async function getItem(key: string): Promise<string | null> {
  if (Platform.OS === 'web') {
    return typeof localStorage === 'undefined' ? null : localStorage.getItem(key);
  }
  return SecureStore.getItemAsync(key);
}

async function setItem(key: string, value: string): Promise<void> {
  if (Platform.OS === 'web') {
    if (typeof localStorage !== 'undefined') localStorage.setItem(key, value);
    return;
  }
  await SecureStore.setItemAsync(key, value);
}

async function deleteItem(key: string): Promise<void> {
  if (Platform.OS === 'web') {
    if (typeof localStorage !== 'undefined') localStorage.removeItem(key);
    return;
  }
  await SecureStore.deleteItemAsync(key);
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
    getItem(TOKENS_KEY).then((raw) => parse<PersistedTokens>(raw)),
    getItem(USER_KEY).then((raw) => parse<UserProfile>(raw)),
  ]);
  if (!tokens || !user) return null;
  return { tokens, user };
}

export async function saveTokens(tokens: PersistedTokens): Promise<void> {
  await setItem(TOKENS_KEY, JSON.stringify(tokens));
}

export async function saveUser(user: UserProfile): Promise<void> {
  await setItem(USER_KEY, JSON.stringify(user));
}

export async function clearStoredSession(): Promise<void> {
  await Promise.all([deleteItem(TOKENS_KEY), deleteItem(USER_KEY)]);
}
