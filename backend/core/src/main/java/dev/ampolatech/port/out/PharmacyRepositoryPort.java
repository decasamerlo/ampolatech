package dev.ampolatech.port.out;

import dev.ampolatech.domain.Pharmacy;
import dev.ampolatech.domain.PharmacyId;
import java.util.Optional;

/** Every method works on the pharmacy it is given and on no other (ADR 0003). */
public interface PharmacyRepositoryPort {

  Optional<Pharmacy> findById(PharmacyId id);
}
