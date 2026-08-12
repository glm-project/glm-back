package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneesDeTravailEnMemoireTest {

  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();

  @Test
  void shouldNotCreateTwiceTheSameIdentite() {
    JourneeDeTravail journee = journeeDeDupontOuverteA7H();
    journees.create(journee);

    assertThatThrownBy(() -> journees.create(journee)).isExactlyInstanceOf(JourneeDeTravailDejaOuverteException.class);
  }

  @Test
  void shouldNotUpdateUnknownIdentite() {
    JourneeDeTravail journee = journeeDeDupontOuverteA7H();

    assertThatThrownBy(() -> journees.update(journee)).isExactlyInstanceOf(JourneeDeTravailIntrouvableException.class);
  }

  @Test
  void shouldCreateThenGet() {
    JourneeDeTravail journee = journees.create(journeeDeDupontOuverteA7H());

    assertThat(journees.get(journee.id())).contains(journee);
    assertThat(journees.getEnCoursPour(OPERATEUR_DUPONT)).contains(journee);
    assertThat(journees.getEnCoursPour(OPERATEUR_MARTIN)).isEmpty();
  }

  @Test
  void shouldNotGetEnCoursPourUneJourneeTerminee() {
    JourneeDeTravail journee = journees.create(journeeDeDupontOuverteA7H());
    journees.update(journee.enregistre(departDeDupontA(LE_10_MAI_2026_A_17H)));

    assertThat(journees.getEnCoursPour(OPERATEUR_DUPONT)).isEmpty();
  }

  @Test
  void shouldGetJourneeContenantUnInstant() {
    JourneeDeTravail journee = journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());

    assertThat(journees.journeeContenant(OPERATEUR_DUPONT, LE_10_MAI_2026_A_9H)).contains(journee);
    assertThat(journees.journeeContenant(OPERATEUR_DUPONT, LE_11_MAI_2026_A_9H15)).isEmpty();
    assertThat(journees.journeeContenant(OPERATEUR_MARTIN, LE_10_MAI_2026_A_9H)).isEmpty();
  }

  @Test
  void shouldListerLesJourneesRetenuesParLesCriteres() {
    JourneeDeTravail journee = journees.create(journeeDeDupontOuverteA7H());

    assertThat(
      journees
        .list(new JourneeDeTravailCriteria(Optional.of(journeeDu10Mai2026()), Optional.of(OPERATEUR_DUPONT)), firstPageOfTen())
        .content()
    ).containsExactly(journee);
  }
}
