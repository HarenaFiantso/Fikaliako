package mg.fikaliako.api.service
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

fun interface SmsSender {
  fun send(
    phone: String,
    message: String,
  )
}

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
