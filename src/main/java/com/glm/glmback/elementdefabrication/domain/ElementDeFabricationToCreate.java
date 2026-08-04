package com.glm.glmback.elementdefabrication.domain;

public sealed interface ElementDeFabricationToCreate permits OrdreDeFabricationToCreate, ProduitToCreate {
  Titre titre();

  Description description();
}
