package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JournalDePresenceTest {

  @Test
  void shouldNotBuildWithoutEvenements() {
    assertThatThrownBy(() -> new JournalDePresence(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evenements");
  }

  @Test
  void shouldNotBuildWithNullEvenement() {
    List<EvenementDePresence> avecNull = Collections.singletonList(null);

    assertThatThrownBy(() -> new JournalDePresence(avecNull)).isExactlyInstanceOf(NullElementInCollectionException.class);
  }

  @Test
  void shouldBuildJournalVide() {
    JournalDePresence journal = JournalDePresence.vide();

    assertThat(journal.evenements()).isEmpty();
    assertThat(journal.fenetres()).isEmpty();
    assertThat(journal.debut()).isEmpty();
    assertThat(journal.amplitude()).isEmpty();
    assertThat(journal.etat()).isEqualTo(EtatDePresence.ABSENT);
  }

  @Test
  void shouldTrierLesEvenementsParDateDeSurvenue() {
    JournalDePresence journal = new JournalDePresence(
      List.of(departDeDupontA(LE_10_MAI_2026_A_17H), arriveeDeDupontA(LE_10_MAI_2026_A_7H))
    );

    assertThat(journal.evenements().stream().map(EvenementDePresence::type)).containsExactly(
      TypeDEvenementDePresence.ARRIVEE,
      TypeDEvenementDePresence.DEPART
    );
  }

  @Test
  void shouldRefuserUneSequenceImpossible() {
    List<EvenementDePresence> repriseSansPause = List.of(arriveeDeDupontA(LE_10_MAI_2026_A_7H), repriseDeDupontA(LE_10_MAI_2026_A_9H));

    assertThatThrownBy(() -> new JournalDePresence(repriseSansPause))
      .isExactlyInstanceOf(TransitionDePresenceInterditeException.class)
      .hasMessageContaining("REPRISE");
  }

  @Test
  void shouldOuvrirUneFenetreDePresenceDesLArrivee() {
    JournalDePresence journal = JournalDePresence.vide().enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_7H));

    assertThat(journal.fenetres()).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.empty()));
    assertThat(journal.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(journal.etat()).isEqualTo(EtatDePresence.PRESENT);
    assertThat(journal.amplitude()).isEmpty();
  }

  @Test
  void shouldScinderLaPresenceAutourDeLaPause() {
    JournalDePresence journal = journeeDeDupontDe7HA17HAvecPauseDeMidi().journal();

    assertThat(journal.fenetres()).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H)),
      new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
    );
    assertThat(journal.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
    assertThat(journal.etat()).isEqualTo(EtatDePresence.ABSENT);
  }

  @Test
  void shouldResterEnPauseTantQueLaRepriseNEstPasPointee() {
    JournalDePresence journal = JournalDePresence.vide()
      .enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_7H))
      .enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H));

    assertThat(journal.etat()).isEqualTo(EtatDePresence.EN_PAUSE);
    assertThat(journal.fenetres()).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H)));
  }

  @Test
  void shouldEcarterDuRepliUnEvenementAnnule() {
    JournalDePresence journal = journeeDeDupontDe7HA17HAvecPauseDeMidi().journal();
    EvenementDePresenceId depart = journal.evenements().getLast().id();

    JournalDePresence sansDepart = journal.annule(depart, annulationParLeroy());

    assertThat(sansDepart.evenements()).hasSize(4);
    assertThat(sansDepart.amplitude()).isEmpty();
    assertThat(sansDepart.fenetres().getLast().estOuverte()).isTrue();
  }

  @Test
  void shouldNotAnnulerUnEvenementInconnu() {
    JournalDePresence journal = journeeDeDupontDe7HA17HAvecPauseDeMidi().journal();
    EvenementDePresenceId inconnu = EvenementDePresenceId.newId();
    Annulation annulation = annulationParLeroy();

    assertThatThrownBy(() -> journal.annule(inconnu, annulation)).isExactlyInstanceOf(EvenementDePresenceIntrouvableException.class);
  }

  @Test
  void shouldRemplacerUnEvenementParSaVersionCorrigeeEnLaissantLesAutresEnPlace() {
    JournalDePresence journal = JournalDePresence.vide()
      .enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_9H))
      .enregistre(pauseDeDupontA(LE_10_MAI_2026_A_12H));
    EvenementDePresenceId arrivee = journal.evenements().getFirst().id();

    JournalDePresence corrige = journal.corrige(arrivee, annulationParLeroy(), arriveeDeDupontA(LE_10_MAI_2026_A_7H));

    assertThat(corrige.evenements()).hasSize(3);
    assertThat(corrige.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(corrige.etat()).isEqualTo(EtatDePresence.EN_PAUSE);
  }

  @Test
  void shouldNotCorrigerUnEvenementInconnu() {
    JournalDePresence journal = JournalDePresence.vide().enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_9H));
    EvenementDePresenceId inconnu = EvenementDePresenceId.newId();
    Annulation annulation = annulationParLeroy();
    EvenementDePresence remplacant = arriveeDeDupontA(LE_10_MAI_2026_A_7H);

    assertThatThrownBy(() -> journal.corrige(inconnu, annulation, remplacant)).isExactlyInstanceOf(
      EvenementDePresenceIntrouvableException.class
    );
  }
}
