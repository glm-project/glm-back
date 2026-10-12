package com.glm.glmback.atelier.infrastructure.secondary.correctiondesactivites;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.correctiondesactivites.ActiviteProjetee;
import com.glm.glmback.atelier.domain.correctiondesactivites.ActivitesDeLOperateur;
import com.glm.glmback.atelier.infrastructure.secondary.NaturesDesFixtures;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La lecture de la projection par la journee a corriger : l'activite porte la nature figee a sa saisie, celle du fait qui
 * parle pour elle, et non celle que le poste porte aujourd'hui.
 */
@IntegrationTest
class JpaActivitesDeLOperateurIT {

  private static final Instant LE_3_FEVRIER_2043_A_8H = Instant.parse("2043-02-03T08:00:00Z");
  private static final Instant LE_3_FEVRIER_2043_A_10H = Instant.parse("2043-02-03T10:00:00Z");
  private static final Instant LE_3_FEVRIER_2043_A_12H = Instant.parse("2043-02-03T12:00:00Z");
  private static final Instant LE_4_FEVRIER_2043_A_9H = Instant.parse("2043-02-04T09:00:00Z");

  @Autowired
  private ActivitesDeLOperateur lecture;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entities;

  @BeforeEach
  void declarerLesNatures() {
    NaturesDesFixtures.declarer(entities, transactions, "impeccmold");
  }

  @Test
  @WithTenant("impeccmold")
  void shouldLireLaNatureFigeeALaSaisieEtCelleDuDernierChangementDePoste() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_3_FEVRIER_2043_A_8H);
    EvenementDAtelier deplacee = debutSurFraiseuse1ParDupontA(LE_3_FEVRIER_2043_A_10H);
    SuiviDAtelier suivi = suiviDAtelierEngageSurUnElementNeuf()
      .enregistre(travail)
      .enregistre(finDe(travail).a(LE_3_FEVRIER_2043_A_10H.minusSeconds(60)))
      .enregistre(deplacee)
      .enregistre(finDe(deplacee).a(LE_3_FEVRIER_2043_A_12H))
      .changeLePoste(changementDePosteVersFraiseuse2ParLeroyDe(deplacee).a(LE_4_FEVRIER_2043_A_9H), LE_4_FEVRIER_2043_A_9H);
    transactions.executeWithoutResult(status -> suivis.create(suivi));

    List<ActiviteProjetee> lues = transactions.execute(status ->
      lecture
        .touchant(OPERATEUR_ID_DUPONT, LE_3_FEVRIER_2043_A_8H.minusSeconds(3600), LE_3_FEVRIER_2043_A_12H.plusSeconds(3600))
        .stream()
        .filter(activite -> activite.suivi().equals(suivi.id()))
        .toList()
    );

    assertThat(lues).hasSize(2);
    assertThat(lues.get(0).id().uuid()).isEqualTo(travail.activite().orElseThrow().uuid());
    assertThat(lues.get(0).nature()).contains(NATURE_FRAISAGE);
    assertThat(lues.get(0).poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(lues.get(1).nature()).contains(NATURE_TOURNAGE);
    assertThat(lues.get(1).poste()).contains(POSTE_ID_FRAISEUSE_2);
    assertThat(lues.get(1).fin()).contains(LE_3_FEVRIER_2043_A_12H);
    assertThat(lues.get(1).element()).isEqualTo(suivi.element());
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNeLireQueLesActivitesQuiTouchentLaPlage() {
    EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(Instant.parse("2043-03-03T08:00:00Z"));
    SuiviDAtelier suivi = suiviDAtelierEngageSurUnElementNeuf()
      .enregistre(travail)
      .enregistre(finDe(travail).a(Instant.parse("2043-03-03T10:00:00Z")));
    transactions.executeWithoutResult(status -> suivis.create(suivi));

    assertThat(lues(suivi, "2043-03-03T10:00:00Z", "2043-03-03T11:00:00Z")).isEmpty();
    assertThat(lues(suivi, "2043-03-03T07:00:00Z", "2043-03-03T08:00:00Z")).isEmpty();
    assertThat(lues(suivi, "2043-03-03T09:59:59Z", "2043-03-03T11:00:00Z")).hasSize(1);
    assertThat(lues(suivi, "2043-03-03T08:00:01Z", "2043-03-03T08:00:02Z")).hasSize(1);
  }

  private List<ActiviteProjetee> lues(SuiviDAtelier suivi, String debut, String finExclue) {
    return transactions.execute(status ->
      lecture
        .touchant(OPERATEUR_ID_DUPONT, Instant.parse(debut), Instant.parse(finExclue))
        .stream()
        .filter(activite -> activite.suivi().equals(suivi.id()))
        .toList()
    );
  }
}
