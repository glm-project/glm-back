package com.glm.glmback.parametrage.domain;

/**
 * Le logo tient dans un carre de 256 x 256 pixels, dans les proportions de son choix : chaque en-tete (supervision,
 * pupitre, PDF) l'ajuste a sa case sans le deformer. GLM ne le recadre ni ne le convertit, et il reste assez leger
 * pour etre garde hors ligne par le pupitre.
 */
public final class DepotDeLogo {

  private static final int COTE_MAXIMAL = 256;
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
    if (image.largeur() > COTE_MAXIMAL || image.hauteur() > COTE_MAXIMAL) {
      throw new LogoInvalideException(
        "Le logo doit tenir dans %d x %d pixels (recu : %d x %d)".formatted(
          COTE_MAXIMAL,
          COTE_MAXIMAL,
          image.largeur(),
          image.hauteur()
        )
      );
    }

    return logos.update(Logo.de(contenu, format));
  }
}
