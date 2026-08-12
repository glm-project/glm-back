package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PosteDeTravail;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierCriteria;
import com.glm.glmback.atelier.domain.SuiviDAtelierDejaExistantException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDElementEngage;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Le schema est partage par toutes les methodes de cette classe et par les scenarios Cucumber : chaque test travaille
 * donc sur ses propres identifiants, et toute assertion de liste se borne a une periode qui n'appartient qu'a lui.
 */
@IntegrationTest
class JpaSuiviDAtelierRepositoryIT {

  private static final String IMPECCMOLD = "impeccmold";
  private static final String KATILYS = "katilys";

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUnSuiviEngageSansAucunEvenement() {
    SuiviDAtelier engage = suiviEngageA(Instant.parse("2040-01-05T07:00:00Z"));

    inTransaction(() -> suivis.create(engage));

    assertThat(inTransaction(() -> suivis.get(engage.id()))).contains(engage);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUnSuiviAvecSonJournalSonPosteEtSaNature() {
    Instant engagement = Instant.parse("2040-01-06T07:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(debutSurFraiseuse1A(engagement.plusSeconds(3600)));

    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu).isEqualTo(engage);
    assertThat(relu.etat()).isEqualTo(EtatDAtelier.EN_COURS);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUnEvenementSansPosteNiNature() {
    Instant engagement = Instant.parse("2040-01-07T07:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(
      evenement(TypeDEvenementDAtelier.DEBUT, Optional.empty(), Optional.empty(), Horodatage.saisiA(engagement.plusSeconds(3600)))
    );

    inTransaction(() -> suivis.create(engage));

    assertThat(inTransaction(() -> suivis.get(engage.id()))).contains(engage);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUnSuiviCloture() {
    Instant engagement = Instant.parse("2040-01-08T07:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement);
    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier cloture = engage.cloture(new Cloture(AUTEUR_LEROY, Horodatage.saisiA(engagement.plusSeconds(36000))));
    inTransaction(() -> suivis.update(cloture));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu).isEqualTo(cloture);
    assertThat(relu.etat()).isEqualTo(EtatDAtelier.CLOTURE);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldAjouterUnEvenementSansReecrireLesPrecedents() {
    Instant engagement = Instant.parse("2040-01-09T07:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(debutSurFraiseuse1A(engagement.plusSeconds(3600)));
    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier poursuivi = engage.enregistre(finSurFraiseuse1A(engagement.plusSeconds(7200)));
    inTransaction(() -> suivis.update(poursuivi));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu).isEqualTo(poursuivi);
    assertThat(relu.etat()).isEqualTo(EtatDAtelier.INTERROMPU);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldConserverUnEvenementAnnuleAuJournal() {
    Instant engagement = Instant.parse("2040-01-10T07:00:00Z");
    EvenementDAtelier debut = debutSurFraiseuse1A(engagement.plusSeconds(3600));
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(debut);
    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier annule = engage.annule(debut.id(), new Annulation(AUTEUR_LEROY, engagement.plusSeconds(10800), MOTIF_ERREUR_DE_SAISIE));
    inTransaction(() -> suivis.update(annule));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu).isEqualTo(annule);
    assertThat(relu.journal().evenements()).hasSize(1);
    assertThat(relu.etat()).isEqualTo(EtatDAtelier.EN_ATTENTE);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRefuserDeCreerDeuxFoisLaMemeIdentite() {
    SuiviDAtelier engage = suiviEngageA(Instant.parse("2040-01-11T07:00:00Z"));
    inTransaction(() -> suivis.create(engage));

    assertThatThrownBy(() -> inTransaction(() -> suivis.create(engage))).isExactlyInstanceOf(SuiviDAtelierDejaExistantException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRefuserDeMettreAJourUnSuiviInconnu() {
    SuiviDAtelier inconnu = suiviEngageA(Instant.parse("2040-01-12T07:00:00Z"));

    assertThatThrownBy(() -> inTransaction(() -> suivis.update(inconnu))).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNePasTrouverUnSuiviInconnu() {
    assertThat(inTransaction(() -> suivis.get(SuiviDAtelierId.newId()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldTrouverLeSuiviEnCoursDeSonElement() {
    SuiviDAtelier engage = suiviEngageA(Instant.parse("2040-01-13T07:00:00Z"));
    inTransaction(() -> suivis.create(engage));

    assertThat(inTransaction(() -> suivis.getEnCoursPour(engage.element().id()))).contains(engage);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNePasTrouverDeSuiviEnCoursPourUnElementCloture() {
    Instant engagement = Instant.parse("2040-01-14T07:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement);
    inTransaction(() -> suivis.create(engage));
    inTransaction(() -> suivis.update(engage.cloture(new Cloture(AUTEUR_LEROY, Horodatage.saisiA(engagement.plusSeconds(36000))))));

    assertThat(inTransaction(() -> suivis.getEnCoursPour(engage.element().id()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNePasTrouverDeSuiviEnCoursPourUnElementJamaisEngage() {
    assertThat(inTransaction(() -> suivis.getEnCoursPour(new ElementEngageId(UUID.randomUUID())))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListerLesSuivisDUnePeriodeDuPlusRecentAuPlusAncien() {
    Instant lundi = Instant.parse("2040-02-05T07:00:00Z");
    Instant mardi = Instant.parse("2040-02-06T07:00:00Z");
    SuiviDAtelier ancien = suiviEngageA(lundi);
    SuiviDAtelier recent = suiviEngageA(mardi);
    inTransaction(() -> suivis.create(ancien));
    inTransaction(() -> suivis.create(recent));

    Page<SuiviDAtelier> page = inTransaction(() -> suivis.list(criteres(new Periode(lundi, mardi), Set.of()), firstPageOfTen()));

    assertThat(page.content()).containsExactly(recent, ancien);
    assertThat(page.totalElementsCount()).isEqualTo(2);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNePasListerLesSuivisHorsPeriode() {
    Instant engagement = Instant.parse("2040-02-07T07:00:00Z");
    inTransaction(() -> suivis.create(suiviEngageA(engagement)));

    Page<SuiviDAtelier> page = inTransaction(() ->
      suivis.list(criteres(new Periode(engagement.plusSeconds(1), engagement.plusSeconds(2)), Set.of()), firstPageOfTen())
    );

    assertThat(page.content()).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldFiltrerLesSuivisDUnePeriodeParEtat() {
    Instant lundi = Instant.parse("2040-03-05T07:00:00Z");
    Instant mardi = Instant.parse("2040-03-06T07:00:00Z");
    SuiviDAtelier enAttente = suiviEngageA(lundi);
    SuiviDAtelier enCours = suiviEngageA(mardi).enregistre(debutSurFraiseuse1A(mardi.plusSeconds(3600)));
    inTransaction(() -> suivis.create(enAttente));
    inTransaction(() -> suivis.create(enCours));

    Periode semaine = new Periode(lundi, mardi);
    Page<SuiviDAtelier> ouverts = inTransaction(() -> suivis.list(criteres(semaine, Set.of(EtatDAtelier.EN_COURS)), firstPageOfTen()));
    Page<SuiviDAtelier> tous = inTransaction(() -> suivis.list(criteres(semaine, Set.of()), firstPageOfTen()));

    assertThat(ouverts.content()).containsExactly(enCours);
    assertThat(tous.content()).containsExactly(enCours, enAttente);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListerSansAucunFiltre() {
    SuiviDAtelier engage = suiviEngageA(Instant.parse("2040-03-07T07:00:00Z"));
    inTransaction(() -> suivis.create(engage));

    Page<SuiviDAtelier> page = inTransaction(() -> suivis.list(new SuiviDAtelierCriteria(Optional.empty(), Set.of()), firstPageOfTen()));

    assertThat(page.totalElementsCount()).isPositive();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRefuserUneSaisieCalculeeSurUnJournalPerime() {
    Instant engagement = Instant.parse("2040-04-05T07:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement);
    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier premiereSaisie = engage.enregistre(debutSurFraiseuse1A(engagement.plusSeconds(3600)));
    SuiviDAtelier secondeSaisie = engage.enregistre(debutSurFraiseuse2A(engagement.plusSeconds(3600)));
    inTransaction(() -> suivis.update(premiereSaisie));

    assertThatThrownBy(() -> inTransaction(() -> suivis.update(secondeSaisie))).isExactlyInstanceOf(SaisieConcurrenteException.class);
  }

  @Test
  void shouldNePasLireLeSuiviDUneAutreEntreprise() {
    Instant engagement = Instant.parse("2040-05-05T07:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement);

    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    inTransaction(() -> suivis.create(engage));

    TenantSecurityContexts.authenticateOn(KATILYS);
    Optional<SuiviDAtelier> chezKatilys = inTransaction(() -> suivis.get(engage.id()));
    Page<SuiviDAtelier> pageChezKatilys = inTransaction(() ->
      suivis.list(criteres(new Periode(engagement, engagement), Set.of()), firstPageOfTen())
    );

    assertThat(chezKatilys).isEmpty();
    assertThat(pageChezKatilys.content()).isEmpty();
  }

  private static SuiviDAtelierCriteria criteres(Periode periode, Set<EtatDAtelier> etats) {
    return new SuiviDAtelierCriteria(Optional.of(periode), etats);
  }

  private static SuiviDAtelier suiviEngageA(Instant date) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), NOM_OF_2026_000042, TypeDElementEngage.ORDRE_DE_FABRICATION))
      .engagement(new Engagement(AUTEUR_LEROY, date))
      .journal(JournalDAtelier.vide());
  }

  private static EvenementDAtelier debutSurFraiseuse1A(Instant date) {
    return evenement(TypeDEvenementDAtelier.DEBUT, Optional.of(POSTE_FRAISEUSE_1), Optional.of(NATURE_FRAISAGE), Horodatage.saisiA(date));
  }

  private static EvenementDAtelier debutSurFraiseuse2A(Instant date) {
    return evenement(TypeDEvenementDAtelier.DEBUT, Optional.of(POSTE_FRAISEUSE_2), Optional.of(NATURE_FRAISAGE), Horodatage.saisiA(date));
  }

  private static EvenementDAtelier finSurFraiseuse1A(Instant date) {
    return evenement(TypeDEvenementDAtelier.FIN, Optional.of(POSTE_FRAISEUSE_1), Optional.of(NATURE_FRAISAGE), Horodatage.saisiA(date));
  }

  private static EvenementDAtelier evenement(
    TypeDEvenementDAtelier type,
    Optional<PosteDeTravail> poste,
    Optional<NatureDOperation> nature,
    Horodatage horodatage
  ) {
    return EvenementDAtelier.builder()
      .id(EvenementDAtelierId.newId())
      .type(type)
      .operateur(OPERATEUR_DUPONT)
      .poste(poste)
      .nature(nature)
      .auteur(AUTEUR_DUPONT)
      .horodatage(horodatage);
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
