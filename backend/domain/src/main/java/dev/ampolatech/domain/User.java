package dev.ampolatech.domain;

import java.util.Objects;

/**
 * A member of a pharmacy's staff who signs in to AmpolaTech. Every user works for exactly one
 * pharmacy.
 */
public record User(UserId id, PharmacyId pharmacyId, String name, Role role) {

  public User {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(pharmacyId, "pharmacyId must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(role, "role must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }
}
