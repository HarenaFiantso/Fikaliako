package mg.fikaliako.api.endpoint.rest.model

/** One shareable thematic top (project book ch. 4.6). */
data class RankingTopicItem(
  val id: String,
  val titleFr: String,
  val titleMg: String,
)

data class RankingCatalog(
  val items: List<RankingTopicItem>,
)

data class RankingPage(
  val topic: RankingTopicItem,
  val items: List<EstablishmentSummary>,
)
