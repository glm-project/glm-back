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

  @Test
  void shouldCiterLActiviteQuUnFaitDuGestionnaireCible() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var annuaire = new AnnuaireDAtelier(Map.of(), Map.of());

    var retrait = JsonMapper.builder()
      .build()
      .valueToTree(RestEvenementDAtelier.from(retraitParLeroyDe(debut).a(LE_11_MAI_2026_A_9H), annuaire));
    var ouverture = JsonMapper.builder().build().valueToTree(RestEvenementDAtelier.from(debut, annuaire));

    assertThat(retrait.path("type").asString()).isEqualTo("RETRAIT");
    assertThat(retrait.path("activiteVisee").asString()).isEqualTo(debut.activite().orElseThrow().uuid().toString());
    assertThat(retrait.path("activite").isNull()).isTrue();
    assertThat(retrait.path("estUneRegularisation").asBoolean()).isTrue();
    assertThat(ouverture.path("activiteVisee").isNull()).isTrue();
  }
}
