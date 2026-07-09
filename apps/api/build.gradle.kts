plugins {
  kotlin("jvm") version "2.3.21"
  kotlin("plugin.spring") version "2.3.21"
  id("org.springframework.boot") version "4.1.0"
  id("io.spring.dependency-management") version "1.1.7"
  kotlin("plugin.jpa") version "2.3.21"
  id("com.diffplug.spotless") version "8.8.0"
  jacoco
}

group = "mg.fikaliako"
version = "0.0.1-SNAPSHOT"
description = "Fikaliako API — plateforme de decouverte culinaire geolocalisee"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.flywaydb:flyway-database-postgresql")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("tools.jackson.module:jackson-module-kotlin")
  implementation("org.hibernate.orm:hibernate-spatial")
  runtimeOnly("org.webjars:swagger-ui:5.25.3")
  runtimeOnly("org.webjars:webjars-locator-lite")
  developmentOnly("org.springframework.boot:spring-boot-docker-compose")
  runtimeOnly("org.postgresql:postgresql")
  testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
  testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
  testImplementation("org.springframework.boot:spring-boot-starter-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("com.h2database:h2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}

allOpen {
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
  annotation("jakarta.persistence.Embeddable")
}

// Formatting: ktlint via Spotless — `spotlessCheck` is wired into `check`,
// so `build` fails on unformatted code; fix with `spotlessApply` (pnpm format)
spotless {
  // ktlint config. Spotless does not reliably resolve the repo-root .editorconfig
  // for these sources, so the code style is pinned here. `no-blank-line-in-list`
  // is disabled to permit a blank line between attributes/properties (e.g.
  // between JPA-annotated entity fields), which the rule would otherwise strip.
  val ktlintConfig =
    mapOf(
      "ktlint_code_style" to "ktlint_official",
      "indent_size" to "2",
      "max_line_length" to "off",
      "ktlint_standard_no-blank-line-in-list" to "disabled",
    )
  kotlin {
    ktlint().editorConfigOverride(ktlintConfig)
  }
  kotlinGradle {
    ktlint().editorConfigOverride(ktlintConfig)
  }
  format("misc") {
    target("src/**/*.yml", "src/**/*.sql")
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
  // compose.yaml lives at the monorepo root, two levels above this Gradle project
  systemProperty(
    "spring.docker.compose.file",
    projectDir.parentFile.parentFile
      .resolve("compose.yaml")
      .absolutePath,
  )
}
