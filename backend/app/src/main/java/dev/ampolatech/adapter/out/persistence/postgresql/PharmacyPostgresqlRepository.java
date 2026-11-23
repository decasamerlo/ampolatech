package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.domain.Pharmacy;
import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.port.out.PharmacyRepositoryPort;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class PharmacyPostgresqlRepository implements PharmacyRepositoryPort {

  private final SpringDataPharmacyRepository springData;
  private final PharmacyPersistenceMapper mapper;

  @Override
  public Optional<Pharmacy> findById(PharmacyId id) {
    return springData.findById(id.value()).map(mapper::toDomain);
  }
}
