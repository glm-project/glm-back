package com.glm.glmback.coutderevient.domain;

import static com.glm.glmback.coutderevient.domain.CoutDeRevientFixture.*;
import static com.glm.glmback.coutderevient.domain.TypeDEvenementDAtelier.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JournalDAtelierTest {

  @Test
  void shouldNotBuildWithoutEvenements() {
    assertThatThrownBy(() -> new JournalDAtelier(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evenements");
  }

  @Test
  void shouldRefuseImpossibleSequence() {
    assertThatThrownBy(() -> new JournalDAtelier(List.of(surFraiseuse(FIN, LE_11_MAI_A_10H)))).isExactlyInstanceOf(
      TransitionDAtelierInterditeException.class
    );
  }

  @Test
  void shouldHaveNoIntervalleWithoutEvenement() {
    assertThat(new JournalDAtelier(List.of()).intervalles(Optional.empty())).isEmpty();
  }

  @Test
  void shouldProduceOneIntervalleBetweenDebutAndFin() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(surFraiseuse(DEBUT, LE_11_MAI_A_9H), surFraiseuse(FIN, LE_11_MAI_A_11H))
    ).intervalles(Optional.empty());

    assertThat(intervalles)
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.plage()).isEqualTo(new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_11H)));
        assertThat(intervalle.activite().categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
        assertThat(intervalle.activite().nature()).contains(NATURE_FRAISAGE);
        assertThat(intervalle.activite().coutHoraire()).contains(COUT_HORAIRE_DE_45_EUROS);
        assertThat(intervalle.activite().tauxHoraire()).contains(TAUX_HORAIRE_DE_20_EUROS);
      });
  }

  /**
   * La categorie se lit sur l'etat atteint : une non conformite ouvre une seconde tranche sans refermer l'activite,
   * et c'est ce qui separe le bon travail de sa reprise sur la ligne du rapport.
   */
  @Test
  void shouldSplitTravailAndNonConformite() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(surFraiseuse(DEBUT, LE_11_MAI_A_9H), surFraiseuse(NON_CONFORMITE, LE_11_MAI_A_10H), surFraiseuse(FIN, LE_11_MAI_A_11H))
    ).intervalles(Optional.empty());

    assertThat(intervalles)
      .extracting(intervalle -> intervalle.activite().categorie())
      .containsExactly(CategorieDActivite.TRAVAIL, CategorieDActivite.NON_CONFORMITE);
    assertThat(intervalles.getFirst().plage()).isEqualTo(new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_10H)));
    assertThat(intervalles.getLast().plage()).isEqualTo(new Plage(LE_11_MAI_A_10H, Optional.of(LE_11_MAI_A_11H)));
  }

  @Test
  void shouldLeaveLastIntervalleOpenWithoutFin() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(List.of(surFraiseuse(DEBUT, LE_11_MAI_A_9H))).intervalles(Optional.empty());

    assertThat(intervalles).singleElement().extracting(IntervalleDActivite::plage).isEqualTo(new Plage(LE_11_MAI_A_9H, Optional.empty()));
  }

  /**
   * La cloture du suivi referme ce que personne n'a arrete : au-dela, plus rien n'a pu etre fait sur l'element.
   */
  @Test
  void shouldCloseLastIntervalleAtFermetureFinale() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(List.of(surFraiseuse(DEBUT, LE_11_MAI_A_9H))).intervalles(
      Optional.of(LE_11_MAI_A_17H)
    );

    assertThat(intervalles)
      .singleElement()
      .extracting(IntervalleDActivite::plage)
      .isEqualTo(new Plage(LE_11_MAI_A_9H, Optional.of(LE_11_MAI_A_17H)));
  }

  /**
   * L'automate se joue par cle d'activite : le meme operateur sur deux postes mene deux activites independantes, ce
   * que refuserait un journal replie en bloc.
   */
  @Test
  void shouldFoldEachActiviteIndependently() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(
        surFraiseuse(DEBUT, LE_11_MAI_A_9H),
        surTour(DEBUT, LE_11_MAI_A_10H),
        surFraiseuse(FIN, LE_11_MAI_A_11H),
        surTour(FIN, LE_11_MAI_A_12H)
      )
    ).intervalles(Optional.empty());

    assertThat(intervalles)
      .extracting(intervalle -> intervalle.activite().poste())
      .containsExactly(Optional.of(POSTE_ID_FRAISEUSE), Optional.of(POSTE_ID_TOUR));
  }

  @Test
  void shouldSortIntervallesByDebut() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(surTour(DEBUT, LE_11_MAI_A_10H), surFraiseuse(DEBUT, LE_11_MAI_A_9H))
    ).intervalles(Optional.empty());

    assertThat(intervalles)
      .extracting(intervalle -> intervalle.plage().debut())
      .containsExactly(LE_11_MAI_A_9H, LE_11_MAI_A_10H);
  }

  @Test
  void shouldProduceNoNatureWithoutPoste() {
    List<IntervalleDActivite> intervalles = new JournalDAtelier(
      List.of(
        EvenementDAtelier.builder()
          .type(DEBUT)
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.empty())
          .nature(Optional.empty())
          .coutHoraire(Optional.empty())
          .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
          .dateDeSurvenue(LE_11_MAI_A_9H)
      )
    ).intervalles(Optional.empty());

    assertThat(intervalles)
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.activite().nature()).isEmpty();
        assertThat(intervalle.activite().coutHoraire()).isEmpty();
      });
  }

  private static EvenementDAtelier surFraiseuse(TypeDEvenementDAtelier type, Instant date) {
    return sur(type, POSTE_ID_FRAISEUSE, NATURE_FRAISAGE, date);
  }

  private static EvenementDAtelier surTour(TypeDEvenementDAtelier type, Instant date) {
    return sur(type, POSTE_ID_TOUR, NATURE_TOURNAGE, date);
  }

  private static EvenementDAtelier sur(TypeDEvenementDAtelier type, PosteDeTravailId poste, NatureDOperation nature, Instant date) {
    return EvenementDAtelier.builder()
      .type(type)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(poste))
      .nature(Optional.of(nature))
      .coutHoraire(Optional.of(COUT_HORAIRE_DE_45_EUROS))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DE_20_EUROS))
      .dateDeSurvenue(date);
  }
}
