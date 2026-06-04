plugins {
    kotlin("multiplatform") version "1.9.24"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jetbrains.dokka") version "1.9.20"
    id("org.jetbrains.kotlinx.kover") version "0.7.6"
    `maven-publish`
}

group = "com.github.elzinko"

// Version is overridable from the release pipeline via `-PreleaseVersion=<tag>`,
// and defaults to the in-development version otherwise.
version = (findProperty("releaseVersion") as String?) ?: "0.4.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)

    jvm {
        withSourcesJar(publish = true)
    }

    js(IR) {
        nodejs()
    }

    sourceSets {
        commonMain {}
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmMain {}
        jvmTest {}
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

// Run Detekt over the Kotlin Multiplatform source sets. Under KMP, the detekt plugin registers
// per-source-set tasks (detektMetadataCommonMain, detektJvmMain, detektJsMain, …) but the plain
// `detekt` task only scans the legacy src/main + src/test dirs (which no longer exist). We make
// the aggregate `detekt` task fan out to the per-source-set tasks so `./gradlew build` (via
// `check`) enforces style on all production + test code across both targets.
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    exclude("**/jsMain/**", "**/jsTest/**") // analysed via the metadata/jvm tasks already
}

val detektSourceSetTasks = listOf(
    "detektMetadataCommonMain",
    "detektJvmMain",
    "detektJvmTest",
)

tasks.named("detekt") {
    dependsOn(detektSourceSetTasks)
}

tasks.named("check") {
    dependsOn(detektSourceSetTasks)
}

// ── Coverage gate (Kover) ─────────────────────────────────────────────────────
// Replaces the former JaCoCo gate. Kover instruments the JVM tests and enforces a
// line-coverage floor; an XML report is produced for Codecov.
koverReport {
    defaults {
        xml {
            onCheck = true
        }
        html {
            onCheck = false
        }
        verify {
            rule {
                bound {
                    minValue = 85
                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
                    aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                }
            }
        }
    }
}

// Enforce the coverage gate as part of `check` (and therefore `build`).
tasks.named("check") {
    dependsOn(tasks.named("koverVerify"))
}

// A javadoc/html jar built from Dokka so published artifacts carry browsable API docs.
val dokkaHtmlJar by tasks.registering(Jar::class) {
    group = "documentation"
    description = "Assembles a javadoc jar from the Dokka HTML output."
    dependsOn(tasks.named("dokkaHtml"))
    from(tasks.named("dokkaHtml"))
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        // KMP auto-creates a publication per target (kexpresso, kexpresso-jvm, kexpresso-js)
        // plus the root metadata publication. Apply the shared POM + the Dokka javadoc jar to all.
        withType<MavenPublication>().configureEach {
            artifact(dokkaHtmlJar)
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
