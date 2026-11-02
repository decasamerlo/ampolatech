package dev.ampolatech.domain;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PharmacyIdTest {

  @Test
  @DisplayName("should reject a null value")
  void should_reject_a_null_value() {
    assertThatNullPointerException().isThrownBy(() -> PharmacyId.of(null));
  }
}
