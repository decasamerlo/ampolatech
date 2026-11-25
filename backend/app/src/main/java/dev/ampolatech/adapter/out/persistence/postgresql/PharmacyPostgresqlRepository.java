package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.domain.Pharmacy;
import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.port.out.PharmacyRepositoryPort;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// A @Component, not a @Repository, so a port's caller receives the exception thrown here: the
// exception translation @Repository turns on would rewrap an IllegalStateException or an
// IllegalArgumentException as Spring's InvalidDataAccessApiUsageException.
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PharmacyPostgresqlRepository implements PharmacyRepositoryPort {

  private final SpringDataPharmacyRepository springData;
  private final PharmacyPersistenceMapper mapper;

  @Override
  public Optional<Pharmacy> findById(PharmacyId id) {
    return springData.findById(id.value()).map(mapper::toDomain);
  }
}
