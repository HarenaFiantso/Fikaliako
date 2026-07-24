'use client';

import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import type { ContributionItem, ContributionStatus } from '@fikaliako/api-client';

import { api } from '@/lib/api/client';
import { unwrap } from '@/lib/api/unwrap';

const QUEUE_KEY = ['moderation', 'contributions'] as const;
const PAGE_SIZE = 20;

/** The moderation queue for one status, oldest first, cursor-paginated. */
export function useModerationQueue(status: ContributionStatus) {
  return useInfiniteQuery({
    queryKey: [...QUEUE_KEY, status],
    queryFn: async ({ pageParam }) =>
      unwrap(
        await api.GET('/v1/moderation/contributions', {
          params: {
            query: { status, limit: PAGE_SIZE, ...(pageParam ? { cursor: pageParam } : {}) },
          },
        })
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) => lastPage.next_cursor,
  });
}

export function useContribution(contributionId: string) {
  return useQuery({
    queryKey: [...QUEUE_KEY, 'detail', contributionId],
    queryFn: async () =>
      unwrap(
        await api.GET('/v1/moderation/contributions/{contributionId}', {
          params: { path: { contributionId } },
        })
      ),
  });
}

/** Refresh every queue view and pin the resolved item into its detail cache. */
function useResolutionCacheUpdate() {
  const queryClient = useQueryClient();
  return (item: ContributionItem) => {
    queryClient.setQueryData([...QUEUE_KEY, 'detail', item.id], item);
    void queryClient.invalidateQueries({ queryKey: QUEUE_KEY });
  };
}

export function useApproveContribution() {
  const onResolved = useResolutionCacheUpdate();
  return useMutation({
    mutationFn: async (contributionId: string) =>
      unwrap(
        await api.POST('/v1/moderation/contributions/{contributionId}/approve', {
          params: { path: { contributionId } },
        })
      ),
    onSuccess: onResolved,
  });
}

export function useRejectContribution() {
  const onResolved = useResolutionCacheUpdate();
  return useMutation({
    mutationFn: async ({ contributionId, reason }: { contributionId: string; reason?: string }) => {
      const trimmed = reason?.trim();
      return unwrap(
        await api.POST('/v1/moderation/contributions/{contributionId}/reject', {
          params: { path: { contributionId } },
          body: trimmed ? { reason: trimmed } : {},
        })
      );
    },
    onSuccess: onResolved,
  });
}
