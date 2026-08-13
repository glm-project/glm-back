package com.glm.glmback.operateur.infrastructure.secondary;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJourneesDesOperateursRepository extends JpaRepository<JourneeDOperateurEntity, UUID> {
  Optional<JourneeDOperateurEntity> findFirstByOperateurId(UUID operateurId);
}
