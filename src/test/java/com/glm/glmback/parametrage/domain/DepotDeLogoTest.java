package com.glm.glmback.parametrage.domain;

import static com.glm.glmback.parametrage.domain.ImagesFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class DepotDeLogoTest {

  private final LogosEnMemoire logos = new LogosEnMemoire();

  @Test
  void shouldDeposerUnPngDe50Sur50() {
    Logo logo = depot(new ImageLue("png", 50, 50)).depose(pngCarre50());

    assertThat(logo.format()).isEqualTo(FormatDImage.PNG);
    assertThat(logo.contenu()).isEqualTo(pngCarre50());
    assertThat(logos.get()).contains(logo);
  }

  @Test
  void shouldDeposerUnJpegQuelQueSoitLaCasseDuFormat() {
    assertThat(depot(new ImageLue("JPEG", 50, 50)).depose(jpegCarre50()).format()).isEqualTo(FormatDImage.JPEG);
  }

  @Test
  void shouldRefuserUnFichierTropLourdSansLeDecoder() {
    DepotDeLogo depot = new DepotDeLogo(
      contenu -> {
        throw new AssertionError("un fichier trop lourd ne se decode pas");
      },
      logos
    );

    assertThatThrownBy(() -> depot.depose(pngCarre50Alourdi()))
      .isExactlyInstanceOf(LogoInvalideException.class)
      .hasMessage("Le logo pese 25600 octets, au plus 20480");
    assertThat(logos.get()).isEmpty();
  }

  @Test
  void shouldAccepterUnFichierDe20KoPile() {
    byte[] contenu = new byte[20 * 1024];

    assertThat(depot(new ImageLue("png", 50, 50)).depose(contenu).contenu()).hasSize(20 * 1024);
  }

  @Test
  void shouldRefuserUnFichierIllisible() {
    assertThatThrownBy(() -> new DepotDeLogo(contenu -> Optional.empty(), logos).depose(texte()))
      .isExactlyInstanceOf(LogoInvalideException.class)
      .hasMessage("Le fichier n'est pas une image lisible");
  }

  @Test
  void shouldRefuserUnAutreFormat() {
    assertThatThrownBy(() -> depot(new ImageLue("gif", 50, 50)).depose(gifCarre50()))
      .isExactlyInstanceOf(LogoInvalideException.class)
      .hasMessage("Le logo doit etre une image PNG ou JPEG (recu : gif)");
  }

  @Test
  void shouldDeposerUnLogoRectangulaire() {
    assertThat(depot(new ImageLue("png", 256, 100)).depose(pngCarre50())).isEqualTo(logos.get().orElseThrow());
  }

  @Test
  void shouldDeposerUnLogoDe256Sur256() {
    assertThat(depot(new ImageLue("png", 256, 256)).depose(pngCarre50())).isEqualTo(logos.get().orElseThrow());
  }

  @Test
  void shouldRefuserUneLargeurAuDelaDe256() {
    assertThatThrownBy(() -> depot(new ImageLue("png", 257, 100)).depose(pngCarre50()))
      .isExactlyInstanceOf(LogoInvalideException.class)
      .hasMessage("Le logo doit tenir dans 256 x 256 pixels (recu : 257 x 100)");
  }

  @Test
  void shouldRefuserUneHauteurAuDelaDe256() {
    assertThatThrownBy(() -> depot(new ImageLue("png", 100, 257)).depose(pngCarre50()))
      .isExactlyInstanceOf(LogoInvalideException.class)
      .hasMessage("Le logo doit tenir dans 256 x 256 pixels (recu : 100 x 257)");
    assertThat(logos.get()).isEmpty();
  }

  private DepotDeLogo depot(ImageLue image) {
    return new DepotDeLogo(contenu -> Optional.of(image), logos);
  }
}
