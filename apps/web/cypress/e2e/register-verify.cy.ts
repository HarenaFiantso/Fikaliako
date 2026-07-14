import { authSession, problem, unverifiedUser } from '../support/api-stubs';

describe('register and phone verification', () => {
  it('enforces field rules client-side', () => {
    cy.visit('/register');
    cy.get('input[name="displayName"]').type('N');
    cy.get('input[name="phone"]').type('12');
    cy.get('input[name="password"]').type('court');
    cy.contains('button', 'Créer mon compte').click();
    cy.contains('Au moins 2 caractères');
    cy.contains('Numéro invalide');
    cy.contains('Au moins 8 caractères');
  });

  it('registers a business account then verifies the phone with the SMS code', () => {
    cy.intercept('POST', '**/v1/auth/register', (req) => {
      expect(req.body.phone).to.equal('+261341234567');
      expect(req.body.display_name).to.equal('Naina');
      expect(req.body.account_type).to.equal('business');
      expect(req.body.locale).to.equal('fr');
      req.reply({ statusCode: 201, body: unverifiedUser });
    }).as('register');

    cy.visit('/register');
    cy.contains('[role="radio"]', 'Établissement').click();
    cy.get('input[name="displayName"]').type('Naina');
    cy.get('input[name="phone"]').type('034 12 345 67');
    cy.get('input[name="password"]').type('motdepasse1');
    cy.contains('button', 'Créer mon compte').click();

    cy.wait('@register');
    cy.location('pathname').should('equal', '/verify-phone');
    cy.contains('034 12 345 67');

    cy.intercept(
      'POST',
      '**/v1/auth/verify-phone',
      problem(400, 'Bad request', 'Invalid or expired code.')
    ).as('verifyBad');
    cy.get('input[autocomplete="one-time-code"]').type('000000');
    cy.wait('@verifyBad');
    cy.contains('[role="alert"]', 'Code incorrect ou expiré');

    cy.intercept('POST', '**/v1/auth/verify-phone', (req) => {
      expect(req.body.phone).to.equal('+261341234567');
      expect(req.body.code).to.equal('123456');
      req.reply({ statusCode: 200, body: authSession });
    }).as('verifyOk');
    cy.get('input[autocomplete="one-time-code"]').type('123456');
    cy.wait('@verifyOk');
    cy.location('pathname').should('equal', '/');
    cy.contains('button', 'Naina');
  });

  it('asks for the phone on a direct visit and applies the resend cooldown', () => {
    cy.clock(Date.now(), ['setInterval']);
    cy.intercept('POST', '**/v1/auth/resend-otp', { statusCode: 202, body: {} }).as('resend');

    cy.visit('/verify-phone');
    cy.contains('Saisissez le numéro à vérifier');
    // The card animates from opacity 0 after hydration — waiting for
    // visibility guarantees the countdown interval is registered on the
    // stubbed clock before ticking.
    cy.contains('button', 'Renvoyer dans').should('be.visible').and('be.disabled');

    cy.tick(30_000);
    cy.get('input[name="phone"]').type('034 12 345 67');
    cy.contains('button', 'Renvoyer le code').click();
    cy.wait('@resend').its('request.body.phone').should('equal', '+261341234567');
    cy.contains('button', 'Renvoyer dans').should('be.disabled');
  });
});
