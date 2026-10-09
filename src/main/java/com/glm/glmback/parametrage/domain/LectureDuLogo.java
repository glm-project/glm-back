package com.glm.glmback.parametrage.domain;

/**
 * Une version de logo designe un contenu, jamais le logo courant quel qu'il soit : une adresse gardee en cache ne doit
 * pas rendre un autre logo que celui qu'elle nomme.
 */
public final class LectureDuLogo {

  private final LogoRepository logos;

  public LectureDuLogo(LogoRepository logos) {
    this.logos = logos;
  }

  public Logo enVersion(VersionDuLogo version) {
    return logos
      .get()
      .filter(logo -> logo.version().equals(version))
      .orElseThrow(() -> new LogoIntrouvableException(version));
  }
}
