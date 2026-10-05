package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.ConflitsFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Activite;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import com.glm.glmback.atelier.domain.gestionanomalies.AnomaliesDAtelierCriteria;
import com.glm.glmback.atelier.domain.gestionanomalies.FinAutomatiqueEnListe;
import com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesDAtelier;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ListeDesFinsAutomatiquesDAtelierIT {

  private static final Instant LE_9_JANVIER_2043_A = Instant.parse("2043-01-09T00:00:00Z");
  private static final long HEURE = 3600;

  @Autowired
  private FinsAutomatiquesDAtelier finsAutomatiques;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entities;

  @Test
  @WithTenant("impeccmold")
  void shouldJugerLEcheanceExactementALInstantDEvaluationBorneComprise() {
    Instant debut = Instant.parse("2044-03-01T08:00:00.123456789Z");
    Instant echeance = Instant.parse("2044-03-01T21:00:00.123456789Z");
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(debut);
    SuiviDAtelier suivi = suiviEngageLe1erJanvier2025Pour(elementDeFinAutomatiqueNomme("FINAUTO_BORNE_2044")).enregistre(ouvrant);
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));
    var criteria = new AnomaliesDAtelierCriteria("", suivi.element().id().uuid().toString());

    var veille = lit(criteria, echeance.minusNanos(1), new Pageable(0, 5));
    var pile = lit(criteria, echeance, new Pageable(0, 5));
    var apres = lit(criteria, echeance.plusNanos(1), new Pageable(0, 5));

    assertThat(veille.totalElementsCount()).isZero();
    assertThat(veille.content()).isEmpty();
    assertThat(pile.totalElementsCount()).isEqualTo(1);
    assertThat(pile.content())
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.adresse().suivi()).isEqualTo(suivi.id());
        assertThat(ligne.adresse().pointage()).isEqualTo(ouvrant.id());
        assertThat(ligne.activite()).isEqualTo(ouvrant.activite().orElseThrow());
        assertThat(ligne.revision()).isEqualTo(suivi.revision());
        assertThat(ligne.element()).isEqualTo(suivi.element());
        assertThat(ligne.cle()).isEqualTo(ouvrant.cle());
        assertThat(ligne.debut()).isEqualTo(debut);
        assertThat(ligne.echeance()).isEqualTo(echeance);
      });
    assertThat(apres.content()).isEqualTo(pile.content());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldExclureLEnCoursLAResoudreEtLesActivitesTerminees() {
    var element = elementDeFinAutomatiqueNomme("FINAUTO_EXCLUSIONS_2026");
    var tardive = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var enCours = debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_8H);
    var aResoudre = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var reelle = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var regularisee = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var cloturee = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var suiviTardif = suiviEngageLe1erJanvier2025Pour(element)
      .enregistre(tardive)
      .enregistre(finDe(tardive).a(Instant.parse("2026-05-10T23:00:00Z")));
    var suiviEnCours = suiviEngageLe1erJanvier2025Pour(element).enregistre(enCours);
    var suiviAResoudre = suiviEngageLe1erJanvier2025Pour(element)
      .enregistre(aResoudre)
      .enregistre(finDe(aResoudre).a(LE_10_MAI_2026_A_9H))
      .enregistre(finDe(aResoudre).a(LE_10_MAI_2026_A_12H));
    var suiviReel = suiviEngageLe1erJanvier2025Pour(element).enregistre(reelle).enregistre(finDe(reelle).a(LE_10_MAI_2026_A_9H));
    var suiviRegularise = suiviEngageLe1erJanvier2025Pour(element)
      .enregistre(regularisee)
      .enregistre(finRegulariseeParLeroyDe(regularisee).a(LE_10_MAI_2026_A_17H));
    var suiviCloture = suiviEngageLe1erJanvier2025Pour(element).enregistre(cloturee).cloture(clotureParLeroyA(LE_10_MAI_2026_A_20H));
    transactions.executeWithoutResult(transaction ->
      List.of(suiviTardif, suiviEnCours, suiviAResoudre, suiviReel, suiviRegularise, suiviCloture).forEach(suivis::create)
    );

    var page = lit(new AnomaliesDAtelierCriteria("", element.id().uuid().toString()), LE_11_MAI_2026_A_9H, new Pageable(0, 10));

    assertThat(page.totalElementsCount()).isEqualTo(1);
    assertThat(page.content())
      .extracting(ligne -> ligne.adresse().suivi())
      .containsExactly(suiviTardif.id());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldPaginerEtGarderLeTotalSurUnePageVide() {
    Instant debut = Instant.parse("2044-03-03T08:00:00Z");
    var element = elementDeFinAutomatiqueNomme("FINAUTO_PAGES_2044");
    var premier = debutSurFraiseuse1ParDupontA(debut);
    var second = debutSurFraiseuse1ParDupontA(debut.plusSeconds(60));
    var premierSuivi = suiviEngageLe1erJanvier2025Pour(element).enregistre(premier);
    var secondSuivi = suiviEngageLe1erJanvier2025Pour(element).enregistre(second);
    transactions.executeWithoutResult(transaction -> {
      suivis.create(secondSuivi);
      suivis.create(premierSuivi);
    });
    var criteria = new AnomaliesDAtelierCriteria("", element.id().uuid().toString());
    var evaluation = debut.plusSeconds(24 * 3600);

    var premiere = lit(criteria, evaluation, new Pageable(0, 1));
    var suivante = lit(criteria, evaluation, new Pageable(1, 1));
    var vide = lit(criteria, evaluation, new Pageable(2, 1));

    assertThat(premiere.totalElementsCount()).isEqualTo(2);
    assertThat(premiere.content())
      .extracting(ligne -> ligne.adresse().pointage())
      .containsExactly(premier.id());
    assertThat(suivante.totalElementsCount()).isEqualTo(2);
    assertThat(suivante.content())
      .extracting(ligne -> ligne.adresse().pointage())
      .containsExactly(second.id());
    assertThat(vide.totalElementsCount()).isEqualTo(2);
    assertThat(vide.content()).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldTrierParNanosecondePuisSuiviPuisOuvrantCanoniquesDesDeuxMoities() {
    Instant debut = Instant.parse("2044-03-04T08:00:00.123456789Z");
    var element = elementDeFinAutomatiqueNomme("FINAUTO_ORDRE_2044");
    var suiviA = new SuiviDAtelierId(UUID.fromString("00000000-0000-0000-8000-0000000000a1"));
    var suiviB = new SuiviDAtelierId(UUID.fromString("80000000-0000-0000-0000-0000000000a1"));
    var suiviC = new SuiviDAtelierId(UUID.fromString("ffffffff-0000-0000-0000-0000000000a1"));
    var ouvrantA1 = ouvrantSurSonPoste("80000000-0000-0000-0000-0000000000b1", debut.plusNanos(1));
    var ouvrantA2 = ouvrantSurSonPoste("00000000-0000-0000-8000-0000000000b1", debut.plusNanos(1));
    var ouvrantB = ouvrantSurSonPoste("00000000-0000-0000-0000-0000000000b2", debut.plusNanos(1));
    var ouvrantC = ouvrantSurSonPoste("00000000-0000-0000-0000-0000000000b3", debut);
    transactions.executeWithoutResult(transaction -> {
      suivis.create(suiviIdentifieEngageLe1erJanvier2025(suiviB, element).enregistre(ouvrantB));
      suivis.create(suiviIdentifieEngageLe1erJanvier2025(suiviA, element).enregistre(ouvrantA1).enregistre(ouvrantA2));
      suivis.create(suiviIdentifieEngageLe1erJanvier2025(suiviC, element).enregistre(ouvrantC));
    });
    var criteria = new AnomaliesDAtelierCriteria("", element.id().uuid().toString());
    var evaluation = debut.plusSeconds(24 * 3600);

    var premiere = lit(criteria, evaluation, new Pageable(0, 2));
    var seconde = lit(criteria, evaluation, new Pageable(1, 2));

    assertThat(premiere.totalElementsCount()).isEqualTo(4);
    assertThat(premiere.content())
      .extracting(ligne -> ligne.adresse().pointage())
      .containsExactly(ouvrantC.id(), ouvrantA2.id());
    assertThat(seconde.content())
      .extracting(ligne -> ligne.adresse().pointage())
      .containsExactly(ouvrantA1.id(), ouvrantB.id());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldCombinerLesRecherchesPartiellesSansCasseAvantLaPagination() {
    var anna = operateurAnnaFinAutomatiquePourcent();
    var zoe = operateurZoeAutre2044();
    transactions.executeWithoutResult(transaction -> {
      insere(anna);
      insere(zoe);
    });
    Instant debut = Instant.parse("2043-01-09T08:00:00Z");
    var premier = debutDu9Janvier2043A8hPar(anna.id());
    var second = debutDu9Janvier2043A8hPar(zoe.id());
    var troisieme = debutDu9Janvier2043A8hPar(anna.id());
    var cherche = suiviEngageLe1erJanvier2025Pour(elementDeFinAutomatiqueNomme("FINAUTO_FILTRE_2044_%A")).enregistre(premier);
    var autreOperateur = suiviEngageLe1erJanvier2025Pour(elementDeFinAutomatiqueNomme("FINAUTO_FILTRE_2044_%B")).enregistre(second);
    var autreElement = suiviEngageLe1erJanvier2025Pour(elementDeFinAutomatiqueNomme("AUTRE_FINAUTO_2044")).enregistre(troisieme);
    transactions.executeWithoutResult(transaction -> List.of(cherche, autreOperateur, autreElement).forEach(suivis::create));
    var evaluation = debut.plusSeconds(24 * 3600);

    var page = lit(new AnomaliesDAtelierCriteria("aNnA fIn_%", "fInAuTo_fIlTrE_2044_%"), evaluation, new Pageable(0, 1));
    var suivante = lit(new AnomaliesDAtelierCriteria("aNnA fIn_%", "fInAuTo_fIlTrE_2044_%"), evaluation, new Pageable(1, 1));
    var parIdentifiant = lit(
      new AnomaliesDAtelierCriteria(anna.id().uuid().toString().substring(0, 12).toUpperCase(java.util.Locale.ROOT), "FINAUTO_FILTRE_2044"),
      evaluation,
      new Pageable(0, 5)
    );
    var parElementId = lit(
      new AnomaliesDAtelierCriteria("", autreElement.element().id().uuid().toString()),
      evaluation,
      new Pageable(0, 5)
    );

    assertThat(page.totalElementsCount()).isEqualTo(1);
    assertThat(page.content())
      .extracting(ligne -> ligne.adresse().suivi())
      .containsExactly(cherche.id());
    assertThat(suivante.totalElementsCount()).isEqualTo(1);
    assertThat(suivante.content()).isEmpty();
    assertThat(parIdentifiant.content())
      .extracting(ligne -> ligne.adresse().suivi())
      .containsExactly(cherche.id());
    assertThat(parElementId.content())
      .extracting(ligne -> ligne.adresse().suivi())
      .containsExactly(autreElement.id());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRechercherPourcentSoulignementEtAntislashCommeDuTexteLitteral() {
    Instant debut = Instant.parse("2044-03-06T08:00:00Z");
    var cherche = suiviEngageLe1erJanvier2025Pour(elementLitteralePourcentSoulignementAntislash2043()).enregistre(
      debutSurFraiseuse1ParDupontA(debut)
    );
    var autre = suiviEngageLe1erJanvier2025Pour(elementLitteraleXX2043()).enregistre(debutSurFraiseuse1ParDupontA(debut));
    transactions.executeWithoutResult(transaction -> {
      suivis.create(cherche);
      suivis.create(autre);
    });

    var page = lit(new AnomaliesDAtelierCriteria("", "lItTeRaLe_%\\2043"), debut.plusSeconds(24 * 3600), new Pageable(0, 5));

    assertThat(page.totalElementsCount()).isEqualTo(1);
    assertThat(page.content())
      .extracting(ligne -> ligne.adresse().suivi())
      .containsExactly(cherche.id());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldLireUneActiviteSansPosteEtGarderLeSuiviCloturePosterieurALEcheance() {
    Instant debut = Instant.parse("2044-03-07T08:00:00Z");
    var ouvrant = debutSansPosteParDupontA(debut);
    var suivi = suiviEngageLe1erJanvier2025Pour(elementDeFinAutomatiqueNomme("FINAUTO_SANS_POSTE_2044"))
      .enregistre(ouvrant)
      .cloture(clotureParLeroyA(debut.plusSeconds(22 * 3600)));
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));

    var page = lit(
      new AnomaliesDAtelierCriteria("", suivi.element().id().uuid().toString()),
      debut.plusSeconds(24 * 3600),
      new Pageable(0, 5)
    );

    assertThat(page.content())
      .singleElement()
      .satisfies(ligne -> assertThat(ligne.cle().poste()).isEmpty());
  }

  /**
   * La population du SQL est celle que la regle du domaine juge terminee automatiquement : {@code Activite.a} pour
   * l'activite, {@code AnomaliesDAtelierCriteria.matches} pour les recherches. Le jeu couvre chaque facon d'ouvrir ou
   * de terminer une activite, et chaque instant d'evaluation est pris de part et d'autre d'une echeance, a la
   * nanoseconde.
   */
  @Test
  @WithTenant("impeccmold")
  void shouldListerLesMemesActivitesQueLaRegleDuDomaineJugeTermineesAutomatiquement() {
    var element = elementDeFinAutomatiqueNomme("FINAUTO_PARITE_2043_%A");
    var berthe = operateurBerthe2043();
    var charles = operateurCharles2043();
    transactions.executeWithoutResult(transaction -> {
      insere(berthe);
      insere(charles);
    });
    var ouverteParTransition = debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE));
    var transition = passageEnNonConformiteDe(ouverteParTransition).a(LE_9_JANVIER_2043_A.plusSeconds(12 * HEURE));
    var corrigee = debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE));
    var aResoudre = debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE));
    var terminee = debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE));
    var finTardive = debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE));
    var clotureeAvant = debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE));
    var clotureeApres = debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE));
    var annulee = debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE));
    var jeu = List.of(
      suiviEngageLe1erJanvier2025Pour(element).enregistre(debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(8 * HEURE))),
      suiviEngageLe1erJanvier2025Pour(element).enregistre(ouverteParTransition).enregistre(transition),
      suiviEngageLe1erJanvier2025Pour(element)
        .enregistre(corrigee)
        .corrige(corrigee.id(), annulationParLeroy(), debutSurFraiseuse1ParDupontA(LE_9_JANVIER_2043_A.plusSeconds(9 * HEURE))),
      suiviEngageLe1erJanvier2025Pour(element)
        .enregistre(aResoudre)
        .enregistre(finDe(aResoudre).a(LE_9_JANVIER_2043_A.plusSeconds(9 * HEURE)))
        .enregistre(finDe(aResoudre).a(LE_9_JANVIER_2043_A.plusSeconds(12 * HEURE))),
      suiviEngageLe1erJanvier2025Pour(element)
        .enregistre(terminee)
        .enregistre(finDe(terminee).a(LE_9_JANVIER_2043_A.plusSeconds(9 * HEURE))),
      suiviEngageLe1erJanvier2025Pour(element)
        .enregistre(finTardive)
        .enregistre(finDe(finTardive).a(LE_9_JANVIER_2043_A.plusSeconds(23 * HEURE))),
      suiviEngageLe1erJanvier2025Pour(element)
        .enregistre(clotureeAvant)
        .cloture(clotureParLeroyA(LE_9_JANVIER_2043_A.plusSeconds(20 * HEURE))),
      suiviEngageLe1erJanvier2025Pour(element)
        .enregistre(clotureeApres)
        .cloture(clotureParLeroyA(LE_9_JANVIER_2043_A.plusSeconds(22 * HEURE))),
      suiviEngageLe1erJanvier2025Pour(element).enregistre(debutSansPosteParDupontA(LE_9_JANVIER_2043_A.plusSeconds(10 * HEURE))),
      suiviEngageLe1erJanvier2025Pour(element).enregistre(debutDu9Janvier2043A8hPar(berthe.id())),
      suiviEngageLe1erJanvier2025Pour(element).enregistre(debutDu9Janvier2043A8hPar(charles.id())),
      suiviEngageLe1erJanvier2025Pour(element).enregistre(annulee).annule(annulee.id(), annulationParLeroy())
    );
    transactions.executeWithoutResult(transaction -> jeu.forEach(suivis::create));
    var annuaire = new AnnuaireDAtelier(Map.of(berthe.id(), berthe, charles.id(), charles), Map.of());
    var recherches = List.of(
      new AnomaliesDAtelierCriteria("", element.id().uuid().toString()),
      new AnomaliesDAtelierCriteria("", "finauto_parite_2043_%a"),
      new AnomaliesDAtelierCriteria("bErThE", "FINAUTO_PARITE_2043"),
      new AnomaliesDAtelierCriteria("CHARLES", "FINAUTO_PARITE_2043"),
      new AnomaliesDAtelierCriteria("berthe parite_%", "FINAUTO_PARITE_2043"),
      new AnomaliesDAtelierCriteria("33333333", "FINAUTO_PARITE_2043"),
      new AnomaliesDAtelierCriteria(berthe.id().uuid().toString().substring(0, 12).toUpperCase(Locale.ROOT), "FINAUTO_PARITE_2043"),
      new AnomaliesDAtelierCriteria("ABSENT", "FINAUTO_PARITE_2043"),
      new AnomaliesDAtelierCriteria("", "FINAUTO_PARITE_ABSENT")
    );
    var evaluations = List.of(
      LE_9_JANVIER_2043_A.plusSeconds(7 * HEURE),
      LE_9_JANVIER_2043_A.plusSeconds(21 * HEURE).minusNanos(1),
      LE_9_JANVIER_2043_A.plusSeconds(21 * HEURE),
      LE_9_JANVIER_2043_A.plusSeconds(22 * HEURE),
      LE_9_JANVIER_2043_A.plusSeconds(23 * HEURE),
      LE_9_JANVIER_2043_A.plusSeconds(25 * HEURE).minusNanos(1),
      LE_9_JANVIER_2043_A.plusSeconds(25 * HEURE),
      LE_9_JANVIER_2043_A.plusSeconds(40 * HEURE)
    );
    var ordreCanonique = Comparator.comparing(FinAutomatiqueEnListe::debut)
      .thenComparing(ligne -> ligne.adresse().suivi().uuid().toString())
      .thenComparing(ligne -> ligne.adresse().pointage().uuid().toString());
    for (var evaluation : evaluations) {
      for (var recherche : recherches) {
        var attendues = jeu
          .stream()
          .flatMap(suivi ->
            suivi
              .activites()
              .stream()
              .filter(activite -> activite.a(evaluation).finAutomatique())
              .map(activite -> ligneDe(suivi, activite))
          )
          .filter(ligne -> recherche.matches(ligne, annuaire))
          .sorted(ordreCanonique)
          .toList();
        var acquises = lit(recherche, evaluation, new Pageable(0, 50));
        assertThat(acquises.totalElementsCount()).describedAs("total a %s pour %s", evaluation, recherche).isEqualTo(attendues.size());
        assertThat(acquises.content()).describedAs("lignes a %s pour %s", evaluation, recherche).containsExactlyElementsOf(attendues);
      }
    }
    var toutes = recherches.getFirst();
    assertThat(
      evaluations
        .stream()
        .map(evaluation -> lit(toutes, evaluation, new Pageable(0, 50)).totalElementsCount())
        .toList()
    )
      .describedAs("le jeu change de population a chaque echeance")
      .containsExactly(0L, 0L, 5L, 6L, 7L, 7L, 8L, 8L);
  }

  private Page<FinAutomatiqueEnListe> lit(AnomaliesDAtelierCriteria criteria, Instant evaluation, Pageable pageable) {
    return transactions.execute(transaction -> finsAutomatiques.list(criteria, evaluation, pageable));
  }

  private static FinAutomatiqueEnListe ligneDe(SuiviDAtelier suivi, Activite activite) {
    return FinAutomatiqueEnListe.builder()
      .adresse(new AdresseDossierAnomalie(suivi.id(), activite.ouvrant().id()))
      .revision(suivi.revision())
      .element(suivi.element())
      .cle(activite.cle())
      .activite(activite.id())
      .debut(activite.debut())
      .echeance(activite.echeance().value());
  }

  private static EvenementDAtelier ouvrantSurSonPoste(String uuid, Instant debut) {
    var id = new EvenementDAtelierId(UUID.fromString(uuid));
    return debutIdentifieSurUnPosteDeMemeUuid(id).horodatage(Horodatage.saisiA(debut));
  }

  private void insere(OperateurConnu operateur) {
    entities
      .createNativeQuery("insert into operateur (id, nom, prenom) values (:id, :nom, :prenom)")
      .setParameter("id", operateur.id().uuid())
      .setParameter("nom", operateur.nom().value())
      .setParameter("prenom", operateur.prenom().value())
      .executeUpdate();
  }
}
