package mg.fikaliako.api.endpoint.rest.model

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

// PATCH /v1/users/me — absent fields stay untouched
data class UpdateProfileRequest(
  @field:Size(min = 2, max = 60)
  val displayName: String? = null,

  @field:Pattern(regexp = "^(fr|mg)$", message = "must be 'fr' or 'mg'")
  val locale: String? = null,
)
