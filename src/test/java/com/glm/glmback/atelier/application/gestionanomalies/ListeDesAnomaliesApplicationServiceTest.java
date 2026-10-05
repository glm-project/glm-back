package com.glm.glmback.atelier.application.gestionanomalies;

import static com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.gestionanomalies.AnomaliesDAtelierCriteria;
import com.glm.glmback.atelier.domain.gestionanomalies.ConflitsDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.FinAutomatiqueEnListe;
import com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesDAtelier;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class ListeDesAnomaliesApplicationServiceTest {

  private final FinsAutomatiquesDAtelier finsAutomatiques = mock(FinsAutomatiquesDAtelier.class);
  private final OperateursConnus operateurs = mock(OperateursConnus.class);
  private final PostesConnus postes = mock(PostesConnus.class);
  private final ListeDesAnomaliesApplicationService service = new ListeDesAnomaliesApplicationService(
    mock(ConflitsDAtelier.class),
    finsAutomatiques,
    operateurs,
    postes,
    () -> LE_10_MAI_2026_A_21H_UTC
  );

  @Test
  void shouldEvaluerLesFinsAutomatiquesALInstantDeLHorloge() {
    var criteria = new AnomaliesDAtelierCriteria("", "");
    var pageable = new Pageable(0, 5);
    var page = Page.<FinAutomatiqueEnListe>builder().content(List.of()).currentPage(0).pageSize(5).totalElementsCount(0);
    when(finsAutomatiques.list(criteria, LE_10_MAI_2026_A_21H_UTC, pageable)).thenReturn(page);
    when(operateurs.parIds(anySet())).thenReturn(List.of());
    when(postes.parIds(anySet())).thenReturn(List.of());

    var lecture = service.listFinsAutomatiques(criteria, pageable);

    assertThat(lecture.page()).isSameAs(page);
    verify(finsAutomatiques).list(criteria, LE_10_MAI_2026_A_21H_UTC, pageable);
  }
}
