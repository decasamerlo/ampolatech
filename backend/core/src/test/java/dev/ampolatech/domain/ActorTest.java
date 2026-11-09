package dev.ampolatech.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActorTest {

  private static final UserId USER_ID = UserId.of(UUID.randomUUID());
  private static final PharmacyId PHARMACY_ID = PharmacyId.of(UUID.randomUUID());
  private static final Set<Role> ROLES = Set.of(Role.PHARMACIST);

  @Test
  @DisplayName("should accept an empty set of roles")
  void should_accept_an_empty_set_of_roles() {
    var actor = new Actor(USER_ID, PHARMACY_ID, Set.of());

    assertThat(actor.roles()).isEmpty();
  }

  @Test
  @DisplayName("should keep its roles when the set it was given changes")
  void should_keep_its_roles_when_the_set_it_was_given_changes() {
    var roles = new HashSet<>(Set.of(Role.PHARMACIST));
    var actor = new Actor(USER_ID, PHARMACY_ID, roles);

    roles.add(Role.SALES_AGENT);

    assertThat(actor.roles()).containsExactly(Role.PHARMACIST);
  }

  @Test
  @DisplayName("should refuse a change to its roles")
  void should_refuse_a_change_to_its_roles() {
    var actor = new Actor(USER_ID, PHARMACY_ID, ROLES);

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> actor.roles().add(Role.SALES_AGENT));
  }

  @Test
  @DisplayName("should reject a null user id")
  void should_reject_a_null_user_id() {
    assertThatNullPointerException().isThrownBy(() -> new Actor(null, PHARMACY_ID, ROLES));
  }

  @Test
  @DisplayName("should reject a null pharmacy id")
  void should_reject_a_null_pharmacy_id() {
    assertThatNullPointerException().isThrownBy(() -> new Actor(USER_ID, null, ROLES));
  }

  @Test
  @DisplayName("should reject a null set of roles")
  void should_reject_a_null_set_of_roles() {
    assertThatNullPointerException().isThrownBy(() -> new Actor(USER_ID, PHARMACY_ID, null));
  }

  @Test
  @DisplayName("should reject a set of roles holding null")
  void should_reject_a_set_of_roles_holding_null() {
    var roles = new HashSet<>(Arrays.asList(Role.PHARMACIST, null));

    assertThatNullPointerException().isThrownBy(() -> new Actor(USER_ID, PHARMACY_ID, roles));
  }
}
