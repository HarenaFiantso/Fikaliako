import { create } from 'zustand';

import type { EstablishmentDetail } from '@fikaliako/api-client';

import { api } from '@/lib/api';

export type DetailStatus = 'loading' | 'ready' | 'error';

export interface DetailEntry {
  status: DetailStatus;
  data?: EstablishmentDetail;
}

interface EstablishmentDetailState {
  entries: Record<string, DetailEntry>;
  load: (idOrSlug: string) => Promise<void>;
  ensureLoaded: (idOrSlug: string) => void;
}

export const useEstablishmentDetail = create<EstablishmentDetailState>((set, get) => {
  const patch = (idOrSlug: string, entry: DetailEntry) =>
    set((state) => ({ entries: { ...state.entries, [idOrSlug]: entry } }));

  return {
    entries: {},

    async load(idOrSlug) {
      const previous = get().entries[idOrSlug]?.data;
      patch(idOrSlug, { status: 'loading', data: previous });
      try {
        const { data } = await api.GET('/v1/establishments/{idOrSlug}', {
          params: { path: { idOrSlug } },
        });
        patch(idOrSlug, data ? { status: 'ready', data } : { status: 'error', data: previous });
      } catch {
        patch(idOrSlug, { status: 'error', data: previous });
      }
    },

    ensureLoaded(idOrSlug) {
      if (!get().entries[idOrSlug]) void get().load(idOrSlug);
    },
  };
});
