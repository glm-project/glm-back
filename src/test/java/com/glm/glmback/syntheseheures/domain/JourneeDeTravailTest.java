package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
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
  void shouldNotHaveFenetreOrPointageWithoutEvenement() {
    assertThat(new JourneeDeTravail(List.of()).fenetres()).isEmpty();
    assertThat(new JourneeDeTravail(List.of()).pointages()).isEmpty();
  }

  /**
   * La pause de midi scinde la journee en deux fenetres : c'est ce qui distingue le temps travaille de l'amplitude.
   */
  @Test
  void shouldScinderLaJourneeSurLaPause() {
    EvenementDePresence arrivee = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);
    EvenementDePresence pause = pauseA(LE_LUNDI_11_MAI_2026_A_12H);
    EvenementDePresence reprise = repriseA(LE_LUNDI_11_MAI_2026_A_13H);
    EvenementDePresence depart = departA(LE_LUNDI_11_MAI_2026_A_17H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arrivee, pause, reprise, depart));

    List<Plage> fenetres = journee.fenetres();

    assertThat(fenetres).hasSize(2);
    assertThat(fenetres.getFirst().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_8H);
    assertThat(fenetres.getFirst().fin()).contains(LE_LUNDI_11_MAI_2026_A_12H);
    assertThat(fenetres.getLast().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_13H);
    assertThat(fenetres.getLast().fin()).contains(LE_LUNDI_11_MAI_2026_A_17H);
    assertThat(journee.pointages()).containsExactly(arrivee, pause, reprise, depart);
  }

  /**
   * Un depart directement depuis la pause, sans reprise, ne referme rien de nouveau : la fenetre s'est deja arretee
   * a la pause.
   */
  @Test
  void shouldNotRouvrirDeFenetreQuandLeDepartSuitDirectementUnePause() {
    JourneeDeTravail journee = new JourneeDeTravail(
      List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_8H), pauseA(LE_LUNDI_11_MAI_2026_A_12H), departA(LE_LUNDI_11_MAI_2026_A_13H))
    );

    assertThat(journee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)));
  }

  @Test
  void shouldLaisserLaFenetreOuverteSansDepart() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_8H)));

    List<Plage> fenetres = journee.fenetres();

    assertThat(fenetres).hasSize(1);
    assertThat(fenetres.getFirst().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_8H);
    assertThat(fenetres.getFirst().fin()).isEmpty();
  }

  /**
   * L'adapter rend les evenements deja tries, mais le repli ne s'y fie pas : une regularisation arrive au journal
   * apres coup, a une heure anterieure.
   */
  @Test
  void shouldReplierDansLOrdreChronologiqueQuelQueSoitLOrdreRecu() {
    JourneeDeTravail dansLOrdre = new JourneeDeTravail(
      List.of(
        arriveeA(LE_LUNDI_11_MAI_2026_A_8H),
        pauseA(LE_LUNDI_11_MAI_2026_A_12H),
        repriseA(LE_LUNDI_11_MAI_2026_A_13H),
        departA(LE_LUNDI_11_MAI_2026_A_17H)
      )
    );
    JourneeDeTravail dansLeDesordre = new JourneeDeTravail(
      List.of(
        departA(LE_LUNDI_11_MAI_2026_A_17H),
        repriseA(LE_LUNDI_11_MAI_2026_A_13H),
        pauseA(LE_LUNDI_11_MAI_2026_A_12H),
        arriveeA(LE_LUNDI_11_MAI_2026_A_8H)
      )
    );

    assertThat(dansLeDesordre.fenetres()).isEqualTo(dansLOrdre.fenetres());
  }

  /**
   * Une transition impossible ne doit jamais empecher la generation du releve : le pointage fautif est ignore
   * silencieusement, absent aussi bien des pointages que des fenetres.
   */
  @Test
  void shouldIgnorerUnPointageFautifSansLeCompterNiDansLesPointagesNiDansLesFenetres() {
    EvenementDePresence pauseSansArrivee = pauseA(LE_LUNDI_11_MAI_2026_A_12H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(pauseSansArrivee));

    assertThat(journee.pointages()).isEmpty();
    assertThat(journee.fenetres()).isEmpty();
  }

  /**
   * Les pointages valides qui encadrent une anomalie construisent quand meme leurs fenetres correctement : le
   * pointage fautif est ignore, pas propage au reste de la journee.
   */
  @Test
  void shouldIgnorerLePointageFautifSansCasserLaSuiteDeLaJournee() {
    EvenementDePresence arrivee = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);
    EvenementDePresence arriveeFautive = arriveeA(LE_LUNDI_11_MAI_2026_A_12H);
    EvenementDePresence depart = departA(LE_LUNDI_11_MAI_2026_A_17H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arrivee, arriveeFautive, depart));

    assertThat(journee.pointages()).containsExactly(arrivee, depart);
    assertThat(journee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H)));
  }

  /**
   * Une reprise sans pause au prealable dans ce journal : l'automate la refuse. La fenetre ouverte a l'arrivee
   * reste ouverte, sans que la reprise orpheline n'y touche.
   */
  @Test
  void shouldOuvrirLaFenetreALArriveeMemeAvecUneRepriseOrphelineApres() {
    EvenementDePresence arrivee = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);
    EvenementDePresence repriseOrpheline = repriseA(LE_LUNDI_11_MAI_2026_A_13H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arrivee, repriseOrpheline));

    assertThat(journee.pointages()).containsExactly(arrivee);
    assertThat(journee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty()));
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
    assertThat(journeeDuLundiDe8HA17HAvecPauseDeMidi().estAbandonneePour(LE_MARDI_12_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
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
    JourneeDeTravail presumee = journeeDuLundiDe7HSansDepart().presumee(AMPLITUDE_MAXIMALE_13H, Optional.empty());

    assertThat(presumee.fenetres()).last().isEqualTo(new Plage(LE_LUNDI_11_MAI_2026_A_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_13H), true));
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
   * l'ordre dans lequel la base les rend.
   */
  @Test
  void shouldFairePasserLArriveeDevantUnDepartSimultane() {
    JourneeDeTravail nulle = new JourneeDeTravail(List.of(departA(LE_MARDI_12_MAI_2026_A_8H), arriveeA(LE_MARDI_12_MAI_2026_A_8H)));

    assertThat(nulle.pointages())
      .extracting(EvenementDePresence::type)
      .containsExactly(TypeDEvenementDePresence.ARRIVEE, TypeDEvenementDePresence.DEPART);
    assertThat(nulle.fenetres()).containsExactly(new Plage(LE_MARDI_12_MAI_2026_A_8H, Optional.of(LE_MARDI_12_MAI_2026_A_8H)));
  }
}
