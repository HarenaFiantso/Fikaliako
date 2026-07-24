package mg.fikaliako.api.util

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SlugsTest {
  @Test
  fun `slugs are lowercase ascii separated by hyphens`() {
    assertEquals("chez-bao", Slugs.from("Chez Bào !"))
    assertEquals("hotely-gasy-n-1", Slugs.from("  Hôtely gasy n°1 "))
    assertEquals("mofo-gasy-d-ambatobe", Slugs.from("Mofo gasy d’Ambatobe"))
  }

  @Test
  fun `a name with no usable characters falls back`() {
    assertEquals("establishment", Slugs.from("!!"))
  }
}
