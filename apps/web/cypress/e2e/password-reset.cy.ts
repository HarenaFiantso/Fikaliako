import { problem } from '../support/api-stubs';

describe('password reset', () => {
  it('walks forgot-password → reset-password → login', () => {
    cy.intercept('POST', '**/v1/auth/forgot-password', { statusCode: 202, body: {} }).as('forgot');

    cy.visit('/forgot-password');
    cy.get('input[name="phone"]').type('034 12 345 67');
    cy.contains('button', 'Recevoir le code').click();
    cy.wait('@forgot').its('request.body.phone').should('equal', '+261341234567');

    cy.location('pathname').should('equal', '/reset-password');
    cy.get('input[name="phone"]').should('have.value', '034 12 345 67');

    cy.intercept('POST', '**/v1/auth/reset-password', (req) => {
      expect(req.body.phone).to.equal('+261341234567');
      expect(req.body.code).to.equal('123456');
      expect(req.body.new_password).to.equal('nouveaumdp22');
      req.reply({ statusCode: 204 });
    }).as('reset');
    cy.get('input[autocomplete="one-time-code"]').type('123456');
    cy.get('input[name="newPassword"]').type('nouveaumdp22');
    cy.contains('button', 'Réinitialiser').click();

    cy.wait('@reset');
    cy.location('pathname').should('equal', '/login');
    cy.contains('Mot de passe réinitialisé');
  });

  it('surfaces a rejected reset code', () => {
    cy.intercept(
      'POST',
      '**/v1/auth/reset-password',
      problem(400, 'Bad request', 'Invalid or expired code.')
    ).as('reset');

    cy.visit('/reset-password');
    cy.get('input[name="phone"]').type('034 12 345 67');
    cy.get('input[autocomplete="one-time-code"]').type('000000');
    cy.get('input[name="newPassword"]').type('nouveaumdp22');
    cy.contains('button', 'Réinitialiser').click();

    cy.wait('@reset');
    cy.contains('[role="alert"]', 'Code incorrect ou expiré');
    cy.location('pathname').should('equal', '/reset-password');
  });
});
