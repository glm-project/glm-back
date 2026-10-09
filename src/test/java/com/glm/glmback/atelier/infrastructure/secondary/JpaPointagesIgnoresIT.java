package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointageIgnore;
import com.glm.glmback.atelier.domain.PointagesIgnores;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RaisonDePointageIgnore;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.VerdictDeReception;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * L'audit des pointages ignores, relu en base : une ligne par pointage ignore, aucune contrainte, et un renvoi se cherche
 * par l'identifiant du geste.
 */
@IntegrationTest
class JpaPointagesIgnoresIT {

  private static final Instant LE_10_MAI_2046_A_8H = Instant.parse("2046-05-10T08:00:00.123456789Z");
  private static final Instant LE_10_MAI_2046_A_9H = Instant.parse("2046-05-10T09:00:00Z");

  @Autowired
  private PointagesIgnores audit;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private EntityManager entities;

  @Test
  @WithTenant("impeccmold")
  void shouldEcrireToutesLesColonnesDUnPointageIgnore() {
    EvenementDAtelierId dernierAccepte = EvenementDAtelierId.newId();
    PointageIgnore ignore = pointageIgnore(
      pointage(Optional.of(POSTE_ID_FRAISEUSE_1)),
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, Optional.of(dernierAccepte))
    );

    inTransaction(() -> audit.enregistre(ignore));

    Object[] ligne = lignesDe(ignore.pointage().evenement()).getFirst();
    assertThat(ligne[0]).hasToString(ignore.pointage().suivi().uuid().toString());
    assertThat(ligne[1]).hasToString(OPERATEUR_ID_DUPONT.uuid().toString());
    assertThat(ligne[2]).hasToString(POSTE_ID_FRAISEUSE_1.uuid().toString());
    assertThat(ligne[3]).isEqualTo("FIN");
    assertThat(instant(ligne[4])).isEqualTo(LE_10_MAI_2046_A_8H);
    assertThat(instant(ligne[5])).isEqualTo(LE_10_MAI_2046_A_9H);
    assertThat(ligne[6]).isEqualTo("ANTERIEUR");
    assertThat(ligne[7]).hasToString(dernierAccepte.uuid().toString());
    assertThat(inTransaction(() -> entities.find(PointageIgnoreEntity.class, ignore.pointage().evenement().uuid()))).isNotNull();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldEcrireUnPointageIgnoreSansPosteNiDernierAccepte() {
    PointageIgnore ignore = pointageIgnore(
      pointage(Optional.empty()),
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.AUCUNE_ACTIVITE, Optional.empty())
    );

    inTransaction(() -> audit.enregistre(ignore));

    Object[] ligne = lignesDe(ignore.pointage().evenement()).getFirst();
    assertThat(ligne[2]).isNull();
    assertThat(ligne[6]).isEqualTo("AUCUNE_ACTIVITE");
    assertThat(ligne[7]).isNull();
  }

  /**
   * La table n'a aucune contrainte d'unicite : deux lignes pour un meme renvoi sont acceptees, et le renvoi se retrouve.
   */
  @Test
  @WithTenant("impeccmold")
  void shouldAccepterDeuxLignesPourUnMemeRenvoi() {
    PointageIgnore ignore = pointageIgnore(
      pointage(Optional.empty()),
      new VerdictDeReception.Ignore(RaisonDePointageIgnore.DEJA_EN_COURS, Optional.empty())
    );

    inTransaction(() -> audit.enregistre(ignore));
    inTransaction(() -> audit.enregistre(ignore));

    assertThat(lignesDe(ignore.pointage().evenement())).hasSize(2);
    assertThat(inTransaction(() -> audit.contient(ignore.pointage().evenement()))).isTrue();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNePasContenirUnPointageJamaisIgnore() {
    assertThat(inTransaction(() -> audit.contient(EvenementDAtelierId.newId()))).isFalse();
  }

  private static PointageAEnregistrer pointage(Optional<PosteDeTravailId> poste) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(SuiviDAtelierId.newId())
      .type(TypeDEvenementDAtelier.FIN)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(poste)
      .auteur(AUTEUR_DUPONT)
      .dateDeSurvenue(Optional.of(LE_10_MAI_2046_A_8H))
      .evenement(EvenementDAtelierId.newId());
  }

  private static PointageIgnore pointageIgnore(PointageAEnregistrer pointage, VerdictDeReception.Ignore verdict) {
    return new PointageIgnore(pointage, new Horodatage(LE_10_MAI_2046_A_8H, LE_10_MAI_2046_A_9H), verdict);
  }

  @SuppressWarnings("unchecked")
  private List<Object[]> lignesDe(EvenementDAtelierId pointage) {
    return inTransaction(() ->
      entities
        .createNativeQuery(
          """
          select suivi_id, operateur_id, poste_id, type, date_de_survenue, date_de_reception, raison, dernier_accepte_id
          from pointage_ignore_d_atelier where id = :id
          """
        )
        .setParameter("id", pointage.uuid())
        .getResultList()
    );
  }

  private static Instant instant(Object secondes) {
    return new ExactInstantConverter().convertToEntityAttribute((BigDecimal) secondes);
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }

  private void inTransaction(Runnable action) {
    transactions.executeWithoutResult(status -> action.run());
  }
}
