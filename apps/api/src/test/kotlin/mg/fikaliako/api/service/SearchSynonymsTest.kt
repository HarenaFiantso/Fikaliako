package mg.fikaliako.api.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchSynonymsTest {
  @Test
  fun `a malagasy dish expands to its cuisine`() {
    val expansion = SearchSynonyms.expand("romazava")
    assertEquals(setOf("malagasy"), expansion.cuisines)
    assertTrue(expansion.types.isEmpty())
  }

  @Test
  fun `multi-word terms match as whole phrases`() {
    assertEquals(setOf("bakery", "street_food"), SearchSynonyms.expand("mofo gasy").cuisines)
    assertEquals(setOf("chinese"), SearchSynonyms.expand("riz cantonais pas loin").cuisines)
  }

  @Test
  fun `type words expand to establishment types`() {
    assertEquals(setOf("gargotte"), SearchSynonyms.expand("gargotte analakely").types)
    assertEquals(setOf("gargotte"), SearchSynonyms.expand("hotely").types)
  }

  @Test
  fun `partial words do not match`() {
    // "variete" contains "vary" as a prefix but is not the word "vary"
    assertTrue(SearchSynonyms.expand("variete").cuisines.isEmpty())
  }

  @Test
  fun `unknown terms expand to nothing`() {
    val expansion = SearchSynonyms.expand("chez mariette")
    assertTrue(expansion.cuisines.isEmpty())
    assertTrue(expansion.types.isEmpty())
  }

  @Test
  fun `several hits merge`() {
    val expansion = SearchSynonyms.expand("pizza ou romazava")
    assertEquals(setOf("fast_food", "malagasy"), expansion.cuisines)
  }
}
