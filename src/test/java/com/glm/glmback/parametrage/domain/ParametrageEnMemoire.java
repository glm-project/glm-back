package com.glm.glmback.parametrage.domain;

import java.util.Optional;

/**
 * Le parametrage d'une seule entreprise, en memoire : une ligne au plus, comme la table semee par Liquibase.
 */
final class ParametrageEnMemoire implements ParametrageRepository {

  private Optional<Parametrage> parametrage;

  private ParametrageEnMemoire(Optional<Parametrage> parametrage) {
    this.parametrage = parametrage;
  }

  static ParametrageEnMemoire seme(Parametrage parametrage) {
    return new ParametrageEnMemoire(Optional.of(parametrage));
  }

  static ParametrageEnMemoire vide() {
    return new ParametrageEnMemoire(Optional.empty());
  }

  @Override
  public Parametrage get() {
    return parametrage.orElseThrow(ParametrageIntrouvableException::new);
  }

  @Override
  public void update(Parametrage nouveau) {
    if (parametrage.isEmpty()) {
      throw new ParametrageIntrouvableException();
    }
    parametrage = Optional.of(nouveau);
  }
}
