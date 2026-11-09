package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.domain.Role;
import dev.ampolatech.domain.User;
import dev.ampolatech.domain.UserId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class UserPersistenceMapper {

  User toDomain(UserEntity user, List<UserRoleEntity> roles) {
    return new User(
        UserId.of(user.getId()), PharmacyId.of(user.getPharmacyId()), user.getName(), roles(roles));
  }

  private static Set<Role> roles(List<UserRoleEntity> roles) {
    return roles.stream().map(userRole -> role(userRole.getRole())).collect(Collectors.toSet());
  }

  // The codes app_user_role_role_check allows, written out rather than read with Role.valueOf, so
  // renaming a Role constant leaves the stored codes as they are.
  private static Role role(String code) {
    return switch (code) {
      case "PHARMACIST" -> Role.PHARMACIST;
      case "SALES_AGENT" -> Role.SALES_AGENT;
      default -> throw new IllegalStateException("Unknown role code " + code);
    };
  }
}
