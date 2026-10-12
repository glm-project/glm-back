package com.glm.glmback.elementdefabrication.domain;

import static com.glm.glmback.elementdefabrication.domain.ElementsDeFabricationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class RechercheTest {

  @Test
  void shouldTrouverDansLeNomSansTenirCompteDeLaCasse() {
    assertThat(new Recherche("of-2026").trouveDans(elementDeFabricationOrdre1015())).isTrue();
  }

  @Test
  void shouldTrouverDansLaReference() {
    assertThat(new Recherche(" 101 ").trouveDans(elementDeFabricationOrdre1015())).isTrue();
  }

  @Test
  void shouldTrouverDansLaDescription() {
    assertThat(new Recherche("FONTE").trouveDans(elementDeFabricationOrdre1015())).isTrue();
  }

  @Test
  void shouldNePasTrouverUnTexteAbsent() {
    assertThat(new Recherche("bride").trouveDans(elementDeFabricationOrdre1015())).isFalse();
  }

  @Test
  void shouldNePasTrouverDansUneFicheVide() {
    assertThat(new Recherche("fonte").trouveDans(elementDeFabricationMouleSansReference())).isFalse();
  }

  @Test
  void shouldNeRienChercherPourUnTexteBlanc() {
    assertThat(Recherche.de(Optional.of("  "))).isEmpty();
    assertThat(Recherche.de(Optional.empty())).isEmpty();
  }

  @Test
  void shouldChercherUnTexteSaisi() {
    assertThat(Recherche.de(Optional.of("Fonte"))).contains(new Recherche("fonte"));
  }

  @Test
  void shouldNotBuildWithoutTexte() {
    assertThatThrownBy(() -> new Recherche(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("recherche");
  }
}
