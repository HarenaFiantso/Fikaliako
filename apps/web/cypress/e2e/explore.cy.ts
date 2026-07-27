import {
  establishmentPage,
  problem,
  testEstablishment,
  testTokens,
  unratedEstablishment,
  visitWithSession,
} from '../support/api-stubs';

/** The persisted session holds no access token, so any authenticated call rotates first. */
function stubRefresh() {
  cy.intercept('POST', '**/v1/auth/refresh', { statusCode: 200, body: testTokens }).as('refresh');
}

describe('explore', () => {
  it('lists establishments with their type, price and rating', () => {
    cy.intercept('GET', '**/v1/establishments?*', {
      statusCode: 200,
      body: establishmentPage([testEstablishment, unratedEstablishment]),
    }).as('browse');

    cy.visit('/explore');
    cy.wait('@browse');

    cy.contains('h1', 'Explorer');
    cy.contains('a', testEstablishment.name);
    cy.contains('Gargotte · ~2 500 Ar');
    cy.contains('4.5');
    cy.contains('(12)');

    cy.contains('a', unratedEstablishment.name);
    cy.contains("Pas encore d'avis");
  });

  it('links each card to its establishment page', () => {
    cy.intercept('GET', '**/v1/establishments?*', {
      statusCode: 200,
      body: establishmentPage([testEstablishment]),
    }).as('browse');

    cy.visit('/explore');
    cy.wait('@browse');

    cy.contains('a', testEstablishment.name).should(
      'have.attr',
      'href',
      `/establishment/${testEstablishment.slug}`
    );
  });

  it('shows the empty state when the community has nothing yet', () => {
    cy.intercept('GET', '**/v1/establishments?*', {
      statusCode: 200,
      body: establishmentPage([]),
    }).as('browse');

    cy.visit('/explore');
    cy.wait('@browse');
    cy.contains('Aucun établissement pour le moment');
  });

  it('surfaces a retry when the browse call fails', () => {
    cy.intercept('GET', '**/v1/establishments?*', problem(500, 'Server Error', 'boom')).as(
      'browse'
    );

    cy.visit('/explore');
    cy.wait('@browse');
    cy.contains("Les lieux autour n'ont pas pu être chargés.");

    cy.intercept('GET', '**/v1/establishments?*', {
      statusCode: 200,
      body: establishmentPage([testEstablishment]),
    }).as('retry');
    cy.contains('button', 'Réessayer').click();
    cy.wait('@retry');
    cy.contains('a', testEstablishment.name);
  });

  it('pages through the cursor with load more', () => {
    cy.intercept('GET', '**/v1/establishments?limit=20', {
      statusCode: 200,
      body: establishmentPage([testEstablishment], 'cursor-2'),
    }).as('page1');
    cy.intercept('GET', '**/v1/establishments?limit=20&cursor=cursor-2', {
      statusCode: 200,
      body: establishmentPage([unratedEstablishment]),
    }).as('page2');

    cy.visit('/explore');
    cy.wait('@page1');
    cy.contains('button', 'Voir plus').click();
    cy.wait('@page2');

    cy.contains('a', testEstablishment.name);
    cy.contains('a', unratedEstablishment.name);
    cy.contains('button', 'Voir plus').should('not.exist');
  });

  it('sends anonymous visitors to login instead of favoriting', () => {
    cy.intercept('GET', '**/v1/establishments?*', {
      statusCode: 200,
      body: establishmentPage([testEstablishment]),
    }).as('browse');

    cy.visit('/explore');
    cy.wait('@browse');
    cy.get('button[aria-label="Ajouter aux favoris"]').click();
    cy.location('pathname').should('equal', '/login');
  });

  it('favorites and un-favorites once signed in', () => {
    stubRefresh();
    cy.intercept('GET', '**/v1/establishments?*', {
      statusCode: 200,
      body: establishmentPage([testEstablishment]),
    }).as('browse');

    // The favorites endpoint is stateful, so the refetch that follows each
    // mutation confirms the optimistic flip instead of undoing it.
    let favorited = false;
    cy.intercept('GET', '**/v1/users/me/favorites?*', (req) => {
      req.reply({
        statusCode: 200,
        body: establishmentPage(favorited ? [testEstablishment] : []),
      });
    }).as('favorites');
    cy.intercept('PUT', '**/v1/users/me/favorites/*', (req) => {
      favorited = true;
      req.reply({ statusCode: 204 });
    }).as('addFavorite');
    cy.intercept('DELETE', '**/v1/users/me/favorites/*', (req) => {
      favorited = false;
      req.reply({ statusCode: 204 });
    }).as('removeFavorite');

    visitWithSession('/explore');
    cy.wait('@browse');
    cy.wait('@favorites');

    cy.get('button[aria-label="Ajouter aux favoris"]').click();
    cy.wait('@addFavorite');
    cy.get('button[aria-label="Retirer des favoris"]').should('have.attr', 'aria-pressed', 'true');

    cy.get('button[aria-label="Retirer des favoris"]').click();
    cy.wait('@removeFavorite');
    cy.get('button[aria-label="Ajouter aux favoris"]').should('have.attr', 'aria-pressed', 'false');
  });
});

describe('propose a place', () => {
  it('redirects anonymous visitors to login', () => {
    cy.visit('/propose');
    cy.location('pathname').should('equal', '/login');
  });

  it('refuses to submit without a position', () => {
    stubRefresh();
    cy.intercept('POST', '**/v1/establishments', cy.spy().as('propose'));

    visitWithSession('/propose');
    cy.contains('h1', 'Ajouter un lieu');
    cy.get('input[name="name"]').type('Chez Mama Fara');
    cy.contains('button', 'Envoyer en modération').click();

    cy.contains('La position est obligatoire');
    cy.get('@propose').should('not.have.been.called');
  });

  it('sends the proposal to moderation', () => {
    stubRefresh();
    cy.intercept('POST', '**/v1/establishments', (req) => {
      expect(req.body).to.deep.include({
        name: 'Chez Mama Fara',
        type: 'gargotte',
        city: 'Antananarivo',
        avg_price_ar: 3000,
      });
      expect(req.body.position).to.deep.equal({ lat: -18.9092, lng: 47.521 });
      req.reply({
        statusCode: 202,
        body: {
          id: 'f4e3d2c1-b0a9-4867-9584-3a2b1c0d9e8f',
          status: 'pending',
          created_at: '2026-07-27T09:00:00Z',
        },
      });
    }).as('propose');

    visitWithSession('/propose');
    cy.get('input[name="name"]').type('Chez Mama Fara');
    cy.get('input[name="lat"]').type('-18.9092');
    cy.get('input[name="lng"]').type('47.521');
    cy.get('input[name="avgPrice"]').type('3000');
    cy.contains('button', 'Envoyer en modération').click();

    cy.wait('@propose');
    cy.contains('Proposition envoyée');
    cy.contains('a', "Retour à l'exploration").should('have.attr', 'href', '/explore');
  });
});
