import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "1.9.24"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
    id("org.jetbrains.dokka") version "1.9.20"
    id("org.jetbrains.kotlinx.kover") version "0.7.6"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

// Maven Central uses the GitHub-namespace `io.github.<user>` (the legacy `com.github.*`
// coordinate is not accepted on Central). The Vanniktech plugin reads this `group` below.
// Overridable via `-PpublishGroup=` so JitPack can keep installing under its expected
// `com.github.elzinko` coordinate (see jitpack.yml) — JitPack derives its groupId from the
// git host, not from this build, and would otherwise not find the published artifact.
group = (findProperty("publishGroup") as String?) ?: "io.github.elzinko"

// Version is overridable from the release pipeline via `-PreleaseVersion=<tag>`,
// and defaults to the in-development version otherwise.
version = (findProperty("releaseVersion") as String?) ?: "0.6.0"

repositories {
    mavenCentral()
}

/**
 * True only when a *full* Xcode install (not just the Command Line Tools) is present, mirroring
 * the exact probe Kotlin/Native runs for Apple targets. Used to gate macosArm64/macosX64
 * registration so a CLT-only macOS host still produces a green `build`.
 */
fun isFullXcodeAvailable(): Boolean = runCatching {
    val process = ProcessBuilder("/usr/bin/xcrun", "xcodebuild", "-version")
        .redirectErrorStream(true)
        .start()
    process.waitFor() == 0
}.getOrDefault(false)

kotlin {
    jvmToolchain(17)

    jvm {}

    js(IR) {
        nodejs()
    }

    // Wasm (wasmJs) — Alpha in Kotlin 1.9.24. Exercises the Kotlin/Wasm regex engine.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    // Host-conditional Kotlin/Native targets. Each native target can only be cross-compiled
    // from a host of the same family, so we register only the targets the *current* host can
    // build. The default hierarchy template (Kotlin 1.9.20+) wires commonMain/commonTest into
    // every registered target automatically — no manual intermediate source sets required.
    //   • macOS  → macosArm64 + macosX64 (requires a full Xcode install — see guard below)
    //   • Linux  → linuxX64 + mingwX64 (mingw cross-compiles fine from Linux; this is the CI runner)
    //   • Windows→ mingwX64
    val host = OperatingSystem.current()
    when {
        host.isMacOsX -> {
            // Kotlin/Native 1.9.x for Apple targets hard-requires a *full Xcode* install
            // (it shells out to `xcrun xcodebuild -version`). A Command-Line-Tools-only host
            // cannot compile/link Apple binaries, and Kotlin/Native fails the whole build with
            // an opaque "xcrun exit code 72". We therefore register the Apple targets only when
            // Xcode is actually present, so a CLT-only dev box still gets a green `build`
            // (jvm + js + wasmJs). A properly provisioned Mac (and macOS CI) builds the Apple
            // targets normally. Linux/Windows CI never enters this branch.
            if (isFullXcodeAvailable()) {
                // macOS is the most capable Kotlin/Native host: it can cross-build every
                // target. We register the FULL set here so the release (which runs on
                // macos-latest — see .github/workflows/release.yml) publishes the complete,
                // consistent multiplatform metadata from a single host. Apple/iOS targets
                // only ever build here; linuxX64/mingwX64 cross-compile fine from macOS.
                macosArm64()
                macosX64()
                iosArm64()
                iosX64()
                iosSimulatorArm64()
                linuxX64()
                mingwX64()
            } else {
                logger.warn(
                    "Kexpresso: skipping Apple/Native targets — no full Xcode install " +
                        "detected (`xcrun xcodebuild -version` failed). Install Xcode to build " +
                        "the Apple/Native targets locally; jvm/js/wasmJs are unaffected.",
                )
            }
        }
        host.isLinux -> {
            linuxX64()
            mingwX64()
        }
        host.isWindows -> {
            mingwX64()
        }
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

// ── Publishing ────────────────────────────────────────────────────────────────
// The Vanniktech Maven Publish plugin configures all KMP publications for the Sonatype
// Central Portal: it adds the sources + Dokka javadoc jars, applies the POM, wires GPG
// signing, and registers the Central Portal upload tasks (publishToMavenCentral /
// publishAndReleaseToMavenCentral). Signing + Central credentials are read from Gradle
// properties / env (ORG_GRADLE_PROJECT_signingInMemoryKey, …mavenCentralUsername, …).
//
// Signing is credential-OPTIONAL for local builds: `signAllPublications()` makes Gradle's
// signing tasks run for every publication (including `publishToMavenLocal`), and they FAIL
// with "no configured signatory" when no GPG key is present. So we enable signing only when
// a signing key is actually provided. With NO credentials set, `./gradlew build` and
// `./gradlew publishToMavenLocal` succeed (unsigned). In CI the SIGNING_IN_MEMORY_KEY secret
// is mapped to ORG_GRADLE_PROJECT_signingInMemoryKey, so Central publications are signed.
val hasSigningKey = (project.findProperty("signingInMemoryKey") as String?)
    ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if (!hasSigningKey.isNullOrBlank()) {
        signAllPublications()
    }
    coordinates(group.toString(), "kexpresso", version.toString())

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

// Keep the existing GitHub Packages target alongside Maven Central. Vanniktech configures
// the publications (and Central repo); we re-add the GitHubPackages repository so the
// `publish` / `publishAllPublicationsToGitHubPackagesRepository` tasks still work.
// Credentials come from the GITHUB_ACTOR / GITHUB_TOKEN env vars in the release workflow.
publishing {
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
