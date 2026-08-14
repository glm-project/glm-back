package com.glm.glmback.postedetravail.domain;

import static com.glm.glmback.postedetravail.domain.PostesDeTravailFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.pagination.domain.Page;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class PostesDeTravailServiceTest {

  private PostesDeTravailEnMemoire repository;
  private HabilitationsEnMemoire habilitations;
  private PointagesEnMemoire pointages;
  private PostesDeTravailService postes;

  @BeforeEach
  void setUp() {
    repository = new PostesDeTravailEnMemoire();
    habilitations = new HabilitationsEnMemoire();
    pointages = new PointagesEnMemoire();
    postes = new PostesDeTravailService(repository, habilitations, pointages);
  }

  @Test
  void shouldCreatePosteDeTravail() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1());

    assertThat(cree.libelle()).isEqualTo(LIBELLE_TOUR_1);
    assertThat(cree.nature()).isEqualTo(NATURE_TOURNAGE);
    assertThat(postes.get(cree.id())).isEqualTo(cree);
  }

  @Test
  void shouldCreatePosteDeTravailWithCoutHoraire() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1AvecCoutHoraire());

    assertThat(cree.coutHoraire()).contains(COUT_HORAIRE_45_50);
  }

  @Test
  void shouldNotCreatePosteDeTravailWithAlreadyUsedLibelle() {
    postes.create(posteDeTravailACreerTour1());

    assertThatThrownBy(() -> postes.create(posteDeTravailACreerTour1()))
      .isExactlyInstanceOf(LibelleDejaUtiliseException.class)
      .hasMessageContaining("Tour 1");
  }

  @Test
  void shouldNotGetUnknownPosteDeTravail() {
    PosteDeTravailId inconnu = PosteDeTravailId.newId();

    assertThatThrownBy(() -> postes.get(inconnu)).isExactlyInstanceOf(PosteDeTravailIntrouvableException.class);
  }

  @Test
  void shouldUpdatePosteDeTravail() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1());

    PosteDeTravail revise = postes.update(posteDeTravailAModifierFraiseuse1(cree.id()));

    assertThat(revise.id()).isEqualTo(cree.id());
    assertThat(revise.libelle()).isEqualTo(LIBELLE_FRAISEUSE_1);
    assertThat(revise.nature()).isEqualTo(NATURE_FRAISAGE);
  }

  @Test
  void shouldUpdatePosteDeTravailWithCoutHoraire() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1());

    PosteDeTravail revise = postes.update(posteDeTravailAModifierTour1AvecCoutHoraire(cree.id()));

    assertThat(revise.coutHoraire()).contains(COUT_HORAIRE_60);
  }

  @Test
  void shouldUpdatePosteDeTravailKeepingItsOwnLibelle() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1());

    PosteDeTravail revise = postes.update(posteDeTravailAModifierTour1(cree.id()));

    assertThat(revise.libelle()).isEqualTo(LIBELLE_TOUR_1);
  }

  @Test
  void shouldNotUpdatePosteDeTravailWithLibelleOfAnother() {
    postes.create(posteDeTravailACreerTour1());
    PosteDeTravail autre = postes.create(posteDeTravailACreerPosteDeSoudure());

    PosteDeTravailAModifier aModifier = posteDeTravailAModifierTour1(autre.id());

    assertThatThrownBy(() -> postes.update(aModifier)).isExactlyInstanceOf(LibelleDejaUtiliseException.class);
  }

  @Test
  void shouldNotUpdateUnknownPosteDeTravail() {
    PosteDeTravailAModifier aModifier = posteDeTravailAModifierTour1(PosteDeTravailId.newId());

    assertThatThrownBy(() -> postes.update(aModifier)).isExactlyInstanceOf(PosteDeTravailIntrouvableException.class);
  }

  @Test
  void shouldDeleteUnusedPosteDeTravail() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1());

    postes.delete(cree.id());

    assertThatThrownBy(() -> postes.get(cree.id())).isExactlyInstanceOf(PosteDeTravailIntrouvableException.class);
  }

  @Test
  void shouldNotDeletePosteDeTravailStillHabilitated() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1());
    habilitations.habilite(cree.id());

    assertThatThrownBy(() -> postes.delete(cree.id())).isExactlyInstanceOf(PosteDeTravailUtiliseException.class);
  }

  @Test
  void shouldDeletePosteDeTravailOnceReleased() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1());
    habilitations.habilite(cree.id());
    habilitations.libere(cree.id());

    postes.delete(cree.id());

    assertThatThrownBy(() -> postes.get(cree.id())).isExactlyInstanceOf(PosteDeTravailIntrouvableException.class);
  }

  /**
   * Le journal d'atelier ne retient que l'identifiant du poste : le supprimer laisserait des heures de travail sans
   * machine. Contrairement a l'habilitation, ce refus est definitif.
   */
  @Test
  void shouldNotDeletePosteDeTravailAlreadyPointed() {
    PosteDeTravail cree = postes.create(posteDeTravailACreerTour1());
    pointages.pointe(cree.id());

    assertThatThrownBy(() -> postes.delete(cree.id())).isExactlyInstanceOf(PosteDeTravailPointeException.class);
  }

  @Test
  void shouldNotDeleteUnknownPosteDeTravail() {
    PosteDeTravailId inconnu = PosteDeTravailId.newId();

    assertThatThrownBy(() -> postes.delete(inconnu)).isExactlyInstanceOf(PosteDeTravailIntrouvableException.class);
  }

  @Test
  void shouldListAllPostesDeTravailSortedByLibelle() {
    postes.create(posteDeTravailACreerTour1());
    postes.create(posteDeTravailACreerPosteDeSoudure());

    Page<PosteDeTravail> page = postes.list(Optional.empty(), firstPageOfTen());

    assertThat(
      page
        .content()
        .stream()
        .map(poste -> poste.libelle().value())
    ).containsExactly("Poste de soudure", "Tour 1");
    assertThat(page.totalElementsCount()).isEqualTo(2);
  }

  @Test
  void shouldListOnlyPostesDeTravailOfExpectedNature() {
    postes.create(posteDeTravailACreerTour1());
    postes.create(posteDeTravailACreerPosteDeSoudure());

    Page<PosteDeTravail> page = postes.list(Optional.of(NATURE_TOURNAGE), firstPageOfTen());

    assertThat(
      page
        .content()
        .stream()
        .map(poste -> poste.libelle().value())
    ).containsExactly("Tour 1");
  }

  @Test
  void shouldNotCreateTwicePosteDeTravailOfSameIdentity() {
    PosteDeTravail poste = posteDeTravailTour1();
    repository.create(poste);

    assertThatThrownBy(() -> repository.create(poste)).isExactlyInstanceOf(PosteDeTravailDejaExistantException.class);
  }
}
