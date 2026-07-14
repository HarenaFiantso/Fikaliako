import { authSession, problem, testUser } from '../support/api-stubs';

function submitLogin(phone: string, password: string) {
  cy.get('input[name="phone"]').clear().type(phone);
  cy.get('input[name="password"]').clear().type(password);
  cy.contains('button', 'Se connecter').click();
}

describe('login', () => {
  beforeEach(() => {
    cy.visit('/login');
  });

  it('rejects an invalid phone client-side, without any request', () => {
    cy.intercept('POST', '**/v1/auth/login', cy.spy().as('loginCall'));
    submitLogin('12', 'whatever');
    cy.contains('Numéro invalide');
    cy.get('@loginCall').should('not.have.been.called');
  });

  it('toggles password visibility', () => {
    cy.get('input[name="password"]').type('secret');
    cy.get('input[name="password"]').should('have.attr', 'type', 'password');
    cy.get('button[aria-label="Afficher le mot de passe"]').click();
    cy.get('input[name="password"]').should('have.attr', 'type', 'text');
    cy.get('button[aria-label="Masquer le mot de passe"]').click();
    cy.get('input[name="password"]').should('have.attr', 'type', 'password');
  });

  it('shows a credentials error on 401', () => {
    cy.intercept(
      'POST',
      '**/v1/auth/login',
      problem(401, 'Unauthorized', 'Invalid phone number or password.')
    ).as('login');
    submitLogin('034 12 345 67', 'wrong-password');
    cy.wait('@login');
    cy.contains('[role="alert"]', 'Numéro ou mot de passe incorrect');
  });

  it('routes unverified accounts to phone verification', () => {
    cy.intercept(
      'POST',
      '**/v1/auth/login',
      problem(
        403,
        'Forbidden',
        'Phone number not verified. Verify it with the code sent by SMS (/v1/auth/verify-phone).'
      )
    ).as('login');
    submitLogin('034 12 345 67', 'motdepasse1');
    cy.wait('@login');
    cy.location('pathname').should('equal', '/verify-phone');
    cy.contains('034 12 345 67');
  });

  it('normalizes the phone, opens a session, and never persists the access token', () => {
    cy.intercept('POST', '**/v1/auth/login', (req) => {
      expect(req.body.phone).to.equal('+261341234567');
      req.reply({ statusCode: 200, body: authSession });
    }).as('login');

    submitLogin('034 12 345 67', 'motdepasse1');
    cy.wait('@login');
    cy.location('pathname').should('equal', '/');
    cy.contains('button', testUser.display_name);

    cy.window().then((win) => {
      const raw = win.localStorage.getItem('fikaliako-auth');
      expect(raw).to.be.a('string');
      const stored = JSON.parse(raw as string);
      expect(stored.state.refreshToken).to.equal('e2e-refresh-token');
      expect(raw).to.not.contain('e2e-access-token');
    });
  });
});
