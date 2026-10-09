package com.glm.glmback.parametrage.infrastructure.secondary;

import static com.glm.glmback.parametrage.domain.ParametrageFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.parametrage.domain.Logo;
import com.glm.glmback.parametrage.domain.LogoRepository;
import com.glm.glmback.parametrage.domain.Parametrage;
import com.glm.glmback.parametrage.domain.ParametrageRepository;
import com.glm.glmback.parametrage.domain.VersionDuLogo;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class JpaLogoRepositoryIT {

  @Autowired
  private LogoRepository logos;

  @Autowired
  private ParametrageRepository parametrages;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant("parametrage_vierge")
  void shouldHaveNoLogoInANewTenant() {
    Optional<Logo> logo = transactions.execute(status -> logos.get());
    Optional<VersionDuLogo> version = transactions.execute(status -> logos.version());

    assertThat(logo).isEmpty();
    assertThat(version).isEmpty();
  }

  @Test
  @WithTenant("parametrage_logo")
  void shouldReadTheVersionWithoutTheContenu() {
    transactions.execute(status -> logos.update(logoPngBleu()));

    Optional<VersionDuLogo> version = transactions.execute(status -> logos.version());
    assertThat(version).contains(VERSION_DU_LOGO_0123);
  }

  @Test
  @WithTenant("parametrage_logo")
  void shouldUpdateLogoWithoutTouchingTheOtherSettings() {
    transactions.execute(status -> parametrages.update(parametrageDixHeures()));

    transactions.execute(status -> logos.update(logoPngBleu()));

    Optional<Logo> logo = transactions.execute(status -> logos.get());
    Parametrage parametrage = transactions.execute(status -> parametrages.get());
    assertThat(logo).contains(logoPngBleu());
    assertThat(parametrage).isEqualTo(parametrageDixHeures());
  }

  @Test
  @WithTenant("parametrage_logo")
  void shouldKeepTheLogoWhenTheOtherSettingsChange() {
    transactions.execute(status -> logos.update(logoPngBleu()));

    transactions.execute(status -> parametrages.update(parametrageDixHeures()));

    Optional<Logo> logo = transactions.execute(status -> logos.get());
    assertThat(logo).contains(logoPngBleu());
  }

  @Test
  @WithTenant("parametrage_logo")
  void shouldDeleteTheLogoWithoutTouchingTheOtherSettings() {
    transactions.execute(status -> parametrages.update(parametrageDixHeures()));
    transactions.execute(status -> logos.update(logoPngBleu()));

    transactions.executeWithoutResult(status -> logos.delete());

    Optional<Logo> logo = transactions.execute(status -> logos.get());
    Parametrage parametrage = transactions.execute(status -> parametrages.get());
    assertThat(logo).isEmpty();
    assertThat(parametrage).isEqualTo(parametrageDixHeures());
  }
}
