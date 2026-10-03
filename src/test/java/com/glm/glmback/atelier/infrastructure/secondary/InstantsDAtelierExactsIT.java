package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class InstantsDAtelierExactsIT {

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private com.glm.glmback.feuilledetemps.domain.ActivitesDeLOperateur feuille;

  @Autowired
  private com.glm.glmback.syntheseheures.domain.ActivitesDeLOperateur synthese;

  @Autowired
  private com.glm.glmback.coutderevient.domain.OccupationDesOperateurs cout;

  @Autowired
  private com.glm.glmback.pupitre.domain.SuivisOuvertsDuPupitre pupitre;

  @Test
  @WithTenant("impeccmold")
  void shouldConserverNeufDecimalesEtDeuxFaitsDistantsDUneNanoseconde() {
    Instant debut = Instant.parse("2042-01-06T08:00:00.123456789Z");
    EvenementDAtelier premier = debutSurFraiseuse1ParDupontA(debut);
    EvenementDAtelier second = debutSurFraiseuse1ParDupontA(debut.plusNanos(1));
    SuiviDAtelier suivi = SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(elementEngageOf2026000042())
      .engagement(new Engagement(AUTEUR_LEROY, debut.minusSeconds(3600)))
      .journal(JournalDAtelier.vide())
      .enregistre(premier)
      .enregistre(second);

    transactions.executeWithoutResult(transaction -> suivis.create(suivi));
    SuiviDAtelier relu = transactions.execute(transaction -> suivis.get(suivi.id()).orElseThrow());

    assertThat(relu.engagement()).isEqualTo(suivi.engagement());
    assertThat(relu.journal().evenements()).containsExactly(premier, second);
    assertThat(relu.activites()).isEqualTo(suivi.activites());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldLireLesBornesEtFiltrerUneDureeDUneNanosecondeDansChaqueLecteur() {
    Instant debut = Instant.parse("2042-01-07T08:00:00.123456789Z");
    Instant fin = debut.plusNanos(1);
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(debut);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(ouvrant).enregistre(finDe(ouvrant).a(fin));
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));

    transactions.executeWithoutResult(transaction -> {
      var lignes = feuille.recouvrant(new com.glm.glmback.feuilledetemps.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()), debut, fin);
      assertThat(lignes)
        .singleElement()
        .satisfies(activite -> {
          assertThat(activite.plage().debut()).isEqualTo(debut);
          assertThat(activite.plage().fin()).contains(fin);
        });
      assertThat(
        feuille.recouvrant(new com.glm.glmback.feuilledetemps.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()), fin, fin.plusNanos(1))
      ).isEmpty();
      assertThat(synthese.recouvrant(new com.glm.glmback.syntheseheures.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()), debut, fin))
        .singleElement()
        .satisfies(element -> {
          assertThat(element.activite().plage().debut()).isEqualTo(debut);
          assertThat(element.activite().plage().fin()).contains(fin);
        });
      assertThat(
        synthese.recouvrant(new com.glm.glmback.syntheseheures.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()), fin, fin.plusNanos(1))
      ).isEmpty();
      var operateurs = Set.of(new com.glm.glmback.coutderevient.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()));
      assertThat(cout.activites(operateurs, new com.glm.glmback.coutderevient.domain.Periode(debut, fin)))
        .singleElement()
        .satisfies(activite -> assertThat(activite.termineeA(fin).orElseThrow().periode().duree()).isEqualTo(Duration.ofNanos(1)));
      assertThat(cout.activites(operateurs, new com.glm.glmback.coutderevient.domain.Periode(fin, fin.plusNanos(1)))).isEmpty();
    });
  }

  @Test
  @WithTenant("impeccmold")
  void shouldConserverLEcheancePreciseDuPupitreEtLesDatesDeClotureEtDAnnulation() {
    Instant debut = Instant.parse("2042-01-08T08:00:00.123456789Z");
    EvenementDAtelier ouvrant = debutSurFraiseuse1ParDupontA(debut);
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(ouvrant);
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));
    transactions.executeWithoutResult(transaction ->
      assertThat(pupitre.tous())
        .filteredOn(tuile -> tuile.id().uuid().equals(suivi.id().uuid()))
        .singleElement()
        .satisfies(tuile ->
          assertThat(tuile.activites())
            .singleElement()
            .satisfies(activite -> {
              assertThat(activite.depuis()).isEqualTo(debut);
              assertThat(activite.echeance()).isEqualTo(debut.plusSeconds(46800));
              assertThat(activite.estEnCoursA(debut.plusSeconds(46800).minusNanos(1))).isTrue();
              assertThat(activite.estEnCoursA(debut.plusSeconds(46800))).isFalse();
            })
        )
    );
    Annulation annulation = new Annulation(AUTEUR_LEROY, debut.plusSeconds(3600), MOTIF_ERREUR_DE_SAISIE);
    Cloture cloture = new Cloture(AUTEUR_LEROY, new Horodatage(debut.plusSeconds(7200), debut.plusSeconds(10800).plusNanos(1)));
    SuiviDAtelier clos = suivi.annule(ouvrant.id(), annulation).cloture(cloture);

    transactions.executeWithoutResult(transaction -> suivis.update(clos));
    SuiviDAtelier relu = transactions.execute(transaction -> suivis.get(suivi.id()).orElseThrow());

    assertThat(relu.cloture()).contains(cloture);
    assertThat(relu.journal().evenement(ouvrant.id()).orElseThrow().annulation()).contains(annulation);
    transactions.executeWithoutResult(transaction ->
      assertThat(pupitre.tous()).noneMatch(tuile -> tuile.id().uuid().equals(suivi.id().uuid()))
    );
  }
}
