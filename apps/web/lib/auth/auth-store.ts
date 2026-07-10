import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

import type { AuthSession, AuthTokens, UserProfile } from '@fikaliako/api-client';

interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
  accessTokenExpiresAt: number | null;
  refreshToken: string | null;
  hydrated: boolean;
  setSession: (session: AuthSession) => void;
  setTokens: (tokens: AuthTokens) => void;
  setUser: (user: UserProfile) => void;
  setHydrated: () => void;
  clearSession: () => void;
}

function tokensToState(tokens: AuthTokens) {
  return {
    accessToken: tokens.access_token,
    accessTokenExpiresAt: Date.now() + tokens.expires_in * 1000,
    refreshToken: tokens.refresh_token,
  };
}

/**
 * Client-side session state. Only the user profile and the refresh token are
 * persisted; the short-lived access JWT stays in memory and is re-obtained
 * via /v1/auth/refresh after a page reload.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      accessTokenExpiresAt: null,
      refreshToken: null,
      hydrated: false,
      setSession: (session) => set({ user: session.user, ...tokensToState(session.tokens) }),
      setTokens: (tokens) => set(tokensToState(tokens)),
      setUser: (user) => set({ user }),
      setHydrated: () => set({ hydrated: true }),
      clearSession: () =>
        set({ user: null, accessToken: null, accessTokenExpiresAt: null, refreshToken: null }),
    }),
    {
      name: 'fikaliako-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ user: state.user, refreshToken: state.refreshToken }),
      // Rehydrated after mount (see app/providers.tsx) so SSR markup and the
      // first client render agree on a logged-out state.
      skipHydration: true,
      onRehydrateStorage: () => (state) => {
        state?.setHydrated();
      },
    }
  )
);
