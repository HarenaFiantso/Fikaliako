package mg.fikaliako.api.util

/**
 * URL slugs for establishment pages ("Chez Bào !" → "chez-bao"): the slug
 * path powers the Google-indexable pages (project book ch. 11.3).
 */
object Slugs {
  private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")

  fun from(value: String): String =
    TextNormalization
      .normalize(value)
      .replace(NON_ALPHANUMERIC, "-")
      .trim('-')
      .ifEmpty { "establishment" }
}
