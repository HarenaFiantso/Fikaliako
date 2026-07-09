package mg.fikaliako.api.repository
import mg.fikaliako.api.model.AuditLogEntry
import org.springframework.data.jpa.repository.JpaRepository

interface AuditLogRepository : JpaRepository<AuditLogEntry, Long>
