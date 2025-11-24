plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "com.agent"
version = "0.0.1"


dependencies {
    implementation("com.explyt.ai.router:ai-router:v1.45.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
}

tasks.test {
    useJUnitPlatform()
}
