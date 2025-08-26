import org.jetbrains.dokka.gradle.tasks.DokkaGeneratePublicationTask
import java.net.URI

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka.core)
  alias(libs.plugins.dokka.javadoc)
  `java-library`
  `maven-publish`
  signing
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(libs.zjsonpatch)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
}

testing {
  suites {
    @Suppress("UnstableApiUsage")
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}



java {
  withJavadocJar()
  withSourcesJar()

  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

tasks.named<Jar>("javadocJar") {
  val dokkaJavadoc = tasks.named<DokkaGeneratePublicationTask>("dokkaGeneratePublicationJavadoc")
  dependsOn(dokkaJavadoc)
  from(dokkaJavadoc.flatMap { it.outputDirectory })
}

object ProjectInfo {
  const val GROUP = "io.github.lbenedetto"
  const val VERSION = "1.0.1"
}

group = ProjectInfo.GROUP
version = ProjectInfo.VERSION

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])

      groupId = ProjectInfo.GROUP
      artifactId = "json-schema-diff-validator"
      version = ProjectInfo.VERSION

      pom {
        name.set("json-schema-diff-validator")
        description.set("Detects breaking changes between two versions of a json schema")
        url.set("https://github.com/lbenedetto/json-schema-diff-validator")

        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }

        developers {
          developer {
            id.set("lbenedetto")
            name.set("Lars Benedetto")
            email.set("larsbenedetto@gmail.com")
          }
        }

        scm {
          connection.set("scm:git@github.com:lbenedetto/json-schema-diff-validator")
          developerConnection.set("scm:git@github.com:lbenedetto/json-schema-diff-validator.git")
          url.set("https://github.com/lbenedetto/json-schema-diff-validator.git")
        }
      }
    }
  }

  repositories {
    maven {
      name = "OSSRH"
      url = URI.create("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")

      credentials {
        username = System.getenv("OSSRH_USERNAME")
        password = System.getenv("OSSRH_PASSWORD")
      }
    }
  }
}

signing {
  val signingKey = System.getenv("SIGNING_KEY")
  val signingPassword = System.getenv("SIGNING_PASSWORD")

  if (signingKey == null || signingPassword == null) {
    logger.warn("No SIGNING_KEY or SIGNING_PASSWORD found, skipping signing!")
    return@signing
  }

  useInMemoryPgpKeys(signingKey, signingPassword)
  sign(publishing.publications["mavenJava"])
}

// Ensure produced JARs (main, sources, javadoc) are named "json-schema-diff-validator-<version>.jar"
// instead of defaulting to the subproject name "lib".
// This affects jar, sourcesJar, and javadocJar tasks (all of type Jar).
tasks.withType<Jar>().configureEach {
  archiveBaseName.set("json-schema-diff-validator")
}
