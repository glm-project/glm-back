package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class FeuillesDeTempsServiceTest {

  private static final OperateursConnus REFERENTIEL = id ->
    Optional.of(OPERATEUR_CONNU_DUPONT).filter(operateur -> operateur.id().equals(id));
  private static final FuseauHoraireDeLEntreprise A_PARIS = () -> ZONE_PARIS;

  @Test
  void shouldNotLireLHistoriqueDUnOperateurInconnu() {
    FeuillesDeTempsService service = new FeuillesDeTempsService(PresencesEnMemoire.sansJournee(), REFERENTIEL, A_PARIS);

    assertThatThrownBy(() -> service.historique(OPERATEUR_ID_MARTIN, SEMAINE_20_DE_2026))
      .isExactlyInstanceOf(OperateurInconnuException.class)
      .hasMessageContaining(OPERATEUR_ID_MARTIN.uuid().toString());
  }

  @Test
  void shouldPorterLIdentiteRelueEtLaSemaineDemandee() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.sansJournee());

    assertThat(feuille.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(feuille.semaine()).isEqualTo(SEMAINE_20_DE_2026);
  }

  /**
   * Sept jours toujours, meme vides : un trou dans la liste obligerait le lecteur a deviner s'il manque une journee
   * ou si l'operateur n'etait pas la.
   */
  @Test
  void shouldRendreLesSeptJoursDeLaSemaineSansPresence() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.sansJournee());

    assertThat(feuille.jours())
      .extracting(JourDeLaSemaine::jour)
      .containsExactly(
        LocalDate.of(2026, 5, 11),
        LocalDate.of(2026, 5, 12),
        LocalDate.of(2026, 5, 13),
        LocalDate.of(2026, 5, 14),
        LocalDate.of(2026, 5, 15),
        LocalDate.of(2026, 5, 16),
        LocalDate.of(2026, 5, 17)
      );
    assertThat(feuille.jours()).allSatisfy(jour -> assertThat(jour.presence()).isEmpty());
  }

  @Test
  void shouldDemanderLesJourneesRecouvrantLaSemaineDansLaZoneDeLEntreprise() {
    PresencesEnMemoire presences = PresencesEnMemoire.sansJournee();

    historiqueDeDupont(presences);

    assertThat(presences.debutDemande()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
    assertThat(presences.finExclusiveDemandee()).isEqualTo(Instant.parse("2026-05-17T22:00:00Z"));
  }

  @Test
  void shouldRattacherLesFenetresAuJourQuiLesPorte() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi())));

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)),
      new Plage(LE_LUNDI_11_MAI_2026_A_13H, Optional.of(LE_LUNDI_11_MAI_2026_A_17H))
    );
    assertThat(presenceDu(feuille, MARDI_12_MAI_2026)).isEmpty();
  }

  /**
   * Une equipe de nuit compte sur deux jours : c'est la raison d'etre de ce contexte, l'atelier ne connaissant que
   * l'arrivee et le depart.
   */
  @Test
  void shouldScinderUneJourneeAChevalSurMinuit() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundi22HAuMardi2H())));

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026)).containsExactly(
      new Plage(LE_LUNDI_11_MAI_2026_A_22H, Optional.of(LE_MARDI_12_MAI_2026_A_MINUIT))
    );
    assertThat(presenceDu(feuille, MARDI_12_MAI_2026)).containsExactly(
      new Plage(LE_MARDI_12_MAI_2026_A_MINUIT, Optional.of(LE_MARDI_12_MAI_2026_A_2H))
    );
  }

  @Test
  void shouldLaisserOuverteLaFenetreDUneJourneeSansDepart() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuMardiOuverteA8H())));

    assertThat(presenceDu(feuille, MARDI_12_MAI_2026)).containsExactly(new Plage(LE_MARDI_12_MAI_2026_A_8H, Optional.empty()));
    assertThat(presenceDu(feuille, MERCREDI_13_MAI_2026)).isEmpty();
  }

  @Test
  void shouldIgnorerUneJourneeHorsDeLaSemaine() {
    FeuilleDeTemps feuille = historiqueDeDupont(PresencesEnMemoire.avec(List.of(journeeDuDimanchePrecedentDe8HA17H())));

    assertThat(feuille.jours()).allSatisfy(jour -> assertThat(jour.presence()).isEmpty());
  }

  /**
   * L'ordre des journees rendues par le port ne doit rien decider : deux venues le meme jour se lisent dans l'ordre
   * des heures.
   */
  @Test
  void shouldTrierLesFenetresDUnJourParHeure() {
    PresencesEnMemoire presences = PresencesEnMemoire.avec(
      List.of(journeeDuMardiOuverteA8H(), journeeDuLundi22HAuMardi2H(), journeeDuLundiDe8HA17HAvecPauseDeMidi())
    );

    FeuilleDeTemps feuille = historiqueDeDupont(presences);

    assertThat(presenceDu(feuille, LUNDI_11_MAI_2026))
      .extracting(Plage::debut)
      .containsExactly(LE_LUNDI_11_MAI_2026_A_8H, LE_LUNDI_11_MAI_2026_A_13H, LE_LUNDI_11_MAI_2026_A_22H);
    assertThat(presenceDu(feuille, MARDI_12_MAI_2026))
      .extracting(Plage::debut)
      .containsExactly(LE_MARDI_12_MAI_2026_A_MINUIT, LE_MARDI_12_MAI_2026_A_8H);
  }

  private static FeuilleDeTemps historiqueDeDupont(PresencesEnMemoire presences) {
    return new FeuillesDeTempsService(presences, REFERENTIEL, A_PARIS).historique(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
  }

  private static List<Plage> presenceDu(FeuilleDeTemps feuille, LocalDate jour) {
    return feuille
      .jours()
      .stream()
      .filter(jourDeLaSemaine -> jourDeLaSemaine.jour().equals(jour))
      .findFirst()
      .orElseThrow()
      .presence();
  }
}
