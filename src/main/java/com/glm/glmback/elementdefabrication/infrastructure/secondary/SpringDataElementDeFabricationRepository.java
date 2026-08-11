package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataElementDeFabricationRepository extends JpaRepository<ElementDeFabricationEntity, UUID> {
  Page<ElementDeFabricationEntity> findByDateDeCreationBetween(Instant debut, Instant fin, Pageable pageable);

  @Query("SELECT element.id FROM ElementDeFabricationEntity element WHERE element.reference = :reference")
  Optional<UUID> findIdByReference(@Param("reference") String reference);
}
