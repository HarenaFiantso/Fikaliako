/**
 * Contract-shaped fixtures and helpers for stubbing the Fikaliako API with
 * cy.intercept — specs run against the web app alone, no Spring stack needed.
 */

export const testUser = {
  id: '7f9d3a10-5c6e-4b2a-9f61-2f4a8f0c1a2b',
  phone: '+261341234567',
  display_name: 'Naina',
  role: 'user',
  phone_verified: true,
  locale: 'fr',
  created_at: '2026-07-01T08:00:00Z',
};

export const unverifiedUser = { ...testUser, phone_verified: false };

export const testTokens = {
  token_type: 'Bearer',
  access_token: 'e2e-access-token',
  expires_in: 900,
  refresh_token: 'e2e-refresh-token',
  refresh_expires_in: 2_592_000,
};

export const authSession = { user: testUser, tokens: testTokens };

export function problem(status: number, title: string, detail: string) {
  return {
    statusCode: status,
    headers: { 'content-type': 'application/problem+json' },
    body: { type: 'about:blank', title, status, detail },
  };
}

export const SEEDED_REFRESH_TOKEN = 'seeded-refresh-token';

/** Visits a page with a persisted session already in localStorage (access token absent by design). */
export function visitWithSession(path: string) {
  cy.visit(path, {
    onBeforeLoad(win) {
      win.localStorage.setItem(
        'fikaliako-auth',
        JSON.stringify({
          state: { user: testUser, refreshToken: SEEDED_REFRESH_TOKEN },
          version: 0,
        })
      );
    },
  });
}
