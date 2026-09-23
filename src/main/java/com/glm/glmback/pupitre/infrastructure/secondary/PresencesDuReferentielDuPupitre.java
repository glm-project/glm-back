package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.EtatDePresence;
import com.glm.glmback.pupitre.domain.OperateurId;
import com.glm.glmback.pupitre.domain.PresencesDesOperateurs;
import com.glm.glmback.pupitre.domain.PresencesDuPupitre;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * L'etat de presence de tous les operateurs en journee, en une requete.
 *
 * <p>
 * Une requete pour tout l'atelier, jamais une par operateur : le pupitre relit ce referentiel entier a chaque
 * synchronisation. Les journees fermees sont ecartees des le SQL — les rapporter pour les filtrer ensuite ferait
 * porter au domaine un tri qui ne le regarde pas.
 * </p>
 *
 * <p>
 * Un operateur peut porter plusieurs journees ouvertes si l'une d'elles n'a jamais ete fermee : la premiere rendue
 * gagne, c'est-a-dire la plus recemment commencee, exactement la journee que l'atelier viserait au prochain
 * pointage.
 * </p>
 */
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
