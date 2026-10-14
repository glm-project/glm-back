package com.glm.glmback.atelier.infrastructure.secondary.correctiondesactivites;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFichesDElementDeLaCorrectionRepository extends JpaRepository<FicheDElementDeLaCorrectionEntity, UUID> {
  List<FicheDElementDeLaCorrectionEntity> findByIdIn(Set<UUID> ids);
}
