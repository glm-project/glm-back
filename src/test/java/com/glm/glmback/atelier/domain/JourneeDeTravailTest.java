package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneeDeTravailTest {

  private static final JourneeDeTravailId ID = JourneeDeTravailId.newId();

  @Test
  void shouldNotBuildWithoutId() {
    assertThatThrownBy(() -> new JourneeDeTravail(null, OPERATEUR_ID_DUPONT, JournalDePresence.vide()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("id");
  }

  @Test
  void shouldNotBuildWithoutOperateur() {
    assertThatThrownBy(() -> new JourneeDeTravail(ID, null, JournalDePresence.vide()))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("operateur");
  }

  @Test
  void shouldNotBuildWithoutJournal() {
    assertThatThrownBy(() -> new JourneeDeTravail(ID, OPERATEUR_ID_DUPONT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldOuvrirUneJourneeVide() {
    JourneeDeTravail journee = JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT);

    assertThat(journee.id()).isEqualTo(ID);
    assertThat(journee.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(journee.etat()).isEqualTo(EtatDePresence.ABSENT);
    assertThat(journee.estEnCours()).isFalse();
    assertThat(journee.debut()).isEmpty();
    assertThat(journee.contient(LE_10_MAI_2026_A_8H)).isFalse();
  }

  @Test
  void shouldEtreEnCoursDesLArrivee() {
    JourneeDeTravail journee = journeeDeDupontOuverteA7H();

    assertThat(journee.estEnCours()).isTrue();
    assertThat(journee.debut()).contains(LE_10_MAI_2026_A_7H);
    assertThat(journee.fenetres()).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.empty()));
    assertThat(journee.amplitude()).isEmpty();
  }

  @Test
  void shouldSeFermerAuDepart() {
    JourneeDeTravail journee = journeeDeDupontDe7HA17H();

    assertThat(journee.estEnCours()).isFalse();
    assertThat(journee.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
    assertThat(journee.fenetres()).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_17H)));
  }

  @Test
  void shouldContenirLesInstantsDeLaJourneeFermee() {
    JourneeDeTravail journee = journeeDeDupontDe7HA17H();

    assertThat(journee.contient(LE_10_MAI_2026_A_7H)).isTrue();
    assertThat(journee.contient(LE_10_MAI_2026_A_12H)).isTrue();
    assertThat(journee.contient(LE_10_MAI_2026_A_17H)).isTrue();
    assertThat(journee.contient(LE_10_MAI_2026_A_7H.minusSeconds(1))).isFalse();
    assertThat(journee.contient(LE_11_MAI_2026_A_9H15)).isFalse();
  }

  @Test
  void shouldContenirTousLesInstantsPosterieursDUneJourneeOuverte() {
    assertThat(journeeDeDupontOuverteA7H().contient(LE_11_MAI_2026_A_9H15)).isTrue();
  }

  @Test
  void shouldAnnulerUnEvenement() {
    JourneeDeTravail journee = journeeDeDupontDe7HA17H();
    EvenementDePresenceId depart = journee.journal().evenements().getLast().id();

    JourneeDeTravail sansDepart = journee.annule(depart, annulationParLeroy());

    assertThat(sansDepart.estEnCours()).isTrue();
    assertThat(sansDepart.id()).isEqualTo(journee.id());
  }

  @Test
  void shouldCorrigerUnEvenement() {
    JourneeDeTravail journee = JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT).enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_9H));
    EvenementDePresenceId arrivee = journee.journal().evenements().getFirst().id();

    JourneeDeTravail corrigee = journee.corrige(arrivee, annulationParLeroy(), arriveeDeDupontA(LE_10_MAI_2026_A_7H));

    assertThat(corrigee.debut()).contains(LE_10_MAI_2026_A_7H);
  }

  @Test
  void shouldNeJamaisEtreAbandonneeSansArrivee() {
    JourneeDeTravail vide = JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT);

    assertThat(vide.estAbandonneePour(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNePasEtreAbandonneeSousLeSeuil() {
    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(LE_10_MAI_2026_A_17H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  /**
   * D2 : abandonnee quand l'amplitude depasse le seuil, pas quand elle l'atteint. Un geste a 20:00 pile reste dans la
   * journee ouverte a 07:00.
   */
  @Test
  void shouldNePasEtreAbandonneeAuSeuilPile() {
    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(LE_10_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldEtreAbandonneeUneSecondeApresLeSeuil() {
    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(LE_10_MAI_2026_A_20H.plusSeconds(1), AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  @Test
  void shouldAppliquerLeSeuilDonne() {
    Instant a17h30 = LE_10_MAI_2026_A_17H.plusSeconds(1800);

    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(a17h30, AMPLITUDE_MAXIMALE_10H)).isTrue();
    assertThat(journeeDeDupontOuverteA7H().estAbandonneePour(a17h30, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNeJamaisAbandonnerUneJourneeFermeeParUnDepart() {
    assertThat(journeeDeDupontDe7HA17H().estAbandonneePour(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldPresumerUneJourneeAbandonnee() {
    assertThat(journeeDeDupontOuverteA7H().estPresumeePour(LE_10_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(journeeDeDupontOuverteA7H().estPresumeePour(LE_10_MAI_2026_A_20H.plusSeconds(1), AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  /**
   * Issue #59 : une journee fermee de plus de 24 h n'a pas pu etre vecue d'une traite. Elle se lit comme abandonnee,
   * a tout instant, sans etre abandonnee pour autant : un geste recu ensuite ne la concerne pas, et l'anomalie reste
   * une amplitude excessive.
   */
  @Test
  void shouldPresumerUneJourneeFermeeDePlusDe24H() {
    assertThat(journeeDeDupontDu10A7HAu11A9H().estPresumeePour(LE_10_MAI_2026_A_8H, AMPLITUDE_MAXIMALE_13H)).isTrue();
    assertThat(journeeDeDupontDu10A7HAu11A9H().estAbandonneePour(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNePasPresumerUneJourneeFermeeDe24HPile() {
    JourneeDeTravail pile = journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_11_MAI_2026_A_7H));
    JourneeDeTravail auDela = journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_11_MAI_2026_A_7H.plusSeconds(1)));

    assertThat(pile.estPresumeePour(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(auDela.estPresumeePour(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isTrue();
  }

  /**
   * Au-dela du seuil mais sous 24 h, une longue journee fermee compte entiere : elle n'est qu'une anomalie a examiner.
   */
  @Test
  void shouldNePasPresumerUneLongueJourneeFermeeSous24H() {
    JourneeDeTravail longue = journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_11_MAI_2026_A_3H));

    assertThat(longue.estPresumeePour(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
    assertThat(longue.fenetresA(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))).isEqualTo(
      longue.fenetres()
    );
  }

  @Test
  void shouldNeJamaisPresumerUneJourneeSansArrivee() {
    assertThat(JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT).estPresumeePour(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H)).isFalse();
  }

  @Test
  void shouldNAvoirAucuneEtendueSansEvenement() {
    assertThat(JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT).etendue()).isEmpty();
  }

  @Test
  void shouldEtendreUneJourneeOuverteJusquASonDernierFaitConnu() {
    assertThat(journeeDeLundiRouverteA13H().etendue()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_13H));
  }

  @Test
  void shouldEtendreUneJourneeFermeeDeLArriveeAuDepart() {
    assertThat(journeeDeDupontDe7HA17H().etendue()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldEcarterDeLEtendueLesEvenementsAnnules() {
    EvenementDePresence depart = departDeDupontA(LE_10_MAI_2026_A_17H);
    JourneeDeTravail journee = journeeDeDupontOuverteA7H().enregistre(depart).annule(depart.id(), annulationParLeroy());

    assertThat(journee.etendue()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_7H));
  }

  @Test
  void shouldChercherLesFaitsConnusEntreLArriveeEtLeSeuil() {
    assertThat(journeeDeDupontOuverteA7H().fenetreDeRecherche(AMPLITUDE_MAXIMALE_13H)).contains(
      new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_20H)
    );
    assertThat(journeeDeDupontOuverteA7H().fenetreDeRecherche(AMPLITUDE_MAXIMALE_10H)).contains(
      new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H)
    );
  }

  @Test
  void shouldNeRienChercherSansArrivee() {
    assertThat(JourneeDeTravail.ouverte(ID, OPERATEUR_ID_DUPONT).fenetreDeRecherche(AMPLITUDE_MAXIMALE_13H)).isEmpty();
  }

  /**
   * E2 : lundi sans depart, lu mardi. La journee abandonnee se ferme au dernier fait connu, la fin de l'OF 43 a 16:00.
   * Sans depart, elle n'a qu'une fenetre : toute la journee est presumee.
   */
  @Test
  void shouldFermerUneJourneeAbandonneeASaFinPresumee() {
    assertThat(
      journeeDeDupontOuverteA7H().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))
    ).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_16H), true));
  }

  @Test
  void shouldPresumerLaFinAuDernierEvenementDePresenceSansPointage() {
    assertThat(journeeDeDupontOuverteA7H().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.empty())).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_7H), true)
    );
  }

  /**
   * Une venue close avant la fin presumee reste pointee : seule la derniere, restee ouverte, est presumee.
   */
  @Test
  void shouldGarderPointeeUneVenueCloseAvantLaFinPresumee() {
    assertThat(
      journeeDeLundiRouverteA13H().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))
    ).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H)),
      new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H), true)
    );
  }

  @Test
  void shouldIgnorerUnPointageAnterieurAuDernierFaitDePresence() {
    assertThat(journeeDeLundiRouverteA13H().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_9H)))
      .last()
      .isEqualTo(new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_13H), true));
  }

  @Test
  void shouldIgnorerUnPointageHorsDeLaFenetreDeRecherche() {
    assertThat(journeeDeDupontOuverteA7H().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_11_MAI_2026_A_7H)))
      .last()
      .isEqualTo(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_7H), true));
  }

  @Test
  void shouldLaisserOuverteUneJourneeNonAbandonnee() {
    assertThat(journeeDeDupontOuverteA7H().fenetresA(LE_10_MAI_2026_A_17H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H)))
      .last()
      .isEqualTo(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.empty()));
  }

  @Test
  void shouldLaisserOuverteUneJourneeLueAuSeuilPile() {
    assertThat(journeeDeDupontOuverteA7H().fenetresA(LE_10_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H)))
      .last()
      .matches(FenetreDePresence::estOuverte);
  }

  @Test
  void shouldNeRienPresumerDUneJourneeFermee() {
    assertThat(
      journeeDeDupontDe7HA17H().fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))
    ).isEqualTo(journeeDeDupontDe7HA17H().fenetres());
  }

  /**
   * Un fait de presence regularise au-dela du seuil reste un fait connu : la fin presumee ne le precede jamais.
   */
  @Test
  void shouldNeJamaisPresumerAvantLeDernierFaitDePresence() {
    JourneeDeTravail revenuTard = journeeDeDupontOuverteA7H()
      .enregistre(departDeDupontA(LE_10_MAI_2026_A_12H))
      .enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_20H.plusSeconds(3600)));

    assertThat(revenuTard.fenetresA(LE_11_MAI_2026_A_9H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H)))
      .last()
      .isEqualTo(new FenetreDePresence(LE_10_MAI_2026_A_20H.plusSeconds(3600), Optional.of(LE_10_MAI_2026_A_20H.plusSeconds(3600)), true));
  }

  /**
   * Issue #59 : ici, c'est le depart qu'on ne croit pas. Seuls les faits de la fenetre de recherche comptent : le
   * depart du 11 en est exclu, et ce qui suit la fin presumee disparait.
   */
  @Test
  void shouldFermerUneJourneeDePlusDe24HAuDernierFaitDeLaFenetreDeRecherche() {
    assertThat(journeeDeDupontDu10A7HAu11A9H().fenetresA(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H, Optional.empty())).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_7H), true)
    );
  }

  @Test
  void shouldFermerUneJourneeDePlusDe24HAuDernierPointageDeLaFenetre() {
    assertThat(
      journeeDeDupontDu10A7HAu11A9H().fenetresA(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))
    ).containsExactly(new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_16H), true));
  }

  /**
   * Issue #59, sur une journee de deux venues : celle du matin, close avant la fin presumee, reste pointee ; celle du
   * lendemain commence apres, et disparait.
   */
  @Test
  void shouldEcarterUneVenueCommenceeApresLaFinPresumee() {
    JourneeDeTravail deuxVenues = journeeDeDupontOuverteA7H()
      .enregistre(departDeDupontA(LE_10_MAI_2026_A_12H))
      .enregistre(arriveeDeDupontA(LE_11_MAI_2026_A_7H))
      .enregistre(departDeDupontA(LE_11_MAI_2026_A_9H));

    assertThat(deuxVenues.fenetresA(LE_11_MAI_2026_A_20H, AMPLITUDE_MAXIMALE_13H, Optional.of(LE_10_MAI_2026_A_16H))).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H))
    );
  }

  /**
   * Lundi, Dupont part a midi ; une arrivee a 13 h, regularisee sur la meme journee, la rouvre sans depart.
   */
  private static JourneeDeTravail journeeDeLundiRouverteA13H() {
    return journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_10_MAI_2026_A_12H)).enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_13H));
  }
}
