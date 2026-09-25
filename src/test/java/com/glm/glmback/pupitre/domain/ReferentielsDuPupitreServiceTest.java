package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class ReferentielsDuPupitreServiceTest {

  /**
   * La date vient du port, jamais de l'horloge de la machine : c'est ce qui rend la lecture reproductible en test.
   */
  @Test
  void shouldDaterLInstantaneDeLHeureDuPort() {
    ReferentielsDuPupitreService service = ReferentielsDuPupitreService.builder()
      .operateurs(presences -> List.of(OPERATEUR_DUPONT))
      .suivis(() -> List.of(suiviOf42(JournalDuPupitre.vide())))
      .presences(() -> new JourneesEnCours(Map.of()))
      .seuil(() -> AMPLITUDE_MAXIMALE_13H)
      .clock(() -> LE_10_MAI_2026_A_9H);

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.genereLe()).isEqualTo(LE_10_MAI_2026_A_9H);
    assertThat(referentiel.operateurs()).containsExactly(OPERATEUR_DUPONT);
    assertThat(referentiel.suivis()).containsExactly(suiviOf42(JournalDuPupitre.vide()));
  }

  @Test
  void shouldRemettreLesPresencesReleveesALaLectureDesOperateurs() {
    ReferentielDuPupitre referentiel = referentielA(LE_10_MAI_2026_A_9H, new JourneeEnCours(EtatDePresence.EN_PAUSE, LE_10_MAI_2026_A_7H));

    assertThat(referentiel.operateurs()).extracting(OperateurDuPupitre::etat).containsExactly(EtatDePresence.EN_PAUSE);
    assertThat(referentiel.operateurs()).extracting(OperateurDuPupitre::presentJusqua).containsExactly(Optional.of(LE_10_MAI_2026_A_20H));
  }

  /**
   * E2 : lundi 21:00, la journee ouverte a 07:00 est abandonnee depuis 20:00. Le pupitre montre Dupont absent et ne
   * lui propose que l'arrivee.
   */
  @Test
  void shouldMontrerAbsentUnOperateurDontLaJourneeEstAbandonnee() {
    ReferentielDuPupitre referentiel = referentielA(LE_10_MAI_2026_A_21H, new JourneeEnCours(EtatDePresence.PRESENT, LE_10_MAI_2026_A_7H));

    assertThat(referentiel.operateurs()).extracting(OperateurDuPupitre::etat).containsExactly(EtatDePresence.ABSENT);
    assertThat(referentiel.operateurs()).extracting(OperateurDuPupitre::presentJusqua).containsExactly(Optional.empty());
  }

  @Test
  void shouldMontrerPresentJusquAuSeuilPile() {
    ReferentielDuPupitre referentiel = referentielA(LE_10_MAI_2026_A_20H, new JourneeEnCours(EtatDePresence.PRESENT, LE_10_MAI_2026_A_7H));

    assertThat(referentiel.operateurs()).extracting(OperateurDuPupitre::etat).containsExactly(EtatDePresence.PRESENT);
  }

  @Test
  void shouldAppliquerLeSeuilDuPort() {
    ReferentielsDuPupitreService service = ReferentielsDuPupitreService.builder()
      .operateurs(presences -> List.of(operateurDupont(presences.de(OPERATEUR_ID_DUPONT))))
      .suivis(List::of)
      .presences(() -> new JourneesEnCours(Map.of(OPERATEUR_ID_DUPONT, new JourneeEnCours(EtatDePresence.PRESENT, LE_10_MAI_2026_A_7H))))
      .seuil(() -> new AmplitudeMaximale(Duration.ofHours(2)))
      .clock(() -> LE_10_MAI_2026_A_12H);

    assertThat(service.referentiel().operateurs()).extracting(OperateurDuPupitre::etat).containsExactly(EtatDePresence.ABSENT);
  }

  private static ReferentielDuPupitre referentielA(Instant maintenant, JourneeEnCours journee) {
    return ReferentielsDuPupitreService.builder()
      .operateurs(presences -> List.of(operateurDupont(presences.de(OPERATEUR_ID_DUPONT))))
      .suivis(List::of)
      .presences(() -> new JourneesEnCours(Map.of(OPERATEUR_ID_DUPONT, journee)))
      .seuil(() -> AMPLITUDE_MAXIMALE_13H)
      .clock(() -> maintenant)
      .referentiel();
  }

  @Test
  void shouldRendreUnReferentielVideQuandLEntrepriseNAEncoreRienDeclare() {
    ReferentielsDuPupitreService service = ReferentielsDuPupitreService.builder()
      .operateurs(presences -> List.of())
      .suivis(List::of)
      .presences(() -> new JourneesEnCours(Map.of()))
      .seuil(() -> AMPLITUDE_MAXIMALE_13H)
      .clock(() -> LE_10_MAI_2026_A_7H);

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.operateurs()).isEmpty();
    assertThat(referentiel.suivis()).isEmpty();
  }
}
