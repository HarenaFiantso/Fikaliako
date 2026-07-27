import type { ContributionItem, UserRole } from '@fikaliako/api-client';

/** Roles allowed into the moderation area (project book ch. 7.3). */
const STAFF_ROLES: readonly UserRole[] = ['moderator', 'admin'];

export function isStaff(role: UserRole | undefined): boolean {
  return role !== undefined && STAFF_ROLES.includes(role);
}

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

export function proposalOf(contribution: ContributionItem): CreateProposalPayload {
  return contribution.payload as CreateProposalPayload;
}
