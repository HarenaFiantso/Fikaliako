import { create } from 'zustand';

import type { ReviewInput, ReviewItem } from '@fikaliako/api-client';

import { api } from '@/lib/api';
import { networkError, problemMessage } from '@/lib/auth/errors';

export interface ReviewsEntry {
  status: 'loading' | 'ready' | 'error';
  items: ReviewItem[];
  nextCursor: string | null;
  loadingMore: boolean;
}

interface ReviewsState {
  entries: Record<string, ReviewsEntry>;
  load: (establishmentId: string) => Promise<void>;
  ensureLoaded: (establishmentId: string) => void;
  loadMore: (establishmentId: string) => Promise<void>;
  submit: (establishmentId: string, input: ReviewInput) => Promise<void>;
}

const PAGE_SIZE = 10;

const emptyEntry: ReviewsEntry = {
  status: 'loading',
  items: [],
  nextCursor: null,
  loadingMore: false,
};

export const useReviews = create<ReviewsState>((set, get) => {
  const patch = (establishmentId: string, partial: Partial<ReviewsEntry>) =>
    set((state) => ({
      entries: {
        ...state.entries,
        [establishmentId]: { ...(state.entries[establishmentId] ?? emptyEntry), ...partial },
      },
    }));

  return {
    entries: {},

    async load(establishmentId) {
      patch(establishmentId, { status: 'loading' });
      try {
        const { data } = await api.GET('/v1/establishments/{establishmentId}/reviews', {
          params: { path: { establishmentId }, query: { limit: PAGE_SIZE } },
        });
        if (!data) {
          patch(establishmentId, { status: 'error' });
          return;
        }
        patch(establishmentId, {
          status: 'ready',
          items: data.items,
          nextCursor: data.next_cursor ?? null,
        });
      } catch {
        patch(establishmentId, { status: 'error' });
      }
    },

    ensureLoaded(establishmentId) {
      if (!get().entries[establishmentId]) void get().load(establishmentId);
    },

    async loadMore(establishmentId) {
      const entry = get().entries[establishmentId];
      if (!entry || entry.status !== 'ready' || !entry.nextCursor || entry.loadingMore) return;
      patch(establishmentId, { loadingMore: true });
      try {
        const { data } = await api.GET('/v1/establishments/{establishmentId}/reviews', {
          params: {
            path: { establishmentId },
            query: { limit: PAGE_SIZE, cursor: entry.nextCursor },
          },
        });
        if (data) {
          const current = get().entries[establishmentId] ?? emptyEntry;
          const fresh = data.items.filter(
            (item) => !current.items.some((known) => known.id === item.id)
          );
          patch(establishmentId, {
            items: [...current.items, ...fresh],
            nextCursor: data.next_cursor ?? null,
          });
        }
      } catch {
        // Keep the cursor so the next attempt retries the same page.
      } finally {
        patch(establishmentId, { loadingMore: false });
      }
    },

    async submit(establishmentId, input) {
      let result;
      try {
        result = await api.POST('/v1/establishments/{establishmentId}/reviews', {
          params: { path: { establishmentId } },
          body: input,
        });
      } catch {
        throw networkError();
      }
      const { data, error, response } = result;
      if (data) {
        const current = get().entries[establishmentId];
        if (current) patch(establishmentId, { items: [data, ...current.items] });
        return;
      }
      if (response.status === 409) {
        throw new Error('You have already reviewed this place.');
      }
      throw new Error(problemMessage(error, 'Could not send the review'));
    },
  };
});
