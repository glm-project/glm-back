package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Strategie « bornes de fin de journee », lot 8b : un pointage inhabituel est enregistre quand meme, et signale au
 * gestionnaire. Poste non habilite : le temps compte. Date avant l'engagement ou dans le futur : le geste est ramene
 * a l'engagement ou a sa reception, et la date declaree est conservee. Les actes du gestionnaire, eux, restent
 * refuses.
 */
@UnitTest
class SignalementDesPointagesTest {

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final PointagesSignalesEnMemoire signalements = new PointagesSignalesEnMemoire();
  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final RessourcesDAtelierEnMemoire ressources = RessourcesDAtelierEnMemoire.deLAtelier();
  private final SuivisDAtelierService atelier = SuivisDAtelierService.builder()
    .repository(suivis)
    .elements(id -> Optional.of(elementEngageOf2026000042()).filter(element -> element.id().equals(id)))
    .operateurs(ressources.operateurs())
    .postes(ressources.postes())
    .habilitations(ressources.habilitations())
    .signalements(signalements)
    .clock(maintenant::get);
  private final JourneesDeTravailService presence = JourneesDeTravailService.builder()
    .repository(journees)
    .operateurs(ressources.operateurs())
    .seuil(() -> AMPLITUDE_MAXIMALE_13H)
    .signalements(signalements)
    .clock(maintenant::get);

  /**
   * L'habilitation a ete retiree pendant que le pupitre etait hors ligne : le temps de Martin compte, et le
   * gestionnaire le voit.
   */
  @Test
  void shouldEnregistrerEtSignalerUnPointageSurUnPosteNonHabilite() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite traite = atelier.pointe(pointage(engage.id(), OPERATEUR_ID_MARTIN, Optional.empty()));

    EvenementDAtelier debut = traite.suivi().journal().evenements().getLast();
    assertThat(debut.operateur()).isEqualTo(OPERATEUR_ID_MARTIN);
    assertThat(debut.nature()).contains(NATURE_FRAISAGE);
    assertThat(signalements.get(new PointageSignaleId(debut.id().uuid()))).contains(
      PointageSignale.builder()
        .id(new PointageSignaleId(debut.id().uuid()))
        .cible(new CibleDuSignalement(TypeDeCible.SUIVI_D_ATELIER, engage.id().uuid()))
        .operateur(OPERATEUR_ID_MARTIN)
        .motifs(Set.of(MotifDeSignalement.OPERATEUR_NON_HABILITE))
        .horodatage(Horodatage.saisiA(LE_10_MAI_2026_A_8H))
        .dateDeclaree(Optional.empty())
        .resolution(Optional.empty())
    );
  }

  @Test
  void shouldNeRienSignalerDUnPointageOrdinaire() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);

    atelier.pointe(pointage(engage.id(), OPERATEUR_ID_DUPONT, Optional.empty()));

    assertThat(signalements.tous()).isEmpty();
  }

  /**
   * L'horloge du pupitre retardait : le debut est ramene a l'engagement, la date declaree reste tracee.
   */
  @Test
  void shouldRamenerALEngagementUnPointageAnterieur() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite traite = atelier.pointe(
      pointage(engage.id(), OPERATEUR_ID_DUPONT, Optional.of(LE_10_MAI_2026_A_7H.minusSeconds(3600)))
    );

    EvenementDAtelier debut = traite.suivi().journal().evenements().getLast();
    assertThat(debut.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_7H);
    PointageSignale signale = signalements.get(new PointageSignaleId(debut.id().uuid())).orElseThrow();
    assertThat(signale.motifs()).containsExactly(MotifDeSignalement.DATE_ANTERIEURE_A_L_ENGAGEMENT);
    assertThat(signale.dateDeclaree()).contains(LE_10_MAI_2026_A_7H.minusSeconds(3600));
    assertThat(signale.horodatage()).isEqualTo(new Horodatage(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_8H));
  }

  /**
   * L'horloge du pupitre avancait : le debut est ramene a sa reception.
   */
  @Test
  void shouldRamenerASaReceptionUnPointageDAtelierFutur() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite traite = atelier.pointe(pointage(engage.id(), OPERATEUR_ID_DUPONT, Optional.of(LE_10_MAI_2026_A_9H)));

    EvenementDAtelier debut = traite.suivi().journal().evenements().getLast();
    assertThat(debut.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_8H);
    PointageSignale signale = signalements.get(new PointageSignaleId(debut.id().uuid())).orElseThrow();
    assertThat(signale.motifs()).containsExactly(MotifDeSignalement.DATE_FUTURE);
    assertThat(signale.dateDeclaree()).contains(LE_10_MAI_2026_A_9H);
  }

  @Test
  void shouldCumulerLesMotifsDUnMemePointage() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite traite = atelier.pointe(pointage(engage.id(), OPERATEUR_ID_MARTIN, Optional.of(LE_10_MAI_2026_A_9H)));

    PointageSignale signale = signalements
      .get(new PointageSignaleId(traite.suivi().journal().evenements().getLast().id().uuid()))
      .orElseThrow();
    assertThat(signale.motifs()).containsExactlyInAnyOrder(MotifDeSignalement.OPERATEUR_NON_HABILITE, MotifDeSignalement.DATE_FUTURE);
  }

  @Test
  void shouldNeRienSignalerDUnGesteAbsorbe() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    PointageAEnregistrer fin = PointageAEnregistrer.pupitreBuilder()
      .suivi(engage.id())
      .type(TypeDEvenementDAtelier.FIN)
      .operateur(OPERATEUR_ID_MARTIN)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_MARTIN)
      .dateDeSurvenue(Optional.of(LE_10_MAI_2026_A_9H))
      .evenement(EvenementDAtelierId.newId());

    assertThat(atelier.pointe(fin).absorbe()).isTrue();
    assertThat(signalements.tous()).isEmpty();
  }

  @Test
  void shouldToujoursRefuserUnPosteInconnu() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer surUnPosteInconnu = PointageAEnregistrer.builder()
      .suivi(engage.id())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(new PosteDeTravailId(java.util.UUID.randomUUID())))
      .auteur(AUTEUR_DUPONT);

    assertThatThrownBy(() -> atelier.pointe(surUnPosteInconnu)).isExactlyInstanceOf(PosteDAtelierIntrouvableException.class);
  }

  /**
   * Le gestionnaire, lui, corrige sur le champ : sa regularisation sur un poste non habilite reste refusee.
   */
  @Test
  void shouldToujoursRefuserUneRegularisationSurUnPosteNonHabilite() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_11_MAI_2026_A_9H15);
    RegularisationAEnregistrer regularisation = RegularisationAEnregistrer.builder()
      .suivi(engage.id())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_MARTIN)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(LE_10_MAI_2026_A_8H);

    assertThatThrownBy(() -> atelier.regularise(regularisation)).isExactlyInstanceOf(OperateurNonHabiliteException.class);
  }

  @Test
  void shouldResoudreLeSignalementDUnPointageAnnule() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    EvenementDAtelier debut = atelier
      .pointe(pointage(engage.id(), OPERATEUR_ID_MARTIN, Optional.empty()))
      .suivi()
      .journal()
      .evenements()
      .getLast();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    atelier.annule(
      AnnulationAEnregistrer.builder().suivi(engage.id()).evenement(debut.id()).auteur(AUTEUR_LEROY).motif(MOTIF_ERREUR_DE_SAISIE)
    );

    assertThat(signalements.get(new PointageSignaleId(debut.id().uuid())).orElseThrow().resolution()).contains(
      new Resolution(TypeDeResolution.ANNULE, AUTEUR_LEROY, LE_11_MAI_2026_A_9H15)
    );
  }

  @Test
  void shouldResoudreLeSignalementDUnPointageCorrige() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    EvenementDAtelier debut = atelier
      .pointe(pointage(engage.id(), OPERATEUR_ID_DUPONT, Optional.of(LE_10_MAI_2026_A_9H)))
      .suivi()
      .journal()
      .evenements()
      .getLast();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    atelier.corrige(
      new CorrectionAEnregistrer(
        debut.id(),
        MOTIF_ERREUR_DE_SAISIE,
        RegularisationAEnregistrer.builder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
          .auteur(AUTEUR_LEROY)
          .dateDeSurvenue(LE_10_MAI_2026_A_7H30)
      )
    );

    assertThat(signalements.get(new PointageSignaleId(debut.id().uuid())).orElseThrow().resolution()).contains(
      new Resolution(TypeDeResolution.CORRIGE, AUTEUR_LEROY, LE_11_MAI_2026_A_9H15)
    );
  }

  @Test
  void shouldGarderLAcquittementDUnPointageAnnuleEnsuite() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    EvenementDAtelier debut = atelier
      .pointe(pointage(engage.id(), OPERATEUR_ID_MARTIN, Optional.empty()))
      .suivi()
      .journal()
      .evenements()
      .getLast();
    maintenant.set(LE_10_MAI_2026_A_9H);
    new PointagesSignalesService(signalements, maintenant::get).acquitte(new PointageSignaleId(debut.id().uuid()), AUTEUR_LEROY);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    atelier.annule(
      AnnulationAEnregistrer.builder().suivi(engage.id()).evenement(debut.id()).auteur(AUTEUR_LEROY).motif(MOTIF_ERREUR_DE_SAISIE)
    );

    assertThat(signalements.get(new PointageSignaleId(debut.id().uuid())).orElseThrow().resolution()).contains(
      new Resolution(TypeDeResolution.ACQUITTE, AUTEUR_LEROY, LE_10_MAI_2026_A_9H)
    );
  }

  @Test
  void shouldIgnorerLAnnulationDUnPointageNonSignale() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    EvenementDAtelier debut = atelier
      .pointe(pointage(engage.id(), OPERATEUR_ID_DUPONT, Optional.empty()))
      .suivi()
      .journal()
      .evenements()
      .getLast();

    atelier.annule(
      AnnulationAEnregistrer.builder().suivi(engage.id()).evenement(debut.id()).auteur(AUTEUR_LEROY).motif(MOTIF_ERREUR_DE_SAISIE)
    );

    assertThat(signalements.tous()).isEmpty();
  }

  @Test
  void shouldRamenerASaReceptionUneArriveeFuture() {
    maintenant.set(LE_10_MAI_2026_A_7H);

    JourneeDeTravail journee = presence
      .arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, Optional.of(LE_10_MAI_2026_A_8H)))
      .journee();

    EvenementDePresence arrivee = journee.journal().evenements().getFirst();
    assertThat(arrivee.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_7H);
    PointageSignale signale = signalements.get(new PointageSignaleId(arrivee.id().uuid())).orElseThrow();
    assertThat(signale.cible()).isEqualTo(new CibleDuSignalement(TypeDeCible.JOURNEE_DE_TRAVAIL, journee.id().uuid()));
    assertThat(signale.motifs()).containsExactly(MotifDeSignalement.DATE_FUTURE);
    assertThat(signale.dateDeclaree()).contains(LE_10_MAI_2026_A_8H);
  }

  @Test
  void shouldRamenerASaReceptionUnePauseFuture() {
    presence.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));
    maintenant.set(LE_10_MAI_2026_A_12H);

    JourneeDeTravail journee = presence
      .pointe(
        new PointageDePresenceAEnregistrer(
          OPERATEUR_ID_DUPONT,
          AUTEUR_DUPONT,
          TypeDEvenementDePresence.PAUSE,
          Optional.of(LE_10_MAI_2026_A_13H),
          EvenementDePresenceId.newId()
        )
      )
      .journee();

    EvenementDePresence pause = journee.journal().evenements().getLast();
    assertThat(pause.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_12H);
    assertThat(signalements.get(new PointageSignaleId(pause.id().uuid())).orElseThrow().motifs()).containsExactly(
      MotifDeSignalement.DATE_FUTURE
    );
  }

  @Test
  void shouldNeRienSignalerDUneArriveeAbsorbee() {
    presence.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));
    maintenant.set(LE_10_MAI_2026_A_9H);

    assertThat(
      presence.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, Optional.of(LE_10_MAI_2026_A_12H))).absorbee()
    ).isTrue();
    assertThat(signalements.tous()).isEmpty();
  }

  @Test
  void shouldResoudreLeSignalementDUnePresenceAnnulee() {
    presence.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT));
    maintenant.set(LE_10_MAI_2026_A_12H);
    JourneeDeTravail journee = presence
      .pointe(
        new PointageDePresenceAEnregistrer(
          OPERATEUR_ID_DUPONT,
          AUTEUR_DUPONT,
          TypeDEvenementDePresence.PAUSE,
          Optional.of(LE_10_MAI_2026_A_13H),
          EvenementDePresenceId.newId()
        )
      )
      .journee();
    EvenementDePresenceId pause = journee.journal().evenements().getLast().id();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    presence.annule(
      AnnulationDePresenceAEnregistrer.builder().journee(journee.id()).evenement(pause).auteur(AUTEUR_LEROY).motif(MOTIF_ERREUR_DE_SAISIE)
    );

    assertThat(signalements.get(new PointageSignaleId(pause.uuid())).orElseThrow().resolution().map(Resolution::type)).contains(
      TypeDeResolution.ANNULE
    );
  }

  @Test
  void shouldResoudreLeSignalementDUnePresenceCorrigee() {
    maintenant.set(LE_10_MAI_2026_A_7H);
    JourneeDeTravail journee = presence
      .arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, Optional.of(LE_10_MAI_2026_A_8H)))
      .journee();
    EvenementDePresenceId arrivee = journee.journal().evenements().getFirst().id();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    presence.corrige(
      new CorrectionDePresenceAEnregistrer(
        arrivee,
        MOTIF_ERREUR_DE_SAISIE,
        RegularisationDePresenceAEnregistrer.builder()
          .journee(journee.id())
          .type(TypeDEvenementDePresence.ARRIVEE)
          .auteur(AUTEUR_LEROY)
          .dateDeSurvenue(LE_10_MAI_2026_A_7H30)
      )
    );

    assertThat(signalements.get(new PointageSignaleId(arrivee.uuid())).orElseThrow().resolution().map(Resolution::type)).contains(
      TypeDeResolution.CORRIGE
    );
  }

  private SuiviDAtelier engage() {
    return atelier.engage(new EngagementAEnregistrer(ELEMENT_OF_2026_000042, AUTEUR_LEROY));
  }

  private static PointageAEnregistrer pointage(SuiviDAtelierId suivi, OperateurId operateur, Optional<Instant> date) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(suivi)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(operateur)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT)
      .dateDeSurvenue(date)
      .evenement(EvenementDAtelierId.newId());
  }
}
