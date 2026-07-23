import { z } from 'zod';

import type { EstablishmentProposal, GeoPoint } from '@fikaliako/api-client';

import { api } from '@/lib/api';
import { networkError, problemMessage } from '@/lib/auth/errors';

export const ESTABLISHMENT_TYPES = [
  'restaurant',
  'gargotte',
  'cafe',
  'snack',
  'food_truck',
  'street_vendor',
  'pastry_shop',
  'bar_restaurant',
  'hotel_restaurant',
] as const;

export const proposalSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, 'Use at least 2 characters')
    .max(120, 'Use at most 120 characters'),
  type: z.enum(ESTABLISHMENT_TYPES),
  city: z.string().trim().min(1, 'City is required').max(120, 'Use at most 120 characters'),
  district: z.string().trim().max(120, 'Use at most 120 characters'),
  address: z.string().trim().max(255, 'Use at most 255 characters'),
  phone: z.string().trim().max(30, 'Use at most 30 characters'),
  avgPrice: z
    .string()
    .trim()
    .regex(/^[0-9]*$/, 'Digits only'),
  comment: z.string().trim().max(500, 'Use at most 500 characters'),
});

export type ProposalValues = z.infer<typeof proposalSchema>;

export async function submitProposal(values: ProposalValues, position: GeoPoint): Promise<void> {
  const body: EstablishmentProposal = {
    name: values.name.trim(),
    type: values.type,
    position,
    city: values.city.trim(),
    ...(values.district.trim() ? { district: values.district.trim() } : {}),
    ...(values.address.trim() ? { address: values.address.trim() } : {}),
    ...(values.phone.trim() ? { phone: values.phone.trim() } : {}),
    ...(values.avgPrice.trim() ? { avg_price_ar: Number(values.avgPrice) } : {}),
    ...(values.comment.trim() ? { comment: values.comment.trim() } : {}),
  };

  let result;
  try {
    result = await api.POST('/v1/establishments', { body });
  } catch {
    throw networkError();
  }
  const { error, response } = result;
  if (response.ok) return;
  throw new Error(problemMessage(error, 'Could not send the proposal'));
}
