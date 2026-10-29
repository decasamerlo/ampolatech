plugins {
    id("ampolatech.java-conventions")
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":application"))
    implementation(project(":persistence"))
    implementation(project(":rest"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.resttestclient)
    testImplementation(libs.spring.jdbc)
    testImplementation(libs.testcontainers.postgresql)
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
