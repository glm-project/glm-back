package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.ConflitsFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.ConflitsDAtelier;
import com.glm.glmback.atelier.domain.ConflitsDAtelierCriteria;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Pageable;
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
}
