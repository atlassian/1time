
@Suppress("DSL_SCOPE_VIOLATION") // intellij bug https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
  kotlin("jvm") version "1.6.21"
  application
  `maven-publish`
}

repositories {
  mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
    implementation("commons-codec:commons-codec:1.15")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.12.4")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.2.2")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.2.2")
    testImplementation("io.kotest:kotest-property-jvm:5.2.2")
    testImplementation("io.kotest.extensions:kotest-property-arrow-jvm:1.2.5")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow-jvm:1.2.4")
}

group = "io.atlassian.authentication"
version = "1.0.0"
description = "1time"
java.sourceCompatibility = JavaVersion.VERSION_1_10


tasks.test {
  useJUnitPlatform()
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

publishing {

  publications {
    create<MavenPublication>("default") {
      from(components["java"])
    }
  }

  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/atlassian-labs/1time")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
}
