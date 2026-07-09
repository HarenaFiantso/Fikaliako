package mg.fikaliako.api.service
import mg.fikaliako.api.config.AuthProperties
import mg.fikaliako.api.model.OtpPurpose
import mg.fikaliako.api.model.PhoneOtp
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.TooManyRequestsException
import mg.fikaliako.api.repository.PhoneOtpRepository
import mg.fikaliako.api.util.Hashing
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.UUID

// One-time SMS codes (book ch. 4.7): 6 digits, short-lived, stored hashed.
// Issuance is capped at otp-max-per-hour per number (ch. 7.3) and each code
// allows otp-max-attempts guesses before it dies.
@Service
class OtpService(
  private val repository: PhoneOtpRepository,
  private val smsSender: SmsSender,
  private val props: AuthProperties,
  private val clock: Clock,
) {
  @Transactional
  fun issue(
    phone: String,
    purpose: OtpPurpose,
  ) {
    val now = clock.instant()
    val sentLastHour = repository.countByPhoneAndPurposeAndCreatedAtAfter(phone, purpose, now.minus(Duration.ofHours(1)))
    if (sentLastHour >= props.otpMaxPerHour) {
      throw TooManyRequestsException("Too many codes requested for this number. Try again later.")
    }
    val code = "%06d".format(secureRandom.nextInt(1_000_000))
    repository.save(
      PhoneOtp(
        id = UUID.randomUUID(),
        phone = phone,
        purpose = purpose,
        codeHash = hash(phone, purpose, code),
        expiresAt = now.plus(props.otpTtl),
        createdAt = now,
      ),
    )
    smsSender.send(phone, "Fikaliako: code $code (valide ${props.otpTtl.toMinutes()} min). Aza zaraina.")
  }

  // Consumes the latest live code for (phone, purpose); one failure message on
  // purpose, so a guesser learns nothing about which check failed.
  @Transactional
  fun verify(
    phone: String,
    purpose: OtpPurpose,
    code: String,
  ) {
    val otp =
      repository.findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, purpose)
        ?: throw BadRequestException(INVALID_CODE)
    val now = clock.instant()
    if (requireNotNull(otp.expiresAt) <= now || otp.attempts >= props.otpMaxAttempts) {
      throw BadRequestException(INVALID_CODE)
    }
    otp.attempts++
    if (otp.codeHash != hash(phone, purpose, code)) {
      repository.save(otp)
      throw BadRequestException(INVALID_CODE)
    }
    otp.consumedAt = now
    repository.save(otp)
  }

  private fun hash(
    phone: String,
    purpose: OtpPurpose,
    code: String,
  ) = Hashing.sha256Hex("$phone:$purpose:$code")

  companion object {
    const val INVALID_CODE = "Invalid or expired code."
    private val secureRandom = SecureRandom()
  }
}
