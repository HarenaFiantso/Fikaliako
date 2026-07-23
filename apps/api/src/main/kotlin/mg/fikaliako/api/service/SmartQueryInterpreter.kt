package mg.fikaliako.api.service

/**
 * Deterministic smart search (project book ch. 4.2, "recherche intelligente"):
 * FR/MG natural-language phrases become filter combinations — "j'ai faim" turns
 * into open-now within 1 km ranked by the discovery score, "pas cher" caps the
 * budget, "manger romantique" selects the romantic amenity. The rules are fixed
 * for the MVP by the ch. 4.2 management rule; a learning model only replaces
 * them in V3 (ch. 4.12).
 *
 * Input must already be normalised by [mg.fikaliako.api.util.TextNormalization].
 * Matched phrases are consumed; whatever text remains (minus filler words) is
 * the residual to hand to text search — "pizza pas cher" keeps searching
 * "pizza", just under 5 000 Ar.
 */
object SmartQueryInterpreter {
  data class Interpretation(
    val intents: List<String>,
    val openNow: Boolean = false,
    val maxPriceAr: Int? = null,
    val types: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val payment: String? = null,
    /** Radius suggested by the intent (e.g. hungry → 1 km), applied only when the client sent a position. */
    val radiusM: Double? = null,
    val residualQuery: String,
  ) {
    val isSmart: Boolean get() = intents.isNotEmpty()
  }

  private class Rule(
    val intent: String,
    val phrases: List<String>,
    val openNow: Boolean = false,
    val maxPriceAr: Int? = null,
    val types: Set<String> = emptySet(),
    val amenities: Set<String> = emptySet(),
    val payment: String? = null,
    val radiusM: Double? = null,
  )

  private val RULES =
    listOf(
      // Ch. 4.2 management rule: "J'ai faim" → open, close by, discovery-ranked.
      Rule(
        "hungry",
        listOf("j'ai faim", "jai faim", "faim", "noana aho", "noana", "aiza no hisakafo", "hisakafo"),
        openNow = true,
        radiusM = 1000.0,
      ),
      // "Pas cher" spans the two lowest budget tranches of ch. 3.2.
      Rule(
        "cheap",
        listOf("pas cher", "pas chere", "bon marche", "petit budget", "economique", "mora"),
        maxPriceAr = 5000,
      ),
      Rule(
        "breakfast",
        listOf("petit dejeuner", "petit dej", "sakafo maraina"),
        openNow = true,
        types = setOf("cafe", "pastry_shop"),
      ),
      Rule("lunch", listOf("dejeuner", "sakafo atoandro", "ce midi", "midi"), openNow = true),
      Rule("dinner", listOf("diner", "sakafo hariva", "ce soir"), openNow = true),
      Rule("open_now", listOf("ouvert maintenant", "ouvert", "misokatra"), openNow = true),
      Rule(
        "late_night",
        listOf("24h", "24/24", "toute la nuit", "tard le soir"),
        amenities = setOf("open_24h"),
      ),
      Rule("romantic", listOf("romantique", "en amoureux"), amenities = setOf("romantic")),
      Rule("family", listOf("en famille", "famille", "fianakaviana"), amenities = setOf("family_friendly")),
      Rule("delivery", listOf("livraison", "livre a domicile"), amenities = setOf("delivery")),
      Rule("terrace", listOf("terrasse"), amenities = setOf("terrace")),
      Rule("wifi", listOf("wifi"), amenities = setOf("wifi")),
      Rule("parking", listOf("parking"), amenities = setOf("parking")),
      Rule("scenic_view", listOf("belle vue", "avec vue"), amenities = setOf("scenic_view")),
      Rule("air_conditioning", listOf("climatisation", "climatise", "clim"), amenities = setOf("air_conditioning")),
      Rule("student", listOf("etudiant", "etudiants", "mpianatra"), amenities = setOf("student_friendly")),
      Rule("mobile_payment", listOf("paiement mobile", "mobile money"), payment = "mobile"),
      Rule("mvola", listOf("mvola"), payment = "mvola"),
      Rule("orange_money", listOf("orange money"), payment = "orange_money"),
      Rule("airtel_money", listOf("airtel money"), payment = "airtel_money"),
    )

  /** Longest phrase first, so "petit dejeuner" wins over "dejeuner". */
  private val FLAT_RULES =
    RULES
      .flatMap { rule -> rule.phrases.map { it to rule } }
      .sortedByDescending { it.first.length }

  /** "moins de 8 000 ar" → explicit budget ceiling. */
  private val BUDGET_PATTERN = Regex(" moins de (\\d[\\d ]*) ?(?:ar|ariary)? ")

  /** Filler words dropped from the residual once at least one intent matched. */
  private val STOPWORDS =
    setOf(
      "je",
      "veux",
      "voudrais",
      "cherche",
      "envie",
      "d",
      "de",
      "du",
      "des",
      "un",
      "une",
      "le",
      "la",
      "les",
      "a",
      "au",
      "aux",
      "et",
      "ou",
      "pour",
      "avec",
      "pres",
      "proche",
      "ici",
      "quelque",
      "chose",
      "part",
      "endroit",
      "resto",
      "manger",
      "boire",
      "payer",
      "aiza",
      "no",
      "izay",
      "any",
      "amin",
      "ny",
      "hoe",
      "ve",
    )

  fun interpret(normalizedQuery: String): Interpretation {
    var text = " $normalizedQuery "
    val intents = linkedSetOf<String>()
    val types = linkedSetOf<String>()
    val amenities = linkedSetOf<String>()
    var openNow = false
    var maxPriceAr: Int? = null
    var payment: String? = null
    var radiusM: Double? = null

    BUDGET_PATTERN.find(text)?.let { match ->
      val amount = match.groupValues[1].replace(" ", "").toIntOrNull()
      if (amount != null && amount in 1..MAX_BUDGET_AR) {
        intents += "budget"
        maxPriceAr = amount
        text = text.replace(match.value, " ")
      }
    }

    for ((phrase, rule) in FLAT_RULES) {
      val padded = " $phrase "
      if (!text.contains(padded)) continue
      intents += rule.intent
      openNow = openNow || rule.openNow
      rule.maxPriceAr?.let { maxPriceAr = minOf(maxPriceAr ?: Int.MAX_VALUE, it) }
      types += rule.types
      amenities += rule.amenities
      payment = payment ?: rule.payment
      radiusM = radiusM ?: rule.radiusM
      while (text.contains(padded)) text = text.replace(padded, " ")
    }

    val residual =
      if (intents.isEmpty()) {
        normalizedQuery
      } else {
        text
          .trim()
          .split(" ")
          .filter { it.isNotEmpty() && it !in STOPWORDS }
          .joinToString(" ")
      }

    return Interpretation(
      intents = intents.toList(),
      openNow = openNow,
      maxPriceAr = maxPriceAr,
      types = types.toList(),
      amenities = amenities.toList(),
      payment = payment,
      radiusM = radiusM,
      residualQuery = residual,
    )
  }

  private const val MAX_BUDGET_AR = 1_000_000
}
