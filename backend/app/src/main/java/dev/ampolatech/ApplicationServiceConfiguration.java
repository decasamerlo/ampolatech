package dev.ampolatech;

import dev.ampolatech.application.UserApplicationService;
import dev.ampolatech.port.out.PharmacyRepositoryPort;
import dev.ampolatech.port.out.UserRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The application layer has no Spring dependency, so its services are wired here. */
@Configuration(proxyBeanMethods = false)
class ApplicationServiceConfiguration {

  @Bean
  UserApplicationService userApplicationService(
      UserRepositoryPort users, PharmacyRepositoryPort pharmacies) {
    return new UserApplicationService(users, pharmacies);
  }
}
