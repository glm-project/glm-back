package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

  /**
   * Une arrivee implicite et le geste qu'elle precede partagent leur heure de survenue et d'enregistrement : a instant
   * egal, l'arrivee passe devant, quel que soit l'ordre des identifiants.
   */
  @Test
  void shouldFairePasserLArriveeDevantUnGesteSimultane() {
    EvenementDePresence arrivee = presenceSimultanee(new UUID(0, 2), TypeDEvenementDePresence.ARRIVEE);
    EvenementDePresence depart = presenceSimultanee(new UUID(0, 1), TypeDEvenementDePresence.DEPART);

    JournalDePresence journal = new JournalDePresence(List.of(depart, arrivee));

    assertThat(journal.evenements()).containsExactly(arrivee, depart);
    assertThat(journal.etat()).isEqualTo(EtatDePresence.ABSENT);
  }

  @Test
  void shouldRefuserUneSequenceImpossible() {
    List<EvenementDePresence> departAvantLArrivee = List.of(departDeDupontA(LE_10_MAI_2026_A_7H), arriveeDeDupontA(LE_10_MAI_2026_A_9H));

    assertThatThrownBy(() -> new JournalDePresence(departAvantLArrivee))
      .isExactlyInstanceOf(TransitionDePresenceInterditeException.class)
      .hasMessageContaining("DEPART");
  }

  @Test
  void shouldOuvrirUneFenetreDePresenceDesLArrivee() {
    JournalDePresence journal = JournalDePresence.vide().enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_7H));

    assertThat(journal.fenetres()).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.empty()));
    assertThat(journal.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(journal.etat()).isEqualTo(EtatDePresence.PRESENT);
    assertThat(journal.amplitude()).isEmpty();
  }

  /**
   * La pause n'est pas un fait de presence : de l'arrivee au depart, une seule fenetre, qui couvre l'amplitude.
   */
  @Test
  void shouldFermerLaFenetreDePresenceAuDepart() {
    JournalDePresence journal = journeeDeDupontDe7HA17H().journal();

    assertThat(journal.fenetres()).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_17H)));
    assertThat(journal.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
    assertThat(journal.etat()).isEqualTo(EtatDePresence.ABSENT);
  }

  /**
   * Une arrivee regularisee apres le depart rouvre la journee : chaque venue a sa fenetre.
   */
  @Test
  void shouldOuvrirUneFenetreParVenue() {
    JournalDePresence journal = journeeDeDupontDe7HA17H().journal().enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_20H));

    assertThat(journal.fenetres()).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_17H)),
      new FenetreDePresence(LE_10_MAI_2026_A_20H, Optional.empty())
    );
    assertThat(journal.etat()).isEqualTo(EtatDePresence.PRESENT);
    assertThat(journal.amplitude()).isEmpty();
  }

  @Test
  void shouldEcarterDuRepliUnEvenementAnnule() {
    JournalDePresence journal = journeeDeDupontDe7HA17H().journal();
    EvenementDePresenceId depart = journal.evenements().getLast().id();

    JournalDePresence sansDepart = journal.annule(depart, annulationParLeroy());

    assertThat(sansDepart.evenements()).hasSize(2);
    assertThat(sansDepart.amplitude()).isEmpty();
    assertThat(sansDepart.fenetres().getLast().estOuverte()).isTrue();
  }

  @Test
  void shouldNotAnnulerUnEvenementInconnu() {
    JournalDePresence journal = journeeDeDupontDe7HA17H().journal();
    EvenementDePresenceId inconnu = EvenementDePresenceId.newId();
    Annulation annulation = annulationParLeroy();

    assertThatThrownBy(() -> journal.annule(inconnu, annulation)).isExactlyInstanceOf(EvenementDePresenceIntrouvableException.class);
  }

  @Test
  void shouldRemplacerUnEvenementParSaVersionCorrigeeEnLaissantLesAutresEnPlace() {
    JournalDePresence journal = JournalDePresence.vide()
      .enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_9H))
      .enregistre(departDeDupontA(LE_10_MAI_2026_A_17H));
    EvenementDePresenceId arrivee = journal.evenements().getFirst().id();

    JournalDePresence corrige = journal.corrige(arrivee, annulationParLeroy(), arriveeDeDupontA(LE_10_MAI_2026_A_7H));

    assertThat(corrige.evenements()).hasSize(3);
    assertThat(corrige.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(corrige.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
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

  private static EvenementDePresence presenceSimultanee(UUID id, TypeDEvenementDePresence type) {
    return EvenementDePresence.builder()
      .id(new EvenementDePresenceId(id))
      .type(type)
      .auteur(AUTEUR_DUPONT)
      .horodatage(Horodatage.saisiA(LE_11_MAI_2026_A_8H30));
  }
}
