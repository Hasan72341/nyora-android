plugins { `kotlin-dsl` }
repositories { google(); mavenCentral() }
dependencies {
    compileOnly("com.android.tools.build:gradle-api:9.1.1")
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-commons:9.8")
}
gradlePlugin {
    plugins {
        register("domainObfuscation") {
            id = "nyora.domain-obfuscation"
            implementationClass = "com.nyora.buildlogic.DomainObfuscationPlugin"
        }
    }
}
