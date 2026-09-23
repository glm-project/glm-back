package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.EtatDePresence;
import com.glm.glmback.pupitre.domain.OperateurId;
import com.glm.glmback.pupitre.domain.PresencesDesOperateurs;
import com.glm.glmback.pupitre.domain.PresencesDuPupitre;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
class PresencesDuReferentielDuPupitre implements PresencesDuPupitre {

  private final SpringDataJourneesDuPupitreRepository journees;

  PresencesDuReferentielDuPupitre(SpringDataJourneesDuPupitreRepository journees) {
    this.journees = journees;
  }

  @Override
  public PresencesDesOperateurs toutes() {
    Map<OperateurId, EtatDePresence> etats = new LinkedHashMap<>();
    journees
      .findByEtatNotOrderByDebutDescIdAsc(EtatDePresence.ABSENT)
      .forEach(journee -> etats.putIfAbsent(journee.operateur(), journee.etat()));

    return new PresencesDesOperateurs(etats);
  }
}
