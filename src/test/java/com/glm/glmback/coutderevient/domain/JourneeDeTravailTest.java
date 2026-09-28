package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NotAfterTimeException;
import java.time.Instant;
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

  /**
   * Le calcul du cout ne doit jamais echouer sur la presence (issue #54) : un depart qui ne suit aucune arrivee est
   * ignore, sans fenetre ni borne haute.
   */
  @Test
  void shouldIgnorerUnDepartSansArrivee() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(departA(LE_11_MAI_A_17H)));

    assertThat(journee.journal()).isEmpty();
    assertThat(journee.fenetres()).isEmpty();
    assertThat(journee.contient(LE_11_MAI_A_17H)).isFalse();
  }

  @Test
  void shouldIgnorerUnDepartQuiPrecedeLArrivee() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(departA(LE_11_MAI_A_8H), arriveeA(LE_11_MAI_A_10H)));

    assertThat(journee.journal()).containsExactly(arriveeA(LE_11_MAI_A_10H));
    assertThat(journee.fenetres()).containsExactly(new Plage(LE_11_MAI_A_10H, Optional.empty()));
  }

  @Test
  void shouldIgnorerUneSecondeArriveePendantLaPresence() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arriveeA(LE_11_MAI_A_8H), arriveeA(LE_11_MAI_A_10H), departA(LE_11_MAI_A_17H)));

    assertThat(journee.fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_17H)));
  }

  @Test
  void shouldGarderLePremierDepartQuandUnSecondSuit() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arriveeA(LE_11_MAI_A_8H), departA(LE_11_MAI_A_17H), departA(LE_11_MAI_A_21H)));

    assertThat(journee.fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_17H)));
    assertThat(journee.contient(LE_11_MAI_A_17H)).isTrue();
    assertThat(journee.contient(LE_11_MAI_A_21H)).isFalse();
  }

  /**
   * Un geste ignore en fin de journal ne referme pas la journee : sans depart retenu, elle reste ouverte, et c'est la
   * lecture qui la presume abandonnee.
   */
  @Test
  void shouldNePasFermerUneJourneeParUnGesteIgnore() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arriveeA(LE_11_MAI_A_8H), arriveeA(LE_11_MAI_A_9H)));

    assertThat(journee.contient(LE_12_MAI_A_8H)).isTrue();
    assertThat(journee.presumee(LE_12_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of()).finPresumee()).contains(LE_11_MAI_A_8H);
  }

  @Test
  void shouldNePasPresumerUneJourneeEntierementIgnoree() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(departA(LE_11_MAI_A_17H)));

    assertThat(journee.presumee(LE_12_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of())).isEqualTo(journee);
  }

  @Test
  void shouldHaveNoFenetreWithoutEvenement() {
    assertThat(new JourneeDeTravail(List.of()).fenetres()).isEmpty();
  }

  /**
   * La pause de midi n'est pas un geste de presence : la journee se lit en une seule fenetre, de l'arrivee au depart.
   */
  @Test
  void shouldCloseTheFenetreAtDepart() {
    assertThat(journeeDe8HA17H().fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_17H)));
  }

  /**
   * Une arrivee regularisee apres le depart rouvre la journee : chaque venue a sa fenetre.
   */
  @Test
  void shouldOpenOneFenetrePerVenue() {
    assertThat(journeeRouverteA13H().fenetres()).containsExactly(
      new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_12H)),
      new Plage(LE_11_MAI_A_13H, Optional.empty())
    );
  }

  @Test
  void shouldKeepLastFenetreOpenWithoutDepart() {
    assertThat(journeeOuverteDepuis8H().fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.empty()));
  }

  @Test
  void shouldSortJournalByDateDeSurvenue() {
    JourneeDeTravail journee = new JourneeDeTravail(List.of(departA(LE_11_MAI_A_17H), arriveeA(LE_11_MAI_A_8H)));

    assertThat(journee.fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_17H)));
  }

  /**
   * Une arrivee implicite et le depart d'un geste tardif partagent leur heure : l'arrivee passe devant, quel que soit
   * l'ordre dans lequel la base les rend. Sans ce departage, le repli strict de ce contexte echouerait (issue #54).
   */
  @Test
  void shouldFairePasserLArriveeDevantUnDepartSimultane() {
    JourneeDeTravail nulle = new JourneeDeTravail(List.of(departA(LE_11_MAI_A_17H), arriveeA(LE_11_MAI_A_17H)));

    assertThat(nulle.fenetres()).containsExactly(new Plage(LE_11_MAI_A_17H, Optional.of(LE_11_MAI_A_17H)));
  }

  @Test
  void shouldContainInstantBetweenArriveeAndDepart() {
    assertThat(journeeDe8HA17H().contient(LE_11_MAI_A_12H)).isTrue();
  }

  @Test
  void shouldNotContainInstantBeforeArrivee() {
    assertThat(journeeDe8HA17H().contient(LE_11_MAI_A_8H.minusSeconds(1))).isFalse();
  }

  @Test
  void shouldNotContainInstantAfterDepart() {
    assertThat(journeeDe8HA17H().contient(LE_12_MAI_A_8H)).isFalse();
  }

  /**
   * Une journee sans depart n'a pas de borne haute : l'operateur n'est simplement pas encore parti.
   */
  @Test
  void shouldContainAnyLaterInstantWhenStillOpen() {
    assertThat(journeeOuverteDepuis8H().contient(LE_12_MAI_A_8H)).isTrue();
  }

  @Test
  void shouldContainNothingWithoutEvenement() {
    assertThat(new JourneeDeTravail(List.of()).contient(LE_11_MAI_A_8H)).isFalse();
  }

  @Test
  void shouldNotBuildWithoutFinPresumee() {
    List<EvenementDePresence> journal = List.of(arriveeA(LE_11_MAI_A_8H));

    assertThatThrownBy(() -> new JourneeDeTravail(journal, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("fin presumee");
  }

  @Test
  void shouldNotBuildFinPresumeeSansEvenement() {
    List<EvenementDePresence> vide = List.of();
    Optional<Instant> fin = Optional.of(LE_11_MAI_A_8H);

    assertThatThrownBy(() -> new JourneeDeTravail(vide, fin))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal d'une journee presumee");
  }

  @Test
  void shouldNotBuildWithFinPresumeeBeforeArrivee() {
    List<EvenementDePresence> journal = List.of(arriveeA(LE_11_MAI_A_8H));
    Optional<Instant> avant = Optional.of(LE_11_MAI_A_8H.minusSeconds(1));

    assertThatThrownBy(() -> new JourneeDeTravail(journal, avant))
      .isExactlyInstanceOf(NotAfterTimeException.class)
      .hasMessageContaining("fin presumee");
  }

  /**
   * Lue mardi, la journee de lundi sans depart est abandonnee : elle s'arrete au dernier fait connu, le debut sur un
   * autre element a 15:00. Le relance de mardi 07:00 tombe hors de la fenetre de recherche.
   */
  @Test
  void shouldFermerUneJourneeAbandonneeASaFinPresumee() {
    JourneeDeTravail presumee = journeeOuverteDepuis8H().presumee(
      LE_12_MAI_A_8H,
      AMPLITUDE_MAXIMALE_13H,
      List.of(LE_11_MAI_A_9H, LE_11_MAI_A_15H, LE_12_MAI_A_7H)
    );

    assertThat(presumee.finPresumee()).contains(LE_11_MAI_A_15H);
    assertThat(presumee.fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_15H)));
    assertThat(presumee.contient(LE_11_MAI_A_14H)).isTrue();
    assertThat(presumee.contient(LE_11_MAI_A_15H)).isTrue();
    assertThat(presumee.contient(LE_11_MAI_A_17H)).isFalse();
    assertThat(presumee.contient(LE_12_MAI_A_9H)).isFalse();
  }

  @Test
  void shouldPresumerAuDernierEvenementDePresenceSansPointage() {
    JourneeDeTravail presumee = journeeOuverteDepuis8H().presumee(LE_12_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of());

    assertThat(presumee.finPresumee()).contains(LE_11_MAI_A_8H);
    assertThat(presumee.fenetres()).last().isEqualTo(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_8H)));
  }

  @Test
  void shouldIgnorerUnPointageAnterieurAuDernierFaitDePresence() {
    JourneeDeTravail presumee = journeeRouverteA13H().presumee(LE_12_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of(LE_11_MAI_A_9H));

    assertThat(presumee.finPresumee()).contains(LE_11_MAI_A_13H);
  }

  @Test
  void shouldIgnorerUnPointageAnterieurALArrivee() {
    JourneeDeTravail presumee = journeeOuverteDepuis8H().presumee(
      LE_12_MAI_A_8H,
      AMPLITUDE_MAXIMALE_13H,
      List.of(LE_11_MAI_A_8H.minusSeconds(3600))
    );

    assertThat(presumee.finPresumee()).contains(LE_11_MAI_A_8H);
  }

  /**
   * Une venue close avant la fin presumee reste entiere : seule la derniere, restee ouverte, s'y arrete.
   */
  @Test
  void shouldGarderEntiereUneVenueCloseAvantLaFinPresumee() {
    JourneeDeTravail presumee = journeeRouverteA13H().presumee(LE_12_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of(LE_11_MAI_A_15H));

    assertThat(presumee.fenetres()).containsExactly(
      new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_12H)),
      new Plage(LE_11_MAI_A_13H, Optional.of(LE_11_MAI_A_15H))
    );
  }

  @Test
  void shouldNePasPresumerUneJourneeNonAbandonnee() {
    assertThat(journeeOuverteDepuis8H().presumee(LE_11_MAI_A_17H, AMPLITUDE_MAXIMALE_13H, List.of(LE_11_MAI_A_15H))).isEqualTo(
      journeeOuverteDepuis8H()
    );
  }

  @Test
  void shouldNePasPresumerAuSeuilPile() {
    assertThat(
      journeeOuverteDepuis8H().presumee(LE_11_MAI_A_21H, AMPLITUDE_MAXIMALE_13H, List.of(LE_11_MAI_A_15H)).finPresumee()
    ).isEmpty();
  }

  @Test
  void shouldNePasPresumerUneJourneeFermee() {
    assertThat(journeeDe8HA17H().presumee(LE_12_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of(LE_11_MAI_A_15H))).isEqualTo(journeeDe8HA17H());
  }

  @Test
  void shouldNePasPresumerUneJourneeSansEvenement() {
    JourneeDeTravail vide = new JourneeDeTravail(List.of());

    assertThat(vide.presumee(LE_12_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of(LE_11_MAI_A_15H))).isEqualTo(vide);
  }

  /**
   * Issue #59 : une journee fermee de plus de 24 h n'a pas pu etre vecue d'une traite. Elle se ferme a sa fin
   * presumee, le dernier fait de la fenetre de recherche : le depart de mercredi en est exclu.
   */
  @Test
  void shouldFermerUneJourneeDePlusDe24HAuDernierFaitDeLaFenetreDeRecherche() {
    JourneeDeTravail presumee = journeeDuLundi8HAuMercredi8H().presumee(LE_13_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of());

    assertThat(presumee.finPresumee()).contains(LE_11_MAI_A_8H);
    assertThat(presumee.fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_8H)));
    assertThat(presumee.contient(LE_11_MAI_A_8H)).isTrue();
    assertThat(presumee.contient(LE_11_MAI_A_9H)).isFalse();
    assertThat(presumee.contient(LE_12_MAI_A_9H)).isFalse();
  }

  @Test
  void shouldFermerUneJourneeDePlusDe24HAuDernierPointageDeLaFenetre() {
    JourneeDeTravail presumee = journeeDuLundi8HAuMercredi8H().presumee(
      LE_13_MAI_A_8H,
      AMPLITUDE_MAXIMALE_13H,
      List.of(LE_11_MAI_A_15H, LE_12_MAI_A_9H)
    );

    assertThat(presumee.finPresumee()).contains(LE_11_MAI_A_15H);
    assertThat(presumee.fenetres()).containsExactly(new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_15H)));
  }

  /**
   * Issue #59, sur une journee de deux venues : celle du lundi, close avant la fin presumee, reste entiere ; celle du
   * mardi commence apres, et disparait.
   */
  @Test
  void shouldEcarterUneVenueCommenceeApresLaFinPresumee() {
    JourneeDeTravail deuxVenues = new JourneeDeTravail(
      List.of(arriveeA(LE_11_MAI_A_8H), departA(LE_11_MAI_A_12H), arriveeA(LE_12_MAI_A_9H), departA(LE_13_MAI_A_8H))
    );

    assertThat(deuxVenues.presumee(LE_13_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of(LE_11_MAI_A_15H)).fenetres()).containsExactly(
      new Plage(LE_11_MAI_A_8H, Optional.of(LE_11_MAI_A_12H))
    );
  }

  /**
   * Une journee fermee n'a pas besoin de l'horloge : son amplitude est connue, lue avant meme le seuil.
   */
  @Test
  void shouldPresumerUneJourneeDePlusDe24HAToutInstant() {
    assertThat(journeeDuLundi8HAuMercredi8H().presumee(LE_11_MAI_A_9H, AMPLITUDE_MAXIMALE_13H, List.of()).finPresumee()).contains(
      LE_11_MAI_A_8H
    );
  }

  @Test
  void shouldNePasPresumerUneJourneeFermeeDe24HPile() {
    JourneeDeTravail pile = new JourneeDeTravail(List.of(arriveeA(LE_11_MAI_A_8H), departA(LE_12_MAI_A_8H)));
    JourneeDeTravail auDela = new JourneeDeTravail(List.of(arriveeA(LE_11_MAI_A_8H), departA(LE_12_MAI_A_8H.plusSeconds(1))));

    assertThat(pile.presumee(LE_13_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of())).isEqualTo(pile);
    assertThat(auDela.presumee(LE_13_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of()).finPresumee()).contains(LE_11_MAI_A_8H);
  }

  /**
   * Au-dela du seuil mais sous 24 h, une longue journee fermee compte entiere : elle n'est qu'une anomalie a examiner.
   */
  @Test
  void shouldNePasPresumerUneLongueJourneeFermeeSous24H() {
    JourneeDeTravail longue = new JourneeDeTravail(List.of(arriveeA(LE_11_MAI_A_8H), departA(LE_12_MAI_A_7H)));

    assertThat(longue.presumee(LE_13_MAI_A_8H, AMPLITUDE_MAXIMALE_13H, List.of())).isEqualTo(longue);
  }

  /**
   * Lundi, Dupont part a midi ; une arrivee a 13 h, regularisee sur la meme journee, la rouvre sans depart.
   */
  private static JourneeDeTravail journeeRouverteA13H() {
    return new JourneeDeTravail(List.of(arriveeA(LE_11_MAI_A_8H), departA(LE_11_MAI_A_12H), arriveeA(LE_11_MAI_A_13H)));
  }
}
