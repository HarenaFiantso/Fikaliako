import type { Problem } from '@fikaliako/api-client';

/** Human-readable message out of an RFC 9457 problem response. */
export function problemMessage(problem: Problem | undefined, fallback: string): string {
  return problem?.detail ?? problem?.title ?? fallback;
}
