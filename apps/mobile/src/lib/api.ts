import { createAuthenticatedFikaliakoClient } from '@fikaliako/api-client';

import { getValidAccessToken } from '@/lib/auth/token-manager';
import { API_URL } from '@/lib/config';

/**
 * Shared Fikaliako API client. Attaches a bearer token whenever a session is
 * live, transparently refreshing it before expiry.
 *
 * Point EXPO_PUBLIC_API_URL at the API when testing on a device or emulator —
 * `localhost` only resolves on simulators running on the same machine
 * (Android emulators reach the host via http://10.0.2.2:8080).
 */
export const api = createAuthenticatedFikaliakoClient(API_URL, getValidAccessToken);
