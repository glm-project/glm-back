package com.glm.glmback.elementdefabrication.domain;

public interface CompteurDElementsDeFabrication {
  long prochainNumeroDOrdreDeFabrication(Annee annee);

  long prochainNumeroDeProduit(Annee annee);
}
