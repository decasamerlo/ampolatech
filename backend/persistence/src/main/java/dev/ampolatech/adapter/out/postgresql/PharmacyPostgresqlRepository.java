package dev.ampolatech.adapter.out.postgresql;

import dev.ampolatech.domain.Pharmacy;
import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.port.out.PharmacyRepositoryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class PharmacyPostgresqlRepository implements PharmacyRepositoryPort {

  private final SpringDataPharmacyRepository springData;
  private final PharmacyPersistenceMapper mapper;

  PharmacyPostgresqlRepository(
      SpringDataPharmacyRepository springData, PharmacyPersistenceMapper mapper) {
    this.springData = springData;
    this.mapper = mapper;
  }

  @Override
  public Optional<Pharmacy> findById(PharmacyId id) {
    return springData.findById(id.value()).map(mapper::toDomain);
  }
}
