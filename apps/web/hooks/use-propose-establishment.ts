'use client';

import { useMutation } from '@tanstack/react-query';

import type { EstablishmentProposal } from '@fikaliako/api-client';

import { api } from '@/lib/api/client';
import { unwrap } from '@/lib/api/unwrap';

/**
 * Proposes a new establishment (project book ch. 4.8). Nothing is published
 * directly: the API answers 202 with a receipt and the proposal enters the
 * moderation queue as a `create` contribution.
 */
export function useProposeEstablishment() {
  return useMutation({
    mutationFn: async (body: EstablishmentProposal) =>
      unwrap(await api.POST('/v1/establishments', { body })),
  });
}
