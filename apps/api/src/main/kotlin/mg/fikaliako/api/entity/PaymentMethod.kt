package mg.fikaliako.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "payment_methods")
class PaymentMethod(
  @Id
  @Column(columnDefinition = "text")
  var code: String = "",

  @Column(name = "label_fr", columnDefinition = "text")
  var labelFr: String = "",

  @Column(name = "label_mg", columnDefinition = "text")
  var labelMg: String = "",

  @Column(name = "is_mobile_money")
  var isMobileMoney: Boolean = false,

  @Column(name = "sort_order")
  var sortOrder: Short = 0,
)
