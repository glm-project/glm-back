package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class EvenementDePresenceTest {

  private static final EvenementDePresenceId ID = EvenementDePresenceId.newId();
  private static final Horodatage HORODATAGE = Horodatage.saisiA(LE_10_MAI_2026_A_7H);

  @ParameterizedTest
  @MethodSource("composantsManquants")
  void shouldNotBuildWithoutMandatoryComposant(Runnable construction, String champ) {
    assertThatThrownBy(construction::run).isExactlyInstanceOf(MissingMandatoryValueException.class).hasMessageContaining(champ);
  }

  @Test
  void shouldBuildEvenementNonAnnule() {
    EvenementDePresence evenement = EvenementDePresence.builder()
      .id(ID)
      .type(TypeDEvenementDePresence.ARRIVEE)
      .auteur(AUTEUR_DUPONT)
      .horodatage(HORODATAGE);

    assertThat(evenement.id()).isEqualTo(ID);
    assertThat(evenement.type()).isEqualTo(TypeDEvenementDePresence.ARRIVEE);
    assertThat(evenement.auteur()).isEqualTo(AUTEUR_DUPONT);
    assertThat(evenement.horodatage()).isEqualTo(HORODATAGE);
    assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_7H);
    assertThat(evenement.dateDEnregistrement()).isEqualTo(LE_10_MAI_2026_A_7H);
    assertThat(evenement.estAnnule()).isFalse();
  }

  @Test
  void shouldReadDatesFromHorodatageDUneRegularisation() {
    EvenementDePresence evenement = departRegulariseParLeroyA(LE_10_MAI_2026_A_17H);

    assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H);
    assertThat(evenement.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
  }

  @Test
  void shouldMarquerLEvenementCommeAnnule() {
    EvenementDePresence annule = arriveeDeDupontA(LE_10_MAI_2026_A_7H).annule(annulationParLeroy());

    assertThat(annule.estAnnule()).isTrue();
    assertThat(annule.annulation()).contains(annulationParLeroy());
  }

  @Test
  void shouldNotAnnulerUnEvenementDejaAnnule() {
    EvenementDePresence annule = arriveeDeDupontA(LE_10_MAI_2026_A_7H).annule(annulationParLeroy());
    Annulation seconde = annulationParLeroy();

    assertThatThrownBy(() -> annule.annule(seconde))
      .isExactlyInstanceOf(EvenementDePresenceDejaAnnuleException.class)
      .hasMessageContaining("annule");
  }

  private static Stream<Arguments> composantsManquants() {
    return Stream.of(
      Arguments.of((Runnable) () -> evenement(null, TypeDEvenementDePresence.ARRIVEE, AUTEUR_DUPONT, HORODATAGE), "id"),
      Arguments.of((Runnable) () -> evenement(ID, null, AUTEUR_DUPONT, HORODATAGE), "type"),
      Arguments.of((Runnable) () -> evenement(ID, TypeDEvenementDePresence.ARRIVEE, null, HORODATAGE), "auteur"),
      Arguments.of((Runnable) () -> evenement(ID, TypeDEvenementDePresence.ARRIVEE, AUTEUR_DUPONT, null), "horodatage"),
      Arguments.of(
        (Runnable) () -> new EvenementDePresence(ID, TypeDEvenementDePresence.ARRIVEE, AUTEUR_DUPONT, HORODATAGE, null),
        "annulation"
      )
    );
  }

  private static void evenement(EvenementDePresenceId id, TypeDEvenementDePresence type, Auteur auteur, Horodatage horodatage) {
    new EvenementDePresence(id, type, auteur, horodatage, Optional.empty());
  }
}
