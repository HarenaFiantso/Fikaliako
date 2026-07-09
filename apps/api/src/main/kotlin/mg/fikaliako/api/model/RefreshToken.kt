package mg.fikaliako.api.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
  @Id
  var id: UUID? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  var user: UserAccount? = null,

  @Column(name = "token_hash", columnDefinition = "text")
  var tokenHash: String = "",

  @Column(name = "family_id")
  var familyId: UUID? = null,

  @Column(name = "issued_at")
  var issuedAt: Instant? = null,

  @Column(name = "expires_at")
  var expiresAt: Instant? = null,

  @Column(name = "revoked_at")
  var revokedAt: Instant? = null,
)
