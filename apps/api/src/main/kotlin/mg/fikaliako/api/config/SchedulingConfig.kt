package mg.fikaliako.api.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** Enables the nightly jobs (rating aggregation — project book ch. 4.6). */
@Configuration
@EnableScheduling
class SchedulingConfig
