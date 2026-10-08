package com.glm.glmback.pupitre.infrastructure.primary;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.pupitre.domain.ActiviteSansFin;
import com.glm.glmback.pupitre.domain.CategorieDActivite;
import com.glm.glmback.pupitre.domain.EtatDuSuivi;
import com.glm.glmback.pupitre.domain.OperateurDuPupitre;
import com.glm.glmback.pupitre.domain.ReferentielDuPupitre;
import com.glm.glmback.pupitre.domain.SuiviDuPupitre;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class RestReferentielDuPupitreTest {

  @Test
  void shouldExposerLaDateDeLInstantaneEtSesDeuxCollections() {
    RestReferentielDuPupitre referentiel = RestReferentielDuPupitre.from(
      new ReferentielDuPupitre(LE_10_MAI_2026_A_8H, List.of(OPERATEUR_DUPONT), List.of(suiviOf42Vierge()))
    );

    assertThat(referentiel.genereLe()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(referentiel.operateurs()).hasSize(1);
    assertThat(referentiel.suivis()).hasSize(1);
  }

  @Test
  void shouldExposerLOperateurEtSesPostes() {
    RestOperateurDuPupitre operateur = RestOperateurDuPupitre.from(OPERATEUR_DUPONT);

    assertThat(operateur.id()).isEqualTo(OPERATEUR_ID_DUPONT.uuid());
    assertThat(operateur.nom()).isEqualTo("Dupont");
    assertThat(operateur.prenom()).isEqualTo("Jean");
    assertThat(operateur.identifiant()).isEqualTo("049");
    assertThat(operateur.postes()).containsExactly(
      new RestPosteDuPupitre(POSTE_ID_FRAISEUSE_1.uuid(), "Fraiseuse 1"),
      new RestPosteDuPupitre(POSTE_ID_FRAISEUSE_2.uuid(), "Fraiseuse 2")
    );
  }

  @Test
  void shouldTaireLeIdentifiantDUnOperateurQuiNEnAPas() {
    OperateurDuPupitre sansIdentifiant = OperateurDuPupitre.builder()
      .id(OPERATEUR_ID_MARTIN)
      .nom(NOM_DUPONT)
      .prenom(PRENOM_JEAN)
      .identifiant(null)
      .postes(List.of());

    assertThat(RestOperateurDuPupitre.from(sansIdentifiant).identifiant()).isNull();
  }

  @Test
  void shouldExposerLaTuileEtSonEtat() {
    RestSuiviDuPupitre suivi = RestSuiviDuPupitre.from(
      suiviOf42Pointe(List.of(travailDeDupontSurFraiseuse1Depuis8H())),
      LE_10_MAI_2026_A_9H
    );

    assertThat(suivi.id()).isEqualTo(SUIVI_ID_OF_42.uuid());
    assertThat(suivi.nom()).isEqualTo("OF-2026-000042");
    assertThat(suivi.reference()).isEqualTo("M-1187");
    assertThat(suivi.categorie()).isEqualTo("OF");
    assertThat(suivi.type()).isEqualTo("ORDRE_DE_FABRICATION");
    assertThat(suivi.etat()).isEqualTo(EtatDuSuivi.EN_COURS);
    assertThat(suivi.activites()).containsExactly(
      new RestActiviteDuPupitre(
        OPERATEUR_ID_DUPONT.uuid(),
        POSTE_ID_FRAISEUSE_1.uuid(),
        CategorieDActivite.TRAVAIL,
        LE_10_MAI_2026_A_8H,
        ACTIVITE_ID_88888888.uuid(),
        LE_10_MAI_2026_A_21H
      )
    );
  }

  @Test
  void shouldExposerLesIdentitesDuConflitSeparementDesActivitesCourantes() {
    RestConflitDuPupitre conflit = RestConflitDuPupitre.from(SEQUENCE_DUPONT_SUR_FRAISEUSE_1);

    assertThat(conflit.operateur()).isEqualTo(OPERATEUR_ID_DUPONT.uuid());
    assertThat(conflit.poste()).isEqualTo(POSTE_ID_FRAISEUSE_1.uuid());
    assertThat(conflit.activites()).containsExactly(ACTIVITE_ID_88888888.uuid());
    assertThat(conflit.pointages()).containsExactly(POINTAGE_ID_99999999.uuid());
  }

  @Test
  void shouldTaireLaReferenceDUnElementQuiNEnAPas() {
    SuiviDuPupitre sansReference = SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(NOM_OF_42)
      .reference(null)
      .categorie(CATEGORIE_MOULE.value())
      .activites(List.of())
      .conflits(List.of())
      .dejaPointe(false);

    assertThat(RestSuiviDuPupitre.from(sansReference, LE_10_MAI_2026_A_9H).reference()).isNull();
  }

  @Test
  void shouldTaireLePosteDUneActiviteQuiNEnAPas() {
    ActiviteSansFin sansPoste = ActiviteSansFin.builder()
      .ouverture(ACTIVITE_ID_88888888)
      .activite(ACTIVITE_DUPONT_SANS_POSTE)
      .categorie(CategorieDActivite.TRAVAIL)
      .depuis(LE_10_MAI_2026_A_8H)
      .echeance(LE_10_MAI_2026_A_21H);

    assertThat(RestActiviteDuPupitre.from(sansPoste).poste()).isNull();
  }
}
