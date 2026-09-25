package com.glm.glmback.syntheseheures.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.syntheseheures.domain.OperateurId;
import com.glm.glmback.syntheseheures.domain.Plage;
import com.glm.glmback.syntheseheures.domain.PointagesDAtelier;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La fin presumee d'une journee abandonnee cherche le dernier pointage d'OF de l'operateur, tous elements confondus.
 * Le journal d'atelier est seme en SQL, dans une transaction annulee : ce contexte lit ces tables sans importer
 * celui qui les ecrit.
 */
@IntegrationTest
class PointagesDeLaSyntheseIT {

  private static final Instant LUNDI = Instant.parse("2043-03-02T07:00:00Z");

  @Autowired
  private PointagesDAtelier pointages;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldTrouverLeDernierPointageActifDeLOperateurSurLaPeriode() {
    UUID dupont = UUID.randomUUID();
    UUID martin = UUID.randomUUID();

    transactions.executeWithoutResult(status -> {
      status.setRollbackOnly();
      UUID of42 = suivi();
      UUID of43 = suivi();
      pointage(of42, dupont, LUNDI.plusSeconds(3600), false);
      pointage(of43, dupont, LUNDI.plusSeconds(10800), false);
      pointage(of43, dupont, LUNDI.plusSeconds(14400), true);
      pointage(of42, martin, LUNDI.plusSeconds(18000), false);

      assertThat(dernier(dupont, LUNDI, LUNDI.plusSeconds(46800))).contains(LUNDI.plusSeconds(10800));
      assertThat(dernier(dupont, LUNDI, LUNDI.plusSeconds(3600))).contains(LUNDI.plusSeconds(3600));
      assertThat(dernier(dupont, LUNDI.plusSeconds(10801), LUNDI.plusSeconds(46800))).isEmpty();
      assertThat(dernier(martin, LUNDI, LUNDI.plusSeconds(46800))).contains(LUNDI.plusSeconds(18000));
    });
  }

  private Optional<Instant> dernier(UUID operateur, Instant debut, Instant fin) {
    return pointages.dernierPointage(new OperateurId(operateur), new Plage(debut, Optional.of(fin)));
  }

  private UUID suivi() {
    UUID id = UUID.randomUUID();
    entityManager
      .createNativeQuery(
        "insert into suivi_d_atelier (id, element_id, element_nom, element_type, engagement_auteur, engagement_date, etat) "
          + "values (?, ?, 'OF-IT', 'ORDRE_DE_FABRICATION', 'leroy', ?, 'EN_COURS')"
      )
      .setParameter(1, id)
      .setParameter(2, UUID.randomUUID())
      .setParameter(3, LUNDI)
      .executeUpdate();

    return id;
  }

  private void pointage(UUID suivi, UUID operateur, Instant date, boolean annule) {
    String annulation = annule ? "?" : "null";
    var requete = entityManager
      .createNativeQuery(
        "insert into evenement_d_atelier (id, suivi_id, type, operateur_id, auteur, date_de_survenue, date_d_enregistrement, annulation_date) "
          + "values (?, ?, 'DEBUT', ?, 'dupont', ?, ?, "
          + annulation
          + ")"
      )
      .setParameter(1, UUID.randomUUID())
      .setParameter(2, suivi)
      .setParameter(3, operateur)
      .setParameter(4, date)
      .setParameter(5, date);
    if (annule) {
      requete.setParameter(6, date);
    }
    requete.executeUpdate();
  }
}
