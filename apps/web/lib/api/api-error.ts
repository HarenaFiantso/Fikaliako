import type { Problem } from '@fikaliako/api-client';

export class ApiError extends Error {
  readonly status: number;
  readonly problem: Problem | null;

  constructor(status: number, problem: Problem | null) {
    super(problem?.detail ?? `HTTP ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.problem = problem;
  }
}

export type AuthErrorContext =
  'login' | 'register' | 'verify-phone' | 'resend-otp' | 'forgot-password' | 'reset-password';

type AuthErrorKey =
  | 'invalid-credentials'
  | 'phone-not-verified'
  | 'account-suspended'
  | 'phone-taken'
  | 'otp-invalid'
  | 'too-many-requests'
  | 'session-expired'
  | 'network'
  | 'generic';

/**
 * Maps a failed auth call to a message key under `auth.errors`. The API
 * emits generic problem+json (status + title + detail, no machine-readable
 * type), so the mapping keys off the HTTP status per endpoint; the one
 * ambiguous case — login 403, unverified vs suspended — is disambiguated
 * by the detail text.
 */
export function toAuthErrorKey(error: unknown, context: AuthErrorContext): AuthErrorKey {
  if (!(error instanceof ApiError)) return 'network';

  if (error.status === 429) return 'too-many-requests';
  if (error.status >= 500) return 'generic';

  switch (context) {
    case 'login':
      if (error.status === 401) return 'invalid-credentials';
      if (error.status === 403) {
        return /not verified/i.test(error.problem?.detail ?? '')
          ? 'phone-not-verified'
          : 'account-suspended';
      }
      return 'generic';
    case 'register':
      return error.status === 409 ? 'phone-taken' : 'generic';
    case 'verify-phone':
      if (error.status === 400) return 'otp-invalid';
      if (error.status === 403) return 'account-suspended';
      return 'generic';
    case 'reset-password':
      return error.status === 400 ? 'otp-invalid' : 'generic';
    case 'resend-otp':
    case 'forgot-password':
      return 'generic';
  }
}
