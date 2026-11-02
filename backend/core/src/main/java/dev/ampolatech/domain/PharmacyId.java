package dev.ampolatech.domain;

import java.util.Objects;
import java.util.UUID;

public record PharmacyId(UUID value) {

  public PharmacyId {
    Objects.requireNonNull(value, "PharmacyId must not be null");
  }

  public static PharmacyId of(UUID value) {
    return new PharmacyId(value);
  }
}
