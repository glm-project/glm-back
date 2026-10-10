package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSuivisValorisesRepository extends JpaRepository<SuiviValoriseEntity, UUID> {
  List<SuiviValoriseEntity> findByElementId(UUID elementId);
}
