package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.ApercusDeResolution;
import com.glm.glmback.atelier.application.ConfirmerLesActes;
import com.glm.glmback.atelier.application.RecusDActes;
import com.glm.glmback.atelier.domain.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class ConfirmationsMateriellesIT {

  @Autowired
  private ApercusDeResolution apercus;

  @Autowired
  private ConfirmerLesActes confirmations;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private RecusDActes recus;

  @Autowired
  private TransactionTemplate transactions;

  @MockitoBean
  private Clock clock;

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnApercuEncoreValideQuandLActiviteFranchitSonEcheanceSansEcriture() {
    // GIVEN
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T20:59:59Z"));
    var suivi = transactions.execute(status -> suivis.create(suiviAvecTransitionDeMemeCategorie()));
    var acte = preuveDAnnulationDeTransition(suivi);
    var commande = UUID.randomUUID();
    var apercu = apercus.apercu(commande, acte.adresse(), suivi.revision(), acte.acte(), CONTEXTE_LEROY_IMPECCMOLD);
    assertThat(apercu.apres().activites())
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).isEmpty();
        assertThat(activite.finAutomatique()).isFalse();
      });
    assertThat(apercu.reference().preuve().expireLe()).isEqualTo(Instant.parse("2026-05-10T21:14:59Z"));
    when(clock.now()).thenReturn(Instant.parse("2026-05-10T21:00:00Z"));
    // WHEN THEN
    assertThatThrownBy(() ->
      confirmations.confirmer(suivi.id(), commande, apercu.reference().opaque(), CONTEXTE_LEROY_IMPECCMOLD)
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    var relu = transactions.execute(status -> suivis.get(suivi.id()));
    var recu = transactions.execute(status -> recus.get(commande));
    assertThat(relu).contains(suivi);
    assertThat(recu).isEmpty();
  }
}
