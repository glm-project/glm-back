package com.glm.glmback.parametrage.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.parametrage.domain.AmplitudeMaximale;
import com.glm.glmback.parametrage.domain.Auteur;
import com.glm.glmback.parametrage.domain.Modification;
import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.ParametrageIntrouvableException;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chaque test tourne dans une transaction annulee : le parametrage est une ligne unique par entreprise, partagee par
 * tous les tests et scenarios, qui doit rester a sa valeur semee.
 */
@IntegrationTest
class JpaParametrageRepositoryIT {

  private static final String IMPECCMOLD = "impeccmold";
  private static final String KATILYS = "katilys";
  private static final AmplitudeMaximale TREIZE_HEURES = new AmplitudeMaximale(Duration.ofHours(13));

  @Autowired
  private ParametrageRepository parametrages;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(KATILYS)
  void shouldSemerTreizeHeuresSansModification() {
    Parametrage seme = annule(status -> parametrages.get());

    assertThat(seme).isEqualTo(new Parametrage(TREIZE_HEURES, Optional.empty()));
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUnParametrageModifie() {
    Parametrage modifie = new Parametrage(
      new AmplitudeMaximale(Duration.ofMinutes(750)),
      Optional.of(new Modification(new Auteur("leroy"), Instant.parse("2026-09-24T09:00:00.123456Z")))
    );

    Parametrage relu = annule(status -> {
      parametrages.update(modifie);
      entityManager.flush();
      entityManager.clear();
      return parametrages.get();
    });

    assertThat(relu).isEqualTo(modifie);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUnParametrageRevenuSansModification() {
    Parametrage revenu = new Parametrage(TREIZE_HEURES, Optional.empty());

    Parametrage relu = annule(status -> {
      parametrages.update(new Parametrage(new AmplitudeMaximale(Duration.ofHours(10)), Optional.empty()));
      parametrages.update(revenu);
      entityManager.flush();
      entityManager.clear();
      return parametrages.get();
    });

    assertThat(relu).isEqualTo(revenu);
  }

  /**
   * Seul test qui valide sa transaction : une modification annulee ne prouverait rien de l'isolation. La valeur semee
   * est remise en place quoi qu'il arrive.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldIsolerLeParametrageDeChaqueEntreprise() {
    try {
      transactions.executeWithoutResult(status ->
        parametrages.update(new Parametrage(new AmplitudeMaximale(Duration.ofHours(10)), Optional.empty()))
      );
      TenantSecurityContexts.authenticateOn(KATILYS);

      Parametrage autre = annule(status -> parametrages.get());

      assertThat(autre.amplitudeMaximale()).isEqualTo(TREIZE_HEURES);
    } finally {
      TenantSecurityContexts.authenticateOn(IMPECCMOLD);
      transactions.executeWithoutResult(status -> parametrages.update(new Parametrage(TREIZE_HEURES, Optional.empty())));
    }
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldSignalerUnParametrageAbsentALaLecture() {
    assertThatThrownBy(() ->
      annule(status -> {
        entityManager.createNativeQuery("delete from parametrage").executeUpdate();
        return parametrages.get();
      })
    ).isExactlyInstanceOf(ParametrageIntrouvableException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldSignalerUnParametrageAbsentALaModification() {
    Parametrage parametrage = new Parametrage(TREIZE_HEURES, Optional.empty());

    assertThatThrownBy(() ->
      annule(status -> {
        entityManager.createNativeQuery("delete from parametrage").executeUpdate();
        parametrages.update(parametrage);
        return null;
      })
    ).isExactlyInstanceOf(ParametrageIntrouvableException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRefuserUneSecondeLigne() {
    assertThatThrownBy(() ->
      annule(status ->
        entityManager.createNativeQuery("insert into parametrage (id, amplitude_maximale_minutes) values (2, 600)").executeUpdate()
      )
    ).isInstanceOf(PersistenceException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRefuserUneAmplitudeHorsBornesEnBase() {
    assertThatThrownBy(() ->
      annule(status -> entityManager.createNativeQuery("update parametrage set amplitude_maximale_minutes = 1440").executeUpdate())
    ).isInstanceOf(PersistenceException.class);
  }

  private <T> T annule(Function<TransactionStatus, T> action) {
    return transactions.execute(status -> {
      status.setRollbackOnly();
      return action.apply(status);
    });
  }
}
