package com.glm.glmback.operateur.domain;

import static com.glm.glmback.operateur.domain.OperateursFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.pagination.domain.Page;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class OperateursServiceTest {

  private OperateursEnMemoire repository;
  private PostesHabilitablesEnMemoire postes;
  private PointagesEnMemoire pointages;
  private OperateursService operateurs;

  @BeforeEach
  void setUp() {
    repository = new OperateursEnMemoire();
    postes = new PostesHabilitablesEnMemoire();
    postes.declare(POSTE_HABILITABLE_TOUR_1);
    postes.declare(POSTE_HABILITABLE_POSTE_DE_SOUDURE);
    pointages = new PointagesEnMemoire();
    operateurs = new OperateursService(repository, postes, pointages);
  }

  /**
   * Le test qui porte le lot : les metiers viennent des postes, jamais de la personne.
   */
  @Test
  void shouldCreateOperateurWhoseMetiersComeFromHisPostes() {
    ProfilDOperateur profil = operateurs.create(operateurACreerDupont());

    assertThat(profil.operateur().nom()).isEqualTo(NOM_DUPONT);
    assertThat(profil.operateur().matricule()).contains(MATRICULE_049);
    assertThat(profil.natures()).containsExactly(NATURE_SOUDAGE, NATURE_TOURNAGE);
  }

  @Test
  void shouldCreateOperateurWithTauxHoraire() {
    ProfilDOperateur profil = operateurs.create(operateurACreerDupontAvecTauxHoraire());

    assertThat(profil.operateur().tauxHoraire()).contains(TAUX_HORAIRE_22);
  }

  @Test
  void shouldCreateOperateurWithoutMatriculeNorPoste() {
    ProfilDOperateur profil = operateurs.create(operateurACreerMartinSansMatricule());

    assertThat(profil.operateur().matricule()).isEmpty();
    assertThat(profil.natures()).isEmpty();
  }

  @Test
  void shouldCreateManyOperateursWithoutMatricule() {
    operateurs.create(operateurACreerMartinSansMatricule());

    ProfilDOperateur second = operateurs.create(operateurACreerDupontSansMatricule());

    assertThat(second.operateur().matricule()).isEmpty();
  }

  @Test
  void shouldNotCreateOperateurWithAlreadyUsedIdentite() {
    operateurs.create(operateurACreerDupont());

    assertThatThrownBy(() -> operateurs.create(operateurACreerDupont()))
      .isExactlyInstanceOf(IdentiteDejaUtiliseeException.class)
      .hasMessageContaining("Jean Dupont");
  }

  @Test
  void shouldNotCreateOperateurWithAlreadyUsedMatricule() {
    operateurs.create(operateurACreerDupont());
    OperateurACreer homonymeDeMatricule = new OperateurACreer(
      NOM_MARTIN,
      PRENOM_SOPHIE,
      Optional.of(MATRICULE_049),
      Optional.empty(),
      Set.of()
    );

    assertThatThrownBy(() -> operateurs.create(homonymeDeMatricule))
      .isExactlyInstanceOf(MatriculeDejaUtiliseException.class)
      .hasMessageContaining("049");
  }

  @Test
  void shouldNotCreateOperateurReferencingUnknownPoste() {
    PosteHabilitableId inconnu = new PosteHabilitableId(java.util.UUID.fromString("99999999-9999-9999-9999-999999999999"));
    OperateurACreer aCreer = new OperateurACreer(NOM_DUPONT, PRENOM_JEAN, Optional.empty(), Optional.empty(), Set.of(inconnu));

    assertThatThrownBy(() -> operateurs.create(aCreer)).isExactlyInstanceOf(PosteHabilitableIntrouvableException.class);
  }

  @Test
  void shouldGetOperateurWithHisResolvedPostes() {
    ProfilDOperateur cree = operateurs.create(operateurACreerDupont());

    ProfilDOperateur relu = operateurs.get(cree.operateur().id());

    assertThat(relu.postes()).containsExactly(POSTE_HABILITABLE_POSTE_DE_SOUDURE, POSTE_HABILITABLE_TOUR_1);
  }

  @Test
  void shouldNotGetUnknownOperateur() {
    OperateurId inconnu = OperateurId.newId();

    assertThatThrownBy(() -> operateurs.get(inconnu)).isExactlyInstanceOf(OperateurIntrouvableException.class);
  }

  @Test
  void shouldUpdateOperateurHabilitations() {
    ProfilDOperateur cree = operateurs.create(operateurACreerDupont());

    ProfilDOperateur revise = operateurs.update(operateurAModifierDupont(cree.operateur().id()));

    assertThat(revise.operateur().postes()).containsExactly(ID_TOUR_1);
    assertThat(revise.natures()).containsExactly(NATURE_TOURNAGE);
  }

  @Test
  void shouldUpdateOperateurWithTauxHoraire() {
    ProfilDOperateur cree = operateurs.create(operateurACreerDupont());

    ProfilDOperateur revise = operateurs.update(operateurAModifierDupontAvecTauxHoraire(cree.operateur().id()));

    assertThat(revise.operateur().tauxHoraire()).contains(TAUX_HORAIRE_25);
  }

  @Test
  void shouldUpdateOperateurKeepingHisOwnIdentiteAndMatricule() {
    ProfilDOperateur cree = operateurs.create(operateurACreerDupont());

    ProfilDOperateur revise = operateurs.update(operateurAModifierDupont(cree.operateur().id()));

    assertThat(revise.operateur().nom()).isEqualTo(NOM_DUPONT);
    assertThat(revise.operateur().matricule()).contains(MATRICULE_049);
  }

  @Test
  void shouldNotUpdateOperateurWithIdentiteOfAnother() {
    operateurs.create(operateurACreerDupont());
    ProfilDOperateur autre = operateurs.create(operateurACreerMartin());

    OperateurAModifier aModifier = new OperateurAModifier(
      autre.operateur().id(),
      NOM_DUPONT,
      PRENOM_JEAN,
      Optional.of(MATRICULE_050),
      Optional.empty(),
      Set.of()
    );

    assertThatThrownBy(() -> operateurs.update(aModifier)).isExactlyInstanceOf(IdentiteDejaUtiliseeException.class);
  }

  @Test
  void shouldNotUpdateOperateurWithMatriculeOfAnother() {
    operateurs.create(operateurACreerDupont());
    ProfilDOperateur autre = operateurs.create(operateurACreerMartin());

    OperateurAModifier aModifier = new OperateurAModifier(
      autre.operateur().id(),
      NOM_MARTIN,
      PRENOM_SOPHIE,
      Optional.of(MATRICULE_049),
      Optional.empty(),
      Set.of()
    );

    assertThatThrownBy(() -> operateurs.update(aModifier)).isExactlyInstanceOf(MatriculeDejaUtiliseException.class);
  }

  @Test
  void shouldNotUpdateUnknownOperateur() {
    OperateurAModifier aModifier = operateurAModifierDupont(OperateurId.newId());

    assertThatThrownBy(() -> operateurs.update(aModifier)).isExactlyInstanceOf(OperateurIntrouvableException.class);
  }

  @Test
  void shouldDeleteOperateur() {
    ProfilDOperateur cree = operateurs.create(operateurACreerDupont());
    OperateurId id = cree.operateur().id();

    operateurs.delete(id);

    assertThatThrownBy(() -> operateurs.get(id)).isExactlyInstanceOf(OperateurIntrouvableException.class);
  }

  /**
   * Le journal d'atelier et les journees de travail ne retiennent que l'identifiant : supprimer l'operateur laisserait
   * des heures sans personne a payer.
   */
  @Test
  void shouldNotDeleteOperateurWhoAlreadyPointed() {
    OperateurId id = operateurs.create(operateurACreerDupont()).operateur().id();
    pointages.pointe(id);

    assertThatThrownBy(() -> operateurs.delete(id)).isExactlyInstanceOf(OperateurAPointeException.class);
  }

  @Test
  void shouldNotDeleteUnknownOperateur() {
    OperateurId inconnu = OperateurId.newId();

    assertThatThrownBy(() -> operateurs.delete(inconnu)).isExactlyInstanceOf(OperateurIntrouvableException.class);
  }

  @Test
  void shouldListOperateursSortedByIdentiteWithTheirMetiers() {
    operateurs.create(operateurACreerDupont());
    operateurs.create(operateurACreerMartin());

    Page<ProfilDOperateur> page = operateurs.list(Optional.empty(), firstPageOfTen());

    assertThat(
      page
        .content()
        .stream()
        .map(profil -> profil.operateur().nom().value())
    ).containsExactly("Dupont", "Martin");
    assertThat(page.content().getFirst().natures()).containsExactly(NATURE_SOUDAGE, NATURE_TOURNAGE);
    assertThat(page.totalElementsCount()).isEqualTo(2);
  }

  @Test
  void shouldListOnlyOperateursHabilitatedOnPoste() {
    operateurs.create(operateurACreerDupont());
    operateurs.create(operateurACreerMartinSansMatricule());

    Page<ProfilDOperateur> page = operateurs.list(Optional.of(ID_POSTE_DE_SOUDURE), firstPageOfTen());

    assertThat(
      page
        .content()
        .stream()
        .map(profil -> profil.operateur().nom().value())
    ).containsExactly("Dupont");
  }

  @Test
  void shouldNotCreateTwiceOperateurOfSameIdentity() {
    Operateur operateur = operateurDupont();
    repository.create(operateur);

    assertThatThrownBy(() -> repository.create(operateur)).isExactlyInstanceOf(OperateurDejaExistantException.class);
  }
}
