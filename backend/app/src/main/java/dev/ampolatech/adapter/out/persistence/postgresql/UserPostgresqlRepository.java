package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.domain.User;
import dev.ampolatech.domain.UserId;
import dev.ampolatech.port.out.UserRepositoryPort;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// A @Component, not a @Repository, so a port's caller receives the exception thrown here: the
// exception translation @Repository turns on would rewrap an IllegalStateException or an
// IllegalArgumentException as Spring's InvalidDataAccessApiUsageException.
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class UserPostgresqlRepository implements UserRepositoryPort {

  private final SpringDataUserRepository users;
  private final SpringDataUserRoleRepository userRoles;
  private final UserPersistenceMapper mapper;

  @Override
  public Optional<User> findById(UserId id, PharmacyId pharmacyId) {
    return users
        .findByIdAndPharmacyId(id.value(), pharmacyId.value())
        .map(
            user ->
                mapper.toDomain(
                    user, userRoles.findByUserIdAndPharmacyId(id.value(), pharmacyId.value())));
  }
}
