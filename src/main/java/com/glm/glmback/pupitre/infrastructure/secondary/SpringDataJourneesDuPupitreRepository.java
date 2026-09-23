package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.EtatDePresence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJourneesDuPupitreRepository extends JpaRepository<JourneeDuPupitreEntity, UUID> {
  /**
   * Toutes les journees encore ouvertes, dans l'ordre ou l'atelier retrouve la journee en cours d'un operateur : la
   * plus recemment commencee d'abord. Aucune borne de date — une journee ouverte hier et jamais fermee est toujours
   * la journee en cours.
   */
  List<JourneeDuPupitreEntity> findByEtatNotOrderByDebutDescIdAsc(EtatDePresence etat);
}
