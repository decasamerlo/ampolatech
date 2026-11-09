package dev.ampolatech.adapter.in.signin.demo;

import dev.ampolatech.domain.Role;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(DemoSignInProperties.PREFIX)
record DemoSignInProperties(UUID userId, UUID pharmacyId, Set<Role> roles) {

  static final String PREFIX = "ampolatech.sign-in.demo";
  static final String ENABLED = PREFIX + ".enabled";
}
