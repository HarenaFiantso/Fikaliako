describe('landing page', () => {
  it('renders the French hero with auth CTAs', () => {
    cy.visit('/');
    cy.contains('h1', 'Trouvez où manger, ici et maintenant');
    cy.contains('a', 'Se connecter');
    cy.contains('a', 'Créer un compte').click();
    cy.location('pathname').should('equal', '/register');
  });

  it('switches locale to English and back to French', () => {
    cy.visit('/');
    cy.get('button[aria-label="Langue"]').click();
    cy.contains('[role="menuitemradio"]', 'English').click();
    cy.contains('a', 'Sign in');
    cy.contains('h1', 'Find where to eat, here and now');

    cy.get('button[aria-label="Language"]').click();
    cy.contains('[role="menuitemradio"]', 'Français').click();
    cy.contains('a', 'Se connecter');
  });

  it('falls back to French for untranslated Malagasy keys', () => {
    cy.visit('/');
    cy.get('button[aria-label="Langue"]').click();
    cy.contains('[role="menuitemradio"]', 'Malagasy').click();
    cy.contains('a', 'Hiditra');
    cy.contains('h1', 'Trouvez où manger, ici et maintenant');
  });
});
