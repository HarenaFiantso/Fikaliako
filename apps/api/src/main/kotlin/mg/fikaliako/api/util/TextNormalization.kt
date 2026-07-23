package mg.fikaliako.api.util

import java.text.Normalizer

/**
 * Query normalisation shared by search and the smart-query interpreter:
 * lowercase, diacritics stripped ("pâtisserie" → "patisserie"), curly
 * apostrophes straightened, whitespace collapsed. Keeps FR and MG lexicon
 * matching accent-insensitive (project book ch. 4.2).
 */
object TextNormalization {
  private val DIACRITICS = Regex("\\p{M}+")
  private val APOSTROPHES = Regex("[’ʼ`]")
  private val WHITESPACE = Regex("\\s+")

  fun normalize(value: String): String =
    Normalizer
      .normalize(value.lowercase(), Normalizer.Form.NFD)
      .replace(DIACRITICS, "")
      .replace(APOSTROPHES, "'")
      .replace(WHITESPACE, " ")
      .trim()
}
