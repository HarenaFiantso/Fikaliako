import type { ContributionItem } from '@fikaliako/api-client';

/** A contribution as react-admin sees it (`id` is already the record key). */
export type ContributionRecord = ContributionItem;

/**
 * Payload of a `create` contribution: the keys mirror EstablishmentProposal
 * with the position flattened to lat/lng (see the API contract). The payload
 * is schemaless on the wire, hence every field optional.
 */
export type CreateProposalPayload = {
  name?: string;
  type?: string;
  lat?: number;
  lng?: number;
  address?: string;
  district?: string;
  city?: string;
  phone?: string;
  avg_price_ar?: number;
  comment?: string;
};

export function proposalOf(record: ContributionRecord): CreateProposalPayload {
  return record.payload as CreateProposalPayload;
}

export function formatAriary(amount: number): string {
  return `${new Intl.NumberFormat('fr-FR').format(amount)} Ar`;
}
