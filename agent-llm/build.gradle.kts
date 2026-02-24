plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}

group = "com.agent"
version = "0.0.1"


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.7")
    implementation("org.springframework:spring-webflux:6.2.12")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jsoup:jsoup:1.22.1")
}

tasks.test {
    useJUnitPlatform()
}
