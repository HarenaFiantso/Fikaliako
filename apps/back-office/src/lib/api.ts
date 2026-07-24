import { createFikaliakoClient } from '@fikaliako/api-client';

export const API_BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

/** Shared typed client for the Fikaliako API (no auth yet — see the auth module). */
export const api = createFikaliakoClient(API_BASE_URL);
