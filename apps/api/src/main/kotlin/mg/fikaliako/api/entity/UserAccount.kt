package mg.fikaliako.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class UserAccount(
  @Id
  var id: UUID? = null,

  @Column(name = "display_name", columnDefinition = "text")
  var displayName: String = "",
)
