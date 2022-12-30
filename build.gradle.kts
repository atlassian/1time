
plugins {
  kotlin("jvm") version "1.7.22"
  id("org.jetbrains.dokka") version "1.7.10"
  id("com.jfrog.artifactory") version "4.30.1"
  id("maven-publish")
  id("signing")
  application
}

repositories {
  mavenCentral()
}

java {
  withSourcesJar()
  withJavadocJar()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20")
    implementation("commons-codec:commons-codec:1.15")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.2")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.4.2")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.4")
    testImplementation("io.kotest:kotest-property-jvm:5.4.2")
    testImplementation("io.kotest.extensions:kotest-property-arrow-jvm:1.2.5")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow-jvm:1.2.5")
}

group = "com.atlassian"
version = "1.0.0"
description = "onetime"
java.sourceCompatibility = JavaVersion.VERSION_1_10


tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
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
          url.set("https://github.com/atlassian-labs/1time")
          scm {
            connection.set("git@github.com:atlassian-labs/1time.git")
            url.set("https://github.com/atlassian-labs/1time.git")
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
  }

  signing {
    useInMemoryPgpKeys(
      System.getenv("SIGNING_KEY"),
      System.getenv("SIGNING_PASSWORD"),
    )
    sign(publishing.publications["release"])
  }
}

artifactory {
  publish {
    setContextUrl("https://packages.atlassian.com/")

    repository {
      setRepoKey("maven-central")
      setUsername(System.getenv("ARTIFACTORY_USERNAME"))
      setPassword(System.getenv("ARTIFACTORY_API_KEY"))
    }
    defaults {
      publications("release")
      setPublishIvy(false)
      clientConfig.publisher.isPublishBuildInfo = false
    }
  }
}
