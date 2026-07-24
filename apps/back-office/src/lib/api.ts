import { createAuthenticatedFikaliakoClient } from '@fikaliako/api-client';

import { getValidAccessToken } from '../auth/token-manager';
import { API_BASE_URL } from './base-url';

/**
 * App-wide typed Fikaliako client. The bearer middleware asks the token
 * manager before every request, so calls are authenticated whenever a
 * back-office session exists.
 */
export const api = createAuthenticatedFikaliakoClient(API_BASE_URL, getValidAccessToken);
