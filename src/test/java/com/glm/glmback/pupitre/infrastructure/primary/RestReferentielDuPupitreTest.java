package com.glm.glmback.pupitre.infrastructure.primary;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.pupitre.domain.ActiviteEnCours;
import com.glm.glmback.pupitre.domain.CategorieDActivite;
import com.glm.glmback.pupitre.domain.EtatDePresence;
import com.glm.glmback.pupitre.domain.EtatDuSuivi;
import com.glm.glmback.pupitre.domain.JournalDuPupitre;
import com.glm.glmback.pupitre.domain.OperateurDuPupitre;
import com.glm.glmback.pupitre.domain.ReferentielDuPupitre;
import com.glm.glmback.pupitre.domain.SuiviDuPupitre;
import com.glm.glmback.pupitre.domain.TypeDElementEngage;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class RestReferentielDuPupitreTest {

  @Test
  void shouldExposerLaDateDeLInstantaneEtSesDeuxCollections() {
    RestReferentielDuPupitre referentiel = RestReferentielDuPupitre.from(
      new ReferentielDuPupitre(LE_10_MAI_2026_A_8H, List.of(OPERATEUR_DUPONT), List.of(suiviOf42(JournalDuPupitre.vide())))
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
    assertThat(operateur.matricule()).isEqualTo("049");
    assertThat(operateur.etat()).isEqualTo(EtatDePresence.PRESENT);
    assertThat(operateur.presentJusqua()).isEqualTo(LE_10_MAI_2026_A_20H);
    assertThat(operateur.postes()).containsExactly(
      new RestPosteDuPupitre(POSTE_ID_FRAISEUSE_1.uuid(), "Fraiseuse 1"),
      new RestPosteDuPupitre(POSTE_ID_FRAISEUSE_2.uuid(), "Fraiseuse 2")
    );
  }

  @Test
  void shouldTaireLeMatriculeDUnOperateurQuiNEnAPas() {
    OperateurDuPupitre sansMatricule = OperateurDuPupitre.builder()
      .id(OPERATEUR_ID_MARTIN)
      .nom(NOM_DUPONT)
      .prenom(PRENOM_JEAN)
      .matricule(null)
      .etat(EtatDePresence.ABSENT)
      .presentJusqua(Optional.empty())
      .postes(List.of());

    assertThat(RestOperateurDuPupitre.from(sansMatricule).matricule()).isNull();
    assertThat(RestOperateurDuPupitre.from(sansMatricule).presentJusqua()).isNull();
  }

  @Test
  void shouldExposerLaTuileEtSonEtat() {
    RestSuiviDuPupitre suivi = RestSuiviDuPupitre.from(suiviOf42(new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H)))));

    assertThat(suivi.id()).isEqualTo(SUIVI_ID_OF_42.uuid());
    assertThat(suivi.nom()).isEqualTo("OF-2026-000042");
    assertThat(suivi.reference()).isEqualTo("M-1187");
    assertThat(suivi.type()).isEqualTo(TypeDElementEngage.ORDRE_DE_FABRICATION);
    assertThat(suivi.etat()).isEqualTo(EtatDuSuivi.EN_COURS);
    assertThat(suivi.activites()).containsExactly(
      new RestActiviteDuPupitre(OPERATEUR_ID_DUPONT.uuid(), POSTE_ID_FRAISEUSE_1.uuid(), CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H)
    );
  }

  @Test
  void shouldTaireLaReferenceDUnElementQuiNEnAPas() {
    SuiviDuPupitre sansReference = SuiviDuPupitre.builder()
      .id(SUIVI_ID_OF_42)
      .nom(NOM_OF_42)
      .reference(null)
      .type(TypeDElementEngage.PRODUIT)
      .journal(JournalDuPupitre.vide());

    assertThat(RestSuiviDuPupitre.from(sansReference).reference()).isNull();
  }

  @Test
  void shouldTaireLePosteDUneActiviteQuiNEnAPas() {
    ActiviteEnCours sansPoste = new ActiviteEnCours(ACTIVITE_DUPONT_SANS_POSTE, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H);

    assertThat(RestActiviteDuPupitre.from(sansPoste).poste()).isNull();
  }
}
