plugins {
    kotlin("jvm") version "1.8.20"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jetbrains.dokka") version "2.2.0"
    id("me.champeau.jmh") version "0.7.2"
    jacoco
    `java-library`
    `maven-publish`
}

group = "com.github.elzinko"

// Version is overridable from the release pipeline via `-PreleaseVersion=<tag>`,
// and defaults to the in-development version otherwise.
version = (findProperty("releaseVersion") as String?) ?: "0.3.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// Coverage gate: fail the build if line coverage regresses below the floor.
// Enforces quality natively (no third-party service). Current coverage is ~99%.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.85".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

// A javadoc jar built from Dokka so published artifacts carry browsable API docs.
val dokkaJavadocJar by tasks.registering(Jar::class) {
    group = "documentation"
    description = "Assembles a javadoc jar from the Dokka HTML output."
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

// ── JMH benchmarks ──────────────────────────────────────────────────────────
jmh {
    warmupIterations.set(2)
    iterations.set(3)
    fork.set(1)
    timeOnIteration.set("1s")
    warmup.set("1s")
}

// Exclude the JMH source set from Detekt so benchmark boilerplate doesn't
// fail the style checks that are applied to production code.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/jmh/**")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(dokkaJavadocJar)
            artifactId = "kexpresso"
            pom {
                name.set("Kexpresso")
                description.set("A fluent Kotlin DSL that makes regular expressions readable.")
                url.set("https://github.com/elzinko/kexpresso")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("elzinko")
                        name.set("Thomas Couderc")
                    }
                }
                scm {
                    url.set("https://github.com/elzinko/kexpresso")
                    connection.set("scm:git:git://github.com/elzinko/kexpresso.git")
                    developerConnection.set("scm:git:ssh://git@github.com/elzinko/kexpresso.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/elzinko/kexpresso")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
