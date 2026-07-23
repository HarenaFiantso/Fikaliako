import { create } from 'zustand';

/**
 * One-shot celebration overlay state. Rendered at the root so the moment
 * survives the auth → app stack switch that follows a successful sign-up.
 */
interface CelebrationState {
  message: string | null;
  celebrate: (message: string) => void;
  dismiss: () => void;
}

export const useCelebration = create<CelebrationState>((set) => ({
  message: null,
  celebrate: (message) => set({ message }),
  dismiss: () => set({ message: null }),
}));
