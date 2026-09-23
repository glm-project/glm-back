package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.EtatDePresence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJourneesDuPupitreRepository extends JpaRepository<JourneeDuPupitreEntity, UUID> {
  List<JourneeDuPupitreEntity> findByEtatNotOrderByDebutDescIdAsc(EtatDePresence etat);
}
