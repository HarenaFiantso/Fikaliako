package mg.fikaliako.api.service
import mg.fikaliako.api.config.AuthProperties
import mg.fikaliako.api.model.OtpPurpose
import mg.fikaliako.api.model.PhoneOtp
import mg.fikaliako.api.model.exception.BadRequestException
import mg.fikaliako.api.model.exception.TooManyRequestsException
import mg.fikaliako.api.repository.PhoneOtpRepository
import mg.fikaliako.api.util.Hashing
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OtpServiceTest {
  private val now = Instant.parse("2026-07-09T12:00:00Z")
  private val props = AuthProperties(jwtSecret = "test-only-fikaliako-jwt-secret-0123456789abcdef")
  private val repository = Mockito.mock(PhoneOtpRepository::class.java)
  private val sentMessages = mutableListOf<String>()
  private val service = OtpService(repository, { _, message -> sentMessages += message }, props, Clock.fixed(now, ZoneOffset.UTC))

  private val phone = "+261340000001"

  private fun storedOtp(
    code: String,
    purpose: OtpPurpose = OtpPurpose.VERIFY_PHONE,
    expiresAt: Instant = now.plusSeconds(600),
    attempts: Short = 0,
  ) = PhoneOtp(
    id = UUID.randomUUID(),
    phone = phone,
    purpose = purpose,
    codeHash = Hashing.sha256Hex("$phone:$purpose:$code"),
    expiresAt = expiresAt,
    attempts = attempts,
    createdAt = now.minusSeconds(60),
  )

  @Test
  fun `issues a hashed six-digit code and sends it by SMS`() {
    Mockito
      .`when`(repository.countByPhoneAndPurposeAndCreatedAtAfter(phone, OtpPurpose.VERIFY_PHONE, now.minusSeconds(3600)))
      .thenReturn(0)

    service.issue(phone, OtpPurpose.VERIFY_PHONE)

    val captor = ArgumentCaptor.forClass(PhoneOtp::class.java)
    Mockito.verify(repository).save(captor.capture())
    val saved = captor.value
    val code = Regex("\\d{6}").find(sentMessages.single())?.value
    assertNotNull(code, "the SMS must carry the 6-digit code")
    assertEquals(Hashing.sha256Hex("$phone:VERIFY_PHONE:$code"), saved.codeHash)
    assertEquals(now.plusSeconds(600), saved.expiresAt)
    assertTrue(saved.codeHash != code, "the raw code must never be persisted")
  }

  @Test
  fun `caps issuance at five codes per hour per number`() {
    Mockito
      .`when`(repository.countByPhoneAndPurposeAndCreatedAtAfter(phone, OtpPurpose.VERIFY_PHONE, now.minusSeconds(3600)))
      .thenReturn(5)

    assertFailsWith<TooManyRequestsException> { service.issue(phone, OtpPurpose.VERIFY_PHONE) }
    Mockito.verify(repository, Mockito.never()).save(Mockito.any())
    assertTrue(sentMessages.isEmpty())
  }

  @Test
  fun `a correct code is consumed`() {
    val otp = storedOtp("123456")
    Mockito
      .`when`(repository.findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, OtpPurpose.VERIFY_PHONE))
      .thenReturn(otp)

    service.verify(phone, OtpPurpose.VERIFY_PHONE, "123456")

    assertEquals(now, otp.consumedAt)
  }

  @Test
  fun `a wrong code burns an attempt and fails`() {
    val otp = storedOtp("123456")
    Mockito
      .`when`(repository.findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, OtpPurpose.VERIFY_PHONE))
      .thenReturn(otp)

    assertFailsWith<BadRequestException> { service.verify(phone, OtpPurpose.VERIFY_PHONE, "000000") }
    assertEquals(1, otp.attempts.toInt())
    assertNull(otp.consumedAt)
  }

  @Test
  fun `an expired code is rejected even if correct`() {
    val otp = storedOtp("123456", expiresAt = now)
    Mockito
      .`when`(repository.findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, OtpPurpose.VERIFY_PHONE))
      .thenReturn(otp)

    assertFailsWith<BadRequestException> { service.verify(phone, OtpPurpose.VERIFY_PHONE, "123456") }
  }

  @Test
  fun `a code with exhausted attempts is dead`() {
    val otp = storedOtp("123456", attempts = 5)
    Mockito
      .`when`(repository.findTopByPhoneAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(phone, OtpPurpose.VERIFY_PHONE))
      .thenReturn(otp)

    assertFailsWith<BadRequestException> { service.verify(phone, OtpPurpose.VERIFY_PHONE, "123456") }
  }

  @Test
  fun `verification without any live code fails`() {
    assertFailsWith<BadRequestException> { service.verify(phone, OtpPurpose.RESET_PASSWORD, "123456") }
  }
}
