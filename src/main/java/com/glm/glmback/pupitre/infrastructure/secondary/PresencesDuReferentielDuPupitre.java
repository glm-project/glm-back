package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.EtatDePresence;
import com.glm.glmback.pupitre.domain.JourneeEnCours;
import com.glm.glmback.pupitre.domain.JourneesEnCours;
import com.glm.glmback.pupitre.domain.OperateurId;
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

  /**
   * La journee en cours de chaque operateur : la plus recente de celles qui n'ont pas de depart. Juger si elle est
   * abandonnee appartient au domaine, qui connait l'horloge et le seuil.
   */
  @Override
  public JourneesEnCours toutes() {
    Map<OperateurId, JourneeEnCours> enCours = new LinkedHashMap<>();
    journees
      .findByEtatNotOrderByDebutDescIdAsc(EtatDePresence.ABSENT)
      .forEach(journee -> enCours.putIfAbsent(journee.operateur(), journee.toDomain()));

    return new JourneesEnCours(enCours);
  }
}
