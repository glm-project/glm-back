package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class ReferentielsDuPupitreServiceTest {

  /**
   * La date vient du port, jamais de l'horloge de la machine : c'est ce qui rend la lecture reproductible en test.
   */
  @Test
  void shouldDaterLInstantaneDeLHeureDuPort() {
    ReferentielsDuPupitreService service = new ReferentielsDuPupitreService(
      () -> List.of(OPERATEUR_DUPONT),
      () -> List.of(suiviOf42(JournalDuPupitre.vide())),
      () -> LE_10_MAI_2026_A_9H
    );

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.genereLe()).isEqualTo(LE_10_MAI_2026_A_9H);
    assertThat(referentiel.operateurs()).containsExactly(OPERATEUR_DUPONT);
    assertThat(referentiel.suivis()).containsExactly(suiviOf42(JournalDuPupitre.vide()));
  }

  @Test
  void shouldRendreUnReferentielVideQuandLEntrepriseNAEncoreRienDeclare() {
    ReferentielsDuPupitreService service = new ReferentielsDuPupitreService(List::of, List::of, () -> LE_10_MAI_2026_A_7H);

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.operateurs()).isEmpty();
    assertThat(referentiel.suivis()).isEmpty();
  }
}
