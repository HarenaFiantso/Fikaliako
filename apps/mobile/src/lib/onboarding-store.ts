import { create } from 'zustand';

import { getStoredItem, setStoredItem } from '@/lib/storage';

const ONBOARDED_KEY = 'fikaliako.onboarding.completed';

interface OnboardingState {
  ready: boolean;
  completed: boolean;
  restore: () => Promise<void>;
  complete: () => Promise<void>;
}

export const useOnboarding = create<OnboardingState>((set) => ({
  ready: false,
  completed: false,

  async restore() {
    const stored = await getStoredItem(ONBOARDED_KEY);
    set({ ready: true, completed: stored === '1' });
  },

  async complete() {
    set({ completed: true });
    await setStoredItem(ONBOARDED_KEY, '1');
  },
}));
