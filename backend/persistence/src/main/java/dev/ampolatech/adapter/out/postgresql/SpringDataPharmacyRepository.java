package dev.ampolatech.adapter.out.postgresql;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPharmacyRepository extends JpaRepository<PharmacyEntity, UUID> {}
