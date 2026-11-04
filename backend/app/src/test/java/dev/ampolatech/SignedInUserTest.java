package dev.ampolatech;

import dev.ampolatech.domain.Actor;
import dev.ampolatech.domain.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

@DisplayName("GET /api/me")
class SignedInUserTest extends HttpSeamTest {

  @Test
  @DisplayName("should answer with the signed-in pharmacist's name, role and pharmacy")
  void should_answer_with_the_signed_in_pharmacists_name_role_and_pharmacy() {
    var primavera = data.pharmacy("Farmácia Primavera");
    var horizonte = data.pharmacy("Farmácia Horizonte");
    var pharmacist = data.user(primavera, "Beatriz Lima", Role.PHARMACIST);
    data.user(horizonte, "Carlos Souza", Role.SALES_AGENT);
    signIn.signIn(pharmacist);

    ResponseSpec response = http.get().uri("/api/me").exchange();

    response
        .expectStatus()
        .isOk()
        .expectBody()
        .json(
            """
            {
              "name": "Beatriz Lima",
              "role": "PHARMACIST",
              "pharmacy": {"name": "Farmácia Primavera"}
            }
            """,
            JsonCompareMode.STRICT);
  }

  @Test
  @DisplayName("should answer with the signed-in sales agent's name, role and pharmacy")
  void should_answer_with_the_signed_in_sales_agents_name_role_and_pharmacy() {
    var primavera = data.pharmacy("Farmácia Primavera");
    var horizonte = data.pharmacy("Farmácia Horizonte");
    data.user(primavera, "Beatriz Lima", Role.PHARMACIST);
    var salesAgent = data.user(horizonte, "Carlos Souza", Role.SALES_AGENT);
    signIn.signIn(salesAgent);

    ResponseSpec response = http.get().uri("/api/me").exchange();

    response
        .expectStatus()
        .isOk()
        .expectBody()
        .json(
            """
            {
              "name": "Carlos Souza",
              "role": "SALES_AGENT",
              "pharmacy": {"name": "Farmácia Horizonte"}
            }
            """,
            JsonCompareMode.STRICT);
  }

  @Test
  @DisplayName("should refuse a user signed in for a pharmacy they don't work for")
  void should_refuse_a_user_signed_in_for_a_pharmacy_they_dont_work_for() {
    var primavera = data.pharmacy("Farmácia Primavera");
    var horizonte = data.pharmacy("Farmácia Horizonte");
    data.user(primavera, "Beatriz Lima", Role.PHARMACIST);
    var salesAgent = data.user(horizonte, "Carlos Souza", Role.SALES_AGENT);
    signIn.signIn(new Actor(salesAgent.id(), primavera.id(), salesAgent.role()));

    ResponseSpec response = http.get().uri("/api/me").exchange();

    response
        .expectStatus()
        .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        .expectBody()
        .json(
            """
            {"title": "Internal Server Error", "status": 500}
            """,
            JsonCompareMode.STRICT);
  }
}
