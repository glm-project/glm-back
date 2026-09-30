package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.List;

/**
 * Le temps reellement passe sur un element : les intervalles de ses activites, tels que le journal les interprete.
 *
 * <p>
 * Une activite que rien n'a terminee se termine automatiquement a son echeance, son debut plus 13 heures, avec une
 * anomalie ; une fin pointee ou regularisee la termine a son heure. La pause de midi se lit
 * dans le journal de l'element, ou le pupitre l'a pointee par une fin et un debut.
 * </p>
 *
 * <p>
 * L'instant d'evaluation vient de l'appelant : ce service n'a pas d'horloge, et tout ce qui depend de l'heure de la
 * lecture se juge sur cet instant-la.
 * </p>
 */
public final class TempsDAtelierService {

  private final SuiviDAtelierRepository suivis;

  public TempsDAtelierService(SuiviDAtelierRepository suivis) {
    this.suivis = suivis;
  }

  public List<IntervalleDActivite> tempsEffectif(SuiviDAtelierId id, Instant evaluation) {
    return suivis
      .get(id)
      .orElseThrow(() -> new SuiviDAtelierIntrouvableException(id))
      .intervalles(evaluation);
  }
}
