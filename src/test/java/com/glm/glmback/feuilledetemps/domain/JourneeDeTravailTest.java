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
   * La pause de midi n'est pas un pointage de presence : de l'arrivee au depart, une seule fenetre.
   */
  @Test
  void shouldFermerLaFenetreAuDepart() {
    assertThat(journeeDuLundiDe8HA17H().fenetres()).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H))
    );
  }

  /**
   * Une arrivee regularisee apres le depart rouvre la journee : chaque venue a sa fenetre.
   */
  @Test
  void shouldOuvrirUneFenetreParVenue() {
    assertThat(journeeDuLundiRouverteA13H().fenetres()).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)),
      new Plage(LE_LUNDI_11_MAI_2026_A_13H, Optional.empty())
    );
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
    JourneeDeTravail journee = new JourneeDeTravail(List.of(departA(LE_LUNDI_11_MAI_2026_A_17H), arriveeA(LE_LUNDI_11_MAI_2026_A_8H)));

    assertThat(journee.fenetres()).isEqualTo(journeeDuLundiDe8HA17H().fenetres());
  }

  @Test
  void shouldRefuserUneSequenceImpossible() {
    List<EvenementDePresence> journal = List.of(departA(LE_LUNDI_11_MAI_2026_A_12H));

    assertThatThrownBy(() -> new JourneeDeTravail(journal))
      .isExactlyInstanceOf(TransitionDePresenceInterditeException.class)
      .hasMessageContaining("DEPART")
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
  void shouldPresumerUneJourneeAbandonnee() {
    JourneeDeTravail lundi = journeeDuLundiDe7HSansDepart();

    assertThat(lundi.estPresumeePour(LE_LUNDI_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(lundi.estPresumeePour(LE_LUNDI_11_MAI_2026_A_20H.plusSeconds(1), AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  /**
   * Issue #59 : une journee fermee de plus de 24 h n'a pas pu etre vecue d'une traite. Elle se lit comme abandonnee,
   * a tout instant, sans cesser pour autant d'etre fermee : l'atelier ne la tient pas pour abandonnee.
   */
  @Test
  void shouldPresumerUneJourneeFermeeDePlusDe24H() {
    JourneeDeTravail troisJours = journeeDuLundi7HAuMercredi8H();

    assertThat(troisJours.estPresumeePour(LE_LUNDI_11_MAI_2026_A_8H, AMPLITUDE_MAXIMALE_13H)).isTrue();
    assertThat(troisJours.estAbandonneePour(LE_MERCREDI_13_MAI_2026_A_8H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNePasPresumerUneJourneeFermeeDe24HPile() {
    JourneeDeTravail pile = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_MARDI_12_MAI_2026_A_7H)));
    JourneeDeTravail auDela = new JourneeDeTravail(
      List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_MARDI_12_MAI_2026_A_7H.plusSeconds(1)))
    );

    assertThat(pile.estPresumeePour(LE_MERCREDI_13_MAI_2026_A_8H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(auDela.estPresumeePour(LE_MERCREDI_13_MAI_2026_A_8H, AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  /**
   * Au-dela du seuil mais sous 24 h, une longue journee fermee compte entiere : elle n'est qu'une anomalie a examiner.
   */
  @Test
  void shouldNePasPresumerUneLongueJourneeFermeeSous24H() {
    JourneeDeTravail longue = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_MARDI_12_MAI_2026_A_2H)));

    assertThat(longue.estPresumeePour(LE_MERCREDI_13_MAI_2026_A_8H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNeJamaisPresumerUneJourneeSansEvenement() {
    assertThat(new JourneeDeTravail(List.of()).estPresumeePour(LE_MERCREDI_13_MAI_2026_A_8H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  /**
   * Le dernier fait connu se cherche dans la fenetre de recherche : le depart de mercredi en est exclu, et tout ce qui
   * suit la fin presumee disparait.
   */
  @Test
  void shouldFermerUneJourneeDePlusDe24HAuDernierFaitDeLaFenetreDeRecherche() {
    JourneeDeTravail presumee = journeeDuLundi7HAuMercredi8H().presumee(AMPLITUDE_MAXIMALE_13H, Optional.empty());

    assertThat(presumee.finPresumee()).contains(LE_LUNDI_11_MAI_2026_A_7H);
    assertThat(presumee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_7H), true));
  }

  @Test
  void shouldFermerUneJourneeDePlusDe24HAuDernierPointageDAtelierDeLaFenetre() {
    JourneeDeTravail presumee = journeeDuLundi7HAuMercredi8H().presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H));

    assertThat(presumee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H), true));
  }

  /**
   * Issue #59, sur une journee de deux venues : celle du lundi, close avant la fin presumee, reste pointee ; celle du
   * mardi commence apres, et disparait.
   */
  @Test
  void shouldEcarterUneVenueCommenceeApresLaFinPresumee() {
    JourneeDeTravail deuxVenues = new JourneeDeTravail(
      List.of(
        arriveeA(LE_LUNDI_11_MAI_2026_A_7H),
        departA(LE_LUNDI_11_MAI_2026_A_12H),
        arriveeA(LE_MARDI_12_MAI_2026_A_10H),
        departA(LE_MERCREDI_13_MAI_2026_A_8H)
      )
    );

    assertThat(deuxVenues.presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H)).fenetres()).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
    );
  }

  @Test
  void shouldChercherLesFaitsConnusEntreLArriveeEtLeSeuil() {
    assertThat(journeeDuLundiDe7HSansDepart().fenetreDeRecherche(AMPLITUDE_MAXIMALE_13H)).contains(
      new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_20H))
    );
    assertThat(new JourneeDeTravail(List.of()).fenetreDeRecherche(AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  /**
   * Sans depart, la journee n'a qu'une fenetre : elle se ferme a sa fin presumee, et devient presumee en entier.
   */
  @Test
  void shouldFermerLaDerniereFenetreASaFinPresumee() {
    JourneeDeTravail presumee = journeeDuLundiDe7HSansDepart().presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H));

    assertThat(presumee.finPresumee()).contains(LE_LUNDI_11_MAI_2026_A_16H);
    assertThat(presumee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H), true));
  }

  /**
   * Une venue close avant la fin presumee reste pointee : seule la derniere, restee ouverte, est presumee.
   */
  @Test
  void shouldGarderPointeeUneVenueCloseAvantLaFinPresumee() {
    JourneeDeTravail presumee = journeeDuLundiRouverteA13H().presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H));

    assertThat(presumee.fenetres()).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)),
      new Plage(LE_LUNDI_11_MAI_2026_A_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H), true)
    );
  }

  @Test
  void shouldPresumerAuDernierPointageDePresenceSansPointageDAtelier() {
    assertThat(journeeDuLundiDe7HSansDepart().presumee(AMPLITUDE_MAXIMALE_13H, Optional.empty()).fenetres())
      .last()
      .isEqualTo(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_7H), true));
  }

  @Test
  void shouldIgnorerUnPointageHorsDeLaFenetreDeRechercheOuAnterieur() {
    JourneeDeTravail lundi = journeeDuLundiRouverteA13H();

    assertThat(lundi.presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_MARDI_12_MAI_2026_A_8H)).finPresumee()).contains(
      LE_LUNDI_11_MAI_2026_A_13H
    );
    assertThat(lundi.presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_8H)).finPresumee()).contains(
      LE_LUNDI_11_MAI_2026_A_13H
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

  /**
   * Lundi, Dupont part a midi ; une arrivee a 13 h, regularisee sur la meme journee, la rouvre sans depart.
   */
  private static JourneeDeTravail journeeDuLundiRouverteA13H() {
    return new JourneeDeTravail(
      List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_LUNDI_11_MAI_2026_A_12H), arriveeA(LE_LUNDI_11_MAI_2026_A_13H))
    );
  }
}
