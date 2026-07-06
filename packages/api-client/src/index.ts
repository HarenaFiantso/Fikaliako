import createClient, { type ClientOptions } from 'openapi-fetch';

import type { components, operations, paths } from './schema.js';

/**
 * Typed client for the Fikaliako API, generated from the hand-managed
 * OpenAPI contract (apps/api/src/main/resources/static/v1/openapi.yaml).
 *
 * Platform-neutral: relies only on the global `fetch`, so it works in
 * Next.js (server and client components) and React Native alike.
 *
 * Regenerate the types after every contract change:
 *   pnpm --filter @fikaliako/api-client generate
 */
export function createFikaliakoClient(baseUrl: string, options?: Omit<ClientOptions, 'baseUrl'>) {
  return createClient<paths>({ baseUrl, ...options });
}

export type FikaliakoClient = ReturnType<typeof createFikaliakoClient>;

/** Raw generated types, for advanced use. */
export type { components, operations, paths };

/** Schema shortcuts — one alias per component schema in the contract. */
export type Schemas = components['schemas'];
export type PingResponse = Schemas['PingResponse'];
