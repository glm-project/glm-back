package com.glm.glmback.parametrage.domain;

public final class ParametrageEnMemoire implements ParametrageRepository {

  private Parametrage parametrage = new Parametrage(DureeMaxDActivite.parDefaut());

  @Override
  public Parametrage get() {
    return parametrage;
  }

  @Override
  public Parametrage update(Parametrage nouveau) {
    parametrage = nouveau;

    return nouveau;
  }
}
