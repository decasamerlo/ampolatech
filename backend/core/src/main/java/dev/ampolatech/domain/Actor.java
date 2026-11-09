package dev.ampolatech.domain;

import java.util.Objects;
import java.util.Set;

/**
 * The signed-in user a request acts for, as far as the request needs them: who they are, the
 * pharmacy they work for, and the roles they hold, if any. The rest of what AmpolaTech knows about
 * them is a {@link User}. {@code roles} is an unmodifiable copy of the set given, in no particular
 * order.
 */
public record Actor(UserId userId, PharmacyId pharmacyId, Set<Role> roles) {

  public Actor {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(pharmacyId, "pharmacyId must not be null");
    Objects.requireNonNull(roles, "roles must not be null");
    // Set.copyOf also refuses a null role.
    roles = Set.copyOf(roles);
  }
}
