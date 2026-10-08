package com.glm.glmback.categoriedeproduit.domain;

public final class OrdreIncompletException extends RuntimeException {

  public OrdreIncompletException() {
    super("L'ordre doit citer chaque categorie de l'entreprise, une fois et une seule");
  }
}
