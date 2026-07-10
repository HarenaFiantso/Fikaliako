import type { Problem } from '@fikaliako/api-client';

import { ApiError } from '@/lib/api/api-error';

interface FetchResult<T> {
  data?: T;
  error?: unknown;
  response: Response;
}

/**
 * Turns an openapi-fetch result into data-or-throw for TanStack Query.
 * Success is keyed off `response.ok` — empty 202/204 bodies come back with
 * `data: undefined` and must not be treated as failures.
 */
export function unwrap<T>({ data, error, response }: FetchResult<T>): T {
  if (response.ok) return data as T;
  const problem = error && typeof error === 'object' ? (error as Problem) : null;
  throw new ApiError(response.status, problem);
}
