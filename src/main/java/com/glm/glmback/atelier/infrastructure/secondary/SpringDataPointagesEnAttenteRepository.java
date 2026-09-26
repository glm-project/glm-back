package com.glm.glmback.atelier.infrastructure.secondary;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface SpringDataPointagesEnAttenteRepository
  extends JpaRepository<PointageEnAttenteEntity, UUID>, JpaSpecificationExecutor<PointageEnAttenteEntity>
{
  List<PointageEnAttenteEntity> findByEvenementDuPupitre(UUID evenementDuPupitre);
}
