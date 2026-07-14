import { SEEDED_REFRESH_TOKEN, testTokens, testUser, visitWithSession } from '../support/api-stubs';

describe('persisted session', () => {
  it('restores the session from localStorage without any network call', () => {
    visitWithSession('/');
    cy.contains('button', testUser.display_name);
    cy.contains(`Tongasoa, ${testUser.display_name}`);
    cy.contains('a', 'Se connecter').should('not.exist');
  });

  it('redirects authenticated users away from auth pages', () => {
    visitWithSession('/login');
    cy.location('pathname').should('equal', '/');
  });

  it('rotates the refresh token before logout, then drops the session', () => {
    cy.intercept('POST', '**/v1/auth/refresh', (req) => {
      expect(req.body.refresh_token).to.equal(SEEDED_REFRESH_TOKEN);
      req.reply({ statusCode: 200, body: testTokens });
    }).as('refresh');
    cy.intercept('POST', '**/v1/auth/logout', { statusCode: 204 }).as('logout');

    visitWithSession('/');
    cy.contains('button', testUser.display_name).click();
    cy.contains('[role="menuitem"]', 'Se déconnecter').click();

    cy.wait('@refresh');
    cy.wait('@logout');
    cy.contains('a', 'Créer un compte');
    cy.window().then((win) => {
      const stored = JSON.parse(win.localStorage.getItem('fikaliako-auth') as string);
      expect(stored.state.user).to.equal(null);
      expect(stored.state.refreshToken).to.equal(null);
    });
  });
});
