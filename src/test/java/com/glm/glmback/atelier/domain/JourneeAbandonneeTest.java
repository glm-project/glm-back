package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Strategie « bornes de fin de journee », lot 3 : un geste recu pour une journee abandonnee ouvre une nouvelle journee,
 * une arrivee sous le seuil est absorbee. Le seuil vaut 13 h sauf mention contraire.
 */
@UnitTest
class JourneeAbandonneeTest {

  private static final EvenementDePresenceId ARRIVEE_IMPLICITE = EvenementDePresenceId.newId();

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final AtomicReference<AmplitudeMaximale> seuil = new AtomicReference<>(AMPLITUDE_MAXIMALE_13H);
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final JourneesDeTravailService service = JourneesDeTravailService.builder()
    .repository(journees)
    .operateurs(RessourcesDAtelierEnMemoire.deLAtelier().operateurs())
    .seuil(seuil::get)
    .clock(maintenant::get);

  /**
   * E1 : l'operateur de nuit se reidentifie a 3 h. Sept heures apres son arrivee, il est deja la.
   */
  @Test
  void shouldAbsorberUneArriveeSousLeSeuil() {
    JourneeDeTravail nuit = arriveA(LE_10_MAI_2026_A_20H).journee();

    PresenceTraitee reidentification = arriveA(LE_11_MAI_2026_A_3H);

    assertThat(reidentification.absorbee()).isTrue();
    assertThat(reidentification.journee()).isEqualTo(nuit);
    assertThat(journees.get(nuit.id()).orElseThrow().journal().evenements()).hasSize(1);
  }

  @Test
  void shouldAbsorberUneArriveeAuSeuilPile() {
    JourneeDeTravail matin = arriveA(LE_10_MAI_2026_A_7H).journee();

    PresenceTraitee arrivee = arriveA(LE_10_MAI_2026_A_20H);

    assertThat(arrivee.absorbee()).isTrue();
    assertThat(arrivee.journee().id()).isEqualTo(matin.id());
  }

  @Test
  void shouldOuvrirUneNouvelleJourneeUneSecondeApresLeSeuil() {
    JourneeDeTravail matin = arriveA(LE_10_MAI_2026_A_7H).journee();

    PresenceTraitee arrivee = arriveA(LE_10_MAI_2026_A_20H.plusSeconds(1));

    assertThat(arrivee.absorbee()).isFalse();
    assertThat(arrivee.journee().id()).isNotEqualTo(matin.id());
    assertThat(arrivee.journee().debut()).contains(LE_10_MAI_2026_A_20H.plusSeconds(1));
  }

  /**
   * E2 : Dupont part lundi sans rien pointer et revient mardi. La journee de lundi reste telle quelle, sans depart.
   */
  @Test
  void shouldOuvrirUneNouvelleJourneeLeLendemainDUnDepartOublie() {
    JourneeDeTravail lundi = arriveA(LE_10_MAI_2026_A_7H).journee();

    PresenceTraitee mardi = arriveA(LE_11_MAI_2026_A_7H);

    assertThat(mardi.absorbee()).isFalse();
    assertThat(mardi.journee().id()).isNotEqualTo(lundi.id());
    assertThat(mardi.journee().debut()).contains(LE_11_MAI_2026_A_7H);
    assertThat(journees.get(lundi.id())).contains(lundi);
    assertThat(journees.getEnCoursPour(OPERATEUR_ID_DUPONT)).contains(mardi.journee());
  }

  @Test
  void shouldPointerLesGestesDuLendemainSurLaNouvelleJournee() {
    JourneeDeTravail lundi = arriveA(LE_10_MAI_2026_A_7H).journee();
    JourneeDeTravail mardi = arriveA(LE_11_MAI_2026_A_7H).journee();

    JourneeDeTravail enPause = pointeA(TypeDEvenementDePresence.PAUSE, LE_11_MAI_2026_A_9H);

    assertThat(enPause.id()).isEqualTo(mardi.id());
    assertThat(enPause.etat()).isEqualTo(EtatDePresence.EN_PAUSE);
    assertThat(journees.get(lundi.id())).contains(lundi);
  }

  /**
   * E3 : le poste de nuit oublie n'est abandonne qu'a 09:00 le lendemain ; le retour du soir ouvre une journee.
   */
  @Test
  void shouldOuvrirUneNouvelleJourneeApresUnPosteDeNuitOublie() {
    JourneeDeTravail nuit = arriveA(LE_10_MAI_2026_A_20H).journee();

    PresenceTraitee soir = arriveA(LE_11_MAI_2026_A_20H);

    assertThat(soir.absorbee()).isFalse();
    assertThat(soir.journee().id()).isNotEqualTo(nuit.id());
  }

  /**
   * E5 : le pupitre hors ligne envoie un depart le lendemain. Rien n'est refuse : une journee de duree nulle s'ouvre
   * et se ferme a l'heure du geste.
   */
  @Test
  void shouldOuvrirEtFermerUneJourneeSurUnDepartTardif() {
    JourneeDeTravail lundi = arriveA(LE_10_MAI_2026_A_7H).journee();

    JourneeDeTravail mardi = pointeA(TypeDEvenementDePresence.DEPART, LE_11_MAI_2026_A_8H30);

    assertThat(mardi.id()).isNotEqualTo(lundi.id());
    assertThat(mardi.etat()).isEqualTo(EtatDePresence.ABSENT);
    assertThat(mardi.amplitude()).contains(new Periode(LE_11_MAI_2026_A_8H30, LE_11_MAI_2026_A_8H30));
    assertThat(mardi.journal().evenements())
      .extracting(EvenementDePresence::type)
      .containsExactly(TypeDEvenementDePresence.ARRIVEE, TypeDEvenementDePresence.DEPART);
    assertThat(journees.get(lundi.id())).contains(lundi);
  }

  @Test
  void shouldOuvrirUneJourneeQuiCommenceEnPauseSurUnePauseTardive() {
    arriveA(LE_10_MAI_2026_A_7H);

    JourneeDeTravail mardi = pointeA(TypeDEvenementDePresence.PAUSE, LE_11_MAI_2026_A_8H30);

    assertThat(mardi.etat()).isEqualTo(EtatDePresence.EN_PAUSE);
    assertThat(mardi.debut()).contains(LE_11_MAI_2026_A_8H30);
    assertThat(mardi.journal().evenements())
      .extracting(EvenementDePresence::type)
      .containsExactly(TypeDEvenementDePresence.ARRIVEE, TypeDEvenementDePresence.PAUSE);
  }

  /**
   * Une reprise suppose une pause : sur une journee abandonnee, seule l'arrivee a un sens, la reprise est absorbee.
   */
  @Test
  void shouldNOuvrirQuUneArriveeSurUneRepriseTardive() {
    JourneeDeTravail lundi = arriveA(LE_10_MAI_2026_A_7H).journee();
    maintenant.set(LE_10_MAI_2026_A_12H);
    service.pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.PAUSE));

    JourneeDeTravail mardi = pointeA(TypeDEvenementDePresence.REPRISE, LE_11_MAI_2026_A_8H30);

    assertThat(mardi.id()).isNotEqualTo(lundi.id());
    assertThat(mardi.etat()).isEqualTo(EtatDePresence.PRESENT);
    assertThat(mardi.journal().evenements()).extracting(EvenementDePresence::type).containsExactly(TypeDEvenementDePresence.ARRIVEE);
  }

  /**
   * L'arrivee implicite est tracee : identifiant fourni par le serveur, distinct du geste, a l'heure du geste et au
   * nom de son auteur.
   */
  @Test
  void shouldTracerLArriveeImplicite() {
    arriveA(LE_10_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H);
    EvenementDePresenceId geste = EvenementDePresenceId.newId();
    PointageDePresenceAEnregistrer depart = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_MARTIN,
      TypeDEvenementDePresence.DEPART,
      Optional.of(LE_11_MAI_2026_A_8H30),
      geste
    );

    JourneeDeTravail mardi = service.pointe(depart, () -> ARRIVEE_IMPLICITE).journee();

    EvenementDePresence arrivee = mardi.journal().evenements().getFirst();
    assertThat(arrivee.id()).isEqualTo(ARRIVEE_IMPLICITE);
    assertThat(arrivee.dateDeSurvenue()).isEqualTo(LE_11_MAI_2026_A_8H30);
    assertThat(arrivee.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H);
    assertThat(arrivee.auteur()).isEqualTo(AUTEUR_MARTIN);
    assertThat(mardi.journal().evenements().getLast().id()).isEqualTo(geste);
  }

  @Test
  void shouldNeDemanderAucuneArriveeImpliciteSousLeSeuil() {
    arriveA(LE_10_MAI_2026_A_7H);
    maintenant.set(LE_10_MAI_2026_A_12H);

    JourneeDeTravail journee = service
      .pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.PAUSE), () -> {
        throw new AssertionError("aucune arrivee implicite ne doit etre demandee");
      })
      .journee();

    assertThat(journee.etat()).isEqualTo(EtatDePresence.EN_PAUSE);
  }

  /**
   * Un geste rejoue hors ligne est juge sur son heure, pas sur l'horloge du serveur a sa reception.
   */
  @Test
  void shouldJugerLeSeuilALHeureDuGeste() {
    JourneeDeTravail lundi = arriveA(LE_10_MAI_2026_A_7H).journee();
    maintenant.set(LE_11_MAI_2026_A_9H);

    PresenceTraitee soir = service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, Optional.of(LE_10_MAI_2026_A_17H)));

    assertThat(soir.absorbee()).isTrue();
    assertThat(soir.journee().id()).isEqualTo(lundi.id());
  }

  /**
   * E8 : le gestionnaire passe le seuil a 10 h ; il vaut pour les gestes qui suivent.
   */
  @Test
  void shouldAppliquerLeSeuilCourant() {
    JourneeDeTravail matin = arriveA(LE_10_MAI_2026_A_7H).journee();
    seuil.set(AMPLITUDE_MAXIMALE_10H);

    PresenceTraitee arrivee = arriveA(LE_10_MAI_2026_A_17H.plusSeconds(1800));

    assertThat(arrivee.absorbee()).isFalse();
    assertThat(arrivee.journee().id()).isNotEqualTo(matin.id());
  }

  @Test
  void shouldOuvrirNormalementApresUneJourneeFermee() {
    arriveA(LE_10_MAI_2026_A_7H);
    pointeA(TypeDEvenementDePresence.DEPART, LE_10_MAI_2026_A_17H);

    PresenceTraitee soir = arriveA(LE_10_MAI_2026_A_20H);

    assertThat(soir.absorbee()).isFalse();
    assertThat(soir.journee().debut()).contains(LE_10_MAI_2026_A_20H);
  }

  @Test
  void shouldAbandonnerChaqueJourneeAbandonneeALaSuite() {
    arriveA(LE_10_MAI_2026_A_7H);
    JourneeDeTravail mardi = pointeA(TypeDEvenementDePresence.DEPART, LE_11_MAI_2026_A_8H30);

    PresenceTraitee mercredi = arriveA(LE_11_MAI_2026_A_8H30.plusSeconds(24 * 3600));

    assertThat(mercredi.absorbee()).isFalse();
    assertThat(mercredi.journee().id()).isNotEqualTo(mardi.id());
  }

  private PresenceTraitee arriveA(Instant instant) {
    maintenant.set(instant);

    return service.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));
  }

  private JourneeDeTravail pointeA(TypeDEvenementDePresence type, Instant instant) {
    maintenant.set(instant);

    return service.pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, type)).journee();
  }
}
