package dev.ampolatech.adapter.in.signin.demo;

import dev.ampolatech.domain.Actor;
import dev.ampolatech.domain.CurrentActor;
import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.domain.UserId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Signs everyone in as one user, whose actor it reads from configuration (ADR 0004). It reads no
 * database: ADR 0003's row-level security, which ADR 0009 defers, takes each transaction's pharmacy
 * from the actor, so finding the actor can't need a query. It signs in anyone who opens the app, so
 * {@link DemoSignInGuard} refuses to start the app with it unless the local profile is the only one
 * active.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(DemoSignInProperties.ENABLED)
@EnableConfigurationProperties(DemoSignInProperties.class)
class DemoSignInConfiguration {

  @Bean
  CurrentActor demoCurrentActor(DemoSignInProperties properties) {
    var actor =
        new Actor(
            UserId.of(properties.userId()),
            PharmacyId.of(properties.pharmacyId()),
            properties.role());
    return () -> actor;
  }
}
