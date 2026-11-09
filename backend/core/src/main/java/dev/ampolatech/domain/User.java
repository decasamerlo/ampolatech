package dev.ampolatech.domain;

import java.util.Objects;
import java.util.Set;

/**
 * A member of a pharmacy's staff who signs in to AmpolaTech. Every user works for exactly one
 * pharmacy, and holds any number of roles, none included. {@code roles} is an unmodifiable copy of
 * the set given, in no particular order.
 */
public record User(UserId id, PharmacyId pharmacyId, String name, Set<Role> roles) {

  public User {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(pharmacyId, "pharmacyId must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(roles, "roles must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    // Set.copyOf also refuses a null role.
    roles = Set.copyOf(roles);
  }
}
