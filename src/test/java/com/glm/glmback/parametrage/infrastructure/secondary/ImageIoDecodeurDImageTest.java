package com.glm.glmback.parametrage.infrastructure.secondary;

import static com.glm.glmback.parametrage.domain.ImagesFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.parametrage.domain.ImageLue;
import org.junit.jupiter.api.Test;

@UnitTest
class ImageIoDecodeurDImageTest {

  private final ImageIoDecodeurDImage decodeur = new ImageIoDecodeurDImage();

  @Test
  void shouldReadPngFormatAndDimensions() {
    assertThat(decodeur.lis(pngDe120Sur80())).contains(new ImageLue("png", 120, 80));
  }

  @Test
  void shouldReadJpegFormat() {
    assertThat(decodeur.lis(jpegCarre50())).contains(new ImageLue("JPEG", 50, 50));
  }

  @Test
  void shouldReadGifFormat() {
    assertThat(decodeur.lis(gifCarre50())).contains(new ImageLue("gif", 50, 50));
  }

  @Test
  void shouldNotReadText() {
    assertThat(decodeur.lis(texte())).isEmpty();
  }

  @Test
  void shouldNotReadATruncatedImage() {
    assertThat(decodeur.lis(pngTronque())).isEmpty();
  }
}
