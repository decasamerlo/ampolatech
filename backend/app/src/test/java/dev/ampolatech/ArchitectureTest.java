package dev.ampolatech;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * The boundaries the compiler can't hold (ADR 0007): inside {@code core}, between the domain and
 * the ports and application services beside it, and between them and the JDK, which every module
 * compiles against; inside {@code app}, where every adapter compiles against every library. It also
 * holds every port to an interface.
 *
 * <p>The domain, the ports and the services share an allow-list of the packages their classes use,
 * and each adapter category gets its own, written and grown as docs/conventions/hexagonal.md says.
 */
@AnalyzeClasses(
    packagesOf = AmpolaTechApplication.class,
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  // An adapter layer takes `.*..` after its category, so a class in a package that names no
  // implementation sits in no layer, and the rule fails it as outside the architecture.
  @ArchTest
  static final ArchRule the_layers_should_reach_each_other_only_through_the_ports =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Domain")
          .definedBy("dev.ampolatech.domain..")
          .layer("Ports")
          .definedBy("dev.ampolatech.port..")
          .layer("Application services")
          .definedBy("dev.ampolatech.application..")
          .layer("Web adapters")
          .definedBy("dev.ampolatech.adapter.in.web.*..")
          .layer("Sign-in adapters")
          .definedBy("dev.ampolatech.adapter.in.signin.*..")
          .layer("Persistence adapters")
          .definedBy("dev.ampolatech.adapter.out.persistence.*..")
          .layer("Composition root")
          .definedBy("dev.ampolatech")
          .whereLayer("Domain")
          .mayNotAccessAnyLayer()
          .whereLayer("Application services")
          .mayOnlyBeAccessedByLayers("Composition root")
          .whereLayer("Web adapters")
          .mayOnlyBeAccessedByLayers("Composition root")
          .whereLayer("Sign-in adapters")
          .mayOnlyBeAccessedByLayers("Composition root")
          .whereLayer("Persistence adapters")
          .mayOnlyBeAccessedByLayers("Composition root")
          .ensureAllClassesAreContainedInArchitecture();

  // Every module compiles against the whole JDK, so it is this rule, not core's classpath, that
  // stops a class here from using java.sql, java.net.http or any JDK package beyond the four named.
  @ArchTest
  static final ArchRule the_domain_ports_and_services_should_use_only_their_allowed_packages =
      classes()
          .that()
          .resideInAnyPackage(
              "dev.ampolatech.domain..", "dev.ampolatech.port..", "dev.ampolatech.application..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java.lang..",
              "java.util..",
              "java.time..",
              "java.math..",
              "dev.ampolatech.domain..",
              "dev.ampolatech.port..",
              "dev.ampolatech.application..");

  // A port's Command and Result records nest inside it, so the rule holds only the top-level types.
  @ArchTest
  static final ArchRule ports_should_be_interfaces =
      classes()
          .that()
          .resideInAPackage("dev.ampolatech.port..")
          .and()
          .areTopLevelClasses()
          .should()
          .beInterfaces();

  @ArchTest
  static final ArchRule web_adapters_should_use_only_their_allowed_packages =
      classes()
          .that()
          .resideInAPackage("dev.ampolatech.adapter.in.web..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java.lang..",
              "java.util..",
              "java.time..",
              "java.math..",
              "java.net",
              "dev.ampolatech.domain..",
              "dev.ampolatech.port.in..",
              "dev.ampolatech.adapter.in.web..",
              "com.fasterxml.jackson.annotation",
              "jakarta.validation",
              "jakarta.validation.constraints",
              "org.jspecify.annotations",
              "org.slf4j",
              "org.springframework.http",
              "org.springframework.stereotype",
              "org.springframework.web.bind.annotation",
              "org.springframework.web.context.request",
              "org.springframework.web.servlet.mvc.method.annotation",
              "tools.jackson.databind",
              "tools.jackson.databind.annotation",
              "tools.jackson.databind.util");

  @ArchTest
  static final ArchRule sign_in_adapters_should_use_only_their_allowed_packages =
      classes()
          .that()
          .resideInAPackage("dev.ampolatech.adapter.in.signin..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java.lang..",
              "java.util..",
              "java.time..",
              "java.math..",
              "dev.ampolatech.domain..",
              "dev.ampolatech.port.in..",
              "dev.ampolatech.adapter.in.signin..",
              "org.springframework.boot",
              "org.springframework.boot.autoconfigure.condition",
              "org.springframework.boot.context.properties",
              "org.springframework.context.annotation",
              "org.springframework.core",
              "org.springframework.core.annotation",
              "org.springframework.core.env");

  @ArchTest
  static final ArchRule persistence_adapters_should_use_only_their_allowed_packages =
      classes()
          .that()
          .resideInAPackage("dev.ampolatech.adapter.out.persistence..")
          .should()
          .onlyDependOnClassesThat()
          .resideInAnyPackage(
              "java.lang..",
              "java.util..",
              "java.time..",
              "java.math..",
              "dev.ampolatech.domain..",
              "dev.ampolatech.port.out..",
              "dev.ampolatech.adapter.out.persistence..",
              "jakarta.persistence",
              "org.springframework.data.jpa.repository",
              "org.springframework.stereotype");
}
