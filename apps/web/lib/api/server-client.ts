import { createFikaliakoClient } from '@fikaliako/api-client';

import { API_BASE_URL } from '@/lib/api/base-url';

/**
 * Server-side client for public GETs rendered on the server. No bearer
 * middleware: it never sees a session, which keeps establishment pages
 * cacheable and Google-indexable (project book ch. 11.3).
 */
export const serverApi = createFikaliakoClient(API_BASE_URL);
