package dev.ampolatech;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Tests at the backend's HTTP API: the whole app on a real port, against PostgreSQL, with the
 * signed-in user set per test by {@link TestSignIn}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({TestSignIn.class, TestData.class})
public abstract class HttpSeamTest {

  @Autowired protected RestTestClient http;

  @Autowired protected TestSignIn signIn;

  @Autowired protected TestData data;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    TestDatabase.register(registry);
  }

  @AfterEach
  void signOut() {
    signIn.signOut();
  }
}
