import { z } from 'zod';

import { normalizePhone } from './phone';

const phoneField = z
  .string()
  .trim()
  .min(1, 'Phone number is required')
  .refine((value) => normalizePhone(value) !== null, 'Enter a valid phone number');

const passwordField = z
  .string()
  .min(8, 'Use at least 8 characters')
  .max(128, 'Use at most 128 characters');

const codeField = z.string().regex(/^[0-9]{6}$/, 'Enter the 6-digit code');

export const signInSchema = z.object({
  phone: phoneField,
  password: z.string().min(1, 'Password is required'),
});

export const signUpSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(2, 'Use at least 2 characters')
    .max(60, 'Use at most 60 characters'),
  phone: phoneField,
  password: passwordField,
  accountType: z.enum(['consumer', 'business']),
});

export const verifyPhoneSchema = z.object({
  code: codeField,
});

export const forgotPasswordSchema = z.object({
  phone: phoneField,
});

export const resetPasswordSchema = z.object({
  code: codeField,
  newPassword: passwordField,
});

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Current password is required'),
  newPassword: passwordField,
});

export type SignInValues = z.infer<typeof signInSchema>;
export type SignUpValues = z.infer<typeof signUpSchema>;
export type VerifyPhoneValues = z.infer<typeof verifyPhoneSchema>;
export type ForgotPasswordValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordValues = z.infer<typeof resetPasswordSchema>;
export type ChangePasswordValues = z.infer<typeof changePasswordSchema>;
