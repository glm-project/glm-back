package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class SuiviDuPupitreTest {

  @Test
  void shouldBuildDepuisLaPersistance() {
    SuiviDuPupitre suivi = SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(new NomDElement("OF-2026-000042"))
      .reference("M-1187")
      .type(TypeDElementEngage.ORDRE_DE_FABRICATION)
      .activites(List.of())
      .conflits(List.of())
      .dejaPointe(false);
    assertThat(suivi.id()).isEqualTo(SUIVI_ID_OF_42);
    assertThat(suivi.nom()).isEqualTo(NOM_OF_42);
    assertThat(suivi.reference()).contains(REFERENCE_M_1187);
    assertThat(suivi.type()).isEqualTo(TypeDElementEngage.ORDRE_DE_FABRICATION);
    assertThat(suivi.activites()).isEmpty();
    assertThat(suivi.dejaPointe()).isFalse();
  }

  @Test
  void shouldRefuserLesValeursObligatoiresManquantes() {
    assertThatThrownBy(() ->
      SuiviDuPupitre.builder()
        .id(null)
        .nom(NOM_OF_42)
        .reference(null)
        .type(TypeDElementEngage.PRODUIT)
        .activites(List.of())
        .conflits(List.of())
        .dejaPointe(false)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      SuiviDuPupitre.builder()
        .id(SUIVI_ID_OF_42)
        .nom(null)
        .reference(null)
        .type(TypeDElementEngage.PRODUIT)
        .activites(List.of())
        .conflits(List.of())
        .dejaPointe(false)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      SuiviDuPupitre.builder()
        .id(SUIVI_ID_OF_42)
        .nom(NOM_OF_42)
        .reference(null)
        .type(null)
        .activites(List.of())
        .conflits(List.of())
        .dejaPointe(false)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() -> suiviOf42Pointe(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("activites");
    assertThatThrownBy(() -> suiviOf42Pointe(Arrays.asList(travailDeDupontSurFraiseuse1Depuis8H(), null))).isExactlyInstanceOf(
      NullElementInCollectionException.class
    );
  }

  @Test
  void shouldBuildSansReference() {
    SuiviDuPupitre suivi = SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(NOM_OF_42)
      .reference(null)
      .type(TypeDElementEngage.PRODUIT)
      .activites(List.of())
      .conflits(List.of())
      .dejaPointe(false);
    assertThat(suivi.reference()).isEmpty();
  }

  @Test
  void shouldCopierSesActivites() {
    List<ActiviteSansFin> activites = new ArrayList<>(List.of(travailDeDupontSurFraiseuse1Depuis8H()));
    SuiviDuPupitre suivi = suiviOf42Pointe(activites);
    activites.clear();
    assertThat(suivi.activites()).containsExactly(travailDeDupontSurFraiseuse1Depuis8H());
    assertThatThrownBy(() -> suivi.activites().clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldGarderLesConflitsSeparesDesActivitesCourantes() {
    List<SequenceEnConflitDuPupitre> conflits = new ArrayList<>(List.of(SEQUENCE_DUPONT_SUR_FRAISEUSE_1));
    SuiviDuPupitre suivi = SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(NOM_OF_42)
      .reference(null)
      .type(TypeDElementEngage.PRODUIT)
      .activites(List.of())
      .conflits(conflits)
      .dejaPointe(true);
    conflits.clear();
    assertThat(suivi.conflits()).containsExactly(SEQUENCE_DUPONT_SUR_FRAISEUSE_1);
    assertThat(suivi.activitesEnCoursA(LE_10_MAI_2026_A_9H)).isEmpty();
    assertThat(suivi.etatA(LE_10_MAI_2026_A_9H)).isEqualTo(EtatDuSuivi.INTERROMPU);
    assertThatThrownBy(() -> suivi.conflits().clear()).isExactlyInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void shouldRefuserLesConflitsManquantsOuLeursTrous() {
    assertThatThrownBy(() ->
      SuiviDuPupitre.builder()
        .id(SUIVI_ID_OF_42)
        .nom(NOM_OF_42)
        .reference(null)
        .type(TypeDElementEngage.PRODUIT)
        .activites(List.of())
        .conflits(null)
        .dejaPointe(false)
    ).isExactlyInstanceOf(MissingMandatoryValueException.class);
    assertThatThrownBy(() ->
      SuiviDuPupitre.builder()
        .id(SUIVI_ID_OF_42)
        .nom(NOM_OF_42)
        .reference(null)
        .type(TypeDElementEngage.PRODUIT)
        .activites(List.of())
        .conflits(Arrays.asList(SEQUENCE_DUPONT_SUR_FRAISEUSE_1, null))
        .dejaPointe(false)
    ).isExactlyInstanceOf(NullElementInCollectionException.class);
  }

  @Test
  void shouldEtreEnAttenteSansPointageActif() {
    assertThat(suiviOf42Vierge().etatA(LE_10_MAI_2026_A_9H)).isEqualTo(EtatDuSuivi.EN_ATTENTE);
  }

  @Test
  void shouldEtreEnCoursAvantLEcheance() {
    SuiviDuPupitre suivi = suiviOf42Pointe(List.of(travailDeDupontSurFraiseuse1Depuis8H()));
    assertThat(suivi.etatA(Instant.parse("2026-05-10T20:59:59Z"))).isEqualTo(EtatDuSuivi.EN_COURS);
    assertThat(suivi.activitesEnCoursA(LE_10_MAI_2026_A_9H)).containsExactly(travailDeDupontSurFraiseuse1Depuis8H());
  }

  @Test
  void shouldEtreInterrompuALecheancePileEtApres() {
    SuiviDuPupitre suivi = suiviOf42Pointe(List.of(travailDeDupontSurFraiseuse1Depuis8H()));
    assertThat(suivi.etatA(LE_10_MAI_2026_A_21H)).isEqualTo(EtatDuSuivi.INTERROMPU);
    assertThat(suivi.activitesEnCoursA(LE_10_MAI_2026_A_21H)).isEmpty();
    assertThat(suivi.activitesEnCoursA(Instant.parse("2026-05-11T09:00:00Z"))).isEmpty();
  }

  @Test
  void shouldEtreInterrompuAvecPointageActifSansActiviteInterpretable() {
    assertThat(suiviOf42Pointe(List.of()).etatA(LE_10_MAI_2026_A_9H)).isEqualTo(EtatDuSuivi.INTERROMPU);
  }

  @Test
  void shouldResterEnCoursEnNonConformite() {
    ActiviteSansFin nonConformite = ActiviteSansFin.builder()
      .ouverture(ACTIVITE_ID_88888888)
      .activite(ACTIVITE_DUPONT_SUR_FRAISEUSE_1)
      .categorie(CategorieDActivite.NON_CONFORMITE)
      .depuis(LE_10_MAI_2026_A_8H)
      .echeance(LE_10_MAI_2026_A_21H);
    assertThat(suiviOf42Pointe(List.of(nonConformite)).etatA(LE_10_MAI_2026_A_9H)).isEqualTo(EtatDuSuivi.EN_COURS);
  }
}
