package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.ContributionReceipt
import mg.fikaliako.api.endpoint.rest.model.EstablishmentProposal
import mg.fikaliako.api.model.Contribution
import mg.fikaliako.api.model.ContributionStatus
import mg.fikaliako.api.model.ContributionType
import mg.fikaliako.api.model.EstablishmentType
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.UnauthorizedException
import mg.fikaliako.api.repository.ContributionRepository
import mg.fikaliako.api.repository.UserAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Clock
import java.util.UUID

@Service
class ContributionService(
  private val contributionRepository: ContributionRepository,
  private val userRepository: UserAccountRepository,
  private val objectMapper: ObjectMapper,
  private val clock: Clock,
) {
  /**
   * Registers a `create` contribution (book ch. 4.8): the proposal is stored
   * as the moderation-queue payload, not as an establishment row — nothing is
   * publicly visible until a moderator applies it.
   */
  @Transactional
  fun proposeEstablishment(
    authorId: UUID,
    proposal: EstablishmentProposal,
  ): ContributionReceipt {
    if (!userRepository.existsById(authorId)) {
      throw UnauthorizedException("Account no longer exists.")
    }
    val type =
      EstablishmentType.entries.find { it.name == proposal.type }
        ?: throw BadRequestException("Unknown establishment type '${proposal.type}'.")
    val position = requireNotNull(proposal.position)

    val payload =
      buildMap<String, Any> {
        put("name", proposal.name.trim())
        put("type", type.name)
        put("lat", position.lat)
        put("lng", position.lng)
        put("city", proposal.city.orDefault(DEFAULT_CITY))
        proposal.address.cleaned()?.let { put("address", it) }
        proposal.district.cleaned()?.let { put("district", it) }
        proposal.phone.cleaned()?.let { put("phone", it) }
        proposal.avgPriceAr?.let { put("avg_price_ar", it) }
        proposal.comment.cleaned()?.let { put("comment", it) }
      }

    val contribution =
      Contribution(
        id = UUID.randomUUID(),
        establishmentId = null,
        authorId = authorId,
        type = ContributionType.create,
        payload = objectMapper.writeValueAsString(payload),
        status = ContributionStatus.pending,
        createdAt = clock.instant(),
      )
    contributionRepository.save(contribution)

    return ContributionReceipt(
      id = requireNotNull(contribution.id),
      status = contribution.status.name,
      createdAt = requireNotNull(contribution.createdAt),
    )
  }

  private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

  private fun String?.orDefault(default: String): String = cleaned() ?: default

  companion object {
    const val DEFAULT_CITY = "Antananarivo"
  }
}
