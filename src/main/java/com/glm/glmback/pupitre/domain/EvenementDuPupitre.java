package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * Un pointage du journal d'atelier, reduit a ce dont le repli a besoin.
 *
 * <p>
 * Ni auteur, ni date d'enregistrement, ni annulation : l'ordre chronologique et l'ecart des evenements annules sont
 * decides par l'adapter, des la requete. Ni cout horaire ni taux horaire non plus — un ecran d'atelier partage n'a
 * aucune raison de les recevoir.
 * </p>
 */
public record EvenementDuPupitre(TypeDePointage type, CleDActivite activite, Instant dateDeSurvenue) {
  public EvenementDuPupitre {
    Assert.notNull("type", type);
    Assert.notNull("activite", activite);
    Assert.notNull("date de survenue", dateDeSurvenue);
  }
}
