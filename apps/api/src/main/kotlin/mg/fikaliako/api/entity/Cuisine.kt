package mg.fikaliako.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** Cuisine referential — the "craving" axis (project book ch. 3, 4.3). */
@Entity
@Table(name = "cuisines")
class Cuisine(
    @Id
    @Column(columnDefinition = "text")
    var code: String = "",
    @Column(name = "label_fr", columnDefinition = "text")
    var labelFr: String = "",
    @Column(name = "label_mg", columnDefinition = "text")
    var labelMg: String = "",
    @Column(name = "sort_order")
    var sortOrder: Short = 0,
)
