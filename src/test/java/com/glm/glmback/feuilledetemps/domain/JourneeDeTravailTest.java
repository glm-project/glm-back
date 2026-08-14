package com.glm.glmback.feuilledetemps.domain;

import static com.glm.glmback.feuilledetemps.domain.FeuilleDeTempsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

@UnitTest
class JourneeDeTravailTest {

  @Test
  void shouldNotBuildWithoutJournal() {
    assertThatThrownBy(() -> new JourneeDeTravail(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldNotBuildWithNullEvenement() {
    List<EvenementDePresence> journal = Arrays.asList(arriveeA(LE_LUNDI_11_MAI_2026_A_8H), null);

    assertThatThrownBy(() -> new JourneeDeTravail(journal))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("journal");
  }

  @Test
  void shouldNotHaveFenetreWithoutEvenement() {
    assertThat(new JourneeDeTravail(List.of()).fenetres()).isEmpty();
  }

  /**
   * La pause de midi scinde la journee en deux fenetres : c'est ce qui distingue le temps travaille de l'amplitude.
   */
  @Test
  void shouldScinderLaJourneeSurLaPause() {
    List<Plage> fenetres = journeeDuLundiDe8HA17HAvecPauseDeMidi().fenetres();

    assertThat(fenetres).hasSize(2);
    assertThat(fenetres.getFirst().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_8H);
    assertThat(fenetres.getFirst().fin()).contains(LE_LUNDI_11_MAI_2026_A_12H);
    assertThat(fenetres.getLast().debut()).isEqualTo(LE_LUNDI_11_MAI_2026_A_13H);
    assertThat(fenetres.getLast().fin()).contains(LE_LUNDI_11_MAI_2026_A_17H);
  }

  @Test
  void shouldLaisserLaFenetreOuverteSansDepart() {
    List<Plage> fenetres = journeeDuMardiOuverteA8H().fenetres();

    assertThat(fenetres).hasSize(1);
    assertThat(fenetres.getFirst().debut()).isEqualTo(LE_MARDI_12_MAI_2026_A_8H);
    assertThat(fenetres.getFirst().fin()).isEmpty();
  }

  /**
   * L'adapter rend les evenements deja tries, mais le repli ne s'y fie pas : une regularisation arrive au journal
   * apres coup, a une heure anterieure.
   */
  @Test
  void shouldReplierDansLOrdreChronologiqueQuelQueSoitLOrdreRecu() {
    JourneeDeTravail journee = new JourneeDeTravail(
      List.of(
        departA(LE_LUNDI_11_MAI_2026_A_17H),
        repriseA(LE_LUNDI_11_MAI_2026_A_13H),
        pauseA(LE_LUNDI_11_MAI_2026_A_12H),
        arriveeA(LE_LUNDI_11_MAI_2026_A_8H)
      )
    );

    assertThat(journee.fenetres()).isEqualTo(journeeDuLundiDe8HA17HAvecPauseDeMidi().fenetres());
  }

  @Test
  void shouldRefuserUneSequenceImpossible() {
    List<EvenementDePresence> journal = List.of(pauseA(LE_LUNDI_11_MAI_2026_A_12H));

    assertThatThrownBy(() -> new JourneeDeTravail(journal))
      .isExactlyInstanceOf(TransitionDePresenceInterditeException.class)
      .hasMessageContaining("PAUSE")
      .hasMessageContaining("ABSENT");
  }
}
