package mg.fikaliako.api.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import mg.fikaliako.api.model.Establishment
import mg.fikaliako.api.model.UserAccount
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reviews")
class Review(
  @Id
  var id: UUID? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "establishment_id")
  var establishment: Establishment? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  var author: UserAccount? = null,

  @Column(name = "rating_quality")
  var ratingQuality: Short = 0,

  @Column(name = "rating_price")
  var ratingPrice: Short = 0,

  @Column(name = "rating_cleanliness")
  var ratingCleanliness: Short = 0,

  @Column(name = "rating_speed")
  var ratingSpeed: Short = 0,

  @Column(name = "rating_welcome")
  var ratingWelcome: Short = 0,

  @Column(name = "global_note", insertable = false, updatable = false)
  var globalNote: BigDecimal = BigDecimal.ZERO,

  @Column(columnDefinition = "text")
  var comment: String? = null,

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "review_status")
  var status: ReviewStatus = ReviewStatus.PUBLISHED,

  // set by the service on insert so the freshly created review can be echoed
  // without a DB round-trip; never touched on update
  @Column(name = "created_at", updatable = false)
  var createdAt: Instant? = null,

  @Column(name = "updated_at", insertable = false, updatable = false)
  var updatedAt: Instant? = null,
)
