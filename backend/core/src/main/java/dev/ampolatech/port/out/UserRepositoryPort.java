package dev.ampolatech.port.out;

import dev.ampolatech.domain.PharmacyId;
import dev.ampolatech.domain.User;
import dev.ampolatech.domain.UserId;
import java.util.Optional;

/**
 * Every method works within the pharmacy it is given: a user of another pharmacy is never found
 * (ADR 0003).
 */
public interface UserRepositoryPort {

  Optional<User> findById(UserId id, PharmacyId pharmacyId);
}
