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
   * La pause de midi n'est pas un pointage de presence : de l'arrivee au depart, une seule fenetre.
   */
  @Test
  void shouldFermerLaFenetreAuDepart() {
    EvenementDePresence arrivee = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);
    EvenementDePresence depart = departA(LE_LUNDI_11_MAI_2026_A_17H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arrivee, depart));

    assertThat(journee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H)));
    assertThat(journee.pointages()).containsExactly(arrivee, depart);
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
    JourneeDeTravail dansLOrdre = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_8H), departA(LE_LUNDI_11_MAI_2026_A_17H)));
    JourneeDeTravail dansLeDesordre = new JourneeDeTravail(
      List.of(departA(LE_LUNDI_11_MAI_2026_A_17H), arriveeA(LE_LUNDI_11_MAI_2026_A_8H))
    );

    assertThat(dansLeDesordre.fenetres()).isEqualTo(dansLOrdre.fenetres());
  }

  /**
   * Une transition impossible ne doit jamais empecher la generation du releve : le pointage fautif est ignore
   * silencieusement, absent aussi bien des pointages que des fenetres.
   */
  @Test
  void shouldIgnorerUnPointageFautifSansLeCompterNiDansLesPointagesNiDansLesFenetres() {
    EvenementDePresence departSansArrivee = departA(LE_LUNDI_11_MAI_2026_A_12H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(departSansArrivee));

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
   * Une seconde arrivee sans depart au prealable dans ce journal : l'automate la refuse. La fenetre ouverte a la
   * premiere reste ouverte, sans que l'arrivee orpheline n'y touche.
   */
  @Test
  void shouldOuvrirLaFenetreALArriveeMemeAvecUneArriveeOrphelineApres() {
    EvenementDePresence arrivee = arriveeA(LE_LUNDI_11_MAI_2026_A_8H);
    EvenementDePresence arriveeOrpheline = arriveeA(LE_LUNDI_11_MAI_2026_A_13H);
    JourneeDeTravail journee = new JourneeDeTravail(List.of(arrivee, arriveeOrpheline));

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
    assertThat(journeeDuLundiDe8HA17H().estAbandonneePour(LE_MARDI_12_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldPresumerUneJourneeAbandonnee() {
    JourneeDeTravail lundi = journeeDuLundiDe7HSansDepart();

    assertThat(lundi.estPresumeePour(LE_LUNDI_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(lundi.estPresumeePour(LE_LUNDI_11_MAI_2026_A_20H.plusSeconds(1), AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  /**
   * Issue #59 : une journee fermee de plus de 24 h n'a pas pu etre vecue d'une traite. Elle se lit comme abandonnee,
   * a tout instant, sans cesser pour autant d'etre fermee.
   */
  @Test
  void shouldPresumerUneJourneeFermeeDePlusDe24H() {
    JourneeDeTravail troisJours = journeeDuLundi7HAuMercredi12H();

    assertThat(troisJours.estPresumeePour(LE_LUNDI_11_MAI_2026_A_8H, AMPLITUDE_MAXIMALE_13H)).isTrue();
    assertThat(troisJours.estAbandonneePour(LE_MERCREDI_13_MAI_2026_A_12H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNePasPresumerUneJourneeFermeeDe24HPile() {
    JourneeDeTravail pile = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_MARDI_12_MAI_2026_A_7H)));
    JourneeDeTravail auDela = new JourneeDeTravail(
      List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_MARDI_12_MAI_2026_A_7H.plusSeconds(1)))
    );

    assertThat(pile.estPresumeePour(LE_MERCREDI_13_MAI_2026_A_12H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(auDela.estPresumeePour(LE_MERCREDI_13_MAI_2026_A_12H, AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  /**
   * Au-dela du seuil mais sous 24 h, une longue journee fermee compte entiere : elle n'est qu'une anomalie a examiner.
   */
  @Test
  void shouldNePasPresumerUneLongueJourneeFermeeSous24H() {
    JourneeDeTravail longue = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_MARDI_12_MAI_2026_A_2H)));

    assertThat(longue.estPresumeePour(LE_MERCREDI_13_MAI_2026_A_12H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNeJamaisPresumerUneJourneeSansEvenement() {
    assertThat(new JourneeDeTravail(List.of()).estPresumeePour(LE_MERCREDI_13_MAI_2026_A_12H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  /**
   * Le dernier fait connu se cherche dans la fenetre de recherche : le depart de mercredi en est exclu, et tout ce qui
   * suit la fin presumee disparait.
   */
  @Test
  void shouldFermerUneJourneeDePlusDe24HAuDernierFaitDeLaFenetreDeRecherche() {
    JourneeDeTravail presumee = journeeDuLundi7HAuMercredi12H().presumee(AMPLITUDE_MAXIMALE_13H, Optional.empty());

    assertThat(presumee.finPresumee()).contains(LE_LUNDI_11_MAI_2026_A_7H);
    assertThat(presumee.fenetres()).containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_7H), true));
  }

  @Test
  void shouldFermerUneJourneeDePlusDe24HAuDernierPointageDAtelierDeLaFenetre() {
    JourneeDeTravail presumee = journeeDuLundi7HAuMercredi12H().presumee(AMPLITUDE_MAXIMALE_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_16H));

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
        departA(LE_MERCREDI_13_MAI_2026_A_12H)
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
    JourneeDeTravail presumee = journeeDuLundiDe7HSansDepart().presumee(AMPLITUDE_MAXIMALE_13H, Optional.empty());

    assertThat(presumee.fenetres()).last().isEqualTo(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_7H), true));
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

  /**
   * Lundi, Dupont part a midi ; une arrivee a 13 h, regularisee sur la meme journee, la rouvre sans depart.
   */
  private static JourneeDeTravail journeeDuLundiRouverteA13H() {
    return new JourneeDeTravail(
      List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_7H), departA(LE_LUNDI_11_MAI_2026_A_12H), arriveeA(LE_LUNDI_11_MAI_2026_A_13H))
    );
  }
}
