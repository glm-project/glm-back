package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataElementDeFabricationRepository extends JpaRepository<ElementDeFabricationEntity, UUID> {
  Page<ElementDeFabricationEntity> findByDateDeCreationBetween(Instant debut, Instant fin, Pageable pageable);
}
