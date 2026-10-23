package dev.ampolatech.adapter.in.signin.demo;

import java.util.Arrays;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Refuses to start the app with the demo sign-in unless the local profile is the only one active.
 * It checks the environment before any bean exists, so a refused start runs no migration, the local
 * demo seed included, and lazy initialization can't put the check off.
 */
// Last, after ConfigDataEnvironmentPostProcessor has loaded application-local.yml, so the check
// sees the demo sign-in turned on there.
@Order(Ordered.LOWEST_PRECEDENCE)
class DemoSignInGuard implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (!environment.getProperty(DemoSignInProperties.ENABLED, Boolean.class, false)) {
      return;
    }

    String[] activeProfiles = environment.getActiveProfiles();
    if (!Arrays.equals(activeProfiles, new String[] {"local"})) {
      throw new IllegalStateException(
          "The demo sign-in runs only under the local profile alone,"
              + " and the active profiles are "
              + Arrays.toString(activeProfiles));
    }
  }
}
