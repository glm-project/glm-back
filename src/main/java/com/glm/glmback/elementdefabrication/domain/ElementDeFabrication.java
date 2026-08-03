package com.glm.glmback.elementdefabrication.domain;

import java.time.Instant;

public sealed interface ElementDeFabrication permits OrdreDeFabrication, Produit {
  ElementDeFabricationId id();

  Fiche fiche();

  default Titre titre() {
    return fiche().titre();
  }

  default Description description() {
    return fiche().description();
  }

  default Instant dateDeCreation() {
    return fiche().dateDeCreation();
  }

  default Instant dateDeModification() {
    return fiche().dateDeModification();
  }
}
