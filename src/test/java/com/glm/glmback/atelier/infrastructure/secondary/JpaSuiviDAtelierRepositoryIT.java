package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.CoutHoraire;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OrigineDuPointage;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SequenceEnConflit;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierCriteria;
import com.glm.glmback.atelier.domain.SuiviDAtelierDejaExistantException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TauxHoraire;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

  @Autowired
  private EntityManager entities;

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
    assertThat(relu.etat(engagement.plusSeconds(7200))).isEqualTo(EtatDAtelier.EN_COURS);
  }

  /**
   * Le cout horaire du poste et le taux horaire de l'operateur survivent au round-trip base, sur le meme patron que
   * la nature : figes a la saisie, jamais relus depuis le referentiel.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireLeCoutEtLeTauxHorairesDUnEvenement() {
    Instant engagement = Instant.parse("2040-01-06T08:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(debutSurFraiseuse1A(engagement.plusSeconds(3600)));

    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu.journal().evenements())
      .singleElement()
      .satisfies(evenement -> {
        assertThat(evenement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1);
        assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT);
      });
  }

  /**
   * Un poste non valorise doit relire une absence, pas un montant reconstitue a partir de rien.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUnEvenementSansCoutHoraireQuandLePosteNEnAPas() {
    Instant engagement = Instant.parse("2040-01-06T09:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(debutSurFraiseuse2A(engagement.plusSeconds(3600)));

    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu.journal().evenements())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.coutHoraire()).isEmpty());
  }

  /**
   * L'origine d'un fait survit au round-trip base, et c'est elle qui dit la regularisation : le pointage rejoue hors
   * ligne arrive apres coup sans en etre une, la fin regularisee a l'heure meme de sa saisie en reste une.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireLOrigineDeChaqueEvenement() {
    Instant engagement = Instant.parse("2040-01-06T10:00:00Z");
    EvenementDAtelier debut = debutRejoueHorsLigne(engagement.plusSeconds(3600), engagement.plusSeconds(7200));
    SuiviDAtelier engage = suiviEngageA(engagement)
      .enregistre(debut)
      .enregistre(finRegulariseeParLeroyDe(debut, engagement.plusSeconds(10800)));

    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu).isEqualTo(engage);
    assertThat(relu.journal().evenements())
      .extracting(EvenementDAtelier::origine)
      .containsExactly(OrigineDuPointage.POINTAGE, OrigineDuPointage.REGULARISATION);
  }

  /**
   * L'intention et les activites d'un fait survivent au round-trip base : l'ouverture porte son activite, la transition
   * la sienne et celle qu'elle remplace, la fin seulement celle qu'elle termine.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireLIntentionEtLesActivitesDeChaqueEvenement() {
    Instant engagement = Instant.parse("2040-01-06T11:00:00Z");
    EvenementDAtelier debut = debutSurFraiseuse1A(engagement.plusSeconds(3600));
    EvenementDAtelier nonConformite = passageEnNonConformiteDe(debut).a(engagement.plusSeconds(7200));
    SuiviDAtelier engage = suiviEngageA(engagement)
      .enregistre(debut)
      .enregistre(nonConformite)
      .enregistre(finDe(nonConformite).a(engagement.plusSeconds(10800)));

    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu).isEqualTo(engage);
    assertThat(relu.journal().evenements())
      .extracting(EvenementDAtelier::intention, EvenementDAtelier::activite, EvenementDAtelier::activiteVisee)
      .containsExactly(
        tuple(IntentionDePointage.OUVERTURE, Optional.of(ActiviteId.ouvertePar(debut.id())), Optional.empty()),
        tuple(
          IntentionDePointage.TRANSITION,
          Optional.of(ActiviteId.ouvertePar(nonConformite.id())),
          Optional.of(ActiviteId.ouvertePar(debut.id()))
        ),
        tuple(IntentionDePointage.FIN, Optional.empty(), Optional.of(ActiviteId.ouvertePar(nonConformite.id())))
      );
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUnEvenementSansPosteNiNature() {
    Instant engagement = Instant.parse("2040-01-07T07:00:00Z");
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(
      evenement(
        TypeDEvenementDAtelier.DEBUT,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Horodatage.saisiA(engagement.plusSeconds(3600))
      )
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

    SuiviDAtelier clotureAEnregistrer = engage.cloture(new Cloture(AUTEUR_LEROY, Horodatage.saisiA(engagement.plusSeconds(36000))));
    SuiviDAtelier cloture = inTransaction(() -> suivis.update(clotureAEnregistrer));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu).isEqualTo(cloture);
    assertThat(relu.etat(engagement.plusSeconds(36000))).isEqualTo(EtatDAtelier.CLOTURE);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldAjouterUnEvenementSansReecrireLesPrecedents() {
    Instant engagement = Instant.parse("2040-01-09T07:00:00Z");
    EvenementDAtelier debut = debutSurFraiseuse1A(engagement.plusSeconds(3600));
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(debut);
    inTransaction(() -> suivis.create(engage));

    SuiviDAtelier poursuiviAEnregistrer = engage.enregistre(finDe(debut).a(engagement.plusSeconds(7200)));
    SuiviDAtelier poursuivi = inTransaction(() -> suivis.update(poursuiviAEnregistrer));

    SuiviDAtelier relu = inTransaction(() -> suivis.get(engage.id())).orElseThrow();
    assertThat(relu).isEqualTo(poursuivi);
    assertThat(relu.etat(engagement.plusSeconds(7200))).isEqualTo(EtatDAtelier.INTERROMPU);
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

    Page<SuiviDAtelier> page = inTransaction(() -> suivis.list(criteres(new Periode(lundi, mardi), Set.of(), mardi), firstPageOfTen()));

    assertThat(page.content()).containsExactly(recent, ancien);
    assertThat(page.totalElementsCount()).isEqualTo(2);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNePasListerLesSuivisHorsPeriode() {
    Instant engagement = Instant.parse("2040-02-07T07:00:00Z");
    inTransaction(() -> suivis.create(suiviEngageA(engagement)));

    Page<SuiviDAtelier> page = inTransaction(() ->
      suivis.list(criteres(new Periode(engagement.plusSeconds(1), engagement.plusSeconds(2)), Set.of(), engagement), firstPageOfTen())
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
    Instant lecture = mardi.plusSeconds(7200);
    Page<SuiviDAtelier> ouverts = inTransaction(() ->
      suivis.list(criteres(semaine, Set.of(EtatDAtelier.EN_COURS), lecture), firstPageOfTen())
    );
    Page<SuiviDAtelier> tous = inTransaction(() -> suivis.list(criteres(semaine, Set.of(), lecture), firstPageOfTen()));

    assertThat(ouverts.content()).containsExactly(enCours);
    assertThat(tous.content()).containsExactly(enCours, enAttente);
  }

  /**
   * Un suivi dont l'activite a atteint son echeance n'est plus en cours : l'etat se juge a l'instant de la lecture, sur
   * la projection de ses activites, sans aucune ecriture depuis.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldFiltrerLEtatALInstantDeLaLecture() {
    Instant engagement = Instant.parse("2040-03-08T07:00:00Z");
    Instant echeance = engagement.plusSeconds(3600).plus(Duration.ofHours(13));
    SuiviDAtelier enCours = suiviEngageA(engagement).enregistre(debutSurFraiseuse1A(engagement.plusSeconds(3600)));
    inTransaction(() -> suivis.create(enCours));
    Periode jour = new Periode(engagement, engagement);

    assertThat(liste(jour, EtatDAtelier.EN_COURS, echeance.minusSeconds(1))).containsExactly(enCours);
    assertThat(liste(jour, EtatDAtelier.EN_COURS, echeance)).isEmpty();
    assertThat(liste(jour, EtatDAtelier.INTERROMPU, echeance)).containsExactly(enCours);
  }

  /**
   * La projection suit chaque ecriture : une fin termine l'activite qui n'a plus d'echeance a juger, une ouverture sur
   * un autre poste en ajoute une autre.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldReecrireLaProjectionDesActivitesAChaqueEcriture() {
    Instant engagement = Instant.parse("2040-03-09T07:00:00Z");
    Instant a22h = engagement.plus(Duration.ofHours(15));
    EvenementDAtelier debut = debutSurFraiseuse1A(engagement.plusSeconds(3600));
    SuiviDAtelier engage = suiviEngageA(engagement).enregistre(debut);
    inTransaction(() -> suivis.create(engage));
    Periode jour = new Periode(engagement, engagement);
    assertThat(liste(jour, EtatDAtelier.EN_COURS, a22h)).isEmpty();

    SuiviDAtelier termineAEnregistrer = engage.enregistre(finDe(debut).a(engagement.plus(Duration.ofHours(6))));
    SuiviDAtelier termine = inTransaction(() -> suivis.update(termineAEnregistrer));
    assertThat(liste(jour, EtatDAtelier.INTERROMPU, a22h)).containsExactly(termine);

    EvenementDAtelier relance = debutSurFraiseuse2A(engagement.plus(Duration.ofHours(7)));
    SuiviDAtelier relanceAEnregistrer = termine.enregistre(relance);
    SuiviDAtelier avecRelance = inTransaction(() -> suivis.update(relanceAEnregistrer));
    assertThat(liste(jour, EtatDAtelier.EN_COURS, a22h)).containsExactly(avecRelance);
  }

  /**
   * Un suivi dont la seule activite sans fin est a resoudre n'est pas en cours : la projection porte la sequence en
   * conflit, et le filtre juge l'etat sur les seules activites interpretables.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNePasCompterEnCoursUneActiviteAResoudre() {
    Instant engagement = Instant.parse("2040-03-10T07:00:00Z");
    EvenementDAtelier premiere = debutSurFraiseuse1A(engagement.plusSeconds(3600));
    SuiviDAtelier enConflit = suiviEngageA(engagement)
      .enregistre(premiere)
      .enregistre(debutSurFraiseuse1A(engagement.plusSeconds(7200)))
      .enregistre(finDe(premiere).a(engagement.plusSeconds(10800)));
    inTransaction(() -> suivis.create(enConflit));
    Periode jour = new Periode(engagement, engagement);
    Instant lecture = engagement.plus(Duration.ofHours(5));

    assertThat(liste(jour, EtatDAtelier.EN_COURS, lecture)).isEmpty();
    assertThat(liste(jour, EtatDAtelier.INTERROMPU, lecture)).containsExactly(enConflit);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListerSansAucunFiltre() {
    SuiviDAtelier engage = suiviEngageA(Instant.parse("2040-03-07T07:00:00Z"));
    inTransaction(() -> suivis.create(engage));

    Page<SuiviDAtelier> page = inTransaction(() ->
      suivis.list(new SuiviDAtelierCriteria(Optional.empty(), Set.of(), Instant.parse("2040-03-07T08:00:00Z")), firstPageOfTen())
    );

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
      suivis.list(criteres(new Periode(engagement, engagement), Set.of(), engagement), firstPageOfTen())
    );

    assertThat(chezKatilys).isEmpty();
    assertThat(pageChezKatilys.content()).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldProjeterFidelementLesSequencesEnConflit() {
    Instant engagement = Instant.parse("2041-03-10T07:00:00Z");
    EvenementDAtelier premiere = debutSurFraiseuse1A(engagement.plusSeconds(3600));
    EvenementDAtelier relance = debutSurFraiseuse1A(engagement.plusSeconds(7200));
    SuiviDAtelier enConflit = suiviEngageA(engagement)
      .enregistre(premiere)
      .enregistre(relance)
      .enregistre(finDe(premiere).a(engagement.plusSeconds(10800)));
    inTransaction(() -> suivis.create(enConflit));

    assertThat(conflitsProjetes(enConflit.id())).isEqualTo(enConflit.conflits());
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldProjeterEtReecrireLaFinAuPlusTardDuConflit() {
    Instant engagement = Instant.parse("2041-03-14T07:00:00Z");
    EvenementDAtelier premiere = debutSurFraiseuse1A(engagement.plusSeconds(3600));
    EvenementDAtelier relance = debutSurFraiseuse1A(engagement.plusSeconds(7200));
    SuiviDAtelier enConflit = suiviEngageA(engagement)
      .enregistre(premiere)
      .enregistre(relance)
      .enregistre(finDe(premiere).a(engagement.plusSeconds(10800)));
    inTransaction(() -> suivis.create(enConflit));

    assertThat(finsAuPlusTard(enConflit.id())).containsExactly(engagement.plusSeconds(50400), engagement.plusSeconds(54000));

    SuiviDAtelier closAEnregistrer = enConflit.cloture(clotureParLeroyA(engagement.plusSeconds(18000)));
    SuiviDAtelier clos = inTransaction(() -> suivis.update(closAEnregistrer));
    assertThat(finsAuPlusTard(clos.id())).containsExactly(engagement.plusSeconds(18000), engagement.plusSeconds(18000));
  }

  private List<Instant> finsAuPlusTard(SuiviDAtelierId suivi) {
    return inTransaction(() ->
      entities
        .createQuery(
          "select activite.finAuPlusTard from ActiviteDAtelierEntity activite "
            + "where activite.suivi.id = :suivi and activite.finAuPlusTard is not null order by activite.debut",
          Instant.class
        )
        .setParameter("suivi", suivi.uuid())
        .getResultList()
    );
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldReecrireLaProjectionDuConflit() {
    Instant engagement = Instant.parse("2041-03-11T07:00:00Z");
    EvenementDAtelier premiere = debutSurFraiseuse1A(engagement.plusSeconds(3600));
    EvenementDAtelier relance = debutSurFraiseuse1A(engagement.plusSeconds(7200));
    SuiviDAtelier enConflit = suiviEngageA(engagement)
      .enregistre(premiere)
      .enregistre(relance)
      .enregistre(finDe(premiere).a(engagement.plusSeconds(10800)));
    inTransaction(() -> suivis.create(enConflit));
    SuiviDAtelier encoreEnConflitAEnregistrer = enConflit.enregistre(finDe(relance).a(engagement.plusSeconds(14400)));
    SuiviDAtelier encoreEnConflit = inTransaction(() -> suivis.update(encoreEnConflitAEnregistrer));
    assertThat(conflitsProjetes(encoreEnConflit.id())).isEqualTo(encoreEnConflit.conflits());
    assertThat(inTransaction(() -> suivis.get(encoreEnConflit.id()))).contains(encoreEnConflit);
  }

  private List<SequenceEnConflit> conflitsProjetes(SuiviDAtelierId suivi) {
    return inTransaction(() ->
      lignes("select id, operateur_id, poste_id from sequence_en_conflit where suivi_id = :suivi order by id", suivi.uuid())
        .stream()
        .map(ligne -> {
          UUID sequence = (UUID) ligne[0];
          List<ActiviteId> activites = identites(
            "select id from activite_d_atelier where sequence_id = :suivi order by ordre_dans_sequence",
            sequence
          )
            .stream()
            .map(ActiviteId::new)
            .toList();
          List<EvenementDAtelierId> pointages = identites(
            "select evenement_id from pointage_en_conflit where sequence_id = :suivi order by ordre",
            sequence
          )
            .stream()
            .map(EvenementDAtelierId::new)
            .toList();
          return new SequenceEnConflit(
            new CleDActivite(new OperateurId((UUID) ligne[1]), Optional.ofNullable((UUID) ligne[2]).map(PosteDeTravailId::new)),
            activites,
            pointages
          );
        })
        .toList()
    );
  }

  @SuppressWarnings("unchecked")
  private List<Object[]> lignes(String sql, UUID identite) {
    return entities.createNativeQuery(sql).setParameter("suivi", identite).getResultList();
  }

  @SuppressWarnings("unchecked")
  private List<UUID> identites(String sql, UUID identite) {
    return entities.createNativeQuery(sql, UUID.class).setParameter("suivi", identite).getResultList();
  }

  private List<SuiviDAtelier> liste(Periode periode, EtatDAtelier etat, Instant evaluation) {
    return inTransaction(() -> suivis.list(criteres(periode, Set.of(etat), evaluation), firstPageOfTen())).content();
  }

  private static SuiviDAtelierCriteria criteres(Periode periode, Set<EtatDAtelier> etats, Instant evaluation) {
    return new SuiviDAtelierCriteria(Optional.of(periode), etats, evaluation);
  }

  private static SuiviDAtelier suiviEngageA(Instant date) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), NOM_OF_2026_000042, CATEGORIE_OF))
      .engagement(new Engagement(AUTEUR_LEROY, date))
      .journal(JournalDAtelier.vide());
  }

  private static EvenementDAtelier debutSurFraiseuse1A(Instant date) {
    return evenement(
      TypeDEvenementDAtelier.DEBUT,
      Optional.of(POSTE_ID_FRAISEUSE_1),
      Optional.of(NATURE_FRAISAGE),
      Optional.of(COUT_HORAIRE_FRAISEUSE_1),
      Optional.of(TAUX_HORAIRE_DUPONT),
      Horodatage.saisiA(date)
    );
  }

  /**
   * Fraiseuse 2 n'est pas valorisee : de quoi prouver, par le round-trip base, que l'absence de cout horaire survit
   * elle aussi a la persistance, pas seulement sa presence.
   */
  private static EvenementDAtelier debutSurFraiseuse2A(Instant date) {
    return evenement(
      TypeDEvenementDAtelier.DEBUT,
      Optional.of(POSTE_ID_FRAISEUSE_2),
      Optional.of(NATURE_FRAISAGE),
      Optional.empty(),
      Optional.of(TAUX_HORAIRE_DUPONT),
      Horodatage.saisiA(date)
    );
  }

  private static EvenementDAtelier debutRejoueHorsLigne(Instant survenue, Instant enregistrement) {
    return evenement(
      TypeDEvenementDAtelier.DEBUT,
      Optional.of(POSTE_ID_FRAISEUSE_1),
      Optional.of(NATURE_FRAISAGE),
      Optional.of(COUT_HORAIRE_FRAISEUSE_1),
      Optional.of(TAUX_HORAIRE_DUPONT),
      new Horodatage(survenue, enregistrement)
    );
  }

  private static EvenementDAtelier finRegulariseeParLeroyDe(EvenementDAtelier ouvrant, Instant date) {
    return EvenementDAtelier.builder()
      .id(EvenementDAtelierId.newId())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activite(Optional.empty())
      .activiteVisee(ouvrant.activite())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_FRAISEUSE_1))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DUPONT))
      .auteur(AUTEUR_LEROY)
      .origine(OrigineDuPointage.REGULARISATION)
      .horodatage(Horodatage.saisiA(date));
  }

  private static EvenementDAtelier evenement(
    TypeDEvenementDAtelier type,
    Optional<PosteDeTravailId> poste,
    Optional<NatureDOperation> nature,
    Optional<CoutHoraire> coutHoraire,
    Optional<TauxHoraire> tauxHoraire,
    Horodatage horodatage
  ) {
    EvenementDAtelierId id = EvenementDAtelierId.newId();

    return EvenementDAtelier.builder()
      .id(id)
      .type(type)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(poste)
      .nature(nature)
      .coutHoraire(coutHoraire)
      .tauxHoraire(tauxHoraire)
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .horodatage(horodatage);
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
