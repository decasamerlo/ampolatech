package dev.ampolatech.adapter.out.persistence.postgresql;

import dev.ampolatech.adapter.out.persistence.postgresql.UserRoleEntity.UserRoleId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

  List<UserRoleEntity> findByUserIdAndPharmacyId(UUID userId, UUID pharmacyId);
}
