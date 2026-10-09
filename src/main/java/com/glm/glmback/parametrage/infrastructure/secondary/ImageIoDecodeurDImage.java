package com.glm.glmback.parametrage.infrastructure.secondary;

import com.glm.glmback.parametrage.domain.DecodeurDImage;
import com.glm.glmback.parametrage.domain.ImageLue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;

/**
 * Lit l'en-tete de l'image sans en decoder les pixels : le format vient du contenu, jamais du nom du fichier ni du type
 * annonce par le client.
 */
@Component
class ImageIoDecodeurDImage implements DecodeurDImage {

  @Override
  public Optional<ImageLue> lis(byte[] contenu) {
    try (ImageInputStream flux = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenu))) {
      Iterator<ImageReader> lecteurs = ImageIO.getImageReaders(flux);
      if (!lecteurs.hasNext()) {
        return Optional.empty();
      }
      ImageReader lecteur = lecteurs.next();
      try {
        lecteur.setInput(flux);
        return Optional.of(new ImageLue(lecteur.getFormatName(), lecteur.getWidth(0), lecteur.getHeight(0)));
      } finally {
        lecteur.dispose();
      }
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
