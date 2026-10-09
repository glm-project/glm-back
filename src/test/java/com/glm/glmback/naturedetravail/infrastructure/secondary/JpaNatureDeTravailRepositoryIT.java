package com.glm.glmback.naturedetravail.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.naturedetravail.domain.LibelleDeNature;
import com.glm.glmback.naturedetravail.domain.NatureDeTravail;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailDejaCreeeException;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailRepository;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class JpaNatureDeTravailRepositoryIT {

  private static final String NATURES_FIXTURE = "natures_fixture";

  @Autowired
  private NatureDeTravailRepository natures;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldCreateAndGetNature() {
    NatureDeTravail nature = natureDeTest("Soudage");

    inTransaction(() -> natures.create(nature));

    assertThat(inTransaction(() -> natures.get(nature.id()))).contains(nature);
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldNotCreateNatureTwice() {
    NatureDeTravail nature = natureDeTest("Tournage");
    inTransaction(() -> natures.create(nature));

    assertThatThrownBy(() -> inTransaction(() -> natures.create(nature))).isExactlyInstanceOf(NatureDeTravailDejaCreeeException.class);
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldNotGetUnknownNature() {
    assertThat(inTransaction(() -> natures.get(NatureDeTravailId.newId()))).isEmpty();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldFindIdByKey() {
    NatureDeTravail nature = natureDeTest("Fraisage");
    inTransaction(() -> natures.create(nature));

    assertThat(inTransaction(() -> natures.idPourCle(new LibelleDeNature("FRAISÂGE " + suffixe(nature)).cle()))).contains(nature.id());
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldNotFindIdForUnknownKey() {
    assertThat(inTransaction(() -> natures.idPourCle(new LibelleDeNature("Inconnue " + UUID.randomUUID()).cle()))).isEmpty();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldListNaturesByKeyIgnoringAccents() {
    String prefixe = "zz" + UUID.randomUUID().toString().substring(0, 8);
    NatureDeTravail troisieme = new NatureDeTravail(NatureDeTravailId.newId(), new LibelleDeNature(prefixe + " F"));
    NatureDeTravail second = new NatureDeTravail(NatureDeTravailId.newId(), new LibelleDeNature(prefixe + " É"));
    NatureDeTravail premier = new NatureDeTravail(NatureDeTravailId.newId(), new LibelleDeNature(prefixe + " d"));
    inTransaction(() -> natures.create(troisieme));
    inTransaction(() -> natures.create(second));
    inTransaction(() -> natures.create(premier));

    assertThat(inTransaction(() -> natures.list(new Pageable(0, 100))).content()).endsWith(premier, second, troisieme);
  }

  @Test
  void shouldIsolateNaturesByTenant() {
    TenantSecurityContexts.authenticateOn(NATURES_FIXTURE);
    NatureDeTravail nature = natureDeTest("Dessin");
    inTransaction(() -> natures.create(nature));

    TenantSecurityContexts.authenticateOn("katilys");

    assertThat(inTransaction(() -> natures.get(nature.id()))).isEmpty();
  }

  /**
   * Toutes les methodes partagent le schema de la fixture : le suffixe garde chaque libelle unique.
   */
  private static NatureDeTravail natureDeTest(String libelle) {
    return new NatureDeTravail(
      NatureDeTravailId.newId(),
      new LibelleDeNature(libelle + " " + UUID.randomUUID().toString().substring(0, 8))
    );
  }

  private static String suffixe(NatureDeTravail nature) {
    return nature.libelle().value().substring(nature.libelle().value().lastIndexOf(' ') + 1);
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
