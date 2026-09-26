package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.CibleDuSignalement;
import com.glm.glmback.atelier.domain.CriteresDePointageSignale;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.MotifDeSignalement;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageSignale;
import com.glm.glmback.atelier.domain.PointageSignaleDejaExistantException;
import com.glm.glmback.atelier.domain.PointageSignaleId;
import com.glm.glmback.atelier.domain.PointageSignaleIntrouvableException;
import com.glm.glmback.atelier.domain.PointagesSignales;
import com.glm.glmback.atelier.domain.Resolution;
import com.glm.glmback.atelier.domain.TypeDeCible;
import com.glm.glmback.atelier.domain.TypeDeResolution;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Le schema est partage : chaque test se donne son propre operateur, qui borne a lui seul ce qu'il liste.
 */
@IntegrationTest
class JpaPointagesSignalesIT {

  @Autowired
  private PointagesSignales signalements;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRelireUnSignalementAvecSesMotifsEtSaDateDeclaree() {
    PointageSignale signale = signale(new OperateurId(UUID.randomUUID()), Instant.parse("2043-01-05T08:00:00Z"))
      .withMotifs(Set.of(MotifDeSignalement.DATE_FUTURE, MotifDeSignalement.OPERATEUR_NON_HABILITE))
      .withDateDeclaree(Instant.parse("2043-01-05T09:00:00Z"));

    inTransaction(() -> signalements.create(signale));

    assertThat(inTransaction(() -> signalements.get(signale.id()))).contains(signale);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRelireUnSignalementResolu() {
    PointageSignale signale = signale(new OperateurId(UUID.randomUUID()), Instant.parse("2043-01-06T08:00:00Z")).build();
    inTransaction(() -> signalements.create(signale));
    PointageSignale acquitte = signale.resolu(
      new Resolution(TypeDeResolution.ACQUITTE, AUTEUR_LEROY, Instant.parse("2043-01-07T09:00:00Z"))
    );

    inTransaction(() -> signalements.update(acquitte));

    assertThat(inTransaction(() -> signalements.get(signale.id()))).contains(acquitte);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserDeCreerDeuxFoisLeMemeSignalement() {
    PointageSignale signale = signale(new OperateurId(UUID.randomUUID()), Instant.parse("2043-01-08T08:00:00Z")).build();
    inTransaction(() -> signalements.create(signale));

    assertThatThrownBy(() -> inTransaction(() -> signalements.create(signale))).isExactlyInstanceOf(
      PointageSignaleDejaExistantException.class
    );
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserDeMettreAJourUnSignalementInconnu() {
    PointageSignale inconnu = signale(new OperateurId(UUID.randomUUID()), Instant.parse("2043-01-09T08:00:00Z")).build();

    assertThatThrownBy(() -> inTransaction(() -> signalements.update(inconnu))).isExactlyInstanceOf(
      PointageSignaleIntrouvableException.class
    );
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNePasTrouverUnSignalementInconnu() {
    assertThat(inTransaction(() -> signalements.get(new PointageSignaleId(UUID.randomUUID())))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldListerLesSignalementsNonResolusDUnOperateurLePlusRecentDAbord() {
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    PointageSignale matin = signale(operateur, Instant.parse("2043-01-10T08:00:00Z")).build();
    PointageSignale soir = signale(operateur, Instant.parse("2043-01-10T17:00:00Z"))
      .withMotifs(Set.of(MotifDeSignalement.OPERATEUR_NON_HABILITE))
      .build();
    PointageSignale resolu = signale(operateur, Instant.parse("2043-01-10T12:00:00Z"))
      .build()
      .resolu(new Resolution(TypeDeResolution.ANNULE, AUTEUR_LEROY, Instant.parse("2043-01-11T09:00:00Z")));
    PointageSignale autre = signale(new OperateurId(UUID.randomUUID()), Instant.parse("2043-01-10T13:00:00Z")).build();
    for (PointageSignale signalement : java.util.List.of(matin, soir, resolu, autre)) {
      inTransaction(() -> signalements.create(signalement));
    }

    Page<PointageSignale> tous = liste(operateur, Optional.empty(), new Pageable(0, 10));
    Page<PointageSignale> nonHabilites = liste(operateur, Optional.of(MotifDeSignalement.OPERATEUR_NON_HABILITE), new Pageable(0, 10));
    Page<PointageSignale> premier = liste(operateur, Optional.empty(), new Pageable(0, 1));

    assertThat(tous.content()).containsExactly(soir, matin);
    assertThat(nonHabilites.content()).containsExactly(soir);
    assertThat(premier.content()).containsExactly(soir);
    assertThat(premier.totalElementsCount()).isEqualTo(2);
  }

  private Page<PointageSignale> liste(OperateurId operateur, Optional<MotifDeSignalement> motif, Pageable pageable) {
    return inTransaction(() -> signalements.list(new CriteresDePointageSignale(Optional.of(operateur), motif), pageable));
  }

  private static Brouillon signale(OperateurId operateur, Instant date) {
    return new Brouillon(operateur, date, Set.of(MotifDeSignalement.DATE_FUTURE), Optional.empty());
  }

  private record Brouillon(OperateurId operateur, Instant date, Set<MotifDeSignalement> motifs, Optional<Instant> dateDeclaree) {
    Brouillon withMotifs(Set<MotifDeSignalement> autres) {
      return new Brouillon(operateur, date, autres, dateDeclaree);
    }

    PointageSignale withDateDeclaree(Instant declaree) {
      return new Brouillon(operateur, date, motifs, Optional.of(declaree)).build();
    }

    PointageSignale build() {
      return PointageSignale.builder()
        .id(new PointageSignaleId(UUID.randomUUID()))
        .cible(new CibleDuSignalement(TypeDeCible.JOURNEE_DE_TRAVAIL, UUID.randomUUID()))
        .operateur(operateur)
        .motifs(motifs)
        .horodatage(new Horodatage(date, date.plusSeconds(60)))
        .dateDeclaree(dateDeclaree)
        .resolution(Optional.empty());
    }
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
