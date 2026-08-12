package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailCriteria;
import com.glm.glmback.atelier.domain.JourneeDeTravailDejaOuverteException;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailIntrouvableException;
import com.glm.glmback.atelier.domain.Operateur;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Comme pour les suivis, les gardes d'identite de cet adapter ne sont pas atteignables par l'API : elles sont
 * eprouvees ici, avec le tri total qui rend la pagination correcte.
 */
@UnitTest
class JourneesDeTravailEnMemoireTest {

  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();

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
    JourneeDeTravail journee = journees.create(journeeDeDupontOuverteA7H());

    assertThatThrownBy(() -> journees.create(journee)).isInstanceOf(JourneeDeTravailDejaOuverteException.class);
  }

  @Test
  void shouldRefuserDeMettreAJourUneJourneeInconnue() {
    assertThatThrownBy(() -> journees.update(journeeDeDupontOuverteA7H())).isInstanceOf(JourneeDeTravailIntrouvableException.class);
  }

  @Test
  void shouldNePasTrouverUneJourneeInconnue() {
    assertThat(journees.get(JourneeDeTravailId.newId())).isEmpty();
  }

  @Test
  void shouldTrouverLaJourneeEnCoursDUnOperateur() {
    JourneeDeTravail journee = journees.create(journeeDeDupontOuverteA7H());

    assertThat(journees.getEnCoursPour(OPERATEUR_DUPONT)).contains(journee);
    assertThat(journees.getEnCoursPour(OPERATEUR_MARTIN)).isEmpty();
  }

  @Test
  void shouldNePasTrouverDeJourneeEnCoursApresLeDepart() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());

    assertThat(journees.getEnCoursPour(OPERATEUR_DUPONT)).isEmpty();
  }

  @Test
  void shouldTrouverLaJourneeContenantUnInstant() {
    JourneeDeTravail journee = journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());

    assertThat(journees.journeeContenant(OPERATEUR_DUPONT, LE_10_MAI_2026_A_12H)).contains(journee);
    assertThat(journees.journeeContenant(OPERATEUR_DUPONT, LE_11_MAI_2026_A_9H15)).isEmpty();
    assertThat(journees.journeeContenant(OPERATEUR_MARTIN, LE_10_MAI_2026_A_12H)).isEmpty();
  }

  @Test
  void shouldListerLesJourneesTrieesParDebutDescendant() {
    JourneeDeTravail ancienne = journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    JourneeDeTravail recente = journees.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_DUPONT).enregistre(arriveeDeDupontA(LE_11_MAI_2026_A_9H15))
    );

    assertThat(journees.list(toutesLesJournees(), new Pageable(0, 20)).content()).containsExactly(recente, ancienne);
  }

  @Test
  void shouldFiltrerLesJourneesParOperateur() {
    journees.create(journeeDeDupontOuverteA7H());

    assertThat(journees.list(journeesDe(OPERATEUR_DUPONT), new Pageable(0, 20)).content()).hasSize(1);
    assertThat(journees.list(journeesDe(OPERATEUR_MARTIN), new Pageable(0, 20)).content()).isEmpty();
  }

  @Test
  void shouldNePasVoirLesJourneesDUneAutreEntreprise() {
    journees.create(journeeDeDupontOuverteA7H());

    TenantSecurityContexts.authenticateOn("katilys");

    assertThat(journees.list(toutesLesJournees(), new Pageable(0, 20)).content()).isEmpty();
  }

  private static JourneeDeTravailCriteria toutesLesJournees() {
    return new JourneeDeTravailCriteria(Optional.empty(), Optional.empty());
  }

  private static JourneeDeTravailCriteria journeesDe(Operateur operateur) {
    return new JourneeDeTravailCriteria(Optional.empty(), Optional.of(operateur));
  }
}
