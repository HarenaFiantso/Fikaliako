package mg.fikaliako.api.util

import java.security.MessageDigest

object Hashing {
  fun sha256Hex(value: String): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(value.toByteArray())
      .joinToString("") { "%02x".format(it) }
}
