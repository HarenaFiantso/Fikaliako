import type { Problem } from '@fikaliako/api-client';

export type AuthErrorKind =
  | 'invalidCredentials'
  | 'phoneUnverified'
  | 'accountExists'
  | 'invalidCode'
  | 'rateLimited'
  | 'network'
  | 'generic';

export class AuthError extends Error {
  readonly kind: AuthErrorKind;

  constructor(message: string, kind: AuthErrorKind = 'generic') {
    super(message);
    this.name = 'AuthError';
    this.kind = kind;
  }
}

export function problemMessage(error: unknown, fallback: string): string {
  const problem = error as Partial<Problem> | null | undefined;
  return problem?.detail ?? problem?.title ?? fallback;
}

export function networkError(): AuthError {
  return new AuthError('Could not reach the server. Check your connection.', 'network');
}
