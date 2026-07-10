import { createAuthenticatedFikaliakoClient } from '@fikaliako/api-client';

import { API_BASE_URL } from '@/lib/api/base-url';
import { getValidAccessToken } from '@/lib/auth/token-manager';

/**
 * The app-wide API client. Public GETs go out untouched; once a session
 * exists every call carries a bearer token, refreshed transparently.
 */
export const api = createAuthenticatedFikaliakoClient(API_BASE_URL, getValidAccessToken);
