package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.application.EmpreintesDesConsequences;
import com.glm.glmback.atelier.application.PreparationDesActes;
import com.glm.glmback.atelier.application.ReferencesDApercu;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
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
}
