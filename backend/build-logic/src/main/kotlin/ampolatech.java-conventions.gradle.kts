plugins {
    `java-library`
    id("com.diffplug.spotless")
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

val catalog = the<VersionCatalogsExtension>().named("libs")

// Each option is google-java-format's own command-line default, so that command line formats
// exactly as the build checks. Spotless defaults reflowLongStrings and reorderImports to false.
spotless {
    java {
        googleJavaFormat(catalog.findVersion("google-java-format").get().requiredVersion)
            .style("GOOGLE")
            .reflowLongStrings(true)
            .reorderImports(true)
            .formatJavadoc(true)
    }
}
