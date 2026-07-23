package mg.fikaliako.api.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TextNormalizationTest {
  @Test
  fun `lowercases, strips accents and collapses whitespace`() {
    assertEquals("patisserie", TextNormalization.normalize("  Pâtisserie "))
    assertEquals("creme brulee", TextNormalization.normalize("Crème  Brûlée"))
    assertEquals("j'ai faim", TextNormalization.normalize("J’ai   FAIM"))
    assertEquals("", TextNormalization.normalize("   "))
  }

  @Test
  fun `keeps malagasy words intact`() {
    assertEquals("mofo gasy", TextNormalization.normalize("Mofo Gasy"))
    assertEquals("ranom-boankazo", TextNormalization.normalize("Ranom-boankazo"))
  }
}
