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
data class EstablishmentManagerId(
  @Column(name = "establishment_id")
  val establishmentId: UUID = UUID(0, 0),

  @Column(name = "user_id")
  val userId: UUID = UUID(0, 0),
) : Serializable

// Grants a BUSINESS account management rights over one establishment.
// Created by an admin (audit-logged); the restaurateur onboarding flow of the
// book's V2 will feed this table.
@Entity
@Table(name = "establishment_managers")
class EstablishmentManager(
  @EmbeddedId
  var id: EstablishmentManagerId? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("establishmentId")
  @JoinColumn(name = "establishment_id")
  var establishment: Establishment? = null,

  @Column(name = "granted_by")
  var grantedBy: UUID? = null,

  @Column(name = "created_at")
  var createdAt: Instant? = null,
)
