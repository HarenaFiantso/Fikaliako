package mg.fikaliako.api.service
import mg.fikaliako.api.endpoint.rest.model.EstablishmentSummary
import mg.fikaliako.api.endpoint.rest.model.GeoPoint
import mg.fikaliako.api.endpoint.rest.model.UserProfile
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentRating
import mg.fikaliako.api.model.UserAccount

// Entity → wire mappings shared across services. Enum-ish strings are
// lowercase on the wire (the contract's enums: restaurant, active, user…).

fun UserAccount.toProfile(): UserProfile =
  UserProfile(
    id = requireNotNull(id),
    phone = phone,
    displayName = displayName,
    role = role.name.lowercase(),
    phoneVerified = phoneVerified,
    locale = locale,
    createdAt = requireNotNull(createdAt),
  )

fun Establishment.toSummary(rating: EstablishmentRating?): EstablishmentSummary =
  EstablishmentSummary(
    id = requireNotNull(id),
    slug = slug,
    name = name,
    type = type.name.lowercase(),
    position = GeoPoint(requireNotNull(position).y, requireNotNull(position).x),
    avgPriceAr = avgPriceAr,
    verified = verified,
    status = status.name.lowercase(),
    ratingAvg = rating?.avgGlobal,
    ratingCount = rating?.reviewCount ?: 0,
  )
