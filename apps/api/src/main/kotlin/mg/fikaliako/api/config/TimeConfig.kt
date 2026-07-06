package mg.fikaliako.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class TimeConfig {
    /** Injectable clock so time-dependent logic (open/closed) is testable. */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
