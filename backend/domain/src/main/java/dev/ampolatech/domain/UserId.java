package dev.ampolatech.domain;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "UserId must not be null");
  }

  public static UserId of(UUID value) {
    return new UserId(value);
  }
}
