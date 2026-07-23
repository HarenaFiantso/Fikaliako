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
@Table(name = "contributions")
class Contribution(
  @Id
  var id: UUID? = null,

  @Column(name = "establishment_id")
  var establishmentId: UUID? = null,

  @Column(name = "author_id")
  var authorId: UUID? = null,

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "contribution_type")
  var type: ContributionType = ContributionType.create,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  var payload: String = "{}",

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "contribution_status")
  var status: ContributionStatus = ContributionStatus.pending,

  @Column(name = "confirmations_count")
  var confirmationsCount: Short = 0,

  @Column(name = "resolved_by")
  var resolvedBy: UUID? = null,

  @Column(name = "resolved_at")
  var resolvedAt: Instant? = null,

  @Column(name = "created_at")
  var createdAt: Instant? = null,
)
