plugins {
    id("ampolatech.java-conventions")
}

dependencies {
    implementation(project(":domain"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.liquibase)
    runtimeOnly(libs.postgresql)
}
