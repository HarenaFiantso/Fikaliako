import { z } from 'zod';

import { normalizePhone } from '@/lib/validation/normalize-phone';

export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 128;
export const DISPLAY_NAME_MIN_LENGTH = 2;
export const DISPLAY_NAME_MAX_LENGTH = 60;

const OTP_PATTERN = /^[0-9]{6}$/;

/** Translator scoped to `auth.errors`, as returned by `useTranslations('auth.errors')`. */
type ErrorsTranslator = (key: string, values?: Record<string, string | number>) => string;

function phoneField(t: ErrorsTranslator) {
  return z
    .string()
    .min(1, t('phone-required'))
    .refine((raw) => normalizePhone(raw) !== null, t('phone-invalid'))
    .transform((raw) => normalizePhone(raw) as string);
}

function newPasswordField(t: ErrorsTranslator) {
  return z
    .string()
    .min(PASSWORD_MIN_LENGTH, t('password-too-short', { min: PASSWORD_MIN_LENGTH }))
    .max(PASSWORD_MAX_LENGTH);
}

function otpField(t: ErrorsTranslator) {
  return z.string().regex(OTP_PATTERN, t('code-invalid'));
}

export function createLoginSchema(t: ErrorsTranslator) {
  return z.object({
    phone: phoneField(t),
    password: z.string().min(1, t('password-required')),
  });
}

export function createRegisterSchema(t: ErrorsTranslator) {
  return z.object({
    displayName: z
      .string()
      .trim()
      .min(DISPLAY_NAME_MIN_LENGTH, t('display-name-too-short', { min: DISPLAY_NAME_MIN_LENGTH }))
      .max(DISPLAY_NAME_MAX_LENGTH),
    phone: phoneField(t),
    password: newPasswordField(t),
    accountType: z.enum(['consumer', 'business']),
  });
}

export function createVerifyPhoneSchema(t: ErrorsTranslator) {
  return z.object({
    phone: phoneField(t),
    code: otpField(t),
  });
}

export function createForgotPasswordSchema(t: ErrorsTranslator) {
  return z.object({
    phone: phoneField(t),
  });
}

export function createResetPasswordSchema(t: ErrorsTranslator) {
  return z.object({
    phone: phoneField(t),
    code: otpField(t),
    newPassword: newPasswordField(t),
  });
}

export type LoginValues = z.infer<ReturnType<typeof createLoginSchema>>;
export type RegisterValues = z.infer<ReturnType<typeof createRegisterSchema>>;
export type VerifyPhoneValues = z.infer<ReturnType<typeof createVerifyPhoneSchema>>;
export type ForgotPasswordValues = z.infer<ReturnType<typeof createForgotPasswordSchema>>;
export type ResetPasswordValues = z.infer<ReturnType<typeof createResetPasswordSchema>>;
