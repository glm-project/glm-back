package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class RestEvenementDAtelierTest {

  @Test
  void shouldConserverLesIdentitesDesFichesNonResolues() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var json = JsonMapper.builder().build().valueToTree(RestEvenementDAtelier.from(debut, new AnnuaireDAtelier(Map.of(), Map.of())));

    assertThat(json.path("operateurId").asString()).isEqualTo(OPERATEUR_ID_DUPONT.uuid().toString());
    assertThat(json.path("posteId").asString()).isEqualTo(POSTE_ID_FRAISEUSE_1.uuid().toString());
    assertThat(json.path("operateur").isNull()).isTrue();
    assertThat(json.path("poste").isNull()).isTrue();
  }
}
