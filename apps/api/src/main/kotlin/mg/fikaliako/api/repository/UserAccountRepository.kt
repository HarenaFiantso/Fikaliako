package mg.fikaliako.api.repository
import mg.fikaliako.api.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAccountRepository : JpaRepository<UserAccount, UUID> {
  fun findByPhone(phone: String): UserAccount?

  fun existsByPhone(phone: String): Boolean
}
