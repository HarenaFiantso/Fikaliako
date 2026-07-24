package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.ContributionItem
import mg.fikaliako.api.endpoint.rest.model.Page
import mg.fikaliako.api.model.AuditLogEntry
import mg.fikaliako.api.model.Contribution
import mg.fikaliako.api.model.ContributionStatus
import mg.fikaliako.api.model.ContributionType
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.EstablishmentStatus
import mg.fikaliako.api.model.EstablishmentType
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.ConflictException
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.AuditLogRepository
import mg.fikaliako.api.repository.ContributionRepository
import mg.fikaliako.api.repository.EstablishmentRepository
import mg.fikaliako.api.repository.UserAccountRepository
import mg.fikaliako.api.util.Cursor
import mg.fikaliako.api.util.Slugs
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.util.UUID

/**
 * Back-office side of the community loop (book ch. 4.8): moderators review
 * queued contributions; approving a `create` proposal materialises it as a
 * publicly visible establishment, rejecting only records the decision. Every
 * resolution is final and lands in the immutable audit log (ch. 7.3).
 */
@Service
class ModerationService(
  private val contributions: ContributionRepository,
  private val establishments: EstablishmentRepository,
  private val users: UserAccountRepository,
  private val auditLog: AuditLogRepository,
  private val objectMapper: ObjectMapper,
  private val clock: Clock,
) {
  private val geometryFactory = GeometryFactory(PrecisionModel(), WGS84_SRID)

  @Transactional(readOnly = true)
  fun queue(
    statusValue: String?,
    limit: Int?,
    cursorValue: String?,
  ): Page<ContributionItem> {
    val status = parseStatus(statusValue)
    val cappedLimit = clampLimit(limit)
    val cursor = cursorValue?.let { Cursor.decode(it) }
    val pageable = PageRequest.of(0, cappedLimit + 1)
    val rows =
      if (cursor == null) {
        contributions.findByStatusOrderByCreatedAtAscIdAsc(status, pageable)
      } else {
        contributions.findByStatusAfter(status, cursor.createdAt, cursor.id, pageable)
      }
    val page = rows.take(cappedLimit)
    val next =
      if (rows.size > cappedLimit) {
        page.lastOrNull()?.let { Cursor(requireNotNull(it.createdAt), requireNotNull(it.id)).encode() }
      } else {
        null
      }
    val authorNames = authorNamesFor(page)
    return Page(page.map { it.toItem(authorNames) }, next)
  }

  @Transactional(readOnly = true)
  fun get(contributionId: UUID): ContributionItem {
    val contribution = find(contributionId)
    return contribution.toItem(authorNamesFor(listOf(contribution)))
  }

  @Transactional
  fun approve(
    actorId: UUID,
    contributionId: UUID,
  ): ContributionItem {
    val contribution = findPending(contributionId)
    if (contribution.type != ContributionType.create) {
      throw BadRequestException(
        "Only 'create' contributions can be approved for now (this one is '${contribution.type.name}').",
      )
    }
    val establishment = materialize(contribution)
    establishments.save(establishment)
    contribution.establishmentId = establishment.id
    resolve(contribution, ContributionStatus.applied, actorId, note = null)
    audit(actorId, "contribution.approved", contributionId, mapOf("establishment_id" to establishment.id.toString()))
    return contribution.toItem(authorNamesFor(listOf(contribution)))
  }

  @Transactional
  fun reject(
    actorId: UUID,
    contributionId: UUID,
    reason: String?,
  ): ContributionItem {
    val contribution = findPending(contributionId)
    val note = reason?.trim()?.takeIf { it.isNotEmpty() }
    resolve(contribution, ContributionStatus.rejected, actorId, note)
    audit(actorId, "contribution.rejected", contributionId, buildMap { note?.let { put("reason", it) } })
    return contribution.toItem(authorNamesFor(listOf(contribution)))
  }

  private fun find(contributionId: UUID): Contribution =
    contributions.findById(contributionId).orElseThrow {
      NotFoundException("Contribution '$contributionId' not found.")
    }

  private fun findPending(contributionId: UUID): Contribution {
    val contribution = find(contributionId)
    if (contribution.status != ContributionStatus.pending) {
      throw ConflictException("Contribution '$contributionId' is already ${contribution.status.name}.")
    }
    return contribution
  }

  /** Builds the establishment a `create` proposal described, visible at once. */
  private fun materialize(contribution: Contribution): Establishment {
    val payload = parsePayload(contribution.payload)
    val name = payload.text("name") ?: corrupt(contribution, "name")
    val typeName = payload.text("type") ?: corrupt(contribution, "type")
    val type =
      EstablishmentType.entries.find { it.name == typeName }
        ?: corrupt(contribution, "a known type (got '$typeName')")
    val lat = payload.number("lat") ?: corrupt(contribution, "lat")
    val lng = payload.number("lng") ?: corrupt(contribution, "lng")
    return Establishment(
      id = UUID.randomUUID(),
      name = name,
      slug = uniqueSlug(name),
      type = type,
      position = geometryFactory.createPoint(Coordinate(lng, lat)),
      address = payload.text("address"),
      district = payload.text("district"),
      city = payload.text("city") ?: ContributionService.DEFAULT_CITY,
      phone = payload.text("phone"),
      avgPriceAr = (payload["avg_price_ar"] as? Number)?.toInt(),
      status = EstablishmentStatus.active,
    )
  }

  private fun uniqueSlug(name: String): String {
    val base = Slugs.from(name)
    var candidate = base
    var suffix = 2
    while (establishments.findBySlug(candidate) != null) {
      candidate = "$base-${suffix++}"
    }
    return candidate
  }

  private fun resolve(
    contribution: Contribution,
    status: ContributionStatus,
    actorId: UUID,
    note: String?,
  ) {
    contribution.status = status
    contribution.resolvedBy = actorId
    contribution.resolvedAt = clock.instant()
    contribution.resolutionNote = note
    contributions.save(contribution)
  }

  private fun audit(
    actorId: UUID,
    action: String,
    contributionId: UUID,
    details: Map<String, Any?>,
  ) {
    auditLog.save(
      AuditLogEntry(
        actorId = actorId,
        action = action,
        entityType = "contribution",
        entityId = contributionId,
        details = objectMapper.writeValueAsString(details),
        createdAt = clock.instant(),
      ),
    )
  }

  private fun authorNamesFor(rows: List<Contribution>): Map<UUID, String> {
    val authorIds = rows.mapNotNull { it.authorId }.distinct()
    if (authorIds.isEmpty()) return emptyMap()
    return users.findAllById(authorIds).associate { requireNotNull(it.id) to it.displayName }
  }

  private fun Contribution.toItem(authorNames: Map<UUID, String>): ContributionItem =
    ContributionItem(
      id = requireNotNull(id),
      type = type.name,
      status = status.name,
      payload = parsePayload(payload),
      establishmentId = establishmentId,
      authorName = authorId?.let { authorNames[it] },
      confirmationsCount = confirmationsCount.toInt(),
      resolutionNote = resolutionNote,
      createdAt = requireNotNull(createdAt),
      resolvedAt = resolvedAt,
    )

  private fun parsePayload(json: String): Map<String, Any?> {
    @Suppress("UNCHECKED_CAST")
    return objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
  }

  private fun Map<String, Any?>.text(key: String): String? = this[key] as? String

  private fun Map<String, Any?>.number(key: String): Double? = (this[key] as? Number)?.toDouble()

  private fun corrupt(
    contribution: Contribution,
    what: String,
  ): Nothing = error("Contribution '${contribution.id}' payload is missing $what.")

  private fun parseStatus(value: String?): ContributionStatus {
    if (value == null) return ContributionStatus.pending
    return ContributionStatus.entries.find { it.name == value }
      ?: throw BadRequestException("Unknown contribution status '$value'.")
  }

  private fun clampLimit(limit: Int?): Int {
    val value = limit ?: DEFAULT_LIMIT
    if (value < 1) throw BadRequestException("limit must be at least 1.")
    return value.coerceAtMost(MAX_PAGE_SIZE)
  }

  companion object {
    const val DEFAULT_LIMIT = 20
    const val MAX_PAGE_SIZE = 100
    private const val WGS84_SRID = 4326
  }
}
