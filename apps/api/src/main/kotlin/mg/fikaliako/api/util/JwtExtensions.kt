package mg.fikaliako.api.util

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

// Access tokens carry the user id as `sub` (see TokenService)
fun Jwt.userId(): UUID = UUID.fromString(subject)
