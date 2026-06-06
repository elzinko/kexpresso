plugins {
    // No version here: the Kotlin plugin is already on the classpath from the root project
    // (root applies kotlin("multiplatform") 1.9.24, which puts the Kotlin plugin jar in the
    // build script classpath for all subprojects). Specifying a version here conflicts.
    kotlin("jvm")
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // The root project is a KMP library; Gradle's attribute-based variant selection picks
    // the JVM variant automatically via the KMP Gradle metadata.
    implementation(project(":"))
}

application {
    mainClass.set("kexpresso.samples.MainKt")
}
