package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Le repli de l'atelier, rejoue ici par poste pour un seul operateur. Memes transitions que dans {@code atelier} et
 * {@code coutderevient} : c'est ce qui tient les cinq automates alignes, avec les scenarios Cucumber.
 */
@UnitTest
class JournalDAtelierTest {

  @Test
  void shouldNotBuildWithoutPointages() {
    assertThatThrownBy(() -> new JournalDAtelier(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("pointages");
  }

  @Test
  void shouldNeRienRendreSansPointage() {
    assertThat(new JournalDAtelier(List.of()).intervalles(ELEMENT_ID_CARTER, Optional.empty())).isEmpty();
  }

  @Test
  void shouldRendreUnIntervalleDuDebutALaFin() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H), finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H))
    ).intervalles(ELEMENT_ID_CARTER, Optional.empty());

    assertThat(intervalles).containsExactly(
      new IntervalleDActivite(
        activiteDeTravailDuCarterSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
      )
    );
  }

  /**
   * Un debut sur une activite en cours la relance : l'intervalle precedent s'arrete a la relance, sans ajouter de
   * temps.
   */
  @Test
  void shouldRelancerUneActiviteEnCours() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(
        debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H),
        debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_10H),
        finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H)
      )
    ).intervalles(ELEMENT_ID_CARTER, Optional.empty());

    assertThat(intervalles)
      .extracting(IntervalleDActivite::plage)
      .containsExactly(
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_10H)),
        new Plage(LE_LUNDI_11_MAI_2026_A_10H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
      );
  }

  @Test
  void shouldOuvrirUneNonConformiteQuiSuitLeTravail() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(
        debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H),
        nonConformiteSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_10H),
        finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H)
      )
    ).intervalles(ELEMENT_ID_CARTER, Optional.empty());

    assertThat(intervalles).containsExactly(
      new IntervalleDActivite(
        activiteDeTravailDuCarterSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_10H))
      ),
      new IntervalleDActivite(
        activiteDeNonConformiteDuCarterSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_10H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
      )
    );
  }

  @Test
  void shouldRelancerUneNonConformiteSurUneNonConformite() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(nonConformiteSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H), nonConformiteSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_10H))
    ).intervalles(ELEMENT_ID_CARTER, Optional.empty());

    assertThat(intervalles).containsExactly(
      new IntervalleDActivite(
        activiteDeNonConformiteDuCarterSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_10H))
      ),
      new IntervalleDActivite(activiteDeNonConformiteDuCarterSurLaDmu50(), new Plage(LE_LUNDI_11_MAI_2026_A_10H, Optional.empty()))
    );
  }

  /**
   * La fin ignoree ne borne rien : l'intervalle precedent s'arrete a la premiere fin, le suivant commence au debut.
   */
  @Test
  void shouldIgnorerUneFinSansActivite() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(
        finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_7H),
        debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H),
        finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_9H),
        finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_10H),
        debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_11H),
        finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H)
      )
    ).intervalles(ELEMENT_ID_CARTER, Optional.empty());

    assertThat(intervalles)
      .extracting(IntervalleDActivite::plage)
      .containsExactly(
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_9H)),
        new Plage(LE_LUNDI_11_MAI_2026_A_11H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
      );
  }

  /**
   * Deux postes, deux activites menees de front : la fin sur l'un n'arrete pas l'autre.
   */
  @Test
  void shouldRejouerChaquePosteSeparement() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(
        debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H),
        debutAuTourA(LE_LUNDI_11_MAI_2026_A_9H),
        finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_10H),
        finAuTourA(LE_LUNDI_11_MAI_2026_A_12H)
      )
    ).intervalles(ELEMENT_ID_CARTER, Optional.empty());

    assertThat(intervalles).containsExactly(
      new IntervalleDActivite(
        activiteDeTravailDuCarterSurLaDmu50(),
        new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_10H))
      ),
      new IntervalleDActivite(
        activiteDeTravailDuCarterAuTour(),
        new Plage(LE_LUNDI_11_MAI_2026_A_9H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H))
      )
    );
  }

  @Test
  void shouldArreterALaFermetureFinaleCeQuePersonneNAArrete() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(List.of(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H))).intervalles(
      ELEMENT_ID_CARTER,
      Optional.of(LE_LUNDI_11_MAI_2026_A_12H)
    );

    assertThat(intervalles)
      .extracting(IntervalleDActivite::plage)
      .containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)));
  }

  @Test
  void shouldTrierLesPointagesParDate() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H), debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H))
    ).intervalles(ELEMENT_ID_CARTER, Optional.empty());

    assertThat(intervalles)
      .extracting(IntervalleDActivite::plage)
      .containsExactly(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_12H)));
  }

  @Test
  void shouldGarderUnPointageSansPoste() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(List.of(debutSansPosteA(LE_LUNDI_11_MAI_2026_A_8H))).intervalles(
      ELEMENT_ID_CARTER,
      Optional.empty()
    );

    assertThat(intervalles)
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.activite().poste()).isEmpty();
        assertThat(intervalle.activite().nature()).isEmpty();
      });
  }
}
