package mg.fikaliako.api.service
import mg.fikaliako.api.endpoint.rest.model.UpdateProfileRequest
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentRating
import mg.fikaliako.api.model.Favorite
import mg.fikaliako.api.model.FavoriteId
import mg.fikaliako.api.model.UserAccount
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.model.exception.UnauthorizedException
import mg.fikaliako.api.repository.EstablishmentRatingRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.FavoriteRepository
import mg.fikaliako.api.repository.UserAccountRepository
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito
import org.springframework.data.domain.Limit
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserServiceTest {
  private val now = Instant.parse("2026-07-09T12:00:00Z")
  private val users = Mockito.mock(UserAccountRepository::class.java)
  private val favorites = Mockito.mock(FavoriteRepository::class.java)
  private val establishments = Mockito.mock(EstablishmentRepository::class.java)
  private val ratings = Mockito.mock(EstablishmentRatingRepository::class.java)
  private val service = UserService(users, favorites, establishments, ratings, Clock.fixed(now, ZoneOffset.UTC))

  private val userId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001")
  private val geometryFactory = GeometryFactory()

  private fun user() =
    UserAccount(
      id = userId,
      displayName = "Naina",
      phone = "+261340000001",
      phoneVerified = true,
      createdAt = now.minusSeconds(86_400),
    )

  private fun establishment(
    id: UUID,
    name: String = "Chez Bao",
  ) = Establishment(
    id = id,
    name = name,
    slug = name.lowercase().replace(' ', '-'),
    position = geometryFactory.createPoint(Coordinate(47.5210, -18.9092)),
  )

  private fun favorite(
    establishmentId: UUID,
    createdAt: Instant,
  ) = Favorite(
    id = FavoriteId(userId = userId, establishmentId = establishmentId),
    establishment = establishment(establishmentId),
    createdAt = createdAt,
  )

  @Test
  fun `me maps the profile and rejects tokens of purged accounts`() {
    Mockito.`when`(users.findById(userId)).thenReturn(Optional.of(user()))
    val profile = service.me(userId)
    assertEquals("Naina", profile.displayName)
    assertEquals("user", profile.role)

    Mockito.`when`(users.findById(userId)).thenReturn(Optional.empty())
    assertFailsWith<UnauthorizedException> { service.me(userId) }
  }

  @Test
  fun `updateMe only touches the provided fields`() {
    val stored = user()
    Mockito.`when`(users.findById(userId)).thenReturn(Optional.of(stored))

    val profile = service.updateMe(userId, UpdateProfileRequest(locale = "mg"))

    assertEquals("Naina", profile.displayName)
    assertEquals("mg", profile.locale)
    assertEquals(now, stored.updatedAt)
  }

  @Test
  fun `addFavorite is idempotent`() {
    val estId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001")
    val favoriteId = FavoriteId(userId = userId, establishmentId = estId)
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.of(establishment(estId)))

    Mockito.`when`(favorites.existsById(favoriteId)).thenReturn(false)
    service.addFavorite(userId, estId)

    Mockito.`when`(favorites.existsById(favoriteId)).thenReturn(true)
    service.addFavorite(userId, estId)

    Mockito.verify(favorites, Mockito.times(1)).save(Mockito.any())
  }

  @Test
  fun `favoriting a missing establishment is a 404`() {
    val estId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000404")
    Mockito.`when`(establishments.findById(estId)).thenReturn(Optional.empty())
    assertFailsWith<NotFoundException> { service.addFavorite(userId, estId) }
  }

  @Test
  fun `favorites list merges ratings and paginates by cursor`() {
    val ids = (1..3).map { UUID.fromString("aaaaaaaa-0000-0000-0000-00000000000$it") }
    val rows = ids.mapIndexed { i, id -> favorite(id, now.minusSeconds(i.toLong())) }
    Mockito.`when`(favorites.findForUser(userId, Limit.of(3))).thenReturn(rows)
    Mockito
      .`when`(ratings.findAllById(ids.take(2)))
      .thenReturn(listOf(EstablishmentRating(establishmentId = ids[0], reviewCount = 12, avgGlobal = BigDecimal("4.50"))))

    val page = service.listFavorites(userId, 2, null)

    assertEquals(2, page.items.size)
    assertNotNull(page.nextCursor)
    assertEquals(12, page.items[0].ratingCount)
    assertEquals(BigDecimal("4.50"), page.items[0].ratingAvg)
    assertEquals(0, page.items[1].ratingCount)
    assertEquals(-18.9092, page.items[0].position.lat)

    Mockito.`when`(favorites.findForUser(userId, Limit.of(3))).thenReturn(rows.take(2))
    assertNull(service.listFavorites(userId, 2, null).nextCursor)
  }
}
