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
    JourneeDeTravail journee = new JourneeDeTravail(
      List.of(
        arriveeA(LE_LUNDI_11_MAI_2026_A_8H),
        pauseA(LE_LUNDI_11_MAI_2026_A_12H),
        repriseA(LE_LUNDI_11_MAI_2026_A_13H),
        departA(LE_LUNDI_11_MAI_2026_A_17H)
      )
    );

    List<Plage> fenetres = journee.fenetres();

    assertThat(fenetres).hasSize(2);
    assertThat(fenetres.getFirst().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_8H);
    assertThat(fenetres.getFirst().fin()).contains(LE_LUNDI_11_MAI_2026_A_12H);
    assertThat(fenetres.getLast().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_13H);
    assertThat(fenetres.getLast().fin()).contains(LE_LUNDI_11_MAI_2026_A_17H);
    assertThat(journee.pointages()).allSatisfy(pointage -> assertThat(pointage.valide()).isTrue());
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
   * Une transition impossible ne doit jamais empecher la generation du releve : le pointage fautif reste visible,
   * marque invalide, et n'entre pour rien dans le calcul des fenetres.
   */
  @Test
  void shouldMarquerUnPointageFautifSansLeCompterDansLesFenetres() {
    EvenementDePresence pauseSansArrivee = pauseA(LE_LUNDI_11_MAI_2026_A_12H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(pauseSansArrivee));

    assertThat(journee.pointages()).containsExactly(new Pointage(pauseSansArrivee, false));
    assertThat(journee.fenetres()).isEmpty();
  }

  /**
   * Les pointages valides qui encadrent une anomalie construisent quand meme leurs fenetres correctement : l'
   * anomalie est ignoree, pas propagee au reste de la journee.
   */
  @Test
  void shouldIgnorerLePointageFautifSansCasserLaSuiteDeLaJournee() {
    EvenementDePresence arrivee = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);
    EvenementDePresence arriveeFautive = arriveeA(LE_LUNDI_11_MAI_2026_A_12H);
    EvenementDePresence depart = departA(LE_LUNDI_11_MAI_2026_A_17H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arrivee, arriveeFautive, depart));

    assertThat(journee.pointages()).containsExactly(
      new Pointage(arrivee, true),
      new Pointage(arriveeFautive, false),
      new Pointage(depart, true)
    );
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

    assertThat(journee.pointages()).containsExactly(new Pointage(arrivee, true), new Pointage(repriseOrpheline, false));
    assertThat(journee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty()));
  }
}
