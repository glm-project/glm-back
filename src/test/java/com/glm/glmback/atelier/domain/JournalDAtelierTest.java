package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
  void shouldNotBuildWithNullEvenement() {
    List<EvenementDAtelier> avecNull = Arrays.asList(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), null);

    assertThatThrownBy(() -> new JournalDAtelier(avecNull))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("evenements");
  }

  @Test
  void shouldBuildJournalVide() {
    JournalDAtelier journal = JournalDAtelier.vide();

    assertThat(journal.evenements()).isEmpty();
    assertThat(journal.actifs()).isEmpty();
    assertThat(journal.intervalles(Optional.empty())).isEmpty();
  }

  @Test
  void shouldTrierLesEvenementsParDateDeSurvenue() {
    EvenementDAtelier fin = finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H);
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    JournalDAtelier journal = new JournalDAtelier(List.of(fin, debut));

    assertThat(journal.evenements()).containsExactly(debut, fin);
  }

  @Test
  void shouldDepartagerLesEvenementsSimultanesParDateDEnregistrement() {
    EvenementDAtelier finRegularisee = finSurFraiseuse1RegulariseeParLeroyA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier debutPointe = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    JournalDAtelier journal = new JournalDAtelier(List.of(finRegularisee, debutPointe));

    assertThat(journal.evenements()).containsExactly(debutPointe, finRegularisee);
  }

  @Test
  void shouldDepartagerLesSaisiesIdentiquesParId() {
    EvenementDAtelier premier = pointage(new EvenementDAtelierId(new UUID(0, 1)), TypeDEvenementDAtelier.DEBUT, LE_10_MAI_2026_A_8H);
    EvenementDAtelier second = pointage(new EvenementDAtelierId(new UUID(0, 2)), TypeDEvenementDAtelier.FIN, LE_10_MAI_2026_A_8H);

    JournalDAtelier journal = new JournalDAtelier(List.of(second, premier));

    assertThat(journal.evenements()).containsExactly(premier, second);
  }

  @Test
  void shouldRefuserUneTransitionInterdite() {
    List<EvenementDAtelier> finSansDebut = List.of(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H));

    assertThatThrownBy(() -> new JournalDAtelier(finSansDebut))
      .isExactlyInstanceOf(TransitionDAtelierInterditeException.class)
      .hasMessageContaining("FIN")
      .hasMessageContaining(OPERATEUR_ID_DUPONT.uuid().toString())
      .hasMessageContaining("ABSENTE");
  }

  @Test
  void shouldJouerLAutomateIndependammentPourChaqueOperateur() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_9H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::operateur)
      .containsExactly(OPERATEUR_ID_DUPONT, OPERATEUR_ID_MARTIN);
  }

  /**
   * Le cas de l'erosionniste : deux pieces du meme element sur deux machines. Sans le poste dans la cle, le second
   * debut se heurterait au premier et serait refuse.
   */
  @Test
  void shouldJouerLAutomateIndependammentPourChaquePosteDUnMemeOperateur() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_9H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::poste)
      .containsExactly(Optional.of(POSTE_ID_FRAISEUSE_1), Optional.of(POSTE_ID_FRAISEUSE_2));
  }

  @Test
  void shouldJouerUneActiviteUniqueQuandAucunPosteNEstRenseigne() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSansPosteParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.intervalles(Optional.empty()))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.poste()).isEmpty());
  }

  @Test
  void shouldFermerUnIntervalleSurLEvenementSuivantDeLaMemeActivite() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_12H);
      });
  }

  @Test
  void shouldLaisserOuvertLeDernierIntervalleDUneActivite() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.intervalles(Optional.empty())).singleElement().matches(IntervalleDActivite::estOuvert);
  }

  @Test
  void shouldFermerLeDernierIntervalleSurLaFermetureFinale() {
    JournalDAtelier journal = new JournalDAtelier(List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H)));

    assertThat(journal.intervalles(Optional.of(LE_10_MAI_2026_A_17H)))
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldNOuvrirAucunIntervalleSurUneFin() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H))
    );

    assertThat(journal.intervalles(Optional.of(LE_10_MAI_2026_A_17H))).hasSize(1);
  }

  @Test
  void shouldCategoriserSurLEtatAtteintEtNonSurLeTypeDEvenement() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H))
    );

    assertThat(journal.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::categorie)
      .containsExactly(CategorieDActivite.NON_CONFORMITE, CategorieDActivite.TRAVAIL);
  }

  @Test
  void shouldEcarterDuRepliLesEvenementsAnnules() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut));

    JournalDAtelier corrige = journal.annule(debut.id(), annulationParLeroy());

    assertThat(corrige.evenements()).hasSize(1);
    assertThat(corrige.actifs()).isEmpty();
    assertThat(corrige.intervalles(Optional.empty())).isEmpty();
  }

  @Test
  void shouldConserverLaTraceDeLEvenementAnnule() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    JournalDAtelier corrige = new JournalDAtelier(List.of(debut)).annule(debut.id(), annulationParLeroy());

    assertThat(corrige.evenement(debut.id()))
      .isPresent()
      .get()
      .satisfies(evenement -> {
        assertThat(evenement.estAnnule()).isTrue();
        assertThat(evenement.annulation()).contains(annulationParLeroy());
      });
  }

  @Test
  void shouldNotAnnulerUnEvenementInconnu() {
    JournalDAtelier journal = JournalDAtelier.vide();
    EvenementDAtelierId inconnu = EvenementDAtelierId.newId();
    Annulation annulation = annulationParLeroy();

    assertThatThrownBy(() -> journal.annule(inconnu, annulation))
      .isExactlyInstanceOf(EvenementDAtelierIntrouvableException.class)
      .hasMessageContaining("introuvable");
  }

  @Test
  void shouldRefuserUneAnnulationQuiLaisseraitUneFinOrpheline() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H)));
    Annulation annulation = annulationParLeroy();

    assertThatThrownBy(() -> journal.annule(debut.id(), annulation)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
  }

  @Test
  void shouldCorrigerUnEvenementEnUnSeulActe() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    JournalDAtelier journal = new JournalDAtelier(List.of(debut, finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H)));

    JournalDAtelier corrige = journal.corrige(
      debut.id(),
      annulationParLeroy(),
      debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_7H30)
    );

    assertThat(corrige.intervalles(Optional.empty()))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_7H30);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_12H);
      });
  }

  @Test
  void shouldNotCorrigerUnEvenementInconnu() {
    JournalDAtelier journal = JournalDAtelier.vide();
    EvenementDAtelierId inconnu = EvenementDAtelierId.newId();
    Annulation annulation = annulationParLeroy();
    EvenementDAtelier remplacant = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    assertThatThrownBy(() -> journal.corrige(inconnu, annulation, remplacant)).isExactlyInstanceOf(
      EvenementDAtelierIntrouvableException.class
    );
  }

  @Test
  void shouldRefermerLIntervallePrecedentSurUneInsertionRetroactive() {
    JournalDAtelier journal = new JournalDAtelier(
      List.of(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H), finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_13H))
    );

    JournalDAtelier regularise = journal.enregistre(nonConformiteSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H));

    assertThat(regularise.intervalles(Optional.empty()))
      .extracting(IntervalleDActivite::debut, IntervalleDActivite::categorie)
      .containsExactly(
        tuple(LE_10_MAI_2026_A_8H, CategorieDActivite.TRAVAIL),
        tuple(LE_10_MAI_2026_A_12H, CategorieDActivite.NON_CONFORMITE)
      );
  }

  @Test
  void shouldNotReadEvenementInconnu() {
    assertThat(JournalDAtelier.vide().evenement(EvenementDAtelierId.newId())).isEmpty();
  }

  private static EvenementDAtelier pointage(EvenementDAtelierId id, TypeDEvenementDAtelier type, Instant date) {
    return EvenementDAtelier.builder()
      .id(id)
      .type(type)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .auteur(AUTEUR_DUPONT)
      .horodatage(Horodatage.saisiA(date));
  }
}
