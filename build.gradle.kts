import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.dokka)
  id("maven-publish")
  id("signing")
}

repositories {
  mavenCentral()
}

java {
  withSourcesJar()
  withJavadocJar()
}

dependencies {
  implementation(libs.kotlin.stdlib)
  implementation(libs.commonsCodecs)
  implementation(libs.kotlinx.coroutinesCore)

  testImplementation(libs.mockk)
  testImplementation(libs.kotest.frameworkDatatest)
  testImplementation(libs.kotest.runnerJUnit5)
  testImplementation(libs.kotest.property)
}

group = "com.atlassian"
version = "2.0.2"
description = "onetime"

val javaVersion = JavaVersion.VERSION_17

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
  sourceCompatibility = javaVersion.toString()
  targetCompatibility = javaVersion.toString()
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "17"
    freeCompilerArgs +=
      listOf(
        "-progressive",
        "-java-parameters",
        "-opt-in=kotlin.time.ExperimentalTime",
        "-opt-in=kotlin.RequiresOptIn",
      )

    // https://youtrack.jetbrains.com/issue/KTIJ-1224
    // This is really an IDE bug.
    // Without explicit languageVersion the Gradle build does compile 1.5 structures (sealed interfaces). So, for Gradle the value is 1.5.
    // The wrong value is only in IDE settings.
    languageVersion = "1.8"
    apiVersion = "1.8"
  }
}

tasks {

  // This task is added by Gradle when we use java.withJavadocJar()
  named<Jar>("javadocJar") {
    from(dokkaJavadoc)
  }

  test {
    useJUnitPlatform()
  }

  publishing {
    publications {
      create<MavenPublication>("release") {
        from(project.components["java"])
        pom {
          packaging = "jar"
          name.set(project.name)
          description.set("TOTP and HOTP generator and validator for multi-factor authentication")
          url.set("https://github.com/atlassian/1time")
          scm {
            connection.set("git@github.com:atlassian/1time.git")
            url.set("https://github.com/atlassian/1time.git")
          }
          developers {
            developer {
              id.set("docampoherrera")
              name.set("Diego Ocampo")
              email.set("docampoherrera@atlassian.com")
            }
          }
          licenses {
            license {
              name.set("Apache License 2.0")
              url.set("https://www.apache.org/licenses/LICENSE-2.0")
              distribution.set("repo")
            }
          }
        }
      }
    }

    repositories {
      maven {
        url = uri("https://packages.atlassian.com/maven-central")
        credentials {
          username = System.getenv("ARTIFACTORY_USERNAME")
          password = System.getenv("ARTIFACTORY_API_KEY")
        }
      }
    }
  }

  signing {
    useInMemoryPgpKeys(
      System.getenv("SIGNING_KEY"),
      System.getenv("SIGNING_PASSWORD"),
    )
    sign(publishing.publications["release"])
  }
}
