package com.glm.glmback.pupitre.domain;

import static com.glm.glmback.pupitre.domain.PupitreFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import com.glm.glmback.shared.error.domain.NullElementInCollectionException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class JournalDuPupitreTest {

  @Test
  void shouldNotBuildWithoutEvenements() {
    assertThatThrownBy(() -> new JournalDuPupitre(null))
      .isExactlyInstanceOf(MissingMandatoryValueException.class)
      .hasMessageContaining("evenements");
  }

  @Test
  void shouldNotBuildWithNullEvenement() {
    List<EvenementDuPupitre> avecUnTrou = Arrays.asList(debut(LE_10_MAI_2026_A_8H), null);

    assertThatThrownBy(() -> new JournalDuPupitre(avecUnTrou))
      .isExactlyInstanceOf(NullElementInCollectionException.class)
      .hasMessageContaining("evenements");
  }

  @Test
  void shouldNAvoirAucuneActiviteSansEvenement() {
    JournalDuPupitre journal = JournalDuPupitre.vide();

    assertThat(journal.estVierge()).isTrue();
    assertThat(journal.activitesEnCours()).isEmpty();
  }

  @Test
  void shouldOuvrirUneActiviteDepuisSonDebut() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H)));

    assertThat(journal.activitesEnCours()).containsExactly(
      new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H)
    );
  }

  @Test
  void shouldFermerLActiviteSurUneFin() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H), fin(LE_10_MAI_2026_A_9H)));

    assertThat(journal.estVierge()).isFalse();
    assertThat(journal.activitesEnCours()).isEmpty();
  }

  /**
   * Une non conformite ne ferme pas l'activite — ce temps-la se compte aussi. Seule sa categorie change, et elle
   * repart de l'instant du bascule.
   */
  @Test
  void shouldGarderLActiviteOuverteEnNonConformite() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H), nonConformite(LE_10_MAI_2026_A_9H)));

    assertThat(journal.activitesEnCours()).containsExactly(
      new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.NON_CONFORMITE, LE_10_MAI_2026_A_9H)
    );
  }

  @Test
  void shouldRepasserAuTravailSurUnDebutApresUneNonConformite() {
    JournalDuPupitre journal = new JournalDuPupitre(
      List.of(debut(LE_10_MAI_2026_A_7H), nonConformite(LE_10_MAI_2026_A_8H), debut(LE_10_MAI_2026_A_9H))
    );

    assertThat(journal.activitesEnCours()).containsExactly(
      new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_9H)
    );
  }

  /**
   * Deux pieces du meme element sur deux machines sont deux activites independantes : c'est le poste, et non la
   * personne ni la nature, qui les distingue.
   */
  @Test
  void shouldMenerDeuxActivitesDeFront() {
    JournalDuPupitre journal = new JournalDuPupitre(
      List.of(
        debut(LE_10_MAI_2026_A_8H),
        new EvenementDuPupitre(TypeDePointage.DEBUT, ACTIVITE_MARTIN_SUR_FRAISEUSE_2, LE_10_MAI_2026_A_9H)
      )
    );

    assertThat(journal.activitesEnCours()).containsExactly(
      new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H),
      new ActiviteEnCours(ACTIVITE_MARTIN_SUR_FRAISEUSE_2, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_9H)
    );
  }

  @Test
  void shouldOuvrirUneActiviteSansPosteDeTravail() {
    JournalDuPupitre journal = new JournalDuPupitre(
      List.of(new EvenementDuPupitre(TypeDePointage.DEBUT, ACTIVITE_DUPONT_SANS_POSTE, LE_10_MAI_2026_A_8H))
    );

    assertThat(journal.activitesEnCours())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
        assertThat(activite.poste()).isEmpty();
      });
  }

  /**
   * Un journal que l'automate refuse n'est pas atteignable par l'API — l'atelier valide tout le journal a chaque
   * ecriture. La tolerance reste une defense en profondeur : un ecran d'atelier ne doit jamais s'eteindre parce
   * qu'un journal est bizarre.
   */
  @Test
  void shouldIgnorerUnPointageQueLAutomateRefuse() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(fin(LE_10_MAI_2026_A_7H), debut(LE_10_MAI_2026_A_8H)));

    assertThat(journal.activitesEnCours()).containsExactly(
      new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_8H)
    );
  }

  /**
   * Demarrer une activite deja en cours la relance : elle reste unique, et repart de l'instant de la relance, comme
   * dans l'atelier.
   */
  @Test
  void shouldRelancerUneActiviteDejaEnCours() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H), debut(LE_10_MAI_2026_A_12H)));

    assertThat(journal.activitesEnCours()).containsExactly(
      new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.TRAVAIL, LE_10_MAI_2026_A_12H)
    );
  }

  @Test
  void shouldRelancerUneNonConformiteDejaEnCours() {
    JournalDuPupitre journal = new JournalDuPupitre(List.of(nonConformite(LE_10_MAI_2026_A_8H), nonConformite(LE_10_MAI_2026_A_12H)));

    assertThat(journal.activitesEnCours()).containsExactly(
      new ActiviteEnCours(ACTIVITE_DUPONT_SUR_FRAISEUSE_1, CategorieDActivite.NON_CONFORMITE, LE_10_MAI_2026_A_12H)
    );
  }

  @Test
  void shouldFermerUneActiviteRelancee() {
    JournalDuPupitre journal = new JournalDuPupitre(
      List.of(debut(LE_10_MAI_2026_A_8H), debut(LE_10_MAI_2026_A_9H), fin(LE_10_MAI_2026_A_12H))
    );

    assertThat(journal.activitesEnCours()).isEmpty();
  }

  @Test
  void shouldExposerLesEvenementsQuiLuiOntEteConfies() {
    assertThat(new JournalDuPupitre(List.of(debut(LE_10_MAI_2026_A_8H))).evenements()).containsExactly(
      new EvenementDuPupitre(
        TypeDePointage.DEBUT,
        new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_1)),
        LE_10_MAI_2026_A_8H
      )
    );
  }
}
