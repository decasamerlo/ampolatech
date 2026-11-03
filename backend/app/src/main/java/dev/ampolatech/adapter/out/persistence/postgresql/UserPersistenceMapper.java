package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.domain.Role;
import dev.ampolatech.domain.User;
import dev.ampolatech.domain.UserId;
import org.springframework.stereotype.Component;

@Component
class UserPersistenceMapper {

  User toDomain(UserEntity entity) {
    return new User(
        UserId.of(entity.getId()),
        PharmacyId.of(entity.getPharmacyId()),
        entity.getName(),
        role(entity.getRole()));
  }

  // The codes app_user_role_check allows, written out rather than read with Role.valueOf, so
  // renaming a Role constant leaves the stored codes as they are.
  private static Role role(String code) {
    return switch (code) {
      case "PHARMACIST" -> Role.PHARMACIST;
      case "SALES_AGENT" -> Role.SALES_AGENT;
      default -> throw new IllegalStateException("Unknown role code " + code);
    };
  }
}
