package mg.fikaliako.api.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_log")
class AuditLogEntry(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @Column(name = "actor_id")
  var actorId: UUID? = null,

  @Column(columnDefinition = "text")
  var action: String = "",

  @Column(name = "entity_type", columnDefinition = "text")
  var entityType: String = "",

  @Column(name = "entity_id")
  var entityId: UUID? = null,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  var details: String? = null,

  @Column(name = "created_at")
  var createdAt: Instant? = null,
)
