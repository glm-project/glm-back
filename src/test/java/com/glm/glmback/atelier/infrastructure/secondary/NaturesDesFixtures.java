package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Le journal ne recopie que l'identifiant de la nature et en relit le libelle dans le referentiel : les natures des
 * fixtures doivent donc y exister, dans le schema de l'entreprise du test.
 *
 * <p>
 * Leur cle est propre a la fixture, pour ne jamais heurter une nature de meme libelle declaree par un autre test du
 * schema partage.
 * </p>
 */
public final class NaturesDesFixtures {

  private NaturesDesFixtures() {}

  /**
   * Declare les natures dans le schema de chacune des entreprises donnees, puis rend au test son contexte de securite.
   */
  public static void declarer(EntityManager entities, TransactionTemplate transactions, String... entreprises) {
    SecurityContext contexteDuTest = SecurityContextHolder.getContext();
    try {
      for (String entreprise : entreprises) {
        TenantSecurityContexts.authenticateOn(entreprise);
        transactions.executeWithoutResult(status ->
          List.of(NATURE_FRAISAGE, NATURE_TOURNAGE).forEach(nature -> declarer(entities, nature))
        );
      }
    } finally {
      SecurityContextHolder.setContext(contexteDuTest);
    }
  }

  private static void declarer(EntityManager entities, NatureDOperation nature) {
    entities
      .createNativeQuery("insert into nature_de_travail (id, libelle, cle) values (:id, :libelle, :cle) on conflict do nothing")
      .setParameter("id", nature.id().uuid())
      .setParameter("libelle", nature.libelle())
      .setParameter("cle", "fixture " + nature.id().uuid())
      .executeUpdate();
  }
}
