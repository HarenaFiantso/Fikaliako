package mg.fikaliako.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * A review (project book ch. 4.4): five 1–5 criteria. `global_note` is the
 * DB-generated weighted mean (quality ×2), so it is read-only here.
 */
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
    var status: ReviewStatus = ReviewStatus.published,
    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: Instant? = null,
    @Column(name = "updated_at", insertable = false, updatable = false)
    var updatedAt: Instant? = null,
)
