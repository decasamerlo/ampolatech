package dev.ampolatech.domain;

import java.util.Objects;

public record Pharmacy(PharmacyId id, String name) {

  public Pharmacy {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(name, "name must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }
}
