package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointagesIgnores;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@UnitTest
class RejeuSurSuiviClotureTest {

  @Test
  void shouldReplayClosedFollowUpsWithoutWritingAgain() {
    // GIVEN
    SuiviDAtelierRepository suivis = Mockito.mock(SuiviDAtelierRepository.class);
    SuiviDAtelier suivi = suiviDAtelierEngage().cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H));
    given(suivis.getForUpdate(suivi.id())).willReturn(Optional.of(suivi));
    given(suivis.contientEvenement(any())).willReturn(true);
    SuivisDAtelierApplicationService atelier = serviceDAtelier(suivis);
    // WHEN
    ResultatDEcriture<?> rejeu = atelier.pointeDuPupitre(pointage(suivi));
    // THEN
    assertThat(rejeu.rejeu()).isTrue();
    then(suivis).should(never()).update(any());
  }

  private static PointageAEnregistrer pointage(SuiviDAtelier suivi) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(suivi.id())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .dateDeSurvenue(Optional.empty())
      .evenement(new EvenementDAtelierId(UUID.randomUUID()));
  }

  private static SuivisDAtelierApplicationService serviceDAtelier(SuiviDAtelierRepository suivis) {
    return new SuivisDAtelierApplicationService(
      suivis,
      Mockito.mock(ElementsEngageables.class),
      Mockito.mock(OperateursConnus.class),
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Habilitations.class),
      Mockito.mock(PointagesIgnores.class),
      () -> DUREE_MAXIMALE_TREIZE_HEURES,
      () -> LE_10_MAI_2026_A_17H,
      new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class))
    );
  }
}
