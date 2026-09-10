package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SyntheseDesHeuresServiceTest {

  private static final OperateursConnus REFERENTIEL = id ->
    Optional.of(OPERATEUR_CONNU_DUPONT).filter(operateur -> operateur.id().equals(id));
  private static final FuseauHoraireDeLEntreprise A_PARIS = () -> ZONE_PARIS;

  @Test
  void shouldNotLireLaSyntheseDUnOperateurInconnu() {
    SynthesesDesHeuresService service = new SynthesesDesHeuresService(PresencesEnMemoire.sansJournee(), REFERENTIEL, A_PARIS);

    assertThatThrownBy(() -> service.synthese(OPERATEUR_ID_MARTIN, SEMAINE_20_DE_2026))
      .isExactlyInstanceOf(OperateurInconnuException.class)
      .hasMessageContaining(OPERATEUR_ID_MARTIN.uuid().toString());
  }

  @Test
  void shouldPorterLIdentiteRelueEtLaSemaineDemandee() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.sansJournee());

    assertThat(synthese.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(synthese.semaine()).isEqualTo(SEMAINE_20_DE_2026);
  }

  /**
   * Sept jours toujours, meme vides : un trou dans la liste obligerait le lecteur a deviner s'il manque une journee
   * ou si l'operateur n'etait pas la.
   */
  @Test
  void shouldRendreLesSeptJoursDeLaSemaineSansPresence() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.sansJournee());

    assertThat(synthese.jours())
      .extracting(JourDeSynthese::jour)
      .containsExactly(
        LocalDate.of(2026, 5, 11),
        LocalDate.of(2026, 5, 12),
        LocalDate.of(2026, 5, 13),
        LocalDate.of(2026, 5, 14),
        LocalDate.of(2026, 5, 15),
        LocalDate.of(2026, 5, 16),
        LocalDate.of(2026, 5, 17)
      );
    assertThat(synthese.jours()).allSatisfy(jour -> {
      assertThat(jour.pointages()).isEmpty();
      assertThat(jour.duree()).isZero();
    });
  }

  @Test
  void shouldDemanderLesJourneesRecouvrantLaSemaineDansLaZoneDeLEntreprise() {
    PresencesEnMemoire presences = PresencesEnMemoire.sansJournee();

    syntheseDeDupont(presences);

    assertThat(presences.debutDemande()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
    assertThat(presences.finExclusiveDemandee()).isEqualTo(Instant.parse("2026-05-17T22:00:00Z"));
  }

  @Test
  void shouldIgnorerUneJourneeHorsDeLaSemaine() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuDimanchePrecedentDe8HA17H())));

    assertThat(synthese.jours()).allSatisfy(jour -> {
      assertThat(jour.pointages()).isEmpty();
      assertThat(jour.duree()).isZero();
    });
  }

  @Test
  void shouldPorterLesPointagesDuJourTriesParHeure() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages())
      .extracting(pointage -> pointage.evenement().dateDeSurvenue())
      .containsExactly(LE_LUNDI_11_MAI_2026_A_8H, LE_LUNDI_11_MAI_2026_A_12H, LE_LUNDI_11_MAI_2026_A_13H, LE_LUNDI_11_MAI_2026_A_17H);
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages()).allSatisfy(pointage -> assertThat(pointage.valide()).isTrue());
  }

  @Test
  void shouldSommerLesFenetresFermeesDeLaPauseDeMidiPourLaDureeDuJour() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isEqualTo(Duration.ofHours(8));
  }

  /**
   * Une equipe de nuit compte sur deux jours : la duree se repartit exactement comme les pointages, de part et
   * d'autre de minuit.
   */
  @Test
  void shouldRepartirLaDureeDUneJourneeAChevalSurMinuitSurSesDeuxJours() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundi22HAuMardi2H())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isEqualTo(Duration.ofHours(2));
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).duree()).isEqualTo(Duration.ofHours(2));
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages())
      .extracting(pointage -> pointage.evenement().dateDeSurvenue())
      .containsExactly(LE_LUNDI_11_MAI_2026_A_22H);
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).pointages())
      .extracting(pointage -> pointage.evenement().dateDeSurvenue())
      .containsExactly(LE_MARDI_12_MAI_2026_A_2H);
  }

  /**
   * Sans depart pointe, aucune horloge dans ce contexte ne peut dire combien de temps s'est ecoule : la duree du
   * jour reste nulle, meme si l'arrivee est bien visible dans le releve.
   */
  @Test
  void shouldNotCompterDeDureePourUneFenetreEncoreOuverte() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuMardiOuverteA8H())));

    assertThat(jourDe(synthese, MARDI_12_MAI_2026).duree()).isZero();
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).pointages())
      .extracting(pointage -> pointage.evenement().dateDeSurvenue())
      .containsExactly(LE_MARDI_12_MAI_2026_A_8H);
  }

  /**
   * Un pointage fautif ne bloque jamais la generation du releve : il reste visible, marque invalide, et le jour
   * signale l'anomalie sans que la duree n'en tienne compte.
   */
  @Test
  void shouldSignalerUneAnomalieSansCompterLePointageFautifDansLaDuree() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuMercrediAvecPauseSansArrivee())));

    JourDeSynthese mercredi = jourDe(synthese, MERCREDI_13_MAI_2026);
    assertThat(mercredi.aUneAnomalie()).isTrue();
    assertThat(mercredi.duree()).isZero();
    assertThat(mercredi.pointages()).extracting(Pointage::valide).containsExactly(false);
  }

  @Test
  void shouldNotSignalerDAnomalieSurUnJourSansPointageFautif() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).aUneAnomalie()).isFalse();
  }

  @Test
  void shouldSommerLaDureeDesSeptJoursPourLaDureeTotale() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi(), journeeDuMardiOuverteA8H()))
    );

    assertThat(synthese.dureeTotale()).isEqualTo(Duration.ofHours(8));
  }

  private static SyntheseDesHeures syntheseDeDupont(PresencesEnMemoire presences) {
    return new SynthesesDesHeuresService(presences, REFERENTIEL, A_PARIS).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
  }

  private static JourDeSynthese jourDe(SyntheseDesHeures synthese, LocalDate jour) {
    return synthese.jours().stream().filter(jourDeSynthese -> jourDeSynthese.jour().equals(jour)).findFirst().orElseThrow();
  }
}
