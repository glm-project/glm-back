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
    .clock(maintenant::get);

  /**
   * Le depart presse par un operateur jamais arrive : une journee de duree nulle, ouverte par une arrivee implicite a
   * l'heure du geste, rien de refuse.
   */
  @Test
  void shouldOuvrirEtFermerUneJourneeSurUnDepartSansJournee() {
    PresenceTraitee traitee = pointeA(TypeDEvenementDePresence.DEPART, LE_10_MAI_2026_A_17H);

    assertThat(traitee.absorbee()).isFalse();
    assertThat(traitee.journee().etat()).isEqualTo(EtatDePresence.ABSENT);
    assertThat(traitee.journee().amplitude()).contains(new Periode(LE_10_MAI_2026_A_17H, LE_10_MAI_2026_A_17H));
    assertThat(traitee.journee().journal().evenements())
      .extracting(EvenementDePresence::id, EvenementDePresence::type)
      .containsExactly(
        tuple(ARRIVEE_IMPLICITE, TypeDEvenementDePresence.ARRIVEE),
        tuple(traitee.journee().journal().evenements().getLast().id(), TypeDEvenementDePresence.DEPART)
      );
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

    PresenceTraitee apresMidi = pointeA(TypeDEvenementDePresence.DEPART, LE_10_MAI_2026_A_16H);

    assertThat(apresMidi.journee().id()).isNotEqualTo(matin.id());
    assertThat(apresMidi.journee().debut()).contains(LE_10_MAI_2026_A_16H);
  }

  @Test
  void shouldToujoursRefuserUnOperateurInconnu() {
    PointageDePresenceAEnregistrer inconnu = new PointageDePresenceAEnregistrer(
      new OperateurId(UUID.randomUUID()),
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.DEPART
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
    PointageDePresenceAEnregistrer departDeMidi = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.DEPART,
      Optional.of(LE_10_MAI_2026_A_12H),
      EvenementDePresenceId.newId()
    );

    assertThatThrownBy(() -> service.pointe(departDeMidi, () -> ARRIVEE_IMPLICITE))
      .isExactlyInstanceOf(AucuneJourneeDeTravailEnCoursException.class)
      .hasMessageContaining(OPERATEUR_ID_DUPONT.uuid().toString());
  }

  @Test
  void shouldNeDemanderAucuneArriveeImplicitePourUnGesteAbsorbe() {
    arriveA(LE_10_MAI_2026_A_7H);
    maintenant.set(LE_10_MAI_2026_A_9H);

    PresenceTraitee arrivee = service.pointe(
      new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.ARRIVEE),
      () -> {
        throw new AssertionError("aucune arrivee implicite pour un geste absorbe");
      }
    );

    assertThat(arrivee.absorbee()).isTrue();
  }

  /**
   * Un depart rejoue dans le desordre, date avant l'arrivee qu'il suppose, n'est pas redondant : il casse
   * l'enchainement et reste refuse.
   */
  @Test
  void shouldToujoursRefuserUnGesteRejoueDansLeDesordre() {
    arriveA(LE_10_MAI_2026_A_9H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    PointageDePresenceAEnregistrer departAnterieur = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.DEPART,
      Optional.of(LE_10_MAI_2026_A_7H),
      EvenementDePresenceId.newId()
    );

    assertThatThrownBy(() -> service.pointe(departAnterieur)).isExactlyInstanceOf(TransitionDePresenceInterditeException.class);
  }

  /**
   * Une arrivee egaree datee avant l'arrivee deja pointee n'est pas un geste redondant : datee avant le dernier fait,
   * elle casse l'enchainement et reste refusee.
   */
  @Test
  void shouldToujoursRefuserUnGesteRedondantRejoueAvantLeDernierFait() {
    arriveA(LE_10_MAI_2026_A_9H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    PointageDePresenceAEnregistrer arriveeAnterieure = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.ARRIVEE,
      Optional.of(LE_10_MAI_2026_A_7H),
      EvenementDePresenceId.newId()
    );

    assertThatThrownBy(() -> service.pointe(arriveeAnterieure)).isExactlyInstanceOf(TransitionDePresenceInterditeException.class);
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
