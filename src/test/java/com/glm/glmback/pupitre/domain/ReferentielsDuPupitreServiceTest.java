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
    ReferentielsDuPupitreService service = ReferentielsDuPupitreService.builder()
      .operateurs(() -> List.of(OPERATEUR_DUPONT))
      .suivis(() -> List.of(suiviOf42Vierge()))
      .categories(() -> List.of(CATEGORIE_OF, CATEGORIE_MOULE))
      .dureeMaximaleDActivite(() -> DUREE_MAXIMALE_TREIZE_HEURES)
      .clock(() -> LE_10_MAI_2026_A_9H);

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.genereLe()).isEqualTo(LE_10_MAI_2026_A_9H);
    assertThat(referentiel.operateurs()).containsExactly(OPERATEUR_DUPONT);
    assertThat(referentiel.suivis()).containsExactly(suiviOf42Vierge());
    assertThat(referentiel.categories()).containsExactly(CATEGORIE_OF, CATEGORIE_MOULE);
  }

  /**
   * La duree maximale d'une activite vient du port du noyau partage, que le parametrage implemente : le pupitre n'a pas
   * sa propre valeur, et il suit celle que le gestionnaire a fixee.
   */
  @Test
  void shouldDonnerLaDureeMaximaleQueLePortRend() {
    ReferentielsDuPupitreService service = ReferentielsDuPupitreService.builder()
      .operateurs(List::of)
      .suivis(List::of)
      .categories(List::of)
      .dureeMaximaleDActivite(() -> DUREE_MAXIMALE_HUIT_HEURES)
      .clock(() -> LE_10_MAI_2026_A_7H);

    assertThat(service.referentiel().dureeMaximaleDActivite()).isEqualTo(DUREE_MAXIMALE_HUIT_HEURES);
  }

  @Test
  void shouldRendreUnReferentielVideQuandLEntrepriseNAEncoreRienDeclare() {
    ReferentielsDuPupitreService service = ReferentielsDuPupitreService.builder()
      .operateurs(List::of)
      .suivis(List::of)
      .categories(List::of)
      .dureeMaximaleDActivite(() -> DUREE_MAXIMALE_TREIZE_HEURES)
      .clock(() -> LE_10_MAI_2026_A_7H);

    ReferentielDuPupitre referentiel = service.referentiel();

    assertThat(referentiel.operateurs()).isEmpty();
    assertThat(referentiel.suivis()).isEmpty();
    assertThat(referentiel.categories()).isEmpty();
  }
}
