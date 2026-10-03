package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.ConflitsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.ConflitsDAtelier;
import com.glm.glmback.atelier.domain.ConflitsDAtelierCriteria;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Pageable;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ListeDesConflitsDAtelierIT {

  @Autowired
  private ConflitsDAtelier conflits;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entities;

  @Test
  @WithTenant("impeccmold")
  void shouldLireLaSequenceParSonAncrageSansPerdreSesReferencesAbsentes() {
    Instant debut = Instant.parse("2043-01-06T08:00:00.123456789Z");
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(debut);
    SuiviDAtelier suivi = suiviOF2026000042EngageA(debut)
      .enregistre(ouvrant)
      .enregistre(finDe(ouvrant).a(debut.plusSeconds(3600)))
      .enregistre(finDe(ouvrant).a(debut.plusSeconds(7200)));
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));

    var page = transactions.execute(transaction ->
      conflits.list(new ConflitsDAtelierCriteria("", suivi.element().id().uuid().toString()), new Pageable(0, 5))
    );

    assertThat(page.totalElementsCount()).isEqualTo(1);
    assertThat(page.content())
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.adresse().suivi()).isEqualTo(suivi.id());
        assertThat(ligne.adresse().pointage()).isEqualTo(ouvrant.id());
        assertThat(ligne.element()).isEqualTo(suivi.element());
        assertThat(ligne.cle()).isEqualTo(ouvrant.cle());
        assertThat(ligne.repere().premierPointage()).isEqualTo(debut);
        assertThat(ligne.repere().nombrePointages()).isEqualTo(3);
      });
  }

  @Test
  @WithTenant("impeccmold")
  void shouldPaginerDeuxSequencesDuMemeSuiviEtGarderLeTotalSurUnePageVide() {
    Instant debut = Instant.parse("2043-01-07T08:00:00Z");
    EvenementDAtelier premier = debutSurFraiseuse1ParDupontA(debut);
    EvenementDAtelier second = debutSurFraiseuse1ParDupontA(debut.plusSeconds(14400));
    SuiviDAtelier suivi = suiviOF2026000042EngageA(debut)
      .enregistre(premier)
      .enregistre(finDe(premier).a(debut.plusSeconds(3600)))
      .enregistre(finDe(premier).a(debut.plusSeconds(7200)))
      .enregistre(second)
      .enregistre(finDe(second).a(debut.plusSeconds(18000)))
      .enregistre(finDe(second).a(debut.plusSeconds(21600)));
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));
    var criteria = new ConflitsDAtelierCriteria("", suivi.element().id().uuid().toString());

    var premiere = transactions.execute(transaction -> conflits.list(criteria, new Pageable(0, 1)));
    var suivante = transactions.execute(transaction -> conflits.list(criteria, new Pageable(1, 1)));
    var vide = transactions.execute(transaction -> conflits.list(criteria, new Pageable(2, 1)));

    assertThat(premiere.totalElementsCount()).isEqualTo(2);
    assertThat(premiere.content())
      .extracting(ligne -> ligne.adresse().pointage())
      .containsExactly(premier.id());
    assertThat(suivante.totalElementsCount()).isEqualTo(2);
    assertThat(suivante.content())
      .extracting(ligne -> ligne.adresse().pointage())
      .containsExactly(second.id());
    assertThat(vide.content()).isEmpty();
    assertThat(vide.totalElementsCount()).isEqualTo(2);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldCombinerLesRecherchesPartiellesSansCasseAvantLaPagination() {
    var jean = operateurJeanMartinPourcent();
    var paul = operateurPaulDurand();
    transactions.executeWithoutResult(transaction -> {
      insere(jean);
      insere(paul);
    });
    var premier = debutDu9Janvier2043A8hPar(jean.id());
    var second = debutDu9Janvier2043A8hPar(paul.id());
    var troisieme = debutDu9Janvier2043A8hPar(jean.id());
    SuiviDAtelier cherche = suiviPourFiltre2043(elementFiltrePourcentA())
      .enregistre(premier)
      .enregistre(finDe(premier).a(premier.dateDeSurvenue().plusSeconds(3600)))
      .enregistre(finDe(premier).a(premier.dateDeSurvenue().plusSeconds(7200)));
    SuiviDAtelier autreOperateur = suiviPourFiltre2043(elementFiltrePourcentB())
      .enregistre(second)
      .enregistre(finDe(second).a(second.dateDeSurvenue().plusSeconds(3600)))
      .enregistre(finDe(second).a(second.dateDeSurvenue().plusSeconds(7200)));
    SuiviDAtelier autreElement = suiviPourFiltre2043(elementAutre2043())
      .enregistre(troisieme)
      .enregistre(finDe(troisieme).a(troisieme.dateDeSurvenue().plusSeconds(3600)))
      .enregistre(finDe(troisieme).a(troisieme.dateDeSurvenue().plusSeconds(7200)));
    transactions.executeWithoutResult(transaction -> {
      suivis.create(cherche);
      suivis.create(autreOperateur);
      suivis.create(autreElement);
    });
    var criteria = new ConflitsDAtelierCriteria("mArTiN_%", "fIlTrE_2043_%");

    var page = transactions.execute(transaction -> conflits.list(criteria, new Pageable(0, 1)));
    var suivante = transactions.execute(transaction -> conflits.list(criteria, new Pageable(1, 1)));

    assertThat(page.totalElementsCount()).isEqualTo(1);
    assertThat(page.content())
      .extracting(ligne -> ligne.adresse().suivi())
      .containsExactly(cherche.id());
    assertThat(suivante.totalElementsCount()).isEqualTo(1);
    assertThat(suivante.content()).isEmpty();
    var annuaire = new com.glm.glmback.atelier.domain.AnnuaireDAtelier(
      java.util.Map.of(jean.id(), jean, paul.id(), paul),
      java.util.Map.of()
    );
    var candidates = java.util.List.of(cherche, autreOperateur, autreElement)
      .stream()
      .map(com.glm.glmback.atelier.domain.ConflitsFixture::ligneDuPremierConflitDe)
      .toList();
    var ordreCanonique = java.util.Comparator.comparing((com.glm.glmback.atelier.domain.ConflitEnListe ligne) ->
      ligne.repere().premierPointage()
    )
      .thenComparing(ligne -> ligne.adresse().suivi().uuid().toString())
      .thenComparing(ligne -> ligne.adresse().pointage().uuid().toString());
    for (var recherche : java.util.List.of(
      criteria,
      new ConflitsDAtelierCriteria("JEAN", "FILTRE_2043"),
      new ConflitsDAtelierCriteria("", "FILTRE_2043_%"),
      new ConflitsDAtelierCriteria(jean.id().uuid().toString().substring(0, 8), "FILTRE_2043"),
      new ConflitsDAtelierCriteria("", cherche.element().id().uuid().toString()),
      new ConflitsDAtelierCriteria("ABSENT", "FILTRE_2043"),
      new ConflitsDAtelierCriteria("", "FILTRE_2043_ABSENT")
    )) {
      var attendues = candidates
        .stream()
        .filter(ligne -> recherche.matches(ligne, annuaire))
        .sorted(ordreCanonique)
        .toList();
      var acquises = transactions.execute(transaction -> conflits.list(recherche, new Pageable(0, 20)));
      assertThat(acquises.totalElementsCount()).isEqualTo(attendues.size());
      assertThat(acquises.content()).containsExactlyElementsOf(attendues);
    }
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRechercherPourcentSoulignementEtAntislashCommeDuTexteLitteral() {
    var premier = debutDu9Janvier2043A8hPar(OPERATEUR_ID_DUPONT);
    var second = debutDu9Janvier2043A8hPar(OPERATEUR_ID_DUPONT);
    SuiviDAtelier cherche = suiviPourFiltre2043(elementLitteralePourcentSoulignementAntislash2043())
      .enregistre(premier)
      .enregistre(finDe(premier).a(premier.dateDeSurvenue().plusSeconds(3600)))
      .enregistre(finDe(premier).a(premier.dateDeSurvenue().plusSeconds(7200)));
    SuiviDAtelier autre = suiviPourFiltre2043(elementLitteraleXX2043())
      .enregistre(second)
      .enregistre(finDe(second).a(second.dateDeSurvenue().plusSeconds(3600)))
      .enregistre(finDe(second).a(second.dateDeSurvenue().plusSeconds(7200)));
    transactions.executeWithoutResult(transaction -> {
      suivis.create(cherche);
      suivis.create(autre);
    });

    var page = transactions.execute(transaction ->
      conflits.list(new ConflitsDAtelierCriteria("", "lItTeRaLe_%\\2043"), new Pageable(0, 5))
    );

    assertThat(page.totalElementsCount()).isEqualTo(1);
    assertThat(page.content())
      .extracting(ligne -> ligne.adresse().suivi())
      .containsExactly(cherche.id());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRechercherUneReferenceOperateurAbsenteParSonIdentifiantPartiel() {
    Instant debut = Instant.parse("2043-01-10T08:00:00Z");
    var ouvrant = debutSurFraiseuse1ParDupontA(debut);
    SuiviDAtelier suivi = suiviOF2026000042EngageA(debut)
      .enregistre(ouvrant)
      .enregistre(finDe(ouvrant).a(debut.plusSeconds(3600)))
      .enregistre(finDe(ouvrant).a(debut.plusSeconds(7200)));
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));
    var criteria = new ConflitsDAtelierCriteria(
      ouvrant.operateur().uuid().toString().substring(0, 12).toUpperCase(java.util.Locale.ROOT),
      suivi.element().id().uuid().toString()
    );

    var page = transactions.execute(transaction -> conflits.list(criteria, new Pageable(0, 5)));

    assertThat(page.totalElementsCount()).isEqualTo(1);
    assertThat(page.content())
      .extracting(ligne -> ligne.adresse().suivi())
      .containsExactly(suivi.id());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldTrierParNanosecondePuisUuidCanoniquesDesDeuxMoities() {
    Instant debut = Instant.parse("2043-01-12T08:00:00.123456789Z");
    var ids = java.util.List.of(
      java.util.UUID.fromString("00000000-0000-0000-0000-000000000010"),
      java.util.UUID.fromString("00000000-0000-0000-8000-000000000010"),
      java.util.UUID.fromString("80000000-0000-0000-0000-000000000010")
    );
    var ancres = java.util.List.of(
      java.util.UUID.fromString("00000000-0000-0000-0000-000000000020"),
      java.util.UUID.fromString("00000000-0000-0000-8000-000000000020"),
      java.util.UUID.fromString("80000000-0000-0000-0000-000000000020"),
      java.util.UUID.fromString("00000000-0000-0000-0000-000000000030"),
      java.util.UUID.fromString("00000000-0000-0000-0000-000000000040"),
      java.util.UUID.fromString("00000000-0000-0000-0000-000000000050")
    );
    var faits = ancres
      .stream()
      .map(id ->
        debutIdentifieSurUnPosteDeMemeUuid(new com.glm.glmback.atelier.domain.EvenementDAtelierId(id)).horodatage(
          com.glm.glmback.atelier.domain.Horodatage.saisiA(id.equals(ancres.get(5)) ? debut : debut.plusNanos(1))
        )
      )
      .toList();
    transactions.executeWithoutResult(transaction -> {
      for (int index = 0; index < ids.size(); index++) {
        var suivi = suiviDu12Janvier2043Identifie(new com.glm.glmback.atelier.domain.SuiviDAtelierId(ids.get(index)));
        var ouvertures = index == 0 ? faits.subList(0, 3) : index == 1 ? faits.subList(3, 4) : faits.subList(4, 6);
        for (var ouvrant : ouvertures) {
          suivi = suivi
            .enregistre(ouvrant)
            .enregistre(finDe(ouvrant).a(debut.plusSeconds(3600)))
            .enregistre(finDe(ouvrant).a(debut.plusSeconds(7200)));
        }
        suivis.create(suivi);
      }
    });

    var premiere = transactions.execute(transaction ->
      conflits.list(new ConflitsDAtelierCriteria("", "ORDRE_2043_01_12"), new Pageable(0, 3))
    );
    var seconde = transactions.execute(transaction ->
      conflits.list(new ConflitsDAtelierCriteria("", "ORDRE_2043_01_12"), new Pageable(1, 3))
    );

    assertThat(premiere.totalElementsCount()).isEqualTo(6);
    assertThat(premiere.content())
      .extracting(ligne -> ligne.adresse().pointage().uuid())
      .containsExactly(ancres.get(5), ancres.get(0), ancres.get(1));
    assertThat(seconde.totalElementsCount()).isEqualTo(6);
    assertThat(seconde.content())
      .extracting(ligne -> ligne.adresse().pointage().uuid())
      .containsExactly(ancres.get(2), ancres.get(3), ancres.get(4));
  }

  @Test
  @WithTenant("impeccmold")
  void shouldConserverUneSequenceSansActiviteSurUnSuiviCloturePuisLaRetirerApresResolution() {
    Instant debut = Instant.parse("2043-01-13T08:00:00.123456789Z");
    var ouvrant = debutSansPosteParDupontA(debut);
    var premiereFin = finDe(ouvrant).a(debut.plusSeconds(3600));
    var secondeFin = finDe(ouvrant).a(debut.plusSeconds(7200));
    var suivi = suiviOF2026000042EngageA(debut)
      .enregistre(ouvrant)
      .enregistre(premiereFin)
      .enregistre(secondeFin)
      .cloture(clotureParLeroyA(debut.plusSeconds(10800)));
    var criteria = new ConflitsDAtelierCriteria("", suivi.element().id().uuid().toString());
    var cree = transactions.execute(transaction -> suivis.create(suivi));
    var cloture = transactions.execute(transaction -> conflits.list(criteria, new Pageable(0, 5)));
    assertThat(cloture.totalElementsCount()).isEqualTo(1);

    var annulation = new com.glm.glmback.atelier.domain.Annulation(AUTEUR_LEROY, debut.plusSeconds(14400), MOTIF_ERREUR_DE_SAISIE);
    var sansActivite = transactions.execute(transaction -> suivis.update(cree.annule(ouvrant.id(), annulation)));
    assertThat(sansActivite.activites()).isEmpty();
    assertThat(sansActivite.cloture()).isEqualTo(suivi.cloture());
    var orpheline = transactions.execute(transaction -> conflits.list(criteria, new Pageable(0, 5)));
    assertThat(orpheline.totalElementsCount()).isEqualTo(1);
    assertThat(orpheline.content())
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.adresse().pointage()).isEqualTo(premiereFin.id());
        assertThat(ligne.repere().nombrePointages()).isEqualTo(2);
        assertThat(ligne.cle().poste()).isEmpty();
        assertThat(ligne.revision()).isEqualTo(sansActivite.revision());
      });

    var uneFin = transactions.execute(transaction -> suivis.update(sansActivite.annule(premiereFin.id(), annulation)));
    var resolu = transactions.execute(transaction -> suivis.update(uneFin.annule(secondeFin.id(), annulation)));
    var vide = transactions.execute(transaction -> conflits.list(criteria, new Pageable(0, 5)));
    assertThat(vide.totalElementsCount()).isZero();
    assertThat(vide.content()).isEmpty();
    assertThat(resolu.activites()).isEmpty();
    assertThat(resolu.cloture()).isEqualTo(suivi.cloture());
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
