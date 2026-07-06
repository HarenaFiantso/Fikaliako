package mg.fikaliako.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "opening_hours")
class OpeningHoursEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "establishment_id")
  var establishment: Establishment? = null,

  @Column(name = "day_of_week")
  var dayOfWeek: Short = 0,

  @Column(name = "opens_at")
  var opensAt: LocalTime = LocalTime.MIDNIGHT,

  @Column(name = "closes_at")
  var closesAt: LocalTime = LocalTime.MIDNIGHT,
)
