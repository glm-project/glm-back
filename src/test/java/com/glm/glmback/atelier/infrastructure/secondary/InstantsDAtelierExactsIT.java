package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class InstantsDAtelierExactsIT {

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

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
}
