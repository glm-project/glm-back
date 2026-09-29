package com.glm.glmback.atelier.infrastructure.primary;

import static com.glm.glmback.atelier.domain.IntentionDePointage.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class CoherenceDuGesteTest {

  private static final UUID CIBLE = UUID.fromString("00000000-0000-0000-0000-000000000042");

  @ParameterizedTest
  @MethodSource("intentionsEtTypes")
  void shouldAdmettreLIntentionSelonLeType(TypeDEvenementDAtelier type, IntentionDePointage intention, boolean admise) {
    assertThat(CoherenceDuGeste.intentionAdmise(type, intention)).isEqualTo(admise);
  }

  @ParameterizedTest
  @MethodSource("intentionsEtCibles")
  void shouldExigerUneCibleSelonLIntention(IntentionDePointage intention, UUID cible, boolean conforme) {
    assertThat(CoherenceDuGeste.cibleConforme(intention, cible)).isEqualTo(conforme);
  }

  /**
   * Un champ absent releve de son propre @NotNull : il ne fait pas, en plus, une combinaison invalide.
   */
  @Test
  void shouldLaisserAuxChampsRequisLeRefusDUnChampAbsent() {
    assertThat(CoherenceDuGeste.intentionAdmise(null, FIN)).isTrue();
    assertThat(CoherenceDuGeste.intentionAdmise(TypeDEvenementDAtelier.FIN, null)).isTrue();
    assertThat(CoherenceDuGeste.cibleConforme(null, CIBLE)).isTrue();
  }

  private static Stream<Arguments> intentionsEtTypes() {
    return Stream.of(
      Arguments.of(TypeDEvenementDAtelier.DEBUT, OUVERTURE, true),
      Arguments.of(TypeDEvenementDAtelier.NON_CONFORMITE, TRANSITION, true),
      Arguments.of(TypeDEvenementDAtelier.FIN, FIN, true),
      Arguments.of(TypeDEvenementDAtelier.FIN, OUVERTURE, false),
      Arguments.of(TypeDEvenementDAtelier.DEBUT, FIN, false)
    );
  }

  private static Stream<Arguments> intentionsEtCibles() {
    return Stream.of(
      Arguments.of(OUVERTURE, null, true),
      Arguments.of(OUVERTURE, CIBLE, false),
      Arguments.of(TRANSITION, CIBLE, true),
      Arguments.of(TRANSITION, null, false),
      Arguments.of(FIN, CIBLE, true),
      Arguments.of(FIN, null, false)
    );
  }
}
