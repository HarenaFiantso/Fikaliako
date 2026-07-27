import { z } from 'zod';

import { ESTABLISHMENT_TYPES } from '@/lib/establishments';

/** Translator scoped to `propose.errors`. */
type ErrorsTranslator = (key: string, values?: Record<string, string | number>) => string;

const NAME_MIN_LENGTH = 2;
const NAME_MAX_LENGTH = 120;

/** Stays a string so the form's input and output types match; parsed on submit. */
function coordinate(t: ErrorsTranslator, bound: number, invalidKey: string) {
  return z
    .string()
    .trim()
    .min(1, t('position-required'))
    .refine((raw) => {
      const value = Number(raw);
      return raw !== '' && Number.isFinite(value) && Math.abs(value) <= bound;
    }, t(invalidKey));
}

export function createProposalSchema(t: ErrorsTranslator) {
  return z.object({
    name: z
      .string()
      .trim()
      .min(NAME_MIN_LENGTH, t('name-too-short', { min: NAME_MIN_LENGTH }))
      .max(NAME_MAX_LENGTH, t('too-long', { max: NAME_MAX_LENGTH })),
    type: z.enum(ESTABLISHMENT_TYPES),
    lat: coordinate(t, 90, 'lat-invalid'),
    lng: coordinate(t, 180, 'lng-invalid'),
    city: z
      .string()
      .trim()
      .min(1, t('city-required'))
      .max(120, t('too-long', { max: 120 })),
    district: z
      .string()
      .trim()
      .max(120, t('too-long', { max: 120 })),
    address: z
      .string()
      .trim()
      .max(255, t('too-long', { max: 255 })),
    phone: z
      .string()
      .trim()
      .max(30, t('too-long', { max: 30 })),
    avgPrice: z
      .string()
      .trim()
      .regex(/^[0-9]*$/, t('digits-only')),
    comment: z
      .string()
      .trim()
      .max(500, t('too-long', { max: 500 })),
  });
}

export type ProposalValues = z.infer<ReturnType<typeof createProposalSchema>>;
