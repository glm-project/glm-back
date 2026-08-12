package com.glm.glmback.atelier.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Les treize autres traductions de cet advice sont eprouvees par les scenarios Cucumber, qui les atteignent toutes par
 * un appel HTTP. La saisie concurrente est la seule qui demanderait deux requetes reellement simultanees pour se
 * produire : elle s'eprouve donc ici, directement.
 */
@UnitTest
class AtelierExceptionAdviceTest {

  @Test
  void shouldTraduireUneSaisieConcurrenteEnConflit() {
    ProblemDetail probleme = new AtelierExceptionAdvice().handleSaisieConcurrente(new SaisieConcurrenteException(SuiviDAtelierId.newId()));

    assertThat(probleme.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(probleme.getTitle()).isEqualTo("saisie concurrente");
    assertThat(probleme.getProperties()).extracting("message").asString().contains("modifie par une autre saisie");
  }
}
