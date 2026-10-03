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

  private void insere(OperateurConnu operateur) {
    entities
      .createNativeQuery("insert into operateur (id, nom, prenom) values (:id, :nom, :prenom)")
      .setParameter("id", operateur.id().uuid())
      .setParameter("nom", operateur.nom().value())
      .setParameter("prenom", operateur.prenom().value())
      .executeUpdate();
  }
}
