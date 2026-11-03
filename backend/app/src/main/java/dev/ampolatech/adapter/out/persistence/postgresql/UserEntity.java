package dev.ampolatech.adapter.out.persistence.postgresql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "app_user")
class UserEntity {

  @Id
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "pharmacy_id", nullable = false)
  private UUID pharmacyId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "role", nullable = false)
  private String role;

  protected UserEntity() {}

  UUID getId() {
    return id;
  }

  UUID getPharmacyId() {
    return pharmacyId;
  }

  String getName() {
    return name;
  }

  String getRole() {
    return role;
  }
}
