package mg.fikaliako.api.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * Account (project book ch. 7.3). Only the fields the read layer needs are
 * mapped — Hibernate validation checks only mapped columns, so the auth-only
 * columns (phone, password_hash, …) are intentionally omitted until the
 * accounts module lands.
 */
@Entity
@Table(name = "users")
class UserAccount(
    @Id
    var id: UUID? = null,
    @Column(name = "display_name", columnDefinition = "text")
    var displayName: String = "",
)
