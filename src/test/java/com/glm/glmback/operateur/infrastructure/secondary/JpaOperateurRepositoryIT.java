package com.glm.glmback.operateur.infrastructure.secondary;

import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.operateur.domain.Matricule;
import com.glm.glmback.operateur.domain.Nom;
import com.glm.glmback.operateur.domain.Operateur;
import com.glm.glmback.operateur.domain.OperateurCriteria;
import com.glm.glmback.operateur.domain.OperateurDejaExistantException;
import com.glm.glmback.operateur.domain.OperateurId;
import com.glm.glmback.operateur.domain.OperateurIntrouvableException;
import com.glm.glmback.operateur.domain.OperateurRepository;
import com.glm.glmback.operateur.domain.PosteHabilitableId;
import com.glm.glmback.operateur.domain.Prenom;
import com.glm.glmback.postedetravail.domain.Libelle;
import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.postedetravail.domain.PosteDeTravailRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La cle etrangere de operateur_poste impose qu'un poste existe avant d'y habiliter quiconque : ce test declare donc
 * ses postes par le repository du contexte voisin. C'est un montage de test, pas une dependance de production -- le
 * code de l'adapter, lui, n'atteint la table voisine que par une entite en lecture seule.
 */
@IntegrationTest
class JpaOperateurRepositoryIT {

  private static final String IMPECCMOLD = "impeccmold";
  private static final String KATILYS = "katilys";
  private static final AtomicLong COMPTEUR = new AtomicLong();
  private static final NatureDeTravail TOURNAGE = new NatureDeTravail("tournage");

  @Autowired
  private OperateurRepository operateurs;

  @Autowired
  private PosteDeTravailRepository postes;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldCreateAndGetOperateurWithHisHabilitations() {
    PosteHabilitableId poste = posteDeclare();
    Operateur operateur = operateurHabiliteSur(Set.of(poste));

    inTransaction(() -> operateurs.create(operateur));

    assertThat(inTransaction(() -> operateurs.get(operateur.id()))).contains(operateur);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldCreateAndGetOperateurWithoutMatriculeNorHabilitation() {
    Operateur operateur = operateurHabiliteSur(Set.of(), null);

    inTransaction(() -> operateurs.create(operateur));

    assertThat(inTransaction(() -> operateurs.get(operateur.id()))).contains(operateur);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldCreateManyOperateursWithoutMatricule() {
    Operateur premier = operateurHabiliteSur(Set.of(), null);
    Operateur second = operateurHabiliteSur(Set.of(), null);

    inTransaction(() -> operateurs.create(premier));
    inTransaction(() -> operateurs.create(second));

    assertThat(inTransaction(() -> operateurs.get(second.id()))).contains(second);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotCreateAlreadyExistingOperateur() {
    Operateur operateur = operateurHabiliteSur(Set.of());
    inTransaction(() -> operateurs.create(operateur));

    assertThatThrownBy(() -> inTransaction(() -> operateurs.create(operateur))).isExactlyInstanceOf(OperateurDejaExistantException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotGetUnknownOperateur() {
    assertThat(inTransaction(() -> operateurs.get(OperateurId.newId()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldUpdateHabilitationsRemovingOneAndAddingAnother() {
    PosteHabilitableId retire = posteDeclare();
    PosteHabilitableId conserve = posteDeclare();
    PosteHabilitableId ajoute = posteDeclare();
    Operateur operateur = operateurHabiliteSur(Set.of(retire, conserve));
    inTransaction(() -> operateurs.create(operateur));

    Operateur revise = operateur.revise(operateur.nom(), operateur.prenom(), operateur.matricule(), Set.of(conserve, ajoute));
    inTransaction(() -> operateurs.update(revise));

    assertThat(
      inTransaction(() -> operateurs.get(operateur.id()))
        .orElseThrow()
        .postes()
    ).containsExactlyInAnyOrder(conserve, ajoute);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldUpdateOperateurRemovingHisMatricule() {
    Operateur operateur = operateurHabiliteSur(Set.of());
    inTransaction(() -> operateurs.create(operateur));

    Operateur revise = operateur.revise(operateur.nom(), operateur.prenom(), Optional.empty(), Set.of());
    inTransaction(() -> operateurs.update(revise));

    assertThat(
      inTransaction(() -> operateurs.get(operateur.id()))
        .orElseThrow()
        .matricule()
    ).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotUpdateUnknownOperateur() {
    Operateur inconnu = operateurHabiliteSur(Set.of());

    assertThatThrownBy(() -> inTransaction(() -> operateurs.update(inconnu))).isExactlyInstanceOf(OperateurIntrouvableException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldDeleteExistingOperateur() {
    Operateur operateur = operateurHabiliteSur(Set.of(posteDeclare()));
    inTransaction(() -> operateurs.create(operateur));

    inTransaction(() -> {
      operateurs.delete(operateur.id());

      return null;
    });

    assertThat(inTransaction(() -> operateurs.get(operateur.id()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotDeleteUnknownOperateur() {
    OperateurId inconnu = OperateurId.newId();

    assertThatThrownBy(() ->
      inTransaction(() -> {
        operateurs.delete(inconnu);

        return null;
      })
    ).isExactlyInstanceOf(OperateurIntrouvableException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldGetIdOfOperateurHoldingIdentite() {
    Operateur operateur = operateurHabiliteSur(Set.of());
    inTransaction(() -> operateurs.create(operateur));

    assertThat(inTransaction(() -> operateurs.idPourIdentite(operateur.nom(), operateur.prenom()))).contains(operateur.id());
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotGetIdOfUnusedIdentite() {
    assertThat(inTransaction(() -> operateurs.idPourIdentite(new Nom("IT-inconnu"), new Prenom("IT-inconnu")))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldGetIdOfOperateurHoldingMatricule() {
    Operateur operateur = operateurHabiliteSur(Set.of());
    inTransaction(() -> operateurs.create(operateur));

    assertThat(inTransaction(() -> operateurs.idPourMatricule(operateur.matricule().orElseThrow()))).contains(operateur.id());
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotGetIdOfUnusedMatricule() {
    assertThat(inTransaction(() -> operateurs.idPourMatricule(new Matricule("IT-matricule-inconnu")))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListOnlyOperateursHabilitatedOnPoste() {
    PosteHabilitableId recherche = posteDeclare();
    Operateur habilite = operateurHabiliteSur(Set.of(recherche));
    Operateur sansHabilitation = operateurHabiliteSur(Set.of());
    inTransaction(() -> operateurs.create(habilite));
    inTransaction(() -> operateurs.create(sansHabilitation));

    Page<Operateur> page = inTransaction(() -> operateurs.list(new OperateurCriteria(Optional.of(recherche)), firstPageOfTen()));

    assertThat(page.content()).containsExactly(habilite);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListOperateursSortedByIdentiteEvenWhenHomonyms() {
    long numero = COMPTEUR.incrementAndGet();
    Nom nom = new Nom("IT-tri-%06d".formatted(numero));
    Operateur second = operateurNomme(nom, new Prenom("Zoe"));
    Operateur premier = operateurNomme(nom, new Prenom("Alice"));
    inTransaction(() -> operateurs.create(second));
    inTransaction(() -> operateurs.create(premier));

    Page<Operateur> page = inTransaction(() -> operateurs.list(new OperateurCriteria(Optional.empty()), firstPageOfTen()));

    assertThat(page.content()).containsSubsequence(premier, second);
  }

  @Test
  void shouldNotReadOperateurOfAnotherTenant() {
    Operateur operateur = operateurHabiliteSur(Set.of());

    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    inTransaction(() -> operateurs.create(operateur));

    TenantSecurityContexts.authenticateOn(KATILYS);

    assertThat(inTransaction(() -> operateurs.get(operateur.id()))).isEmpty();
  }

  private PosteHabilitableId posteDeclare() {
    PosteDeTravail poste = new PosteDeTravail(
      PosteDeTravailId.newId(),
      new Libelle("IT-poste-operateur-%06d".formatted(COMPTEUR.incrementAndGet())),
      TOURNAGE
    );
    inTransaction(() -> postes.create(poste));

    return new PosteHabilitableId(poste.id().uuid());
  }

  private static Operateur operateurHabiliteSur(Set<PosteHabilitableId> habilitations) {
    return operateurHabiliteSur(habilitations, "IT-mat-%06d".formatted(COMPTEUR.incrementAndGet()));
  }

  private static Operateur operateurHabiliteSur(Set<PosteHabilitableId> habilitations, String matricule) {
    long numero = COMPTEUR.incrementAndGet();

    return Operateur.builder()
      .id(OperateurId.newId())
      .nom(new Nom("IT-nom-%06d".formatted(numero)))
      .prenom(new Prenom("IT-prenom-%06d".formatted(numero)))
      .matricule(matricule)
      .postes(habilitations);
  }

  private static Operateur operateurNomme(Nom nom, Prenom prenom) {
    return Operateur.builder()
      .id(OperateurId.newId())
      .nom(nom)
      .prenom(prenom)
      .matricule("IT-mat-%06d".formatted(COMPTEUR.incrementAndGet()))
      .postes(Set.of());
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
