package mg.fikaliako.api.service

import mg.fikaliako.api.endpoint.rest.model.RankingCatalog
import mg.fikaliako.api.endpoint.rest.model.RankingPage
import mg.fikaliako.api.endpoint.rest.model.RankingTopicItem
import mg.fikaliako.api.model.exception.NotFoundException
import mg.fikaliako.api.repository.CuisineRepository
import mg.fikaliako.api.repository.RankingQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

/**
 * Thematic tops (project book ch. 4.6): best rated, best gargottes, best value
 * for money, one top per cuisine, and the monthly revelations. All orderings
 * read the nightly rating snapshot (Bayesian note), so a handful of fresh
 * reviews cannot crown a winner and the tops only move once a day. Each top is
 * shareable — the organic-acquisition lever of ch. 11.4.
 */
@Service
class RankingService(
  private val rankingRepository: RankingQueryRepository,
  private val cuisineRepository: CuisineRepository,
  private val clock: Clock,
) {
  @Transactional(readOnly = true)
  fun catalog(): RankingCatalog =
    RankingCatalog(
      buildList {
        add(RankingTopicItem(TOP_RATED, "Les mieux notés", "Ny tsara naoty indrindra"))
        add(RankingTopicItem(BEST_GARGOTTES, "Meilleures gargottes", "Hotely gasy tsara indrindra"))
        add(RankingTopicItem(BEST_VALUE, "Meilleur rapport qualité/prix", "Vidiny mifanaraka indrindra"))
        add(RankingTopicItem(REVELATIONS, "Révélations du mois", "Sangany vao hita"))
        cuisineRepository.findAllByOrderBySortOrder().forEach {
          add(cuisineTopic(it.code, it.labelFr, it.labelMg))
        }
      },
    )

  @Transactional(readOnly = true)
  fun top(topicId: String): RankingPage {
    val catalog = catalog().items.associateBy { it.id }
    val topic = catalog[topicId] ?: throw NotFoundException("Ranking '$topicId' not found.")
    val items =
      when {
        topicId == TOP_RATED -> {
          rankingRepository.topByBayesian(null, null, MIN_REVIEWS, TOP_SIZE)
        }

        topicId == BEST_GARGOTTES -> {
          rankingRepository.topByBayesian("gargotte", null, MIN_REVIEWS, TOP_SIZE)
        }

        topicId == BEST_VALUE -> {
          rankingRepository.topByValue(MIN_REVIEWS, TOP_SIZE)
        }

        topicId == REVELATIONS -> {
          rankingRepository.revelations(
            clock.instant().minus(Duration.ofDays(REVELATION_WINDOW_DAYS)),
            REVELATION_MIN_REVIEWS,
            TOP_SIZE,
          )
        }

        topicId.startsWith(CUISINE_PREFIX) -> {
          rankingRepository.topByBayesian(
            null,
            topicId.removePrefix(CUISINE_PREFIX),
            MIN_REVIEWS,
            TOP_SIZE,
          )
        }

        else -> {
          throw NotFoundException("Ranking '$topicId' not found.")
        }
      }
    return RankingPage(topic, items)
  }

  private fun cuisineTopic(
    code: String,
    labelFr: String,
    labelMg: String,
  ) = RankingTopicItem("$CUISINE_PREFIX$code", "Meilleurs — $labelFr", "Tsara indrindra — $labelMg")

  companion object {
    const val TOP_RATED = "top-rated"
    const val BEST_GARGOTTES = "best-gargottes"
    const val BEST_VALUE = "best-value"
    const val REVELATIONS = "revelations"
    const val CUISINE_PREFIX = "cuisine-"

    const val TOP_SIZE = 20

    /** Minimum reviews before an establishment may enter a top — keeps rankings honest. */
    const val MIN_REVIEWS = 3

    /** A revelation is fresh (sliding month, ch. 4.6) but already reviewed at least once. */
    const val REVELATION_WINDOW_DAYS = 30L
    const val REVELATION_MIN_REVIEWS = 1
  }
}
