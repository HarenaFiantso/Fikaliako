package mg.fikaliako.api.repository
import mg.fikaliako.api.model.OtpPurpose
import mg.fikaliako.api.model.PhoneOtp
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface PhoneOtpRepository : JpaRepository<PhoneOtp, UUID> {
  fun findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
    phone: String,
    purpose: OtpPurpose,
  ): PhoneOtp?

  // Backs the 5-codes/hour/number issuance limit (book ch. 7.3)
  fun countByPhoneAndPurposeAndCreatedAtAfter(
    phone: String,
    purpose: OtpPurpose,
    after: Instant,
  ): Long
}
