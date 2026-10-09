package com.glm.glmback.postedetravail.domain;

import java.util.Optional;

/**
 * Ce que ce contexte sait du referentiel des natures de travail, sans jamais dependre de son contexte.
 */
public interface NaturesDeclarees {
  Optional<NatureDuPoste> get(NatureDeTravailId id);

  /**
   * La nature qui porte ce libelle a la casse, aux accents et aux espaces pres.
   *
   * @deprecated chemin de transition du libelle saisi en texte, retire quand le front envoie l'identifiant (glm-back#130).
   */
  @Deprecated(forRemoval = true)
  Optional<NatureDuPoste> parLibelle(NatureDeTravail libelle);

  /**
   * @deprecated chemin de transition du libelle saisi en texte, retire quand le front envoie l'identifiant (glm-back#130).
   */
  @Deprecated(forRemoval = true)
  void declare(NatureDuPoste nature);
}
