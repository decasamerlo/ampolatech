package dev.ampolatech.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PharmacyTest {

  private static final PharmacyId ID = PharmacyId.of(UUID.randomUUID());

  @Test
  @DisplayName("should accept a name of a single character")
  void should_accept_a_name_of_a_single_character() {
    var pharmacy = new Pharmacy(ID, "A");

    assertThat(pharmacy.name()).isEqualTo("A");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\t\n"})
  @DisplayName("should reject a blank name")
  void should_reject_a_blank_name(String name) {
    assertThatIllegalArgumentException().isThrownBy(() -> new Pharmacy(ID, name));
  }

  @Test
  @DisplayName("should reject a null id")
  void should_reject_a_null_id() {
    assertThatNullPointerException().isThrownBy(() -> new Pharmacy(null, "Farmácia Primavera"));
  }

  @Test
  @DisplayName("should reject a null name")
  void should_reject_a_null_name() {
    assertThatNullPointerException().isThrownBy(() -> new Pharmacy(ID, null));
  }
}
