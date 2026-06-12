plugins {
    `java-library`
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.nyora.hasan72341"
version = "1.1.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("io.ktor:ktor-client-core:3.0.1")
    api("io.ktor:ktor-client-cio:3.0.1")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

java {
    withSourcesJar()
}
