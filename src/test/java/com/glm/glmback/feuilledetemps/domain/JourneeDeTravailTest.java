package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneeDeTravailTest {

  @Test
  void shouldNotBuildWithoutJournal() {
    assertThatThrownBy(() -> new JourneeDeTravail(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldNotBuildWithNullEvenement() {
    List<EvenementDePresence> journal = Arrays.asList(arriveeA(LE_LUNDI_11_MAI_2026_A_8H), null);

    assertThatThrownBy(() -> new JourneeDeTravail(journal))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldNotHaveFenetreWithoutEvenement() {
    assertThat(new JourneeDeTravail(List.of()).fenetres()).isEmpty();
  }

  /**
   * La pause de midi scinde la journee en deux fenetres : c'est ce qui distingue le temps travaille de l'amplitude.
   */
  @Test
  void shouldScinderLaJourneeSurLaPause() {
    List<Plage> fenetres = journeeDuLundiDe8HA17HAvecPauseDeMidi().fenetres();

    assertThat(fenetres).hasSize(2);
    assertThat(fenetres.getFirst().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_8H);
    assertThat(fenetres.getFirst().fin()).contains(LE_LUNDI_11_MAI_2026_A_12H);
    assertThat(fenetres.getLast().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_13H);
    assertThat(fenetres.getLast().fin()).contains(LE_LUNDI_11_MAI_2026_A_17H);
  }

  @Test
  void shouldLaisserLaFenetreOuverteSansDepart() {
    List<Plage> fenetres = journeeDuMardiOuverteA8H().fenetres();

    assertThat(fenetres).hasSize(1);
    assertThat(fenetres.getFirst().debut()).isEqualTo(LE_MARDI_12_MAI_2026_A_8H);
    assertThat(fenetres.getFirst().fin()).isEmpty();
  }

  /**
   * L'adapter rend les evenements deja tries, mais le repli ne s'y fie pas : une regularisation arrive au journal
   * apres coup, a une heure anterieure.
   */
  @Test
  void shouldReplierDansLOrdreChronologiqueQuelQueSoitLOrdreRecu() {
    JourneeDeTravail journee = new JourneeDeTravail(
      List.of(
        departA(LE_LUNDI_11_MAI_2026_A_17H),
        repriseA(LE_LUNDI_11_MAI_2026_A_13H),
        pauseA(LE_LUNDI_11_MAI_2026_A_12H),
        arriveeA(LE_LUNDI_11_MAI_2026_A_8H)
      )
    );

    assertThat(journee.fenetres()).isEqualTo(journeeDuLundiDe8HA17HAvecPauseDeMidi().fenetres());
  }

  @Test
  void shouldRefuserUneSequenceImpossible() {
    List<EvenementDePresence> journal = List.of(pauseA(LE_LUNDI_11_MAI_2026_A_12H));

    assertThatThrownBy(() -> new JourneeDeTravail(journal))
      .isExactlyInstanceOf(TransitionDePresenceInterditeException.class)
      .hasMessageContaining("PAUSE")
      .hasMessageContaining("ABSENT");
  }

  @Test
  void shouldNeJamaisEtreAbandonneeSansEvenement() {
    assertThat(new JourneeDeTravail(List.of()).estAbandonneePour(LE_MARDI_12_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldEtreAbandonneeStrictementApresLeSeuil() {
    JourneeDeTravail lundi = journeeDuLundiDe7HSansDepart();

    assertThat(lundi.estAbandonneePour(LE_LUNDI_11_MAI_2026_A_17H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(lundi.estAbandonneePour(LE_LUNDI_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(lundi.estAbandonneePour(LE_LUNDI_11_MAI_2026_A_20H.plusSeconds(1), AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  @Test
  void shouldNeJamaisAbandonnerUneJourneeFermee() {
    JourneeDeTravail fermee = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_8H), departA(LE_LUNDI_11_MAI_2026_A_17H)));

    assertThat(fermee.estAbandonneePour(LE_MARDI_12_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldChercherLesFaitsConnusEntreLArriveeEtLeSeuil() {
    assertThat(journeeDuLundiDe7HSansDepart().fenetreDeRecherche(AMPLITUDE_MAXIMALE_13H)).contains(
      new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_20H))
    );
    assertThat(new JourneeDeTravail(List.of()).fenetreDeRecherche(AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  @Test
  void shouldFermerLaDerniereFenetreASaFinPresumee() {
    JourneeDeTravail presumee = journeeDuLundiDe7HSansDepart().presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H));

    assertThat(presumee.finPresumee()).contains(LE_LUNDI_11_MAI_2026_A_16H);
    assertThat(presumee.fenetres()).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)),
      new Plage(LE_LUNDI_11_MAI_2026_A_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H), true)
    );
  }

  @Test
  void shouldPresumerAuDernierPointageDePresenceSansPointageDAtelier() {
    assertThat(journeeDuLundiDe7HSansDepart().presumee(AMPLITUDE_MAXIMALE_13H, Optional.empty()).fenetres())
      .last()
      .isEqualTo(new Plage(LE_LUNDI_11_MAI_2026_A_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_13H), true));
  }

  @Test
  void shouldIgnorerUnPointageHorsDeLaFenetreDeRechercheOuAnterieur() {
    JourneeDeTravail lundi = journeeDuLundiDe7HSansDepart();

    assertThat(lundi.presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_MARDI_12_MAI_2026_A_8H)).finPresumee()).contains(
      LE_LUNDI_11_MAI_2026_A_13H
    );
    assertThat(lundi.presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_8H)).finPresumee()).contains(
      LE_LUNDI_11_MAI_2026_A_13H
    );
  }

  @Test
  void shouldNeRienPresumerDUneJourneeEnPause() {
    JourneeDeTravail enPause = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_8H), pauseA(LE_LUNDI_11_MAI_2026_A_12H)));

    assertThat(enPause.presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H)).fenetres()).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
    );
  }

  @Test
  void shouldNeRienPresumerDUneJourneeSansEvenement() {
    JourneeDeTravail vide = new JourneeDeTravail(List.of());

    assertThat(vide.presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H))).isEqualTo(vide);
  }

  @Test
  void shouldNotBuildWithoutFinPresumee() {
    List<EvenementDePresence> journal = List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_8H));

    assertThatThrownBy(() -> new JourneeDeTravail(journal, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin presumee");
  }

  /**
   * Une arrivee implicite et le depart d'un geste tardif partagent leur heure : l'arrivee passe devant, quel que soit
   * l'ordre dans lequel la base les rend. Sans ce departage, le repli strict de ce contexte echouerait.
   */
  @Test
  void shouldFairePasserLArriveeDevantUnDepartSimultane() {
    JourneeDeTravail nulle = new JourneeDeTravail(List.of(departA(LE_MARDI_12_MAI_2026_A_8H), arriveeA(LE_MARDI_12_MAI_2026_A_8H)));

    assertThat(nulle.fenetres()).containsExactly(new Plage(LE_MARDI_12_MAI_2026_A_8H, Optional.of(LE_MARDI_12_MAI_2026_A_8H)));
  }
}
