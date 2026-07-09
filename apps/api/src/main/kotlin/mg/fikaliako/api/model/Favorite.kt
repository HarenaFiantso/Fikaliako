package mg.fikaliako.api.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class FavoriteId(
  @Column(name = "user_id")
  val userId: UUID = UUID(0, 0),

  @Column(name = "establishment_id")
  val establishmentId: UUID = UUID(0, 0),
) : Serializable

// N–N user ↔ establishment (book ch. 4.6); PUT/DELETE toggle the row.
@Entity
@Table(name = "favorites")
class Favorite(
  @EmbeddedId
  var id: FavoriteId? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("establishmentId")
  @JoinColumn(name = "establishment_id")
  var establishment: Establishment? = null,

  @Column(name = "created_at")
  var createdAt: Instant? = null,
)
