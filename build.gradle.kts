import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.22"
  id("org.jetbrains.dokka") version "1.8.20"
  id("maven-publish")
  id("signing")
  application
  id("org.jlleitschuh.gradle.ktlint") version "11.5.0"
}

repositories {
  mavenCentral()
}

java {
  withSourcesJar()
  withJavadocJar()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
  implementation("commons-codec:commons-codec:1.16.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
  testImplementation(kotlin("test"))
  testImplementation("io.mockk:mockk:1.13.5")
  testImplementation("io.kotest:kotest-assertions-core:5.6.2")
  testImplementation("io.kotest:kotest-framework-datatest:5.6.2")
  testImplementation("io.kotest:kotest-runner-junit5:5.6.2")
  testImplementation("io.kotest:kotest-property:5.6.2")
  testImplementation("io.kotest.extensions:kotest-property-arrow:1.3.3")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.3.3")
}

group = "com.atlassian"
version = "2.0.1"
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
    freeCompilerArgs += listOf(
      "-progressive",
      "-java-parameters",
      "-opt-in=kotlin.time.ExperimentalTime",
      "-opt-in=kotlin.RequiresOptIn"
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
      System.getenv("SIGNING_PASSWORD")
    )
    sign(publishing.publications["release"])
  }
}
