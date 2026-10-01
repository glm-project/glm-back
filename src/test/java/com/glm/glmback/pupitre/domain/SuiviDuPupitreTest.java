package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SuiviDuPupitreTest {

  @Test
  void shouldNotBuildWithoutSituation() {
    assertThatThrownBy(() -> new SuiviDuPupitre(SUIVI_ID_OF_42, NOM_OF_42, Optional.empty(), TypeDElementEngage.PRODUIT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("situation");
  }

  @Test
  void shouldBuildDepuisLaPersistance() {
    SuiviDuPupitre suivi = SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(new NomDElement("OF-2026-000042"))
      .reference("M-1187")
      .type(TypeDElementEngage.ORDRE_DE_FABRICATION)
      .situation(SITUATION_EN_ATTENTE);

    assertThat(suivi.id()).isEqualTo(SUIVI_ID_OF_42);
    assertThat(suivi.nom()).isEqualTo(NOM_OF_42);
    assertThat(suivi.reference()).contains(REFERENCE_M_1187);
    assertThat(suivi.type()).isEqualTo(TypeDElementEngage.ORDRE_DE_FABRICATION);
    assertThat(suivi.situation()).isEqualTo(SITUATION_EN_ATTENTE);
  }

  /**
   * Toutes les entreprises n'attribuent pas de reference : son absence redonne un comportement nominal, pas un cas
   * degrade.
   */
  @Test
  void shouldBuildSansReference() {
    SuiviDuPupitre suivi = SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(NOM_OF_42)
      .reference(null)
      .type(TypeDElementEngage.PRODUIT)
      .situation(SITUATION_EN_ATTENTE);

    assertThat(suivi.reference()).isEmpty();
  }

  @Test
  void shouldEtreEnAttenteQuandPersonneNYAEncoreTouche() {
    assertThat(suiviOf42(SITUATION_EN_ATTENTE).etat()).isEqualTo(EtatDuSuivi.EN_ATTENTE);
  }

  @Test
  void shouldEtreEnCoursDesQuUneActiviteEstOuverte() {
    assertThat(suiviOf42(SITUATION_EN_COURS).etat()).isEqualTo(EtatDuSuivi.EN_COURS);
  }

  /**
   * Une non conformite ne fait pas passer a INTERROMPU : l'activite reste ouverte, seule sa categorie change.
   */
  @Test
  void shouldResterEnCoursEnNonConformite() {
    SituationDuSuivi situation = SITUATION_EN_NC;

    assertThat(suiviOf42(situation).etat()).isEqualTo(EtatDuSuivi.EN_COURS);
  }

  @Test
  void shouldEtreInterrompuQuandLeTravailACesse() {
    SituationDuSuivi situation = SITUATION_INTERROMPUE;

    assertThat(suiviOf42(situation).etat()).isEqualTo(EtatDuSuivi.INTERROMPU);
  }

  @Test
  void shouldExposerLesActivitesDeSonJournal() {
    SituationDuSuivi situation = SITUATION_EN_COURS;

    assertThat(suiviOf42(situation).activitesEnCours()).containsExactly(TRAVAIL_DUPONT_A_8H);
  }
}
