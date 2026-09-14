package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class SuiviDuPupitreTest {

  @Test
  void shouldNotBuildWithoutJournal() {
    assertThatThrownBy(() -> new SuiviDuPupitre(SUIVI_ID_OF_42, NOM_OF_42, Optional.empty(), TypeDElementEngage.PRODUIT, null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldBuildDepuisLaPersistance() {
    SuiviDuPupitre suivi = SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(new NomDElement("OF-2026-000042"))
      .reference("M-1187")
      .type(TypeDElementEngage.ORDRE_DE_FABRICATION)
      .journal(JournalDuPupitre.vide());

    assertThat(suivi.id()).isEqualTo(SUIVI_ID_OF_42);
    assertThat(suivi.nom()).isEqualTo(NOM_OF_42);
    assertThat(suivi.reference()).contains(REFERENCE_M_1187);
    assertThat(suivi.type()).isEqualTo(TypeDElementEngage.ORDRE_DE_FABRICATION);
    assertThat(suivi.journal()).isEqualTo(JournalDuPupitre.vide());
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
      .journal(JournalDuPupitre.vide());

    assertThat(suivi.reference()).isEmpty();
  }

  @Test
  void shouldEtreEnAttenteQuandPersonneNYAEncoreTouche() {
    assertThat(suiviOf42(JournalDuPupitre.vide()).etat()).isEqualTo(EtatDuSuivi.EN_ATTENTE);
  }

  @Test
  void shouldEtreEnCoursDesQuUneActiviteEstOuverte() {
    assertThat(suiviOf42(new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H)))).etat()).isEqualTo(EtatDuSuivi.EN_COURS);
  }

  /**
   * Une non conformite ne fait pas passer a INTERROMPU : l'activite reste ouverte, seule sa categorie change.
   */
  @Test
  void shouldResterEnCoursEnNonConformite() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H), nonConformite(LE_10_MAI_2026_A_9H)));

    assertThat(suiviOf42(journal).etat()).isEqualTo(EtatDuSuivi.EN_COURS);
  }

  @Test
  void shouldEtreInterrompuQuandLeTravailACesse() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H), fin(LE_10_MAI_2026_A_9H)));

    assertThat(suiviOf42(journal).etat()).isEqualTo(EtatDuSuivi.INTERROMPU);
  }

  @Test
  void shouldExposerLesActivitesDeSonJournal() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H)));

    assertThat(suiviOf42(journal).activitesEnCours()).containsExactly(
      new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H)
    );
  }
}
