package com.glm.glmback.naturedetravail.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.NatureDOperationId;
import com.glm.glmback.atelier.domain.OrigineDuPointage;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.naturedetravail.domain.NatureDeTravailId;
import com.glm.glmback.naturedetravail.domain.NaturesEnUsage;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class UsagesDesNaturesIT {

  private static final String NATURES_FIXTURE = "natures_fixture";

  @Autowired
  private NaturesEnUsage usages;

  @Autowired
  private EntityManager entities;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldSeeNatureCarriedByAPoste() {
    NatureDeTravailId nature = natureDeclaree();
    posteDeLaNature(nature);

    assertThat(inTransaction(() -> usages.estUtilisee(nature))).isTrue();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldNotSeeNatureWithoutPoste() {
    NatureDeTravailId nature = natureDeclaree();

    assertThat(inTransaction(() -> usages.estUtilisee(nature))).isFalse();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldSeeUsedNaturesAmongGivenOnes() {
    NatureDeTravailId portee = natureDeclaree();
    NatureDeTravailId libre = natureDeclaree();
    posteDeLaNature(portee);
    posteDeLaNature(portee);

    assertThat(inTransaction(() -> usages.utiliseesParmi(List.of(portee, libre)))).containsExactly(portee);
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldSeeNaturePointedInTheJournal() {
    NatureDeTravailId nature = natureDeclaree();
    pointageDeLaNature(nature);

    assertThat(inTransaction(() -> usages.estPointee(nature))).isTrue();
    assertThat(inTransaction(() -> usages.estUtilisee(nature))).isFalse();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldNotSeeNatureNeverPointed() {
    NatureDeTravailId nature = natureDeclaree();
    posteDeLaNature(nature);

    assertThat(inTransaction(() -> usages.estPointee(nature))).isFalse();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldSeePointedNaturesAmongGivenOnes() {
    NatureDeTravailId portee = natureDeclaree();
    NatureDeTravailId pointee = natureDeclaree();
    NatureDeTravailId libre = natureDeclaree();
    posteDeLaNature(portee);
    pointageDeLaNature(pointee);

    assertThat(inTransaction(() -> usages.utiliseesParmi(List.of(portee, pointee, libre)))).containsExactlyInAnyOrder(portee, pointee);
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldCountPostesOfGivenNatures() {
    NatureDeTravailId deuxPostes = natureDeclaree();
    NatureDeTravailId pointeeSansPoste = natureDeclaree();
    posteDeLaNature(deuxPostes);
    posteDeLaNature(deuxPostes);
    pointageDeLaNature(pointeeSansPoste);

    assertThat(inTransaction(() -> usages.postesParmi(List.of(deuxPostes, pointeeSansPoste)))).containsExactly(entry(deuxPostes, 2));
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldCountNoPosteAmongNone() {
    assertThat(inTransaction(() -> usages.postesParmi(List.of()))).isEmpty();
  }

  @Test
  @WithTenant(NATURES_FIXTURE)
  void shouldSeeNoUsedNatureAmongNone() {
    assertThat(inTransaction(() -> usages.utiliseesParmi(List.of()))).isEmpty();
  }

  @Test
  void shouldOnlySeePostesOfCurrentTenant() {
    TenantSecurityContexts.authenticateOn(NATURES_FIXTURE);
    NatureDeTravailId nature = natureDeclaree();
    posteDeLaNature(nature);

    TenantSecurityContexts.authenticateOn("katilys");

    assertThat(inTransaction(() -> usages.estUtilisee(nature))).isFalse();
  }

  private NatureDeTravailId natureDeclaree() {
    UUID id = UUID.randomUUID();
    inTransaction(() ->
      entities
        .createNativeQuery("insert into nature_de_travail (id, libelle, cle) values (:id, :libelle, :libelle)")
        .setParameter("id", id)
        .setParameter("libelle", "usage " + id)
        .executeUpdate()
    );

    return new NatureDeTravailId(id);
  }

  private void posteDeLaNature(NatureDeTravailId nature) {
    UUID id = UUID.randomUUID();
    inTransaction(() ->
      entities
        .createNativeQuery("insert into poste_de_travail (id, libelle, nature_id) values (:id, :libelle, :nature)")
        .setParameter("id", id)
        .setParameter("libelle", "Poste " + id)
        .setParameter("nature", nature.uuid())
        .executeUpdate()
    );
  }

  /**
   * Un suivi et un debut minimaux, par le repository du contexte voisin : c'est un montage de test, pas une dependance
   * de production. Seule compte ici la nature recopiee.
   */
  private void pointageDeLaNature(NatureDeTravailId nature) {
    Instant engagement = Instant.parse("2040-02-01T07:00:00Z");
    EvenementDAtelierId debut = EvenementDAtelierId.newId();
    SuiviDAtelier suivi = SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), NOM_OF_2026_000042, CATEGORIE_OF))
      .engagement(new Engagement(AUTEUR_LEROY, engagement))
      .journal(JournalDAtelier.vide())
      .enregistre(
        EvenementDAtelier.builder()
          .id(debut)
          .type(TypeDEvenementDAtelier.DEBUT)
          .activite(Optional.of(ActiviteId.ouvertePar(debut)))
          .activiteVisee(Optional.empty())
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.empty())
          .nature(Optional.of(new NatureDOperation(new NatureDOperationId(nature.uuid()), "usage " + nature.uuid())))
          .coutHoraire(Optional.empty())
          .tauxHoraire(Optional.empty())
          .dureeMax(Optional.of(DUREE_MAXIMALE_TREIZE_HEURES))
          .auteur(AUTEUR_DUPONT)
          .origine(OrigineDuPointage.POINTAGE)
          .horodatage(Horodatage.saisiA(engagement.plusSeconds(3600)))
      );

    inTransaction(() -> suivis.create(suivi));
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
