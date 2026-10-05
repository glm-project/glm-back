package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.Locale;

/** Recherches partielles sur l'operateur et sur la designation ou l'identite de l'element. */
public record AnomaliesDAtelierCriteria(String operateur, String element) {
  public AnomaliesDAtelierCriteria {
    Assert.notNull("operateur", operateur);
    Assert.notNull("element", element);
    operateur = operateur.toLowerCase(Locale.ROOT);
    element = element.toLowerCase(Locale.ROOT);
  }

  public boolean matches(ConflitEnListe ligne, AnnuaireDAtelier annuaire) {
    return matches(ligne.cle(), ligne.element(), annuaire);
  }

  public boolean matches(FinAutomatiqueEnListe ligne, AnnuaireDAtelier annuaire) {
    return matches(ligne.cle(), ligne.element(), annuaire);
  }

  private boolean matches(CleDActivite cle, ElementEngage ligneElement, AnnuaireDAtelier annuaire) {
    String libelleOperateur = annuaire
      .operateur(cle.operateur())
      .map(fiche -> fiche.prenom().value() + " " + fiche.nom().value())
      .orElse("");
    return (
      (libelleOperateur.toLowerCase(Locale.ROOT).contains(operateur) || cle.operateur().uuid().toString().contains(operateur))
      && (ligneElement.nom().value().toLowerCase(Locale.ROOT).contains(element) || ligneElement.id().uuid().toString().contains(element))
    );
  }
}
