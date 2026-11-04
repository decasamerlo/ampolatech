plugins {
    id("ampolatech.java-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.openapi.generator)
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.liquibase)
    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.resttestclient)
    testImplementation(libs.spring.jdbc)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.archunit.junit6)
    testImplementation(libs.openapi.request.validator.spring.web.client)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// The web adapter's interfaces and models, generated from the API's spec (ADR 0006). The
// packages sit under the adapter's, so ArchitectureTest holds the generated code to its rules.
openApiGenerate {
    generatorName = "spring"
    inputSpec = rootProject.file("../api/openapi.yaml")
    outputDir = layout.buildDirectory.dir("generated/openapi")
    // Otherwise a model the spec drops stays in outputDir, and code that uses it still compiles.
    cleanupOutput = true
    apiPackage = "dev.ampolatech.adapter.in.web.rest.api"
    modelPackage = "dev.ampolatech.adapter.in.web.rest.model"
    // The suffix keeps a model from sharing a simple name with a domain type, such as Role.
    modelNameSuffix = "Dto"
    // Only the APIs, the models and their package-info. The generator's other supporting files
    // are an ApiUtil the interfaces don't call and a Spring @Configuration of enum converters,
    // which it writes to org.openapitools.configuration: outside dev.ampolatech, where neither
    // the app's component scan nor ArchitectureTest reaches.
    globalProperties = mapOf("apis" to "", "models" to "", "supportingFiles" to "package-info.java")
    configOptions = mapOf(
        "interfaceOnly" to "true",
        "useSpringBoot4" to "true",
        "useJackson3" to "true",
        // ADR 0006 says why: a refused parameter answers a 400 problem, and an operation the
        // spec adds fails the compile until a controller implements it.
        "useSpringBuiltInValidation" to "true",
        "skipDefaultInterface" to "true",
        "useJspecify" to "true",
        "useTags" to "true",
        "useRecords" to "true",
        "documentationProvider" to "none",
        "annotationLibrary" to "none",
        "openApiNullable" to "false",
        "hideGenerationTimestamp" to "true",
        // A null optional property is left out of a body the app writes, where its schema would
        // refuse null.
        "generateJsonIncludeAnnotations" to "true",
        // An explicit null for an optional property in a body the app reads leaves it unset.
        "generateJsonSetterNullsAnnotations" to "true",
    )
}

sourceSets.main {
    java.srcDir(tasks.openApiGenerate.flatMap { it.outputDir.dir("src/main/java") })
}

// Puts postgres/initdb on the test classpath, so the tests set up their database
// with the real init script, and api/openapi.yaml, so the HTTP-seam tests check
// every exchange against it.
sourceSets.test {
    resources.srcDir(rootProject.file("postgres"))
    resources.srcDir(rootProject.file("../api"))
}

// The Dockerfile copies the executable jar by this name, and nothing uses the plain jar.
tasks.bootJar {
    archiveFileName = "ampolatech.jar"
}

tasks.jar {
    enabled = false
}
