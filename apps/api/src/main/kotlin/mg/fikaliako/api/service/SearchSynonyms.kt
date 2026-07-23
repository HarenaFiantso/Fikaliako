package mg.fikaliako.api.service

/**
 * FR/MG synonym expansion for text search (project book ch. 4.2): searching a
 * dish or a food word also matches establishments serving the corresponding
 * cuisine or of the corresponding type, even when the word never appears in
 * their name. Deterministic and code-owned for the MVP; migrates to the
 * Meilisearch synonym dictionary with the search engine (ch. 7.1).
 *
 * Cuisine codes and types reference the referentials of V6/V1 migrations.
 */
object SearchSynonyms {
  data class Expansion(
    val cuisines: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
  )

  private data class Entry(
    val terms: List<String>,
    val cuisines: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
  )

  private val LEXICON =
    listOf(
      // Malagasy staples — the book's own examples: romazava, laoka (ch. 4.2, 16)
      Entry(
        listOf(
          "romazava",
          "ravitoto",
          "laoka",
          "vary",
          "vary gasy",
          "hena ritra",
          "henakisoa",
          "akoho gasy",
          "sakafo gasy",
          "tsaramaso",
        ),
        cuisines = setOf("malagasy"),
      ),
      Entry(
        listOf("mofo gasy", "mofogasy", "menakely", "koba", "godrogodro"),
        cuisines = setOf("bakery", "street_food"),
      ),
      Entry(
        listOf("masikita", "mosakiky", "brochette", "brochettes", "grillade", "grillades", "barbecue"),
        cuisines = setOf("grill"),
      ),
      Entry(
        listOf("pizza", "burger", "hamburger", "tacos", "kebab", "shawarma", "hot dog", "hotdog", "sandwich", "panini"),
        cuisines = setOf("fast_food"),
      ),
      Entry(
        listOf("poisson", "trondro", "crevette", "crevettes", "camaron", "fruits de mer", "hazandrano", "crabe", "foza"),
        cuisines = setOf("seafood"),
      ),
      Entry(
        listOf("soupe chinoise", "misao", "mine sao", "riz cantonais", "nem", "nems"),
        cuisines = setOf("chinese"),
      ),
      Entry(
        listOf("sambos", "sambosa", "composee"),
        cuisines = setOf("street_food"),
      ),
      Entry(
        listOf("legumes", "anana", "vegetarien", "vegetarienne", "vegan"),
        cuisines = setOf("vegetarian"),
      ),
      Entry(
        listOf("jus", "ranom-boankazo", "cafe", "kafe", "the", "dite"),
        cuisines = setOf("drinks"),
        types = setOf("cafe"),
      ),
      Entry(
        listOf("patisserie", "mofomamy", "gateau", "gateaux", "boulangerie", "croissant"),
        cuisines = setOf("bakery"),
        types = setOf("pastry_shop"),
      ),
      Entry(
        listOf("gargotte", "gargote", "hotely"),
        types = setOf("gargotte"),
      ),
      Entry(
        listOf("food truck", "foodtruck"),
        types = setOf("food_truck"),
      ),
    )

  /** Matches whole words/phrases of an already-normalised query and merges every hit. */
  fun expand(normalizedQuery: String): Expansion {
    val padded = " $normalizedQuery "
    val hits = LEXICON.filter { entry -> entry.terms.any { padded.contains(" $it ") } }
    return Expansion(
      cuisines = hits.flatMap { it.cuisines }.toSet(),
      types = hits.flatMap { it.types }.toSet(),
    )
  }
}
