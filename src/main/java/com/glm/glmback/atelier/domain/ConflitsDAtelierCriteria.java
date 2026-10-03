package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Locale;

/** Recherches partielles sur l'operateur et sur la designation ou l'identite de l'element. */
public record ConflitsDAtelierCriteria(String operateur, String element) {
  public ConflitsDAtelierCriteria {
    Assert.notNull("operateur", operateur);
    Assert.notNull("element", element);
    operateur = operateur.toLowerCase(Locale.ROOT);
    element = element.toLowerCase(Locale.ROOT);
  }

  public boolean matches(ConflitEnListe ligne, AnnuaireDAtelier annuaire) {
    String libelleOperateur = annuaire
      .operateur(ligne.cle().operateur())
      .map(fiche -> fiche.prenom().value() + " " + fiche.nom().value())
      .orElse("");
    return (
      (libelleOperateur.toLowerCase(Locale.ROOT).contains(operateur) || ligne.cle().operateur().uuid().toString().contains(operateur))
      && (ligne.element().nom().value().toLowerCase(Locale.ROOT).contains(element)
        || ligne.element().id().uuid().toString().contains(element))
    );
  }
}
