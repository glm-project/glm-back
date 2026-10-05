package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

import static com.glm.glmback.atelier.application.gestionanomalies.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.gestionanomalies.RecusDActes;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.gestionanomalies.ConfirmationReutiliseeException;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class RecusDActesIT {

  @Autowired
  private RecusDActes recus;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @Test
  @WithTenant("impeccmold")
  void shouldRelireLaPreuveEtLeRecuApresLeCommit() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviDAtelierEngage().enregistre(debutSansPosteParDupontA(LE_10_MAI_2026_A_8H))));
    var recu = recuDAnnulation(suivi);
    // WHEN
    inTransaction(() -> {
      recus.create(recu);
      return null;
    });
    // THEN
    assertThat(inTransaction(() -> recus.get(recu.proposition().commande()))).contains(recu);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserUnSecondEnregistrementDeLaMemeCommande() {
    // GIVEN
    var suivi = inTransaction(() -> suivis.create(suiviDAtelierEngage().enregistre(debutSansPosteParDupontA(LE_10_MAI_2026_A_8H))));
    var recu = recuDAnnulation(suivi);
    inTransaction(() -> {
      recus.create(recu);
      return null;
    });
    // WHEN THEN
    assertThatThrownBy(() ->
      inTransaction(() -> {
        recus.create(recu);
        return null;
      })
    ).isExactlyInstanceOf(ConfirmationReutiliseeException.class);
    assertThat(inTransaction(() -> recus.get(recu.proposition().commande()))).contains(recu);
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
