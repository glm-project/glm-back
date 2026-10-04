package com.glm.glmback.atelier.infrastructure.secondary.gestionconflits;

import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.gestionconflits.ConfirmerLesActes;
import com.glm.glmback.atelier.application.gestionconflits.EmpreintesDesConsequences;
import com.glm.glmback.atelier.application.gestionconflits.PreparationDesActes;
import com.glm.glmback.atelier.application.gestionconflits.RecusDActes;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.time.domain.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@UnitTest
class CompositionDesApercusTest {

  @Test
  void shouldComposerLeParcoursSansCleDApercu() {
    new ApplicationContextRunner()
      .withUserConfiguration(CompositionDesApercus.class)
      .withBean(SuiviDAtelierRepository.class, () -> mock(SuiviDAtelierRepository.class))
      .withBean(ElementsEngageables.class, () -> mock(ElementsEngageables.class))
      .withBean(OperateursConnus.class, () -> mock(OperateursConnus.class))
      .withBean(PostesConnus.class, () -> mock(PostesConnus.class))
      .withBean(Habilitations.class, () -> mock(Habilitations.class))
      .withBean(RecusDActes.class, () -> mock(RecusDActes.class))
      .withBean(IdentitesDEvenements.class, () -> mock(IdentitesDEvenements.class))
      .withBean(Clock.class, () -> () -> LE_10_MAI_2026_A_17H)
      .run(context -> {
        assertThat(context)
          .hasSingleBean(EmpreintesDesConsequences.class)
          .hasSingleBean(PreparationDesActes.class)
          .hasSingleBean(ConfirmerLesActes.class);
        var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
        assertThat(context.getBean(EmpreintesDesConsequences.class).calcule(suivi, LE_10_MAI_2026_A_8H)).hasSize(64);
      });
  }
}
