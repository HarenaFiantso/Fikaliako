package mg.fikaliako.api.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.locationtech.jts.geom.Point
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "establishments")
class Establishment(
  @Id
  var id: UUID? = null,

  @Column(columnDefinition = "text")
  var name: String = "",

  @Column(columnDefinition = "text")
  var slug: String = "",

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "establishment_type")
  var type: EstablishmentType = EstablishmentType.RESTAURANT,

  @JdbcTypeCode(SqlTypes.GEOGRAPHY)
  @Column(columnDefinition = "geography(Point,4326)")
  var position: Point? = null,

  @Column(columnDefinition = "text")
  var address: String? = null,

  @Column(columnDefinition = "text")
  var district: String? = null,

  @Column(columnDefinition = "text")
  var city: String = "Antananarivo",

  @Column(columnDefinition = "text")
  var phone: String? = null,

  @Column(columnDefinition = "text")
  var whatsapp: String? = null,

  @Column(name = "facebook_url", columnDefinition = "text")
  var facebookUrl: String? = null,

  @Column(columnDefinition = "text")
  var website: String? = null,

  @Column(name = "avg_price_ar")
  var avgPriceAr: Int? = null,
  var verified: Boolean = false,

  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(columnDefinition = "establishment_status")
  var status: EstablishmentStatus = EstablishmentStatus.PENDING,
  var delivery: Boolean = false,
  var parking: Boolean = false,
  var wifi: Boolean = false,

  @Column(name = "wheelchair_access")
  var wheelchairAccess: Boolean = false,

  @Column(name = "air_conditioning")
  var airConditioning: Boolean = false,
  var terrace: Boolean = false,

  @Column(name = "family_friendly")
  var familyFriendly: Boolean = false,
  var romantic: Boolean = false,

  @Column(name = "student_friendly")
  var studentFriendly: Boolean = false,

  @Column(name = "scenic_view")
  var scenicView: Boolean = false,

  @Column(name = "open_24h")
  var open24h: Boolean = false,

  @Column(name = "created_at", insertable = false, updatable = false)
  var createdAt: Instant? = null,

  @Column(name = "updated_at", insertable = false, updatable = false)
  var updatedAt: Instant? = null,

  // cascade + orphanRemoval: the business profile PUT replaces the whole set
  @OneToMany(mappedBy = "establishment", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  @OrderBy("dayOfWeek, opensAt")
  var openingHours: MutableList<OpeningHoursEntity> = mutableListOf(),

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "establishment_payment_methods",
    joinColumns = [JoinColumn(name = "establishment_id")],
    inverseJoinColumns = [JoinColumn(name = "payment_method_code", referencedColumnName = "code")],
  )
  @OrderBy("sortOrder")
  var paymentMethods: MutableSet<PaymentMethod> = mutableSetOf(),

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
    name = "establishment_cuisines",
    joinColumns = [JoinColumn(name = "establishment_id")],
    inverseJoinColumns = [JoinColumn(name = "cuisine_code", referencedColumnName = "code")],
  )
  @OrderBy("sortOrder")
  var cuisines: MutableSet<Cuisine> = mutableSetOf(),
)
