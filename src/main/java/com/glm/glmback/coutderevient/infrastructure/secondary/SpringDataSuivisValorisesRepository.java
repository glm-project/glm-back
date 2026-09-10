package com.glm.glmback.coutderevient.infrastructure.secondary;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSuivisValorisesRepository extends JpaRepository<SuiviValoriseEntity, UUID> {
  /**
   * Tous les passages de l'element, servis par {@code ix_suivi_d_atelier_element} : un element reengage apres cloture
   * en a plusieurs, et son cout de revient les additionne.
   */
  List<SuiviValoriseEntity> findByElementId(UUID elementId);

  List<SuiviValoriseEntity> findByIdIn(Set<UUID> ids);
}
