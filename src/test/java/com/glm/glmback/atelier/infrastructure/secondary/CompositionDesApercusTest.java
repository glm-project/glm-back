package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.application.ConfirmerLesActes;
import com.glm.glmback.atelier.application.EmpreintesDesConsequences;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.PreparationDesActes;
import com.glm.glmback.atelier.application.RecusDActes;
import com.glm.glmback.atelier.application.ReferencesDApercu;
import com.glm.glmback.atelier.domain.*;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@UnitTest
class CompositionDesApercusTest {

  @Test
  void shouldComposerUnePreparationEtUneReferenceAvecLeTrousseauDuDeploiement() {
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
      .withPropertyValues(
        "atelier.resolution.apercus.cle-active=test",
        "atelier.resolution.apercus.cles.test=" + Base64.getEncoder().encodeToString(new byte[32])
      )
      .run(context -> {
        assertThat(context)
          .hasSingleBean(ReferencesDApercu.class)
          .hasSingleBean(EmpreintesDesConsequences.class)
          .hasSingleBean(PreparationDesActes.class)
          .hasSingleBean(ConfirmerLesActes.class);
        var suivi = suiviDAtelierEngage().enregistre(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H));
        var preuve = recuDAnnulation(suivi).preuve();
        var codec = context.getBean(ReferencesDApercu.class);
        assertThat(codec.read(codec.issue(preuve))).isEqualTo(preuve);
        assertThat(context.getBean(EmpreintesDesConsequences.class).calcule(suivi, LE_10_MAI_2026_A_8H)).hasSize(64);
      });
  }
}
