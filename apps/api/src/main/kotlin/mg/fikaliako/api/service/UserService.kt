package mg.fikaliako.api.service
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.endpoint.rest.model.UpdateProfileRequest
import mg.fikaliako.api.endpoint.rest.model.UserProfile
import mg.fikaliako.api.model.Favorite
import mg.fikaliako.api.model.FavoriteId
import mg.fikaliako.api.model.UserAccount
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.model.exception.UnauthorizedException
import mg.fikaliako.api.repository.EstablishmentRatingRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.FavoriteRepository
import mg.fikaliako.api.repository.UserAccountRepository
import mg.fikaliako.api.util.Cursor
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
class UserService(
  private val users: UserAccountRepository,
  private val favorites: FavoriteRepository,
  private val establishments: EstablishmentRepository,
  private val ratings: EstablishmentRatingRepository,
  private val clock: Clock,
) {
  @Transactional(readOnly = true)
  fun me(userId: UUID): UserProfile = find(userId).toProfile()

  @Transactional
  fun updateMe(
    userId: UUID,
    request: UpdateProfileRequest,
  ): UserProfile {
    val user = find(userId)
    request.displayName?.let { user.displayName = it.trim() }
    request.locale?.let { user.locale = it }
    user.updatedAt = clock.instant()
    users.save(user)
    return user.toProfile()
  }

  @Transactional
  fun addFavorite(
    userId: UUID,
    establishmentId: UUID,
  ) {
    val establishment =
      establishments.findById(establishmentId).orElseThrow {
        NotFoundException("Establishment '$establishmentId' not found.")
      }
    val id = FavoriteId(userId = userId, establishmentId = establishmentId)
    if (!favorites.existsById(id)) {
      favorites.save(Favorite(id = id, establishment = establishment, createdAt = clock.instant()))
    }
  }

  @Transactional
  fun removeFavorite(
    userId: UUID,
    establishmentId: UUID,
  ) = favorites.deleteById(FavoriteId(userId = userId, establishmentId = establishmentId))

  @Transactional(readOnly = true)
  fun listFavorites(
    userId: UUID,
    limit: Int?,
    cursorValue: String?,
  ): Page<EstablishmentSummary> {
    val cappedLimit = clampLimit(limit)
    val cursor = cursorValue?.let { Cursor.decode(it) }
    val fetch = Limit.of(cappedLimit + 1)
    val rows =
      if (cursor == null) {
        favorites.findForUser(userId, fetch)
      } else {
        favorites.findForUserAfter(userId, cursor.createdAt, cursor.id, fetch)
      }
    val page = rows.take(cappedLimit)
    val next =
      if (rows.size > cappedLimit) {
        page.lastOrNull()?.let {
          Cursor(requireNotNull(it.createdAt), requireNotNull(it.id).establishmentId).encode()
        }
      } else {
        null
      }
    val ratingsById =
      ratings
        .findAllById(page.mapNotNull { it.id?.establishmentId })
        .associateBy { it.establishmentId }
    val items =
      page.map {
        val establishment = requireNotNull(it.establishment)
        establishment.toSummary(ratingsById[establishment.id])
      }
    return Page(items, next)
  }

  private fun find(userId: UUID): UserAccount =
    users.findById(userId).orElseThrow {
      UnauthorizedException("Account no longer exists.")
    }

  private fun clampLimit(limit: Int?): Int {
    val value = limit ?: DEFAULT_LIMIT
    if (value < 1) throw BadRequestException("limit must be at least 1.")
    return value.coerceAtMost(MAX_PAGE_SIZE)
  }

  companion object {
    const val DEFAULT_LIMIT = 50
    const val MAX_PAGE_SIZE = 100
  }
}
