'use client';

import { useMemo } from 'react';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type { EstablishmentPage, EstablishmentSummary } from '@fikaliako/api-client';

import { useSession } from '@/hooks/use-session';

import { api } from '@/lib/api/client';
import { unwrap } from '@/lib/api/unwrap';

const FAVORITES_KEY = ['favorites'] as const;

const PAGE_SIZE = 100;

/**
 * The user's favorites (project book ch. 4.6). Anonymous visitors never fetch:
 * the endpoint is authenticated, and the hearts send them to login instead.
 */
export function useFavorites() {
  const { isAuthenticated } = useSession();

  return useQuery({
    queryKey: FAVORITES_KEY,
    enabled: isAuthenticated,
    queryFn: async () =>
      unwrap(await api.GET('/v1/users/me/favorites', { params: { query: { limit: PAGE_SIZE } } })),
  });
}

export function useFavoriteIds(): ReadonlySet<string> {
  const { data } = useFavorites();
  return useMemo(() => new Set((data?.items ?? []).map((item) => item.id)), [data]);
}

/**
 * Optimistic add/remove: the heart flips immediately and the previous page is
 * restored if the (idempotent) PUT/DELETE does not land.
 */
export function useToggleFavorite() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      establishment,
      favorite,
    }: {
      establishment: EstablishmentSummary;
      favorite: boolean;
    }) => {
      const params = { params: { path: { establishmentId: establishment.id } } };
      return unwrap(
        favorite
          ? await api.PUT('/v1/users/me/favorites/{establishmentId}', params)
          : await api.DELETE('/v1/users/me/favorites/{establishmentId}', params)
      );
    },

    onMutate: async ({ establishment, favorite }) => {
      await queryClient.cancelQueries({ queryKey: FAVORITES_KEY });
      const previous = queryClient.getQueryData<EstablishmentPage>(FAVORITES_KEY);

      queryClient.setQueryData<EstablishmentPage>(FAVORITES_KEY, (page) => {
        if (!page) return page;
        const without = page.items.filter((item) => item.id !== establishment.id);
        return { ...page, items: favorite ? [establishment, ...without] : without };
      });

      return { previous };
    },

    onError: (_error, _variables, context) => {
      if (context?.previous) queryClient.setQueryData(FAVORITES_KEY, context.previous);
    },

    onSettled: () => queryClient.invalidateQueries({ queryKey: FAVORITES_KEY }),
  });
}
