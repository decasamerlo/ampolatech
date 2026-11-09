package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.adapter.out.persistence.postgresql.UserRoleEntity.UserRoleId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One role a user holds. It has no association with {@link UserEntity}: {@link
 * UserPostgresqlRepository} reads a user's roles with a query of its own, which filters on the
 * pharmacy as the user's query does (ADR 0003).
 */
// @IdClass, where Hibernate's guide prefers @EmbeddedId, keeps the key's columns beside pharmacy_id
// as plain fields, so a query names them as it names any other column.
@Entity
@Table(name = "app_user_role")
@IdClass(UserRoleId.class)
class UserRoleEntity {

  @Column(name = "pharmacy_id", nullable = false)
  private UUID pharmacyId;

  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Id
  @Column(name = "role", nullable = false)
  private String role;

  protected UserRoleEntity() {}

  String getRole() {
    return role;
  }

  /**
   * The primary key, app_user_role_pkey. Its components take the names of the {@code @Id} fields
   * above, as an id class's must.
   */
  record UserRoleId(UUID userId, String role) {}
}
