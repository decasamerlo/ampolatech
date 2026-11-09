package dev.ampolatech.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

  private static final UserId ID = UserId.of(UUID.randomUUID());
  private static final PharmacyId PHARMACY_ID = PharmacyId.of(UUID.randomUUID());
  private static final Set<Role> ROLES = Set.of(Role.PHARMACIST);

  @Test
  @DisplayName("should accept a name of a single character")
  void should_accept_a_name_of_a_single_character() {
    var user = new User(ID, PHARMACY_ID, "A", ROLES);

    assertThat(user.name()).isEqualTo("A");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   ", "\t\n"})
  @DisplayName("should reject a blank name")
  void should_reject_a_blank_name(String name) {
    assertThatIllegalArgumentException().isThrownBy(() -> new User(ID, PHARMACY_ID, name, ROLES));
  }

  @Test
  @DisplayName("should accept an empty set of roles")
  void should_accept_an_empty_set_of_roles() {
    var user = new User(ID, PHARMACY_ID, "Beatriz Lima", Set.of());

    assertThat(user.roles()).isEmpty();
  }

  @Test
  @DisplayName("should keep its roles when the set it was given changes")
  void should_keep_its_roles_when_the_set_it_was_given_changes() {
    var roles = new HashSet<>(Set.of(Role.PHARMACIST));
    var user = new User(ID, PHARMACY_ID, "Beatriz Lima", roles);

    roles.add(Role.SALES_AGENT);

    assertThat(user.roles()).containsExactly(Role.PHARMACIST);
  }

  @Test
  @DisplayName("should refuse a change to its roles")
  void should_refuse_a_change_to_its_roles() {
    var user = new User(ID, PHARMACY_ID, "Beatriz Lima", ROLES);

    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> user.roles().add(Role.SALES_AGENT));
  }

  @Test
  @DisplayName("should reject a null id")
  void should_reject_a_null_id() {
    assertThatNullPointerException()
        .isThrownBy(() -> new User(null, PHARMACY_ID, "Beatriz Lima", ROLES));
  }

  @Test
  @DisplayName("should reject a null pharmacy id")
  void should_reject_a_null_pharmacy_id() {
    assertThatNullPointerException().isThrownBy(() -> new User(ID, null, "Beatriz Lima", ROLES));
  }

  @Test
  @DisplayName("should reject a null name")
  void should_reject_a_null_name() {
    assertThatNullPointerException().isThrownBy(() -> new User(ID, PHARMACY_ID, null, ROLES));
  }

  @Test
  @DisplayName("should reject a null set of roles")
  void should_reject_a_null_set_of_roles() {
    assertThatNullPointerException()
        .isThrownBy(() -> new User(ID, PHARMACY_ID, "Beatriz Lima", null));
  }

  @Test
  @DisplayName("should reject a set of roles holding null")
  void should_reject_a_set_of_roles_holding_null() {
    var roles = new HashSet<>(Arrays.asList(Role.PHARMACIST, null));

    assertThatNullPointerException()
        .isThrownBy(() -> new User(ID, PHARMACY_ID, "Beatriz Lima", roles));
  }
}
