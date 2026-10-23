package dev.ampolatech.adapter.in.signin.demo;

import dev.ampolatech.domain.Role;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(DemoSignInProperties.PREFIX)
record DemoSignInProperties(UUID userId, UUID pharmacyId, Role role) {

  static final String PREFIX = "ampolatech.sign-in.demo";
  static final String ENABLED = PREFIX + ".enabled";
}
