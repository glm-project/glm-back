package com.glm.glmback.coutderevient.infrastructure.primary;

import java.util.stream.Stream;

/** Les deux versions du compte rendu PDF : la synthese par nature, ou le detail de chaque pointage en plus. */
enum VersionDuDocument {
  SYNTHESE("synthese", "Coût de revient"),
  DETAIL("detail", "Coût de revient — détaillé");

  private final String code;
  private final String titre;

  VersionDuDocument(String code, String titre) {
    this.code = code;
    this.titre = titre;
  }

  /** Le motif de la route a deja refuse toute autre valeur. */
  static VersionDuDocument de(String code) {
    return Stream.of(values())
      .filter(version -> version.code.equals(code))
      .findFirst()
      .orElseThrow();
  }

  String code() {
    return code;
  }

  String titre() {
    return titre;
  }
}
