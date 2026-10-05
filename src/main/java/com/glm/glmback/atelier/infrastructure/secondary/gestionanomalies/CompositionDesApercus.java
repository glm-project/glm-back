package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.gestionanomalies.ConfirmerLesActes;
import com.glm.glmback.atelier.application.gestionanomalies.EmpreintesDesConsequences;
import com.glm.glmback.atelier.application.gestionanomalies.PreparationDesActes;
import com.glm.glmback.atelier.application.gestionanomalies.RecusDActes;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.time.domain.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CompositionDesApercus {

  @Bean
  EmpreintesDesConsequences empreintesDesConsequences() {
    return new Sha256EmpreintesDesConsequences(new EmpreinteSha256());
  }

  @Bean
  PreparationDesActes preparationDesActes(
    SuiviDAtelierRepository repository,
    ElementsEngageables elements,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    EmpreintesDesConsequences empreintes
  ) {
    return PreparationDesActes.builder()
      .repository(repository)
      .elements(elements)
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .empreintes(empreintes);
  }

  @Bean
  ConfirmerLesActes confirmerLesActes(
    SuiviDAtelierRepository suivis,
    RecusDActes recus,
    PreparationDesActes preparation,
    IdentitesDEvenements identites,
    Clock clock
  ) {
    return ConfirmerLesActes.builder().suivis(suivis).recus(recus).preparation(preparation).identites(identites).clock(clock);
  }
}
