package dev.ampolatech.domain;

import java.util.Objects;

/**
 * The signed-in user a request acts for, as far as the request needs them: who they are, the
 * pharmacy they work for, and their role. The rest of what AmpolaTech knows about them is a {@link
 * User}.
 */
public record Actor(UserId userId, PharmacyId pharmacyId, Role role) {

  public Actor {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(pharmacyId, "pharmacyId must not be null");
    Objects.requireNonNull(role, "role must not be null");
  }
}
