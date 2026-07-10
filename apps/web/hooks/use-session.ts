'use client';

import { useAuthStore } from '@/lib/auth/auth-store';

/**
 * Hydration-safe session snapshot: `isAuthenticated` stays false until the
 * persisted store has rehydrated, so SSR markup and the first client render
 * agree (both logged out) and the UI upgrades after mount.
 */
export function useSession() {
  const user = useAuthStore((state) => state.user);
  const refreshToken = useAuthStore((state) => state.refreshToken);
  const hydrated = useAuthStore((state) => state.hydrated);

  return {
    user: hydrated ? user : null,
    isAuthenticated: hydrated && user !== null && refreshToken !== null,
    hydrated,
  };
}
