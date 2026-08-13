package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.Optional;

public interface JourneeDeTravailRepository {
  JourneeDeTravail create(JourneeDeTravail journee);

  JourneeDeTravail update(JourneeDeTravail journee);

  Optional<JourneeDeTravail> get(JourneeDeTravailId id);

  Optional<JourneeDeTravail> getEnCoursPour(OperateurId operateur);

  /**
   * La journee de cet operateur qui recouvre l'instant vise, s'il y en a une.
   *
   * <p>
   * C'est la matiere du temps effectif : un travail commence a cet instant ne vaut que dans les fenetres de presence
   * de cette journee la. Un instant qui ne tombe dans aucune journee rend un resultat vide, la presence restant a
   * regulariser.
   * </p>
   */
  Optional<JourneeDeTravail> journeeContenant(OperateurId operateur, Instant instant);

  Page<JourneeDeTravail> list(JourneeDeTravailCriteria criteria, Pageable pageable);
}
