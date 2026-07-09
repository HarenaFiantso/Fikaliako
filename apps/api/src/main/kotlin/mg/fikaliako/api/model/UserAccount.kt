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
@Table(name = "users")
class UserAccount(
  @Id
  var id: UUID? = null,

  @Column(name = "display_name", columnDefinition = "text")
  var displayName: String = "",

  @Column(columnDefinition = "text")
  var phone: String = "",

  @Column(name = "phone_verified")
  var phoneVerified: Boolean = false,

  @Column(name = "password_hash", columnDefinition = "text")
  var passwordHash: String = "",

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "user_role")
  var role: UserRole = UserRole.user,

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "account_status")
  var status: AccountStatus = AccountStatus.active,

  @Column(columnDefinition = "text")
  var locale: String = "fr",

  @Column(name = "created_at")
  var createdAt: Instant? = null,

  @Column(name = "updated_at")
  var updatedAt: Instant? = null,
)
