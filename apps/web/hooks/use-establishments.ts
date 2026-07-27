'use client';

import { useInfiniteQuery } from '@tanstack/react-query';

import type { ReviewPage } from '@fikaliako/api-client';

import { api } from '@/lib/api/client';
import { unwrap } from '@/lib/api/unwrap';

const ESTABLISHMENTS_KEY = ['establishments'] as const;

const BROWSE_PAGE_SIZE = 20;
const REVIEWS_PAGE_SIZE = 10;

export function useEstablishments() {
  return useInfiniteQuery({
    queryKey: [...ESTABLISHMENTS_KEY, 'browse'],
    queryFn: async ({ pageParam }) =>
      unwrap(
        await api.GET('/v1/establishments', {
          params: {
            query: { limit: BROWSE_PAGE_SIZE, ...(pageParam ? { cursor: pageParam } : {}) },
          },
        })
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.next_cursor,
  });
}

// `initialPage` is the page the server already rendered, so the detail page
// does not refetch its reviews on mount.
export function useEstablishmentReviews(establishmentId: string, initialPage?: ReviewPage) {
  return useInfiniteQuery({
    queryKey: [...ESTABLISHMENTS_KEY, establishmentId, 'reviews'],
    queryFn: async ({ pageParam }) =>
      unwrap(
        await api.GET('/v1/establishments/{establishmentId}/reviews', {
          params: {
            path: { establishmentId },
            query: { limit: REVIEWS_PAGE_SIZE, ...(pageParam ? { cursor: pageParam } : {}) },
          },
        })
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.next_cursor,
    ...(initialPage && {
      initialData: { pages: [initialPage], pageParams: [undefined] },
      staleTime: 60_000,
    }),
  });
}
