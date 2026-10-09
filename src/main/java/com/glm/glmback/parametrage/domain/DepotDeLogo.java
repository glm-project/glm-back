package com.glm.glmback.parametrage.domain;

/**
 * Le logo s'affiche en en-tete de la supervision, du pupitre et des PDF, dans une case de 50 x 50 pixels : il y entre
 * tel quel, sans recadrage ni conversion, et reste assez leger pour etre garde hors ligne par le pupitre.
 */
public final class DepotDeLogo {

  private static final int COTE = 50;
  private static final int POIDS_MAXIMAL = 20 * 1024;

  private final DecodeurDImage decodeur;
  private final LogoRepository logos;

  public DepotDeLogo(DecodeurDImage decodeur, LogoRepository logos) {
    this.decodeur = decodeur;
    this.logos = logos;
  }

  /**
   * Le poids se juge avant tout decodage : un fichier trop lourd n'est jamais lu.
   */
  public Logo depose(byte[] contenu) {
    if (contenu.length > POIDS_MAXIMAL) {
      throw new LogoInvalideException("Le logo pese %d octets, au plus %d".formatted(contenu.length, POIDS_MAXIMAL));
    }
    ImageLue image = decodeur.lis(contenu).orElseThrow(() -> new LogoInvalideException("Le fichier n'est pas une image lisible"));
    FormatDImage format = FormatDImage.depuis(image.format()).orElseThrow(() ->
      new LogoInvalideException("Le logo doit etre une image PNG ou JPEG (recu : %s)".formatted(image.format()))
    );
    if (image.largeur() != COTE || image.hauteur() != COTE) {
      throw new LogoInvalideException(
        "Le logo doit mesurer %d x %d pixels (recu : %d x %d)".formatted(COTE, COTE, image.largeur(), image.hauteur())
      );
    }

    return logos.update(Logo.de(contenu, format));
  }
}
