package com.glm.glmback.atelier.infrastructure.primary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class RestPointageTest {

  @ParameterizedTest
  @EnumSource(value = TypeDEvenementDAtelier.class, names = { "DEBUT", "NON_CONFORMITE", "FIN" })
  void shouldAccepterUnGeste(TypeDEvenementDAtelier type) {
    assertThat(pointage(type).isTypeUnGeste()).isTrue();
  }

  /**
   * Le journal porte aussi les corrections du gestionnaire : le pupitre n'en pointe aucune.
   */
  @ParameterizedTest
  @EnumSource(value = TypeDEvenementDAtelier.class, names = { "RETRAIT", "CHANGEMENT_DE_POSTE" })
  void shouldRefuserUneCorrection(TypeDEvenementDAtelier type) {
    assertThat(pointage(type).isTypeUnGeste()).isFalse();
  }

  /**
   * Un type absent est l'affaire de {@code @NotNull}, pas de cette regle.
   */
  @Test
  void shouldLaisserUnTypeAbsentALaValidationDuChamp() {
    assertThat(pointage(null).isTypeUnGeste()).isTrue();
  }

  private static RestPointage pointage(TypeDEvenementDAtelier type) {
    return new RestPointage(UUID.randomUUID(), type, UUID.randomUUID(), null, Instant.parse("2026-05-10T08:00:00Z"));
  }
}
