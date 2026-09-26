package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.StringTooLongException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class PointageEnAttenteTest {

  private static final UUID EVENEMENT = UUID.fromString("7a1b2c3d-4e5f-4061-8192-a3b4c5d6e7f8");
  private static final GesteDePresence PAUSE_DE_DUPONT = new GesteDePresence(
    OPERATEUR_ID_DUPONT,
    TypeDEvenementDePresence.PAUSE,
    Optional.of(LE_10_MAI_2026_A_12H)
  );

  @Test
  void shouldConserverLeGesteTelQuel() {
    PointageEnAttenteId id = PointageEnAttenteId.newId();

    PointageEnAttente pointage = PointageEnAttente.builder()
      .id(id)
      .evenementDuPupitre(EVENEMENT)
      .geste(PAUSE_DE_DUPONT)
      .motif(MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE)
      .auteur(AUTEUR_DUPONT)
      .dateDeReception(LE_10_MAI_2026_A_13H)
      .traitement(Optional.empty());

    assertThat(pointage.id()).isEqualTo(id);
    assertThat(pointage.evenementDuPupitre()).isEqualTo(EVENEMENT);
    assertThat(pointage.geste()).isEqualTo(PAUSE_DE_DUPONT);
    assertThat(pointage.motif()).isEqualTo(MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE);
    assertThat(pointage.auteur()).isEqualTo(AUTEUR_DUPONT);
    assertThat(pointage.dateDeReception()).isEqualTo(LE_10_MAI_2026_A_13H);
    assertThat(pointage.estTraite()).isFalse();
  }

  @Test
  void shouldGarderLaDateDuPupitre() {
    assertThat(recuA(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_12H)).dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_12H);
  }

  @Test
  void shouldGarderUneDateDuPupitreEgaleALaReception() {
    assertThat(recuA(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_13H)).dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_13H);
  }

  @Test
  void shouldDaterALaReceptionUnGesteSansDate() {
    assertThat(recuA(LE_10_MAI_2026_A_13H, Optional.empty()).dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_13H);
  }

  @Test
  void shouldRamenerALaReceptionUneDateFuture() {
    assertThat(recuA(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H)).dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_13H);
  }

  @Test
  void shouldSortirDeLaListeUneFoisApplique() {
    Application application = new Application(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15);

    PointageEnAttente traite = recuA(LE_10_MAI_2026_A_13H, Optional.empty()).traite(application);

    assertThat(traite.estTraite()).isTrue();
    assertThat(traite.traitement()).contains(application);
  }

  @Test
  void shouldSortirDeLaListeUneFoisEcarte() {
    Ecart ecart = new Ecart(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, new MotifDEcart("Doublon d'un pointage deja saisi"));

    PointageEnAttente traite = recuA(LE_10_MAI_2026_A_13H, Optional.empty()).traite(ecart);

    assertThat(traite.traitement()).contains(ecart);
  }

  @Test
  void shouldNeSeTraiterQuUneFois() {
    PointageEnAttente traite = recuA(LE_10_MAI_2026_A_13H, Optional.empty()).traite(new Application(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15));

    assertThatThrownBy(() -> traite.traite(new Application(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15)))
      .isExactlyInstanceOf(PointageEnAttenteDejaTraiteException.class)
      .hasMessageContaining(traite.id().uuid().toString());
  }

  @Test
  void shouldNotBuildWithoutMandatoryValues() {
    assertThatThrownBy(() ->
      new PointageEnAttente(
        null,
        EVENEMENT,
        PAUSE_DE_DUPONT,
        MotifDeMiseEnAttente.OPERATEUR_INCONNU,
        AUTEUR_DUPONT,
        LE_10_MAI_2026_A_13H,
        Optional.empty()
      )
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new PointageEnAttente(
        PointageEnAttenteId.newId(),
        null,
        PAUSE_DE_DUPONT,
        MotifDeMiseEnAttente.OPERATEUR_INCONNU,
        AUTEUR_DUPONT,
        LE_10_MAI_2026_A_13H,
        Optional.empty()
      )
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new PointageEnAttente(
        PointageEnAttenteId.newId(),
        EVENEMENT,
        null,
        MotifDeMiseEnAttente.OPERATEUR_INCONNU,
        AUTEUR_DUPONT,
        LE_10_MAI_2026_A_13H,
        Optional.empty()
      )
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new PointageEnAttente(
        PointageEnAttenteId.newId(),
        EVENEMENT,
        PAUSE_DE_DUPONT,
        null,
        AUTEUR_DUPONT,
        LE_10_MAI_2026_A_13H,
        Optional.empty()
      )
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new PointageEnAttente(
        PointageEnAttenteId.newId(),
        EVENEMENT,
        PAUSE_DE_DUPONT,
        MotifDeMiseEnAttente.OPERATEUR_INCONNU,
        null,
        LE_10_MAI_2026_A_13H,
        Optional.empty()
      )
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new PointageEnAttente(
        PointageEnAttenteId.newId(),
        EVENEMENT,
        PAUSE_DE_DUPONT,
        MotifDeMiseEnAttente.OPERATEUR_INCONNU,
        AUTEUR_DUPONT,
        null,
        Optional.empty()
      )
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new PointageEnAttente(
        PointageEnAttenteId.newId(),
        EVENEMENT,
        PAUSE_DE_DUPONT,
        MotifDeMiseEnAttente.OPERATEUR_INCONNU,
        AUTEUR_DUPONT,
        LE_10_MAI_2026_A_13H,
        null
      )
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldConstruireUnGesteDAtelier() {
    SuiviDAtelierId suivi = SuiviDAtelierId.newId();

    GesteDAtelier geste = GesteDAtelier.builder()
      .suivi(suivi)
      .operateur(OPERATEUR_ID_MARTIN)
      .type(TypeDEvenementDAtelier.DEBUT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .dateDeclaree(Optional.of(LE_10_MAI_2026_A_8H));

    assertThat(geste.suivi()).isEqualTo(suivi);
    assertThat(geste.operateur()).isEqualTo(OPERATEUR_ID_MARTIN);
    assertThat(geste.type()).isEqualTo(TypeDEvenementDAtelier.DEBUT);
    assertThat(geste.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(geste.dateDeclaree()).contains(LE_10_MAI_2026_A_8H);
  }

  @Test
  void shouldNotBuildGestesWithoutMandatoryValues() {
    SuiviDAtelierId suivi = SuiviDAtelierId.newId();
    Optional<PosteDeTravailId> sansPoste = Optional.empty();
    Optional<Instant> sansDate = Optional.empty();

    assertThatThrownBy(() -> new GesteDePresence(null, TypeDEvenementDePresence.PAUSE, sansDate)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new GesteDePresence(OPERATEUR_ID_DUPONT, null, sansDate)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new GesteDePresence(OPERATEUR_ID_DUPONT, TypeDEvenementDePresence.PAUSE, null)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() ->
      new GesteDAtelier(null, OPERATEUR_ID_DUPONT, TypeDEvenementDAtelier.DEBUT, sansPoste, sansDate)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new GesteDAtelier(suivi, null, TypeDEvenementDAtelier.DEBUT, sansPoste, sansDate)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new GesteDAtelier(suivi, OPERATEUR_ID_DUPONT, null, sansPoste, sansDate)).isExactlyInstanceOf(
      MissingMandatoryValueException.class
    );
    assertThatThrownBy(() ->
      new GesteDAtelier(suivi, OPERATEUR_ID_DUPONT, TypeDEvenementDAtelier.DEBUT, null, sansDate)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      new GesteDAtelier(suivi, OPERATEUR_ID_DUPONT, TypeDEvenementDAtelier.DEBUT, sansPoste, null)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new GesteRecu(null, PAUSE_DE_DUPONT, AUTEUR_DUPONT)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new GesteRecu(EVENEMENT, null, AUTEUR_DUPONT)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new GesteRecu(EVENEMENT, PAUSE_DE_DUPONT, null)).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldNotBuildTraitementsWithoutMandatoryValues() {
    MotifDEcart motif = new MotifDEcart("Doublon");

    assertThatThrownBy(() -> new Application(null, LE_10_MAI_2026_A_13H)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new Application(AUTEUR_LEROY, null)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new Ecart(null, LE_10_MAI_2026_A_13H, motif)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new Ecart(AUTEUR_LEROY, null, motif)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new Ecart(AUTEUR_LEROY, LE_10_MAI_2026_A_13H, null)).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  @Test
  void shouldExigerUnMotifDEcart() {
    assertThatThrownBy(() -> new MotifDEcart(null)).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new MotifDEcart(" ")).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> new MotifDEcart("a".repeat(256))).isExactlyInstanceOf(StringTooLongException.class);
    assertThat(new MotifDEcart("a".repeat(255)).value()).hasSize(255);
  }

  @Test
  void shouldOrdonnerLesIdentifiants() {
    PointageEnAttenteId premier = new PointageEnAttenteId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    PointageEnAttenteId second = new PointageEnAttenteId(UUID.fromString("00000000-0000-0000-0000-000000000002"));

    assertThat(premier).isLessThan(second);
    assertThat(PointageEnAttenteId.newId()).isNotEqualTo(PointageEnAttenteId.newId());
    assertThatThrownBy(() -> new PointageEnAttenteId(null)).isExactlyInstanceOf(MissingMandatoryValueException.class);
  }

  private static PointageEnAttente recuA(Instant reception, Optional<Instant> dateDeclaree) {
    return PointageEnAttente.builder()
      .id(PointageEnAttenteId.newId())
      .evenementDuPupitre(EVENEMENT)
      .geste(new GesteDePresence(OPERATEUR_ID_DUPONT, TypeDEvenementDePresence.PAUSE, dateDeclaree))
      .motif(MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE)
      .auteur(AUTEUR_DUPONT)
      .dateDeReception(reception)
      .traitement(Optional.empty());
  }
}
