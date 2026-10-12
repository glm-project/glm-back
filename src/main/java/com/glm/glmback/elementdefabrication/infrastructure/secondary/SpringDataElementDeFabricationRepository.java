package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataElementDeFabricationRepository extends JpaRepository<ElementDeFabricationEntity, UUID> {
  @Query("SELECT element.id FROM ElementDeFabricationEntity element WHERE element.reference = :reference")
  Optional<UUID> findIdByReference(@Param("reference") String reference);
}
