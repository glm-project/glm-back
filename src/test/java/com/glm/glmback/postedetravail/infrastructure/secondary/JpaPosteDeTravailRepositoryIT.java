package com.glm.glmback.postedetravail.infrastructure.secondary;

import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.postedetravail.domain.CoutHoraire;
import com.glm.glmback.postedetravail.domain.Libelle;
import com.glm.glmback.postedetravail.domain.NatureDeTravail;
import com.glm.glmback.postedetravail.domain.NatureDuPoste;
import com.glm.glmback.postedetravail.domain.PosteDeTravail;
import com.glm.glmback.postedetravail.domain.PosteDeTravailCriteria;
import com.glm.glmback.postedetravail.domain.PosteDeTravailDejaExistantException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailId;
import com.glm.glmback.postedetravail.domain.PosteDeTravailIntrouvableException;
import com.glm.glmback.postedetravail.domain.PosteDeTravailRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class JpaPosteDeTravailRepositoryIT {

  private static final String IMPECCMOLD = "impeccmold";
  private static final String KATILYS = "katilys";
  private static final AtomicLong COMPTEUR = new AtomicLong();

  @Autowired
  private PosteDeTravailRepository postes;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entities;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldCreateAndGetPosteDeTravail() {
    PosteDeTravail poste = posteDeTournage();

    inTransaction(() -> postes.create(poste));

    assertThat(inTransaction(() -> postes.get(poste.id()))).contains(poste);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldCreateAndGetPosteDeTravailWithCoutHoraire() {
    PosteDeTravail poste = new PosteDeTravail(
      PosteDeTravailId.newId(),
      libelleDeTest(),
      nature("tournage"),
      Optional.of(new CoutHoraire(new BigDecimal("45.50")))
    );

    inTransaction(() -> postes.create(poste));

    assertThat(inTransaction(() -> postes.get(poste.id()))).contains(poste);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotCreateAlreadyExistingPosteDeTravail() {
    PosteDeTravail poste = posteDeTournage();
    inTransaction(() -> postes.create(poste));

    assertThatThrownBy(() -> inTransaction(() -> postes.create(poste))).isExactlyInstanceOf(PosteDeTravailDejaExistantException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotGetUnknownPosteDeTravail() {
    assertThat(inTransaction(() -> postes.get(PosteDeTravailId.newId()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldUpdateExistingPosteDeTravail() {
    PosteDeTravail poste = posteDeTournage();
    inTransaction(() -> postes.create(poste));

    PosteDeTravail revise = poste.revise(libelleDeTest(), nature("soudage"), Optional.empty());
    inTransaction(() -> postes.update(revise));

    assertThat(inTransaction(() -> postes.get(poste.id()))).contains(revise);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotUpdateUnknownPosteDeTravail() {
    PosteDeTravail inconnu = posteDeTournage();

    assertThatThrownBy(() -> inTransaction(() -> postes.update(inconnu))).isExactlyInstanceOf(PosteDeTravailIntrouvableException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldDeleteExistingPosteDeTravail() {
    PosteDeTravail poste = posteDeTournage();
    inTransaction(() -> postes.create(poste));

    inTransaction(() -> {
      postes.delete(poste.id());

      return null;
    });

    assertThat(inTransaction(() -> postes.get(poste.id()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotDeleteUnknownPosteDeTravail() {
    PosteDeTravailId inconnu = PosteDeTravailId.newId();

    assertThatThrownBy(() ->
      inTransaction(() -> {
        postes.delete(inconnu);

        return null;
      })
    ).isExactlyInstanceOf(PosteDeTravailIntrouvableException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldGetIdOfPosteDeTravailHoldingLibelle() {
    PosteDeTravail poste = posteDeTournage();
    inTransaction(() -> postes.create(poste));

    assertThat(inTransaction(() -> postes.idPourLibelle(poste.libelle()))).contains(poste.id());
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNotGetIdOfUnusedLibelle() {
    assertThat(inTransaction(() -> postes.idPourLibelle(new Libelle("IT-libelle-inconnu")))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListPostesDeTravailOfExpectedNatureSortedByLibelle() {
    PosteDeTravail tournage = posteDeTournage();
    NatureDuPoste soudure = nature("soudage");
    PosteDeTravail soudage = new PosteDeTravail(PosteDeTravailId.newId(), libelleDeTest(), soudure, Optional.empty());
    inTransaction(() -> postes.create(tournage));
    inTransaction(() -> postes.create(soudage));

    Page<PosteDeTravail> page = inTransaction(() ->
      postes.list(new PosteDeTravailCriteria(Optional.of(soudure.libelle()), Optional.empty()), firstPageOfTen())
    );

    assertThat(page.content()).contains(soudage).doesNotContain(tournage);
  }

  /**
   * La nature de ces deux postes leur est propre : sans elle, la page de dix serait remplie par tout ce que les autres
   * tests ont laisse dans le schema, et le tri ne serait pas observable.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListPostesDeTravailSortedByLibelle() {
    long numero = COMPTEUR.incrementAndGet();
    NatureDuPoste natureDuTest = nature("IT-tri-%06d".formatted(numero));
    PosteDeTravail second = new PosteDeTravail(
      PosteDeTravailId.newId(),
      new Libelle("IT-tri-%06d-b".formatted(numero)),
      natureDuTest,
      Optional.empty()
    );
    PosteDeTravail premier = new PosteDeTravail(
      PosteDeTravailId.newId(),
      new Libelle("IT-tri-%06d-a".formatted(numero)),
      natureDuTest,
      Optional.empty()
    );
    inTransaction(() -> postes.create(second));
    inTransaction(() -> postes.create(premier));

    Page<PosteDeTravail> page = inTransaction(() ->
      postes.list(new PosteDeTravailCriteria(Optional.empty(), Optional.of(natureDuTest.id())), firstPageOfTen())
    );

    assertThat(page.content()).containsExactly(premier, second);
  }

  @Test
  void shouldNotReadPosteDeTravailOfAnotherTenant() {
    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    PosteDeTravail poste = posteDeTournage();
    inTransaction(() -> postes.create(poste));

    TenantSecurityContexts.authenticateOn(KATILYS);

    assertThat(inTransaction(() -> postes.get(poste.id()))).isEmpty();
  }

  @Test
  void shouldReuseSameLibelleInEachTenant() {
    Libelle partage = libelleDeTest();

    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    PosteDeTravail chezImpeccMold = new PosteDeTravail(PosteDeTravailId.newId(), partage, nature("tournage"), Optional.empty());
    inTransaction(() -> postes.create(chezImpeccMold));

    TenantSecurityContexts.authenticateOn(KATILYS);
    PosteDeTravail chezKatilys = new PosteDeTravail(PosteDeTravailId.newId(), partage, nature("tournage"), Optional.empty());
    inTransaction(() -> postes.create(chezKatilys));

    assertThat(inTransaction(() -> postes.get(chezKatilys.id()))).contains(chezKatilys);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldReadCurrentLibelleOfItsNature() {
    PosteDeTravail poste = new PosteDeTravail(
      PosteDeTravailId.newId(),
      libelleDeTest(),
      nature("IT-renommee-%06d".formatted(COMPTEUR.incrementAndGet())),
      Optional.empty()
    );
    inTransaction(() -> postes.create(poste));

    inTransaction(() ->
      entities
        .createNativeQuery("update nature_de_travail set libelle = 'IT-renommee', cle = :cle where id = :id")
        .setParameter("cle", "it-renommee-" + poste.id().uuid())
        .setParameter("id", poste.nature().id().uuid())
        .executeUpdate()
    );

    assertThat(
      inTransaction(() -> postes.get(poste.id()))
        .orElseThrow()
        .nature()
    ).isEqualTo(new NatureDuPoste(poste.nature().id(), new NatureDeTravail("IT-renommee")));
  }

  private NatureDuPoste nature(String libelle) {
    return NaturesEnBase.nature(entities, transactions, libelle);
  }

  private PosteDeTravail posteDeTournage() {
    return new PosteDeTravail(PosteDeTravailId.newId(), libelleDeTest(), nature("tournage"), Optional.empty());
  }

  private static Libelle libelleDeTest() {
    return new Libelle("IT-poste-%06d".formatted(COMPTEUR.incrementAndGet()));
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
