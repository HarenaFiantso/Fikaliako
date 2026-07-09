package mg.fikaliako.api.service
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// Outbound SMS port. The MVP target is a local Malagasy SMS gateway
// (book ch. 7.2); until that adapter lands, the logging implementation makes
// the OTP flows fully exercisable in development.
fun interface SmsSender {
  fun send(
    phone: String,
    message: String,
  )
}

// Dev-only adapter: prints the SMS (including OTP codes) to the application
// log. Must be replaced by the gateway adapter before any real deployment.
@Component
class LoggingSmsSender : SmsSender {
  private val log = LoggerFactory.getLogger(javaClass)

  override fun send(
    phone: String,
    message: String,
  ) {
    log.info("SMS to {}: {}", phone, message)
  }
}
