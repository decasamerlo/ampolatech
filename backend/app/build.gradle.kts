plugins {
    id("ampolatech.java-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.liquibase)
    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.resttestclient)
    testImplementation(libs.spring.jdbc)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit6)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Puts postgres/initdb on the test classpath, so the tests set up their database
// with the real init script.
sourceSets.test {
    resources.srcDir(rootProject.file("postgres"))
}

// The Dockerfile copies the executable jar by this name, and nothing uses the plain jar.
tasks.bootJar {
    archiveFileName = "ampolatech.jar"
}

tasks.jar {
    enabled = false
}
