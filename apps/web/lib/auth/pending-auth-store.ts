import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';

interface PendingAuthState {
  verificationPhone: string | null;
  resetPhone: string | null;
  hydrated: boolean;
  setVerificationPhone: (phone: string | null) => void;
  setResetPhone: (phone: string | null) => void;
  setHydrated: () => void;
}

/**
 * Carries the phone number between auth screens (register → verify-phone,
 * forgot-password → reset-password) without putting it in the URL.
 * Session-scoped: survives a reload, not a new tab.
 */
export const usePendingAuthStore = create<PendingAuthState>()(
  persist(
    (set) => ({
      verificationPhone: null,
      resetPhone: null,
      hydrated: false,
      setVerificationPhone: (phone) => set({ verificationPhone: phone }),
      setResetPhone: (phone) => set({ resetPhone: phone }),
      setHydrated: () => set({ hydrated: true }),
    }),
    {
      name: 'fikaliako-pending-auth',
      storage: createJSONStorage(() => sessionStorage),
      partialize: (state) => ({
        verificationPhone: state.verificationPhone,
        resetPhone: state.resetPhone,
      }),
      // Rehydrated after mount (see app/providers.tsx), like the auth store.
      skipHydration: true,
      onRehydrateStorage: () => (state) => {
        state?.setHydrated();
      },
    }
  )
);
