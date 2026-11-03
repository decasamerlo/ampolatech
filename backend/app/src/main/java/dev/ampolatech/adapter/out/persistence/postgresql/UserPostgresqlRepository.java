package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.domain.User;
import dev.ampolatech.domain.UserId;
import dev.ampolatech.port.out.UserRepositoryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class UserPostgresqlRepository implements UserRepositoryPort {

  private final SpringDataUserRepository springData;
  private final UserPersistenceMapper mapper;

  UserPostgresqlRepository(SpringDataUserRepository springData, UserPersistenceMapper mapper) {
    this.springData = springData;
    this.mapper = mapper;
  }

  @Override
  public Optional<User> findById(UserId id, PharmacyId pharmacyId) {
    return springData.findByIdAndPharmacyId(id.value(), pharmacyId.value()).map(mapper::toDomain);
  }
}
