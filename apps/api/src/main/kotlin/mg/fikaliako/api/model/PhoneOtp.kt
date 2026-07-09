package mg.fikaliako.api.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "phone_otps")
class PhoneOtp(
  @Id
  var id: UUID? = null,

  @Column(columnDefinition = "text")
  var phone: String = "",

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "otp_purpose")
  var purpose: OtpPurpose = OtpPurpose.verify_phone,

  @Column(name = "code_hash", columnDefinition = "text")
  var codeHash: String = "",

  @Column(name = "expires_at")
  var expiresAt: Instant? = null,

  var attempts: Short = 0,

  @Column(name = "consumed_at")
  var consumedAt: Instant? = null,

  @Column(name = "created_at")
  var createdAt: Instant? = null,
)
