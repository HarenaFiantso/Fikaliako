package mg.fikaliako.api.service

import mg.fikaliako.api.util.TextNormalization
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmartQueryInterpreterTest {
  private fun interpret(raw: String) = SmartQueryInterpreter.interpret(TextNormalization.normalize(raw))

  @Test
  fun `j'ai faim means open now, close by, nothing left to text-search`() {
    val i = interpret("J’ai faim")
    assertEquals(listOf("hungry"), i.intents)
    assertTrue(i.openNow)
    assertEquals(1000.0, i.radiusM)
    assertEquals("", i.residualQuery)
  }

  @Test
  fun `malagasy hunger works too`() {
    assertEquals(listOf("hungry"), interpret("noana aho").intents)
    assertEquals(listOf("hungry"), interpret("aiza no hisakafo").intents)
  }

  @Test
  fun `pas cher caps the budget at the two lowest tranches`() {
    val i = interpret("resto pas cher")
    assertEquals(listOf("cheap"), i.intents)
    assertEquals(5000, i.maxPriceAr)
    assertEquals("", i.residualQuery)
  }

  @Test
  fun `an explicit amount beats the cheap default`() {
    val i = interpret("pas cher moins de 3 000 ar")
    assertEquals(3000, i.maxPriceAr)
    assertEquals("", i.residualQuery)
  }

  @Test
  fun `moins de N ar parses grouped digits`() {
    val i = interpret("moins de 10 000 ar")
    assertEquals(listOf("budget"), i.intents)
    assertEquals(10_000, i.maxPriceAr)
  }

  @Test
  fun `manger romantique selects the romantic amenity`() {
    val i = interpret("manger romantique")
    assertEquals(listOf("romantic"), i.intents)
    assertEquals(listOf("romantic"), i.amenities)
    assertEquals("", i.residualQuery)
  }

  @Test
  fun `petit dejeuner is breakfast, not lunch`() {
    val i = interpret("petit déjeuner")
    assertEquals(listOf("breakfast"), i.intents)
    assertTrue(i.openNow)
    assertEquals(listOf("cafe", "pastry_shop"), i.types)
  }

  @Test
  fun `dejeuner alone is lunch`() {
    val i = interpret("déjeuner")
    assertEquals(listOf("lunch"), i.intents)
    assertTrue(i.openNow)
    assertTrue(i.types.isEmpty())
  }

  @Test
  fun `intents combine and the dish keeps searching as text`() {
    val i = interpret("pizza pas cher livraison")
    assertEquals(setOf("cheap", "delivery"), i.intents.toSet())
    assertEquals(5000, i.maxPriceAr)
    assertEquals(listOf("delivery"), i.amenities)
    assertEquals("pizza", i.residualQuery)
  }

  @Test
  fun `late night maps to the open_24h amenity`() {
    val i = interpret("ouvert toute la nuit")
    assertTrue("late_night" in i.intents)
    assertTrue("open_24h" in i.amenities)
  }

  @Test
  fun `operator words select their payment method`() {
    assertEquals("mvola", interpret("payer avec mvola").payment)
    assertEquals("orange_money", interpret("orange money").payment)
    assertEquals("mobile", interpret("paiement mobile").payment)
  }

  @Test
  fun `a plain name search is not smart and keeps its words`() {
    val i = interpret("chez mariette")
    assertFalse(i.isSmart)
    assertNull(i.maxPriceAr)
    assertEquals("chez mariette", i.residualQuery)
  }

  @Test
  fun `stopwords survive in non-smart queries`() {
    // "le petit resto" has no intent; nothing may be stripped from a name search
    assertEquals("le petit resto", interpret("Le Petit Resto").residualQuery)
  }
}
