package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageSignaleTest {

  private static final PointageSignaleId ID = new PointageSignaleId(UUID.randomUUID());
  private static final CibleDuSignalement CIBLE = new CibleDuSignalement(TypeDeCible.SUIVI_D_ATELIER, UUID.randomUUID());

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> signale(null, Set.of(MotifDeSignalement.DATE_FUTURE)))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutMotif() {
    assertThatThrownBy(() -> signale(ID, Set.of()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("motifs");
  }

  @Test
  void shouldNotBuildWithoutCible() {
    assertThatThrownBy(() ->
      PointageSignale.builder()
        .id(ID)
        .cible(null)
        .operateur(OPERATEUR_ID_DUPONT)
        .motifs(Set.of(MotifDeSignalement.DATE_FUTURE))
        .horodatage(Horodatage.saisiA(LE_10_MAI_2026_A_8H))
        .dateDeclaree(Optional.empty())
        .resolution(Optional.empty())
    )
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("cible");
  }

  @Test
  void shouldNotBuildCibleWithoutType() {
    assertThatThrownBy(() -> new CibleDuSignalement(null, UUID.randomUUID())).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldNotBuildResolutionWithoutAuteur() {
    assertThatThrownBy(() -> new Resolution(TypeDeResolution.ACQUITTE, null, LE_10_MAI_2026_A_9H)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
  }

  @Test
  void shouldNaitreNonResolu() {
    assertThat(signale(ID, Set.of(MotifDeSignalement.DATE_FUTURE)).estResolu()).isFalse();
  }

  @Test
  void shouldEtreResolu() {
    Resolution acquittement = new Resolution(TypeDeResolution.ACQUITTE, AUTEUR_LEROY, LE_11_MAI_2026_A_9H15);

    PointageSignale resolu = signale(ID, Set.of(MotifDeSignalement.DATE_FUTURE)).resolu(acquittement);

    assertThat(resolu.estResolu()).isTrue();
    assertThat(resolu.resolution()).contains(acquittement);
    assertThat(resolu.id()).isEqualTo(ID);
  }

  @Test
  void shouldNePasEtreResoluDeuxFois() {
    Resolution acquittement = new Resolution(TypeDeResolution.ACQUITTE, AUTEUR_LEROY, LE_11_MAI_2026_A_9H15);
    PointageSignale resolu = signale(ID, Set.of(MotifDeSignalement.DATE_FUTURE)).resolu(acquittement);

    assertThatThrownBy(() -> resolu.resolu(acquittement))
      .isExactlyInstanceOf(PointageSignaleDejaResoluException.class)
      .hasMessageContaining(ID.uuid().toString());
  }

  private static PointageSignale signale(PointageSignaleId id, Set<MotifDeSignalement> motifs) {
    return PointageSignale.builder()
      .id(id)
      .cible(CIBLE)
      .operateur(OPERATEUR_ID_DUPONT)
      .motifs(motifs)
      .horodatage(Horodatage.saisiA(LE_10_MAI_2026_A_8H))
      .dateDeclaree(Optional.empty())
      .resolution(Optional.empty());
  }
}
