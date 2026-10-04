package com.glm.glmback.atelier.infrastructure.secondary.gestionconflits;

import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.gestionconflits.ConfirmerLesActes;
import com.glm.glmback.atelier.application.gestionconflits.EmpreintesDesConsequences;
import com.glm.glmback.atelier.application.gestionconflits.PreparationDesActes;
import com.glm.glmback.atelier.application.gestionconflits.RecusDActes;
import com.glm.glmback.atelier.application.gestionconflits.ReferencesDApercu;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.time.domain.Clock;
import java.security.SecureRandom;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConfigurationDesApercus.class)
class CompositionDesApercus {

  @Bean
  ReferencesDApercu referencesDApercu(ConfigurationDesApercus configuration) {
    return new AesReferencesDApercu(configuration, new SecureRandom(), new CryptographieDesReferences());
  }

  @Bean
  EmpreintesDesConsequences empreintesDesConsequences() {
    return new Sha256EmpreintesDesConsequences(new CryptographieDesReferences());
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
    ReferencesDApercu references,
    PreparationDesActes preparation,
    IdentitesDEvenements identites,
    Clock clock
  ) {
    return ConfirmerLesActes.builder()
      .suivis(suivis)
      .recus(recus)
      .references(references)
      .preparation(preparation)
      .identites(identites)
      .clock(clock);
  }
}
