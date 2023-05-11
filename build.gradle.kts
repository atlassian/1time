import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.8.21"
  id("org.jetbrains.dokka") version "1.8.10"
  id("maven-publish")
  id("signing")
  application
  id("org.jlleitschuh.gradle.ktlint") version "11.3.2"
}

repositories {
  mavenCentral()
}

java {
  withSourcesJar()
  withJavadocJar()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.21")
  implementation("commons-codec:commons-codec:1.15")
  testImplementation(kotlin("test"))
  testImplementation("io.mockk:mockk:1.13.5")
  testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.5")
  testImplementation("io.kotest:kotest-framework-datatest:5.5.5")
  testImplementation("io.kotest:kotest-runner-junit5-jvm:5.6.2")
  testImplementation("io.kotest:kotest-property-jvm:5.5.5")
  testImplementation("io.kotest.extensions:kotest-property-arrow-jvm:1.3.1")
  testImplementation("io.kotest.extensions:kotest-assertions-arrow-jvm:1.3.1")
}

group = "com.atlassian"
version = "1.0.1"
description = "onetime"

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    jvmTarget = "11"
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
