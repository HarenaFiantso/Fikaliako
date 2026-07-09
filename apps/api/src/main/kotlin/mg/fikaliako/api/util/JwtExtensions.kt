package mg.fikaliako.api.util

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

fun Jwt.userId(): UUID = UUID.fromString(subject)
