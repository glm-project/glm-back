package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.clotureParLeroyA;
import static com.glm.glmback.atelier.domain.AtelierFixture.debutSurFraiseuse1ParDupontA;
import static com.glm.glmback.atelier.domain.AtelierFixture.finDe;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CategorieDElement;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.elementdefabrication.domain.Annee;
import com.glm.glmback.elementdefabrication.domain.AuteurDeLaCloture;
import com.glm.glmback.elementdefabrication.domain.Categorie;
import com.glm.glmback.elementdefabrication.domain.CycleDeVie;
import com.glm.glmback.elementdefabrication.domain.ElementDansSonCycle;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationRepository;
import com.glm.glmback.elementdefabrication.domain.ElementsDansLeurCycle;
import com.glm.glmback.elementdefabrication.domain.ElementsDansLeurCycleCriteria;
import com.glm.glmback.elementdefabrication.domain.EtatEnAtelier;
import com.glm.glmback.elementdefabrication.domain.Nom;
import com.glm.glmback.elementdefabrication.domain.PassageClos;
import com.glm.glmback.elementdefabrication.domain.PassageOuvert;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.elementdefabrication.domain.SuiviId;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La lecture par cycle, sur les vraies tables de l'atelier : les suivis y sont ecrits par le repository de l'atelier,
 * et l'etat lu est confronte a celui que juge son domaine.
 */
@IntegrationTest
class JpaElementsDansLeurCycleIT {

  private static final String IMPECCMOLD = "impeccmold";
  private static final String KATILYS = "katilys";
  private static final Categorie CATEGORIE_CYCLE = new Categorie("CYCLE");
  private static final AtomicLong COMPTEUR = new AtomicLong();
  private static final Auteur LEROY = new Auteur("leroy");

  @Autowired
  private ElementsDansLeurCycle lecture;

  @Autowired
  private ElementDeFabricationRepository elements;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void declarerLaCategorieDeTest() {
    SecurityContext contexteDuTest = SecurityContextHolder.getContext();
    for (String entreprise : List.of(IMPECCMOLD, KATILYS)) {
      TenantSecurityContexts.authenticateOn(entreprise);
      inTransaction(() ->
        entityManager
          .createNativeQuery("INSERT INTO categorie_de_produit (code, rang) VALUES (?, 1) ON CONFLICT (code) DO NOTHING")
          .setParameter(1, CATEGORIE_CYCLE.value())
          .executeUpdate()
      );
    }
    SecurityContextHolder.setContext(contexteDuTest);
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldLireChaqueCycleEtLEtatQueJugeLAtelier() {
    Instant creation = Instant.parse("2041-02-03T06:00:00Z");
    Instant engagement = creation.plusSeconds(3600);
    Instant debut = engagement.plusSeconds(3600);
    Instant evaluation = debut.plusSeconds(4 * 3600);
    ElementDeFabrication enPreparation = cree(creation.plusSeconds(6));
    SuiviDAtelier enAttente = engage(cree(creation.plusSeconds(5)), engagement);
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(debut);
    SuiviDAtelier enCours = engage(cree(creation.plusSeconds(4)), engagement).enregistre(travail);
    EvenementDAtelier arrete = debutSurFraiseuse1ParDupontA(debut);
    SuiviDAtelier interrompu = engage(cree(creation.plusSeconds(3)), engagement)
      .enregistre(arrete)
      .enregistre(finDe(arrete).a(debut.plusSeconds(60)));
    SuiviDAtelier echu = engage(cree(creation.plusSeconds(2)), debut.minusSeconds(15 * 3600)).enregistre(
      debutSurFraiseuse1ParDupontA(debut.minusSeconds(14 * 3600))
    );
    SuiviDAtelier clos = engage(cree(creation.plusSeconds(1)), engagement).cloture(clotureParLeroyA(debut));
    List.of(enAttente, enCours, interrompu, echu, clos).forEach(suivi -> inTransaction(() -> suivis.create(suivi)));

    Page<ElementDansSonCycle> page = lit(new Periode(creation, creation.plusSeconds(6)), evaluation, firstPageOfTen());

    assertThat(page.totalElementsCount()).isEqualTo(6);
    assertThat(page.content())
      .extracting(ElementDansSonCycle::cycle)
      .containsExactly(
        CycleDeVie.EN_PREPARATION,
        CycleDeVie.EN_ATELIER,
        CycleDeVie.EN_ATELIER,
        CycleDeVie.EN_ATELIER,
        CycleDeVie.EN_ATELIER,
        CycleDeVie.CLOTURE
      );
    assertThat(page.content().getFirst().element()).isEqualTo(enPreparation);
    assertThat(page.content().get(1).passage()).contains(
      new PassageOuvert(new SuiviId(enAttente.id().uuid()), engagement, EtatEnAtelier.EN_ATTENTE)
    );
    assertThat(page.content().getLast().passage()).contains(
      new PassageClos(new SuiviId(clos.id().uuid()), engagement, debut, new AuteurDeLaCloture("leroy"))
    );
    assertThat(page.content().subList(1, 5))
      .extracting(lu -> ((PassageOuvert) lu.passage().orElseThrow()).etat().name())
      .describedAs("l'etat lu est celui que juge le domaine de l'atelier")
      .containsExactly(
        enAttente.etat(evaluation).name(),
        enCours.etat(evaluation).name(),
        interrompu.etat(evaluation).name(),
        echu.etat(evaluation).name()
      )
      .containsExactly("EN_ATTENTE", "EN_COURS", "INTERROMPU", "INTERROMPU");
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldPaginerParCreationDecroissante() {
    Instant creation = Instant.parse("2041-03-04T06:00:00Z");
    ElementDeFabrication ancien = cree(creation);
    ElementDeFabrication recent = cree(creation.plusSeconds(1));
    Periode periode = new Periode(creation, creation.plusSeconds(1));

    Page<ElementDansSonCycle> premiere = lit(periode, creation, new Pageable(0, 1));
    Page<ElementDansSonCycle> seconde = lit(periode, creation, new Pageable(1, 1));

    assertThat(premiere.content()).extracting(ElementDansSonCycle::element).containsExactly(recent);
    assertThat(seconde.content()).extracting(ElementDansSonCycle::element).containsExactly(ancien);
    assertThat(seconde.totalElementsCount()).isEqualTo(2);
  }

  @Test
  void shouldNePasLireLesElementsDUneAutreEntreprise() {
    Instant creation = Instant.parse("2041-04-05T06:00:00Z");
    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    cree(creation);

    TenantSecurityContexts.authenticateOn(KATILYS);

    assertThat(lit(new Periode(creation, creation), creation, firstPageOfTen()).content()).isEmpty();
  }

  private Page<ElementDansSonCycle> lit(Periode periode, Instant evaluation, Pageable pageable) {
    return inTransaction(() -> lecture.list(new ElementsDansLeurCycleCriteria(Optional.of(periode), evaluation), pageable));
  }

  private ElementDeFabrication cree(Instant dateDeCreation) {
    long numero = COMPTEUR.incrementAndGet();
    ElementDeFabrication element = ElementDeFabrication.builder()
      .id(ElementDeFabricationId.newId())
      .categorie(CATEGORIE_CYCLE)
      .nom(Nom.of(CATEGORIE_CYCLE, new Annee(2041), numero))
      .reference("CYCLE-" + numero + "-" + dateDeCreation.toEpochMilli())
      .description("Element de la lecture par cycle")
      .dateDeCreation(dateDeCreation)
      .dateDeModification(dateDeCreation);
    return inTransaction(() -> elements.create(element));
  }

  private static SuiviDAtelier engage(ElementDeFabrication element, Instant engagement) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(
        new ElementEngage(
          new ElementEngageId(element.id().uuid()),
          new NomDElement(element.nom().value()),
          new CategorieDElement(element.categorie().value())
        )
      )
      .engagement(new Engagement(LEROY, engagement))
      .journal(JournalDAtelier.vide());
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(transaction -> action.get());
  }
}
