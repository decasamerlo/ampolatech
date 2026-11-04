package dev.ampolatech;

import dev.ampolatech.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/** The local app as it starts: the demo seed, and the demo sign-in configured for it. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@ActiveProfiles("local")
@Import({TestData.class, ApiSpecValidation.class})
@DisplayName("The demo sign-in")
class DemoSignInTest {

  @Autowired private RestTestClient http;

  @Autowired private TestData data;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    TestDatabase.register(registry);
  }

  @Test
  @DisplayName("should sign everyone in as the demo sales agent under the local profile")
  void should_sign_everyone_in_as_the_demo_sales_agent_under_the_local_profile() {
    data.user(data.pharmacy("Farmácia Horizonte"), "Carlos Souza", Role.SALES_AGENT);

    ResponseSpec response = http.get().uri("/api/me").exchange();

    response
        .expectStatus()
        .isOk()
        .expectBody()
        .json(
            """
            {
              "name": "Joana Exemplo",
              "role": "SALES_AGENT",
              "pharmacy": {"name": "Farmácia Exemplo"}
            }
            """,
            JsonCompareMode.STRICT);
  }
}
