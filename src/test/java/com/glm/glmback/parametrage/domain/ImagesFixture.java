package com.glm.glmback.parametrage.domain;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * De vraies images, produites en memoire : le decodeur les lit comme il lirait un fichier depose.
 */
public final class ImagesFixture {

  private static final int POIDS_AU_DELA_DU_MAXIMUM = 60 * 1024;

  private ImagesFixture() {}

  public static byte[] pngCarre50() {
    return image("png", 50, 50, Color.BLUE);
  }

  public static byte[] pngCarre50Rouge() {
    return image("png", 50, 50, Color.RED);
  }

  public static byte[] jpegCarre50() {
    return image("jpeg", 50, 50, Color.BLUE);
  }

  public static byte[] gifCarre50() {
    return image("gif", 50, 50, Color.BLUE);
  }

  public static byte[] pngDe256Sur100() {
    return image("png", 256, 100, Color.BLUE);
  }

  public static byte[] pngDe300Sur80() {
    return image("png", 300, 80, Color.BLUE);
  }

  /**
   * Un PNG de 50 x 50 lisible, suivi d'octets que le decodeur ignore : seul son poids depasse.
   */
  public static byte[] pngCarre50Alourdi() {
    return Arrays.copyOf(pngCarre50(), POIDS_AU_DELA_DU_MAXIMUM);
  }

  public static byte[] texte() {
    return "ceci n'est pas une image".getBytes();
  }

  /**
   * La signature d'un PNG, sans en-tete d'image : un lecteur PNG est choisi, puis echoue a lire les dimensions.
   */
  public static byte[] pngTronque() {
    return Arrays.copyOf(pngCarre50(), 12);
  }

  private static byte[] image(String format, int largeur, int hauteur, Color couleur) {
    BufferedImage image = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
    var dessin = image.createGraphics();
    dessin.setColor(couleur);
    dessin.fillRect(0, 0, largeur, hauteur);
    dessin.dispose();
    try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
      javax.imageio.ImageIO.write(image, format, sortie);
      return sortie.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
