package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.ConflitsFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

@UnitTest
class ListeDesAnomaliesServiceTest {

  private final ConflitsDAtelier conflits = mock(ConflitsDAtelier.class);
  private final FinsAutomatiquesDAtelier finsAutomatiques = mock(FinsAutomatiquesDAtelier.class);
  private final OperateursConnus operateurs = mock(OperateursConnus.class);
  private final PostesConnus postes = mock(PostesConnus.class);
  private final ListeDesAnomaliesService service = new ListeDesAnomaliesService(conflits, finsAutomatiques, operateurs, postes);

  @Test
  void shouldResoudreLesReferencesEnLotSansPerdreLesReferencesAbsentes() {
    var debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var ligne = ligneDuPremierConflitDe(
      suiviOF2026000042EngageA(LE_10_MAI_2026_A_8H)
        .enregistre(debut)
        .enregistre(finDe(debut).a(LE_10_MAI_2026_A_12H))
        .enregistre(finDe(debut).a(LE_10_MAI_2026_A_17H))
    );
    var page = Page.<ConflitEnListe>builder().content(List.of(ligne, ligne)).currentPage(0).pageSize(2).totalElementsCount(2);
    var criteria = new AnomaliesDAtelierCriteria("", "");
    var pageable = new Pageable(0, 2);
    when(conflits.list(criteria, pageable)).thenReturn(page);
    when(operateurs.parIds(anySet())).thenReturn(List.of(OPERATEUR_CONNU_DUPONT));
    when(postes.parIds(anySet())).thenReturn(List.of());

    var lecture = service.listConflits(criteria, pageable);

    assertThat(lecture.page()).isEqualTo(page);
    assertThat(lecture.annuaire().operateur(OPERATEUR_ID_DUPONT)).contains(OPERATEUR_CONNU_DUPONT);
    assertThat(lecture.annuaire().poste(POSTE_ID_FRAISEUSE_1)).isEmpty();
    verify(conflits).list(criteria, pageable);
    verify(operateurs).parIds(Set.of(OPERATEUR_ID_DUPONT));
    verify(postes).parIds(Set.of(POSTE_ID_FRAISEUSE_1));
    verifyNoMoreInteractions(conflits, operateurs, postes);
    verifyNoInteractions(finsAutomatiques);
  }

  @Test
  void shouldNePasTransformerUnEchecDAcquisitionEnLectureVide() {
    var echec = new IllegalStateException("acquisition interrompue");
    when(conflits.list(any(), any())).thenThrow(echec);

    assertThatThrownBy(() -> service.listConflits(new AnomaliesDAtelierCriteria("", ""), new Pageable(0, 5))).isSameAs(echec);
    verifyNoInteractions(operateurs, postes, finsAutomatiques);
  }

  @Test
  void shouldResoudreLesReferencesDesFinsAutomatiquesEnLotALInstantDEvaluation() {
    var ligne = ligneDeLaFinAutomatiqueDe(
      suiviOF2026000042EngageA(LE_10_MAI_2026_A_8H).enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H))
    );
    var page = Page.<FinAutomatiqueEnListe>builder().content(List.of(ligne, ligne)).currentPage(0).pageSize(2).totalElementsCount(2);
    var criteria = new AnomaliesDAtelierCriteria("", "");
    var pageable = new Pageable(0, 2);
    when(finsAutomatiques.list(criteria, LE_10_MAI_2026_A_21H_UTC, pageable)).thenReturn(page);
    when(operateurs.parIds(anySet())).thenReturn(List.of(OPERATEUR_CONNU_DUPONT));
    when(postes.parIds(anySet())).thenReturn(List.of());

    var lecture = service.listFinsAutomatiques(criteria, LE_10_MAI_2026_A_21H_UTC, pageable);

    assertThat(lecture.page()).isEqualTo(page);
    assertThat(lecture.annuaire().operateur(OPERATEUR_ID_DUPONT)).contains(OPERATEUR_CONNU_DUPONT);
    assertThat(lecture.annuaire().poste(POSTE_ID_FRAISEUSE_1)).isEmpty();
    verify(finsAutomatiques).list(criteria, LE_10_MAI_2026_A_21H_UTC, pageable);
    verify(operateurs).parIds(Set.of(OPERATEUR_ID_DUPONT));
    verify(postes).parIds(Set.of(POSTE_ID_FRAISEUSE_1));
    verifyNoMoreInteractions(finsAutomatiques, operateurs, postes);
    verifyNoInteractions(conflits);
  }

  @Test
  void shouldNePasTransformerUnEchecDAcquisitionDesFinsAutomatiquesEnLectureVide() {
    var echec = new IllegalStateException("acquisition interrompue");
    when(finsAutomatiques.list(any(), any(), any())).thenThrow(echec);

    assertThatThrownBy(() ->
      service.listFinsAutomatiques(new AnomaliesDAtelierCriteria("", ""), LE_10_MAI_2026_A_21H_UTC, new Pageable(0, 5))
    ).isSameAs(echec);
    verifyNoInteractions(operateurs, postes, conflits);
  }
}
