package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import java.util.Map;
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
      .presences(() -> new PresencesDesOperateurs(Map.of()))
      .clock(() -> LE_10_MAI_2026_A_9H);

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.genereLe()).isEqualTo(LE_10_MAI_2026_A_9H);
    assertThat(referentiel.operateurs()).containsExactly(OPERATEUR_DUPONT);
    assertThat(referentiel.suivis()).containsExactly(suiviOf42(JournalDuPupitre.vide()));
  }

  /**
   * Les presences sont relevees une fois et remises a la lecture des operateurs : c'est ce qui fait entrer l'etat au
   * referentiel sans une requete de presence par operateur.
   */
  @Test
  void shouldRemettreLesPresencesReleveesALaLectureDesOperateurs() {
    ReferentielsDuPupitreService service = ReferentielsDuPupitreService.builder()
      .operateurs(presences -> List.of(operateurDupont(presences.de(OPERATEUR_ID_DUPONT))))
      .suivis(List::of)
      .presences(() -> new PresencesDesOperateurs(Map.of(OPERATEUR_ID_DUPONT, EtatDePresence.EN_PAUSE)))
      .clock(() -> LE_10_MAI_2026_A_9H);

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.operateurs()).extracting(OperateurDuPupitre::etat).containsExactly(EtatDePresence.EN_PAUSE);
  }

  @Test
  void shouldRendreUnReferentielVideQuandLEntrepriseNAEncoreRienDeclare() {
    ReferentielsDuPupitreService service = ReferentielsDuPupitreService.builder()
      .operateurs(presences -> List.of())
      .suivis(List::of)
      .presences(() -> new PresencesDesOperateurs(Map.of()))
      .clock(() -> LE_10_MAI_2026_A_7H);

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.operateurs()).isEmpty();
    assertThat(referentiel.suivis()).isEmpty();
  }
}
