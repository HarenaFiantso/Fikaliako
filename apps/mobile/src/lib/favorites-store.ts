import { create } from 'zustand';

import type { EstablishmentSummary } from '@fikaliako/api-client';

import { api } from '@/lib/api';
import { useSession } from '@/lib/auth/session-store';

export type FavoritesStatus = 'idle' | 'loading' | 'ready' | 'error';

interface FavoritesState {
  status: FavoritesStatus;
  items: EstablishmentSummary[];
  ids: ReadonlySet<string>;
  nextCursor: string | null;
  loadingMore: boolean;
  load: () => Promise<void>;
  ensureLoaded: () => void;
  loadMore: () => Promise<void>;
  toggle: (establishment: EstablishmentSummary) => Promise<void>;
  reset: () => void;
}

const PAGE_SIZE = 30;

const initialState = {
  status: 'idle' as FavoritesStatus,
  items: [],
  ids: new Set<string>(),
  nextCursor: null,
  loadingMore: false,
};

function idSet(items: EstablishmentSummary[]): Set<string> {
  return new Set(items.map((item) => item.id));
}

export const useFavorites = create<FavoritesState>((set, get) => ({
  ...initialState,

  async load() {
    set({ status: 'loading' });
    try {
      const { data } = await api.GET('/v1/users/me/favorites', {
        params: { query: { limit: PAGE_SIZE } },
      });
      if (!data) {
        set({ status: 'error' });
        return;
      }
      set({
        status: 'ready',
        items: data.items,
        ids: idSet(data.items),
        nextCursor: data.next_cursor ?? null,
      });
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
      const { data } = await api.GET('/v1/users/me/favorites', {
        params: { query: { limit: PAGE_SIZE, cursor: nextCursor } },
      });
      if (data) {
        set((state) => {
          // An optimistic add may already hold an item from this page.
          const fresh = data.items.filter((item) => !state.ids.has(item.id));
          const items = [...state.items, ...fresh];
          return { items, ids: idSet(items), nextCursor: data.next_cursor ?? null };
        });
      }
    } catch {
      // Keep the cursor so the next scroll retries the same page.
    } finally {
      set({ loadingMore: false });
    }
  },

  /**
   * Optimistic add/remove: the list and the heart update immediately, and the
   * previous state is restored if the (idempotent) PUT/DELETE does not land.
   */
  async toggle(establishment) {
    const wasFavorite = get().ids.has(establishment.id);
    const rollback = { items: get().items, ids: get().ids };

    set((state) => {
      const items = wasFavorite
        ? state.items.filter((item) => item.id !== establishment.id)
        : [establishment, ...state.items];
      return { items, ids: idSet(items) };
    });

    const path = { params: { path: { establishmentId: establishment.id } } };
    try {
      const { response } = wasFavorite
        ? await api.DELETE('/v1/users/me/favorites/{establishmentId}', path)
        : await api.PUT('/v1/users/me/favorites/{establishmentId}', path);
      if (!response.ok) set(rollback);
    } catch {
      set(rollback);
    }
  },

  reset() {
    set({ ...initialState, ids: new Set() });
  },
}));

useSession.subscribe((state, previous) => {
  if (previous.status === 'signedIn' && state.status !== 'signedIn') {
    useFavorites.getState().reset();
  }
});
