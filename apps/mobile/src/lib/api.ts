import { createFikaliakoClient } from '@fikaliako/api-client';

/**
 * Shared Fikaliako API client for the mobile app.
 *
 * Point EXPO_PUBLIC_API_URL at the API when testing on a device or emulator —
 * `localhost` only resolves on simulators running on the same machine
 * (Android emulators reach the host via http://10.0.2.2:8080).
 */
export const api = createFikaliakoClient(
  process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080'
);
