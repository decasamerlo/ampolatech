package dev.ampolatech.adapter.out.persistence.postgresql;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

  Optional<UserEntity> findByIdAndPharmacyId(UUID id, UUID pharmacyId);
}
