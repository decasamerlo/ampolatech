package dev.ampolatech.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

  private static final UserId ID = UserId.of(UUID.randomUUID());
  private static final PharmacyId PHARMACY_ID = PharmacyId.of(UUID.randomUUID());

  @Test
  @DisplayName("should accept a name of a single character")
  void should_accept_a_name_of_a_single_character() {
    var user = new User(ID, PHARMACY_ID, "A", Role.PHARMACIST);

    assertThat(user.name()).isEqualTo("A");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\t\n"})
  @DisplayName("should reject a blank name")
  void should_reject_a_blank_name(String name) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new User(ID, PHARMACY_ID, name, Role.PHARMACIST));
  }

  @Test
  @DisplayName("should reject a null id")
  void should_reject_a_null_id() {
    assertThatNullPointerException()
        .isThrownBy(() -> new User(null, PHARMACY_ID, "Beatriz Lima", Role.PHARMACIST));
  }

  @Test
  @DisplayName("should reject a null pharmacy id")
  void should_reject_a_null_pharmacy_id() {
    assertThatNullPointerException()
        .isThrownBy(() -> new User(ID, null, "Beatriz Lima", Role.PHARMACIST));
  }

  @Test
  @DisplayName("should reject a null name")
  void should_reject_a_null_name() {
    assertThatNullPointerException()
        .isThrownBy(() -> new User(ID, PHARMACY_ID, null, Role.PHARMACIST));
  }

  @Test
  @DisplayName("should reject a null role")
  void should_reject_a_null_role() {
    assertThatNullPointerException()
        .isThrownBy(() -> new User(ID, PHARMACY_ID, "Beatriz Lima", null));
  }
}
