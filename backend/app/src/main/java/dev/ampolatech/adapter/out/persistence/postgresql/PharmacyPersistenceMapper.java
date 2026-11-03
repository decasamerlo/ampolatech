package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.domain.Pharmacy;
import dev.ampolatech.domain.PharmacyId;
import org.springframework.stereotype.Component;

@Component
class PharmacyPersistenceMapper {

  Pharmacy toDomain(PharmacyEntity entity) {
    return new Pharmacy(PharmacyId.of(entity.getId()), entity.getName());
  }
}
