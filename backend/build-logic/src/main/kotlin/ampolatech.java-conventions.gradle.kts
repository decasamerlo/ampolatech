import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

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

// A failed test prints its whole message, such as the classes that break an ArchitectureTest rule,
// under --quiet too: .githooks/pre-push runs the tests quietly, in an export of the commit that it
// deletes with the test report.
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        quiet {
            events(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
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
