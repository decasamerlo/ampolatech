package dev.ampolatech.adapter.out.persistence.postgresql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "pharmacy")
class PharmacyEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  protected PharmacyEntity() {}

  UUID getId() {
    return id;
  }

  String getName() {
    return name;
  }
}
