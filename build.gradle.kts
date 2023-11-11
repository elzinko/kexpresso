import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
//    id("org.jetbrains.kotlin.jvm") version "1.8.20"
    kotlin("jvm") version "1.8.20"
}

group = "kexpresso"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.google.guava:guava:31.1-jre")
}


tasks.test {
    useJUnitPlatform()
}

kotlin { // Extension for easy setup
    jvmToolchain(18) // Target version of generated JVM bytecode. See 7️⃣
}