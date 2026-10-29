package dev.ampolatech;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.SpringApplication;

/**
 * Each case boots the whole app with every bean lazy and a database nothing listens on, so only a
 * check made before any bean exists can stop the boot with the demo sign-in's message.
 */
@DisplayName("The demo sign-in")
class DemoSignInGuardTest {

  @ParameterizedTest(name = "active profiles {1}")
  @CsvSource(
      delimiter = '|',
      value = {"''         | []", "production | [production]"})
  @DisplayName("should refuse to start when turned on outside the local profile")
  void should_refuse_to_start_when_turned_on_outside_the_local_profile(
      String profiles, String printedProfiles) {
    Throwable thrown =
        catchThrowable(() -> start(profiles, "--ampolatech.sign-in.demo.enabled=true"));

    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "The demo sign-in runs only under the local profile alone,"
                + " and the active profiles are "
                + printedProfiles);
  }

  private static void start(String profiles, String... moreArguments) {
    String[] arguments =
        Stream.concat(
                Stream.of(
                    "--spring.profiles.active=" + profiles,
                    "--spring.main.lazy-initialization=true",
                    "--spring.datasource.url=jdbc:postgresql://127.0.0.1:1/ampolatech",
                    "--server.port=0"),
                Stream.of(moreArguments))
            .toArray(String[]::new);
    SpringApplication.run(AmpolaTechApplication.class, arguments).close();
  }

  @Test
  @DisplayName("should refuse to start when another profile is active beside the local one")
  void should_refuse_to_start_when_another_profile_is_active_beside_the_local_one() {
    Throwable thrown = catchThrowable(() -> start("local,production"));

    assertThat(thrown)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "The demo sign-in runs only under the local profile alone,"
                + " and the active profiles are [local, production]");
  }
}
