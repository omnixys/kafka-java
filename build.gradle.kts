plugins {
    java
    `java-library`
    `maven-publish`
    id("org.springframework.boot") version "4.0.4" apply false
    id("io.spring.dependency-management") version "1.1.7"
}


group = project.findProperty("group") as String
version = project.findProperty("version") as String
description = "Omnixys Kafka Spring Package"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    // mavenLocal()
    mavenCentral()

    maven {
        name = "GitHubObservability"
        url = uri("https://maven.pkg.github.com/omnixys/observability-java")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
                ?: project.findProperty("gpr.user") as String?
                        ?: ""
            password = System.getenv("GITHUB_TOKEN")
                ?: project.findProperty("gpr.key") as String?
                        ?: ""
        }
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.4")
    }
}

dependencies {
    api("com.omnixys.observability:omnixys-observability:1.0.0")

    api("org.springframework.kafka:spring-kafka")
    api("org.springframework.boot:spring-boot-autoconfigure")


    implementation("com.fasterxml.jackson.core:jackson-databind")

    implementation("org.slf4j:slf4j-api")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

tasks.withType<JavaCompile> {
    options.release.set(25)
}

/**
 * 🔥 Ensure version is visible in build logs
 */
tasks.register("printVersion") {
    doLast {
        println("🚀 Building version: $version")
    }
}

tasks.register("setVersion") {
    doLast {
        val newVersion = project.findProperty("newVersion") as String
        val file = file("gradle.properties")

        val updated = file.readLines().map {
            if (it.startsWith("version=")) {
                "version=$newVersion"
            } else it
        }

        file.writeText(updated.joinToString("\n"))

        println("✅ Version updated to $newVersion")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = project.group.toString()
            artifactId = "omnixys-kafka"
            version = project.version.toString()

            pom {
                name.set("Omnixys Kafka")
                description.set("Omnixys Kafka Spring Integration Package")
                url.set("https://github.com/omnixys/kafka-java")

                licenses {
                    license {
                        name.set("GNU General Public License v3.0 or later")
                        url.set("https://www.gnu.org/licenses/gpl-3.0-standalone.html")
                    }
                }

                developers {
                    developer {
                        id.set("caleb-gyamfi")
                        name.set("Caleb Gyamfi")
                        email.set("caleb-g@omnixys.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/omnixys/kafka-java.git")
                    developerConnection.set("scm:git:ssh://github.com:omnixys/kafka-java.git")
                    url.set("https://github.com/omnixys/kafka-java")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"

            url = uri("https://maven.pkg.github.com/omnixys/kafka-java")

            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: project.findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN")
                    ?: project.findProperty("gpr.key") as String?
            }
        }
    }
}