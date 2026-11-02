package dev.ampolatech.domain;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActorTest {

  private static final UserId USER_ID = UserId.of(UUID.randomUUID());
  private static final PharmacyId PHARMACY_ID = PharmacyId.of(UUID.randomUUID());

  @Test
  @DisplayName("should reject a null user id")
  void should_reject_a_null_user_id() {
    assertThatNullPointerException()
        .isThrownBy(() -> new Actor(null, PHARMACY_ID, Role.PHARMACIST));
  }

  @Test
  @DisplayName("should reject a null pharmacy id")
  void should_reject_a_null_pharmacy_id() {
    assertThatNullPointerException().isThrownBy(() -> new Actor(USER_ID, null, Role.PHARMACIST));
  }

  @Test
  @DisplayName("should reject a null role")
  void should_reject_a_null_role() {
    assertThatNullPointerException().isThrownBy(() -> new Actor(USER_ID, PHARMACY_ID, null));
  }
}
