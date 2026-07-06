package mg.fikaliako.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Denormalised rating snapshot per establishment, recomputed nightly (project
 * book ch. 4.5). Shares the establishment's id as its primary key.
 */
@Entity
@Table(name = "establishment_ratings")
class EstablishmentRating(
    @Id
    @Column(name = "establishment_id")
    var establishmentId: UUID? = null,
    @Column(name = "review_count")
    var reviewCount: Int = 0,
    @Column(name = "avg_quality")
    var avgQuality: BigDecimal? = null,
    @Column(name = "avg_price")
    var avgPrice: BigDecimal? = null,
    @Column(name = "avg_cleanliness")
    var avgCleanliness: BigDecimal? = null,
    @Column(name = "avg_speed")
    var avgSpeed: BigDecimal? = null,
    @Column(name = "avg_welcome")
    var avgWelcome: BigDecimal? = null,
    @Column(name = "avg_global")
    var avgGlobal: BigDecimal? = null,
    @Column(name = "bayesian_note")
    var bayesianNote: BigDecimal? = null,
    @Column(name = "computed_at", insertable = false, updatable = false)
    var computedAt: Instant? = null,
)
