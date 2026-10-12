package com.glm.glmback.syntheseheures.infrastructure.primary;

import java.util.stream.Stream;

/** Les deux versions du rapport PDF : la synthese, ou la synthese suivie du journal de tous les pointages. */
enum VersionDuRapport {
  SYNTHESE("synthese", "Rapport d'activité"),
  DETAIL("detail", "Rapport d'activité · détaillé");

  private final String code;
  private final String titre;

  VersionDuRapport(String code, String titre) {
    this.code = code;
    this.titre = titre;
  }

  /** Le motif de la route a deja refuse toute autre valeur. */
  static VersionDuRapport de(String code) {
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
