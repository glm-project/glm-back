package com.glm.glmback.parametrage.domain;

public final class LogoIntrouvableException extends RuntimeException {

  public LogoIntrouvableException(VersionDuLogo version) {
    super("Le logo en version %s n'est pas le logo courant de l'entreprise".formatted(version.value()));
  }
}
