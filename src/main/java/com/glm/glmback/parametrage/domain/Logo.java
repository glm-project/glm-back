package com.glm.glmback.parametrage.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Le logo de l'entreprise, tel qu'il a ete depose : son contenu, son format et sa version.
 */
public final class Logo {

  private static final int LONGUEUR_DE_VERSION = 16;

  private final byte[] contenu;
  private final FormatDImage format;
  private final VersionDuLogo version;

  public Logo(byte[] contenu, FormatDImage format, VersionDuLogo version) {
    Assert.notNull("contenu", contenu);
    Assert.notNull("format", format);
    Assert.notNull("version", version);
    this.contenu = contenu.clone();
    this.format = format;
    this.version = version;
  }

  static Logo de(byte[] contenu, FormatDImage format) {
    return new Logo(contenu, format, new VersionDuLogo(empreinte(contenu)));
  }

  /**
   * Une empreinte de cache, pas une preuve : deux contenus differents n'ont pratiquement jamais la meme, et aucun secret
   * n'en depend.
   */
  private static String empreinte(byte[] contenu) {
    return UUID.nameUUIDFromBytes(contenu).toString().replace("-", "").substring(0, LONGUEUR_DE_VERSION);
  }

  public byte[] contenu() {
    return contenu.clone();
  }

  public FormatDImage format() {
    return format;
  }

  public VersionDuLogo version() {
    return version;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Logo logo && Arrays.equals(contenu, logo.contenu) && format == logo.format && version.equals(logo.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(contenu), format, version);
  }
}
