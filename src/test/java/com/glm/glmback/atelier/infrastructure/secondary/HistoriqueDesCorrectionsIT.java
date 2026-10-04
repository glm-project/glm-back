package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class HistoriqueDesCorrectionsIT {

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Test
  @WithTenant("impeccmold")
  void shouldRelireLeLienDUneFinCorrigeeSansLInfererDeSesDates() {
    EvenementDAtelier debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(debut).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier remplacant = finDe(debut).a(LE_10_MAI_2026_A_12H.plusSeconds(1800));
    SuiviDAtelier suivi = suiviDAtelierEngage().enregistre(debut).enregistre(fin);
    transactions.executeWithoutResult(transaction -> suivis.create(suivi));
    SuiviDAtelier corrige = suivi.corrige(fin.id(), annulationParLeroy(), remplacant);

    transactions.executeWithoutResult(transaction -> suivis.update(corrige));
    SuiviDAtelier relu = transactions.execute(transaction -> suivis.get(suivi.id()).orElseThrow());

    assertThat(relu.journal().evenement(remplacant.id()).orElseThrow().remplace()).contains(fin.id());
    assertThat(relu.journal().evenement(fin.id()).orElseThrow().annulation()).contains(annulationParLeroy());
    assertThat(relu.journal().evenement(debut.id()).orElseThrow().remplace()).isEmpty();
  }
}
