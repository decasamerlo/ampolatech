package dev.ampolatech.adapter.out.persistence.postgresql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UserEntity {

  @Id
  @Column(name = "id", nullable = false)
  @Getter(AccessLevel.PACKAGE)
  private UUID id;

  @Column(name = "pharmacy_id", nullable = false)
  @Getter(AccessLevel.PACKAGE)
  private UUID pharmacyId;

  @Column(name = "name", nullable = false)
  @Getter(AccessLevel.PACKAGE)
  private String name;
}
