package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierCriteria;
import com.glm.glmback.atelier.domain.SuiviDAtelierDejaExistantException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Les gardes de cet adapter ne sont pas atteignables depuis l'API : {@code create} ne refuse qu'une identite deja
 * prise, et l'application ne genere jamais deux fois le meme identifiant. On les eprouve donc ici.
 */
@UnitTest
class SuivisDAtelierEnMemoireTest {

  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();

  @BeforeEach
  void authentifieUneEntreprise() {
    TenantSecurityContexts.authenticateOn("impeccmold");
  }

  @AfterEach
  void nettoieLeContexte() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldRefuserDeCreerDeuxFoisLaMemeIdentite() {
    SuiviDAtelier suivi = suivis.create(suiviDAtelierEngage());

    assertThatThrownBy(() -> suivis.create(suivi)).isInstanceOf(SuiviDAtelierDejaExistantException.class);
  }

  @Test
  void shouldRefuserDeMettreAJourUnSuiviInconnu() {
    assertThatThrownBy(() -> suivis.update(suiviDAtelierEngage())).isInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  @Test
  void shouldNePasTrouverUnSuiviInconnu() {
    assertThat(suivis.get(SuiviDAtelierId.newId())).isEmpty();
  }

  @Test
  void shouldTrouverLeSuiviEnCoursDeSonElement() {
    SuiviDAtelier suivi = suivis.create(suiviDAtelierEngage());

    assertThat(suivis.getEnCoursPour(suivi.element().id())).contains(suivi);
  }

  @Test
  void shouldNePasTrouverDeSuiviEnCoursPourUnElementCloture() {
    SuiviDAtelier suivi = suivis.create(suiviDAtelierEngage());
    suivis.update(suivi.cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H)));

    assertThat(suivis.getEnCoursPour(suivi.element().id())).isEmpty();
  }

  @Test
  void shouldListerLesSuivisTriesParDateDEngagementDescendante() {
    SuiviDAtelier premier = suivis.create(suiviDAtelierEngage());
    SuiviDAtelier second = suivis.create(suiviDAtelierEngage());

    assertThat(suivis.list(tousLesSuivis(), new Pageable(0, 20)).content()).containsExactlyInAnyOrder(premier, second);
  }

  @Test
  void shouldNePasVoirLesSuivisDUneAutreEntreprise() {
    suivis.create(suiviDAtelierEngage());

    TenantSecurityContexts.authenticateOn("katilys");

    assertThat(suivis.list(tousLesSuivis(), new Pageable(0, 20)).content()).isEmpty();
  }

  private static SuiviDAtelierCriteria tousLesSuivis() {
    return new SuiviDAtelierCriteria(Optional.empty(), Set.<EtatDAtelier>of());
  }
}
