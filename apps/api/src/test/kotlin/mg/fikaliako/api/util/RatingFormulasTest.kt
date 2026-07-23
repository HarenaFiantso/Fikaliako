package mg.fikaliako.api.util

import mg.fikaliako.api.util.RatingFormulas.WeightedNote
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RatingFormulasTest {
  @Test
  fun `recency weight decays by tier`() {
    assertEquals(BigDecimal("1.00"), RatingFormulas.recencyWeight(0))
    assertEquals(BigDecimal("1.00"), RatingFormulas.recencyWeight(30))
    assertEquals(BigDecimal("0.85"), RatingFormulas.recencyWeight(31))
    assertEquals(BigDecimal("0.85"), RatingFormulas.recencyWeight(90))
    assertEquals(BigDecimal("0.70"), RatingFormulas.recencyWeight(91))
    assertEquals(BigDecimal("0.70"), RatingFormulas.recencyWeight(365))
    assertEquals(BigDecimal("0.50"), RatingFormulas.recencyWeight(366))
  }

  @Test
  fun `mean averages at two decimals and is null on empty input`() {
    assertNull(RatingFormulas.mean(emptyList()))
    assertEquals(BigDecimal("4.00"), RatingFormulas.mean(listOf(BigDecimal(4))))
    assertEquals(
      BigDecimal("4.33"),
      RatingFormulas.mean(listOf(BigDecimal(5), BigDecimal(4), BigDecimal(4))),
    )
  }

  @Test
  fun `bayesian note is null without reviews`() {
    assertNull(RatingFormulas.bayesianNote(emptyList(), BigDecimal("4.00")))
  }

  @Test
  fun `few reviews stay close to the global mean`() {
    // Two perfect recent reviews against a 3.50 global mean: (10*3.50 + 2*5.00) / 12
    val note =
      RatingFormulas.bayesianNote(
        List(2) { WeightedNote(BigDecimal("5.00"), BigDecimal("1.00")) },
        BigDecimal("3.50"),
      )
    assertEquals(BigDecimal("3.75"), note)
  }

  @Test
  fun `volume lets the establishment own its note`() {
    val fewReviews =
      RatingFormulas.bayesianNote(
        List(2) { WeightedNote(BigDecimal("5.00"), BigDecimal("1.00")) },
        BigDecimal("3.50"),
      )!!
    val manyReviews =
      RatingFormulas.bayesianNote(
        List(100) { WeightedNote(BigDecimal("5.00"), BigDecimal("1.00")) },
        BigDecimal("3.50"),
      )!!
    assertTrue(manyReviews > fewReviews)
    assertEquals(BigDecimal("4.86"), manyReviews)
  }

  @Test
  fun `older reviews pull less than recent ones`() {
    val recent =
      RatingFormulas.bayesianNote(
        List(10) { WeightedNote(BigDecimal("5.00"), RatingFormulas.recencyWeight(10)) },
        BigDecimal("3.00"),
      )!!
    val stale =
      RatingFormulas.bayesianNote(
        List(10) { WeightedNote(BigDecimal("5.00"), RatingFormulas.recencyWeight(400)) },
        BigDecimal("3.00"),
      )!!
    assertTrue(recent > stale)
  }
}
