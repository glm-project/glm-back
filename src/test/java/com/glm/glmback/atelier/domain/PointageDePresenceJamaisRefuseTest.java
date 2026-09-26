package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Strategie « bornes de fin de journee », lot 8a : un geste de presence n'est jamais refuse a l'operateur. Sans
 * journee, il en ouvre une par une arrivee implicite ; redondant avec l'etat courant, il est absorbe. Les gestes
 * rejoues dans le desordre restent refuses.
 */
@UnitTest
class PointageDePresenceJamaisRefuseTest {

  private static final EvenementDePresenceId ARRIVEE_IMPLICITE = EvenementDePresenceId.newId();

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final JourneesDeTravailService service = JourneesDeTravailService.builder()
    .repository(journees)
    .operateurs(RessourcesDAtelierEnMemoire.deLAtelier().operateurs())
    .seuil(() -> AMPLITUDE_MAXIMALE_13H)
    .signalements(new PointagesSignalesEnMemoire())
    .clock(maintenant::get);

  @Test
  void shouldOuvrirUneJourneeSurUnePauseSansJournee() {
    PresenceTraitee traitee = pointeA(TypeDEvenementDePresence.PAUSE, LE_10_MAI_2026_A_12H);

    assertThat(traitee.absorbee()).isFalse();
    assertThat(traitee.journee().etat()).isEqualTo(EtatDePresence.EN_PAUSE);
    assertThat(traitee.journee().journal().evenements())
      .extracting(EvenementDePresence::id, EvenementDePresence::type, EvenementDePresence::dateDeSurvenue)
      .containsExactly(
        tuple(ARRIVEE_IMPLICITE, TypeDEvenementDePresence.ARRIVEE, LE_10_MAI_2026_A_12H),
        tuple(traitee.journee().journal().evenements().getLast().id(), TypeDEvenementDePresence.PAUSE, LE_10_MAI_2026_A_12H)
      );
    assertThat(journees.getEnCoursPour(OPERATEUR_ID_DUPONT)).contains(traitee.journee());
  }

  /**
   * Le depart presse par un operateur jamais arrive : une journee de duree nulle, rien de refuse.
   */
  @Test
  void shouldOuvrirEtFermerUneJourneeSurUnDepartSansJournee() {
    PresenceTraitee traitee = pointeA(TypeDEvenementDePresence.DEPART, LE_10_MAI_2026_A_17H);

    assertThat(traitee.journee().etat()).isEqualTo(EtatDePresence.ABSENT);
    assertThat(traitee.journee().amplitude()).contains(new Periode(LE_10_MAI_2026_A_17H, LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldNOuvrirQuUneArriveeSurUneRepriseSansJournee() {
    PresenceTraitee traitee = pointeA(TypeDEvenementDePresence.REPRISE, LE_10_MAI_2026_A_13H);

    assertThat(traitee.journee().etat()).isEqualTo(EtatDePresence.PRESENT);
    assertThat(traitee.journee().journal().evenements())
      .extracting(EvenementDePresence::type)
      .containsExactly(TypeDEvenementDePresence.ARRIVEE);
  }

  @Test
  void shouldNOuvrirQuUneArriveeSurUneArriveeEgareeSansJournee() {
    PresenceTraitee traitee = pointeA(TypeDEvenementDePresence.ARRIVEE, LE_10_MAI_2026_A_8H);

    assertThat(traitee.journee().journal().evenements())
      .extracting(EvenementDePresence::type)
      .containsExactly(TypeDEvenementDePresence.ARRIVEE);
  }

  @Test
  void shouldAbsorberUneArriveeEgareeSurUneJourneeEnCours() {
    JourneeDeTravail presente = arriveA(LE_10_MAI_2026_A_7H);

    PresenceTraitee arrivee = pointeA(TypeDEvenementDePresence.ARRIVEE, LE_10_MAI_2026_A_9H);

    assertThat(arrivee.absorbee()).isTrue();
    assertThat(arrivee.journee()).isEqualTo(presente);
  }

  @Test
  void shouldOuvrirUneNouvelleJourneeApresUneJourneeFermee() {
    arriveA(LE_10_MAI_2026_A_7H);
    JourneeDeTravail matin = pointeA(TypeDEvenementDePresence.DEPART, LE_10_MAI_2026_A_12H).journee();

    PresenceTraitee apresMidi = pointeA(TypeDEvenementDePresence.PAUSE, LE_10_MAI_2026_A_16H);

    assertThat(apresMidi.journee().id()).isNotEqualTo(matin.id());
    assertThat(apresMidi.journee().debut()).contains(LE_10_MAI_2026_A_16H);
  }

  @Test
  void shouldToujoursRefuserUnOperateurInconnu() {
    PointageDePresenceAEnregistrer inconnu = new PointageDePresenceAEnregistrer(
      new OperateurId(UUID.randomUUID()),
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.PAUSE
    );

    assertThatThrownBy(() -> service.pointe(inconnu)).isExactlyInstanceOf(OperateurDAtelierIntrouvableException.class);
  }

  /**
   * Un geste sans journee en cours mais date dans une journee deja fermee est un geste rejoue dans le desordre : il
   * reste refuse, plutot que d'ouvrir une journee qui la chevaucherait.
   */
  @Test
  void shouldNePasOuvrirUneJourneeDansUneJourneeFermee() {
    arriveA(LE_10_MAI_2026_A_7H);
    pointeA(TypeDEvenementDePresence.DEPART, LE_10_MAI_2026_A_17H);
    maintenant.set(LE_10_MAI_2026_A_20H);
    PointageDePresenceAEnregistrer pauseDeMidi = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.PAUSE,
      Optional.of(LE_10_MAI_2026_A_12H),
      EvenementDePresenceId.newId()
    );

    assertThatThrownBy(() -> service.pointe(pauseDeMidi, () -> ARRIVEE_IMPLICITE))
      .isExactlyInstanceOf(AucuneJourneeDeTravailEnCoursException.class)
      .hasMessageContaining(OPERATEUR_ID_DUPONT.uuid().toString());
  }

  /**
   * Le double appui sur « pause » : le second geste ne change rien, il est absorbe.
   */
  @Test
  void shouldAbsorberUnePauseDejaEnPause() {
    arriveA(LE_10_MAI_2026_A_7H);
    JourneeDeTravail enPause = pointeA(TypeDEvenementDePresence.PAUSE, LE_10_MAI_2026_A_12H).journee();

    PresenceTraitee second = pointeA(TypeDEvenementDePresence.PAUSE, LE_10_MAI_2026_A_12H.plusSeconds(2));

    assertThat(second.absorbee()).isTrue();
    assertThat(second.journee()).isEqualTo(enPause);
    assertThat(journees.get(enPause.id())).contains(enPause);
  }

  @Test
  void shouldAbsorberUneRepriseDejaPresent() {
    JourneeDeTravail presente = arriveA(LE_10_MAI_2026_A_7H);

    PresenceTraitee reprise = pointeA(TypeDEvenementDePresence.REPRISE, LE_10_MAI_2026_A_9H);

    assertThat(reprise.absorbee()).isTrue();
    assertThat(reprise.journee()).isEqualTo(presente);
  }

  @Test
  void shouldNeDemanderAucuneArriveeImplicitePourUnGesteAbsorbe() {
    arriveA(LE_10_MAI_2026_A_7H);
    maintenant.set(LE_10_MAI_2026_A_9H);

    PresenceTraitee reprise = service.pointe(
      new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.REPRISE),
      () -> {
        throw new AssertionError("aucune arrivee implicite pour un geste absorbe");
      }
    );

    assertThat(reprise.absorbee()).isTrue();
  }

  /**
   * Une reprise rejouee dans le desordre, datee avant la pause qu'elle suppose, n'est pas redondante : elle casse
   * l'enchainement et reste refusee.
   */
  @Test
  void shouldToujoursRefuserUnGesteRejoueDansLeDesordre() {
    arriveA(LE_10_MAI_2026_A_7H);
    pointeA(TypeDEvenementDePresence.PAUSE, LE_10_MAI_2026_A_12H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    PointageDePresenceAEnregistrer repriseAnterieure = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.REPRISE,
      Optional.of(LE_10_MAI_2026_A_9H),
      EvenementDePresenceId.newId()
    );

    assertThatThrownBy(() -> service.pointe(repriseAnterieure)).isExactlyInstanceOf(TransitionDePresenceInterditeException.class);
  }

  /**
   * Une pause rejouee avant la pause deja pointee n'est pas un double appui : datee avant le dernier fait, elle casse
   * l'enchainement et reste refusee.
   */
  @Test
  void shouldToujoursRefuserUnGesteRedondantRejoueAvantLeDernierFait() {
    arriveA(LE_10_MAI_2026_A_7H);
    pointeA(TypeDEvenementDePresence.PAUSE, LE_10_MAI_2026_A_12H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    PointageDePresenceAEnregistrer pauseAnterieure = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.PAUSE,
      Optional.of(LE_10_MAI_2026_A_9H),
      EvenementDePresenceId.newId()
    );

    assertThatThrownBy(() -> service.pointe(pauseAnterieure)).isExactlyInstanceOf(TransitionDePresenceInterditeException.class);
  }

  private JourneeDeTravail arriveA(Instant instant) {
    maintenant.set(instant);

    return service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT)).journee();
  }

  private PresenceTraitee pointeA(TypeDEvenementDePresence type, Instant instant) {
    maintenant.set(instant);

    return service.pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, type), () -> ARRIVEE_IMPLICITE);
  }
}
