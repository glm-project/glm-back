package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class PointagesSignalesServiceTest {

  private final PointagesSignalesEnMemoire signalements = new PointagesSignalesEnMemoire();
  private final PointagesSignalesService service = new PointagesSignalesService(signalements, () -> LE_11_MAI_2026_A_9H15);

  @Test
  void shouldListerLesSignalementsNonResolusLePlusRecentDAbord() {
    PointageSignale matin = signalements.create(signale(OPERATEUR_ID_DUPONT, MotifDeSignalement.DATE_FUTURE, LE_10_MAI_2026_A_8H));
    PointageSignale soir = signalements.create(
      signale(OPERATEUR_ID_DUPONT, MotifDeSignalement.OPERATEUR_NON_HABILITE, LE_10_MAI_2026_A_17H)
    );
    signalements.create(
      signale(OPERATEUR_ID_DUPONT, MotifDeSignalement.DATE_FUTURE, LE_10_MAI_2026_A_12H).resolu(
        new Resolution(TypeDeResolution.ANNULE, AUTEUR_LEROY, LE_11_MAI_2026_A_9H15)
      )
    );

    assertThat(service.list(Optional.empty(), Optional.empty(), firstPageOfTen()).content()).containsExactly(soir, matin);
  }

  @Test
  void shouldFiltrerParOperateurEtParMotif() {
    PointageSignale deMartin = signalements.create(
      signale(OPERATEUR_ID_MARTIN, MotifDeSignalement.OPERATEUR_NON_HABILITE, LE_10_MAI_2026_A_8H)
    );
    signalements.create(signale(OPERATEUR_ID_DUPONT, MotifDeSignalement.DATE_FUTURE, LE_10_MAI_2026_A_9H));

    assertThat(service.list(Optional.of(OPERATEUR_ID_MARTIN), Optional.empty(), firstPageOfTen()).content()).containsExactly(deMartin);
    assertThat(
      service.list(Optional.empty(), Optional.of(MotifDeSignalement.OPERATEUR_NON_HABILITE), firstPageOfTen()).content()
    ).containsExactly(deMartin);
  }

  /**
   * Le gestionnaire juge le pointage legitime : il l'acquitte, le temps compte toujours, la ligne disparait.
   */
  @Test
  void shouldAcquitterUnSignalement() {
    PointageSignale signale = signalements.create(signale(OPERATEUR_ID_DUPONT, MotifDeSignalement.DATE_FUTURE, LE_10_MAI_2026_A_8H));

    PointageSignale acquitte = service.acquitte(signale.id(), AUTEUR_LEROY);

    assertThat(acquitte.resolution()).contains(new Resolution(TypeDeResolution.ACQUITTE, AUTEUR_LEROY, LE_11_MAI_2026_A_9H15));
    assertThat(signalements.get(signale.id())).contains(acquitte);
    assertThat(service.list(Optional.empty(), Optional.empty(), firstPageOfTen()).content()).isEmpty();
  }

  @Test
  void shouldNePasAcquitterUnSignalementInconnu() {
    PointageSignaleId inconnu = new PointageSignaleId(UUID.randomUUID());

    assertThatThrownBy(() -> service.acquitte(inconnu, AUTEUR_LEROY))
      .isExactlyInstanceOf(PointageSignaleIntrouvableException.class)
      .hasMessageContaining(inconnu.uuid().toString());
  }

  @Test
  void shouldNePasAcquitterDeuxFois() {
    PointageSignale signale = signalements.create(signale(OPERATEUR_ID_DUPONT, MotifDeSignalement.DATE_FUTURE, LE_10_MAI_2026_A_8H));
    service.acquitte(signale.id(), AUTEUR_LEROY);

    assertThatThrownBy(() -> service.acquitte(signale.id(), AUTEUR_LEROY)).isExactlyInstanceOf(PointageSignaleDejaResoluException.class);
  }

  @Test
  void shouldNeRetenirQueLesSignalementsNonResolusCorrespondants() {
    CriteresDePointageSignale tous = new CriteresDePointageSignale(Optional.empty(), Optional.empty());
    PointageSignale ouvert = signale(OPERATEUR_ID_DUPONT, MotifDeSignalement.DATE_FUTURE, LE_10_MAI_2026_A_8H);

    assertThat(tous.matches(ouvert)).isTrue();
    assertThat(tous.matches(ouvert.resolu(new Resolution(TypeDeResolution.CORRIGE, AUTEUR_LEROY, LE_11_MAI_2026_A_9H15)))).isFalse();
    assertThat(new CriteresDePointageSignale(Optional.of(OPERATEUR_ID_MARTIN), Optional.empty()).matches(ouvert)).isFalse();
    assertThat(
      new CriteresDePointageSignale(Optional.empty(), Optional.of(MotifDeSignalement.OPERATEUR_NON_HABILITE)).matches(ouvert)
    ).isFalse();
  }

  @Test
  void shouldNotBuildCriteresWithoutFiltres() {
    assertThatThrownBy(() -> new CriteresDePointageSignale(null, Optional.empty())).isExactlyInstanceOf(
      com.glm.glmback.shared.error.domain.MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new CriteresDePointageSignale(Optional.empty(), null)).isExactlyInstanceOf(
      com.glm.glmback.shared.error.domain.MissingMandatoryValueException.class
    );
  }

  private static PointageSignale signale(OperateurId operateur, MotifDeSignalement motif, Instant date) {
    return PointageSignale.builder()
      .id(new PointageSignaleId(UUID.randomUUID()))
      .cible(new CibleDuSignalement(TypeDeCible.SUIVI_D_ATELIER, UUID.randomUUID()))
      .operateur(operateur)
      .motifs(Set.of(motif))
      .horodatage(Horodatage.saisiA(date))
      .dateDeclaree(Optional.empty())
      .resolution(Optional.empty());
  }
}
