plugins {
    `java-library`
}

group = "dev.ampolatech"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Spring reads method parameter names through reflection, which needs -parameters. Spring Boot's
// plugin adds the flag only to the module that applies it, so every module gets it here.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
