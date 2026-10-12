package com.glm.glmback.elementdefabrication.domain;

public final class ClotureHorsDesCloturesException extends RuntimeException {

  public ClotureHorsDesCloturesException() {
    super("Une periode de cloture ne se demande qu'avec le cycle CLOTURE");
  }
}
