package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Strategie « bornes de fin de journee », D8 : deux journees d'un meme operateur ne se chevauchent jamais. Une
 * regularisation ou une correction qui le provoquerait est refusee au gestionnaire. L'etendue d'une journee va de son
 * premier a son dernier fait connu.
 */
@UnitTest
class ChevauchementDeJourneesTest {

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final JourneesDeTravailService service = JourneesDeTravailService.builder()
    .repository(journees)
    .operateurs(RessourcesDAtelierEnMemoire.deLAtelier().operateurs())
    .seuil(() -> AMPLITUDE_MAXIMALE_13H)
    .clock(maintenant::get);

  /**
   * E6 : lundi abandonne, mardi ouvert a 07:00. Regulariser le depart oublie de lundi a 17:00 est accepte.
   */
  @Test
  void shouldAccepterUnDepartRegulariseAvantLaJourneeSuivante() {
    JourneeDeTravail lundi = arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    arriveA(OPERATEUR_ID_DUPONT, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    JourneeDeTravail regularisee = service.regularise(depart(lundi, LE_10_MAI_2026_A_17H));

    assertThat(regularisee.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
  }

  /**
   * E6 : le meme depart saisi a mardi 08:00 ferait chevaucher lundi sur mardi.
   */
  @Test
  void shouldRefuserUnDepartRegulariseDansLaJourneeSuivante() {
    JourneeDeTravail lundi = arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    JourneeDeTravail mardi = arriveA(OPERATEUR_ID_DUPONT, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);
    RegularisationDePresenceAEnregistrer departDeMardi = depart(lundi, LE_11_MAI_2026_A_8H);

    assertThatThrownBy(() -> service.regularise(departDeMardi))
      .isExactlyInstanceOf(ChevauchementDeJourneesException.class)
      .hasMessageContaining(lundi.id().uuid().toString())
      .hasMessageContaining(mardi.id().uuid().toString());
    assertThat(journees.get(lundi.id())).contains(lundi);
  }

  @Test
  void shouldRefuserDeuxJourneesQuiSeTouchent() {
    JourneeDeTravail lundi = arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    arriveA(OPERATEUR_ID_DUPONT, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);
    RegularisationDePresenceAEnregistrer departAuMemeInstant = depart(lundi, LE_11_MAI_2026_A_7H);

    assertThatThrownBy(() -> service.regularise(departAuMemeInstant)).isExactlyInstanceOf(ChevauchementDeJourneesException.class);
  }

  @Test
  void shouldRefuserUneArriveeCorrigeeQuiReculeDansLaJourneePrecedente() {
    arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    maintenant.set(LE_10_MAI_2026_A_12H);
    service.pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.PAUSE));
    JourneeDeTravail mardi = arriveA(OPERATEUR_ID_DUPONT, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);
    CorrectionDePresenceAEnregistrer arriveeDeLundiMatin = new CorrectionDePresenceAEnregistrer(
      mardi.journal().evenements().getFirst().id(),
      MOTIF_ERREUR_DE_SAISIE,
      RegularisationDePresenceAEnregistrer.builder()
        .journee(mardi.id())
        .type(TypeDEvenementDePresence.ARRIVEE)
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(LE_10_MAI_2026_A_9H)
    );

    assertThatThrownBy(() -> service.corrige(arriveeDeLundiMatin)).isExactlyInstanceOf(ChevauchementDeJourneesException.class);
  }

  @Test
  void shouldRefuserUneCorrectionQuiFeraitChevaucher() {
    JourneeDeTravail lundi = arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    maintenant.set(LE_10_MAI_2026_A_17H);
    JourneeDeTravail fermee = service.pointe(
      new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.DEPART)
    );
    arriveA(OPERATEUR_ID_DUPONT, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);
    CorrectionDePresenceAEnregistrer departDeMardi = new CorrectionDePresenceAEnregistrer(
      fermee.journal().evenements().getLast().id(),
      MOTIF_ERREUR_DE_SAISIE,
      depart(lundi, LE_11_MAI_2026_A_8H)
    );

    assertThatThrownBy(() -> service.corrige(departDeMardi)).isExactlyInstanceOf(ChevauchementDeJourneesException.class);
  }

  @Test
  void shouldAccepterUneCorrectionQuiNeChevauchePas() {
    JourneeDeTravail lundi = arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    maintenant.set(LE_10_MAI_2026_A_17H);
    JourneeDeTravail fermee = service.pointe(
      new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.DEPART)
    );
    arriveA(OPERATEUR_ID_DUPONT, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    JourneeDeTravail corrigee = service.corrige(
      new CorrectionDePresenceAEnregistrer(
        fermee.journal().evenements().getLast().id(),
        MOTIF_ERREUR_DE_SAISIE,
        depart(lundi, LE_10_MAI_2026_A_16H)
      )
    );

    assertThat(corrigee.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_16H));
  }

  @Test
  void shouldIgnorerLesJourneesDUnAutreOperateur() {
    JourneeDeTravail lundi = arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    arriveA(OPERATEUR_ID_MARTIN, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    JourneeDeTravail regularisee = service.regularise(depart(lundi, LE_11_MAI_2026_A_8H));

    assertThat(regularisee.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_11_MAI_2026_A_8H));
  }

  /**
   * L'etendue s'arrete au dernier fait connu : une journee abandonnee sans depart ne deborde pas sur la suivante, et
   * sa propre regularisation ne se heurte pas a elle-meme.
   */
  @Test
  void shouldNePasFaireDeborderUneJourneeAbandonneeSurLaSuivante() {
    JourneeDeTravail lundi = arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    JourneeDeTravail mardi = arriveA(OPERATEUR_ID_DUPONT, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    JourneeDeTravail regularisee = service.regularise(depart(mardi, LE_11_MAI_2026_A_9H));

    assertThat(regularisee.amplitude()).contains(new Periode(LE_11_MAI_2026_A_7H, LE_11_MAI_2026_A_9H));
    assertThat(journees.get(lundi.id())).contains(lundi);
  }

  @Test
  void shouldNeJamaisRefuserUneAnnulation() {
    JourneeDeTravail lundi = arriveA(OPERATEUR_ID_DUPONT, LE_10_MAI_2026_A_7H);
    maintenant.set(LE_10_MAI_2026_A_12H);
    JourneeDeTravail enPause = service.pointe(
      new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.PAUSE)
    );
    arriveA(OPERATEUR_ID_DUPONT, LE_11_MAI_2026_A_7H);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    JourneeDeTravail annulee = service.annule(
      AnnulationDePresenceAEnregistrer.builder()
        .journee(lundi.id())
        .evenement(enPause.journal().evenements().getLast().id())
        .auteur(AUTEUR_LEROY)
        .motif(MOTIF_ERREUR_DE_SAISIE)
    );

    assertThat(annulee.etat()).isEqualTo(EtatDePresence.PRESENT);
  }

  private JourneeDeTravail arriveA(OperateurId operateur, Instant instant) {
    maintenant.set(instant);

    return service.arrive(new ArriveeAEnregistrer(operateur, AUTEUR_DUPONT)).journee();
  }

  private static RegularisationDePresenceAEnregistrer depart(JourneeDeTravail journee, Instant instant) {
    return RegularisationDePresenceAEnregistrer.builder()
      .journee(journee.id())
      .type(TypeDEvenementDePresence.DEPART)
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(instant);
  }
}
