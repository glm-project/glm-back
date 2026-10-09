package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;

public interface SuiviDAtelierRepository {
  SuiviDAtelier create(SuiviDAtelier suivi);

  SuiviDAtelier update(SuiviDAtelier suivi);

  Optional<SuiviDAtelier> get(SuiviDAtelierId id);

  Optional<SuiviDAtelier> getForUpdate(SuiviDAtelierId id);

  Optional<SuiviDAtelier> getEnCoursPour(ElementEngageId element);

  /**
   * Vrai si un suivi de l'entreprise porte deja cet evenement : l'identifiant d'un geste est unique dans toute la table,
   * pas seulement dans le journal d'un suivi.
   */
  boolean contientEvenement(EvenementDAtelierId evenement);

  Page<SuiviDAtelier> list(SuiviDAtelierCriteria criteria, Pageable pageable);
}
