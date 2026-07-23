import { create } from 'zustand';

import type { EstablishmentSummary } from '@fikaliako/api-client';

import { api } from '@/lib/api';

export type BrowseStatus = 'idle' | 'loading' | 'ready' | 'error';

interface BrowseState {
  status: BrowseStatus;
  items: EstablishmentSummary[];
  nextCursor: string | null;
  loadingMore: boolean;
  load: () => Promise<void>;
  ensureLoaded: () => void;
  loadMore: () => Promise<void>;
}

const PAGE_SIZE = 20;

export const useEstablishmentBrowse = create<BrowseState>((set, get) => ({
  status: 'idle',
  items: [],
  nextCursor: null,
  loadingMore: false,

  async load() {
    set({ status: 'loading' });
    try {
      const { data } = await api.GET('/v1/establishments', {
        params: { query: { limit: PAGE_SIZE } },
      });
      if (!data) {
        set({ status: 'error' });
        return;
      }
      set({ status: 'ready', items: data.items, nextCursor: data.next_cursor ?? null });
    } catch {
      set({ status: 'error' });
    }
  },

  ensureLoaded() {
    if (get().status === 'idle') void get().load();
  },

  async loadMore() {
    const { status, nextCursor, loadingMore } = get();
    if (status !== 'ready' || !nextCursor || loadingMore) return;
    set({ loadingMore: true });
    try {
      const { data } = await api.GET('/v1/establishments', {
        params: { query: { limit: PAGE_SIZE, cursor: nextCursor } },
      });
      if (data) {
        set((state) => ({
          items: [
            ...state.items,
            ...data.items.filter((item) => !state.items.some((known) => known.id === item.id)),
          ],
          nextCursor: data.next_cursor ?? null,
        }));
      }
    } catch {
      // Keep the cursor so the next scroll retries the same page.
    } finally {
      set({ loadingMore: false });
    }
  },
}));
