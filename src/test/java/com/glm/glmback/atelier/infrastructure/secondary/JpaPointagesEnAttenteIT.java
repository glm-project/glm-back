package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Application;
import com.glm.glmback.atelier.domain.CriteresDePointageEnAttente;
import com.glm.glmback.atelier.domain.Ecart;
import com.glm.glmback.atelier.domain.GesteDAtelier;
import com.glm.glmback.atelier.domain.GesteDePresence;
import com.glm.glmback.atelier.domain.GesteEnAttente;
import com.glm.glmback.atelier.domain.MotifDEcart;
import com.glm.glmback.atelier.domain.MotifDeMiseEnAttente;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageEnAttente;
import com.glm.glmback.atelier.domain.PointageEnAttenteDejaExistantException;
import com.glm.glmback.atelier.domain.PointageEnAttenteId;
import com.glm.glmback.atelier.domain.PointageEnAttenteIntrouvableException;
import com.glm.glmback.atelier.domain.PointagesEnAttente;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
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

/**
 * Le schema est partage : chaque test se donne son propre operateur, qui borne a lui seul ce qu'il liste.
 */
@IntegrationTest
class JpaPointagesEnAttenteIT {

  @Autowired
  private PointagesEnAttente pointages;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRelireUnGesteDePresence() {
    PointageEnAttente recu = recu(
      new GesteDePresence(
        new OperateurId(UUID.randomUUID()),
        TypeDEvenementDePresence.PAUSE,
        Optional.of(Instant.parse("2044-01-05T12:00:00Z"))
      ),
      MotifDeMiseEnAttente.OPERATEUR_INCONNU,
      Instant.parse("2044-01-05T12:00:01Z")
    );

    inTransaction(() -> pointages.create(recu));

    assertThat(inTransaction(() -> pointages.get(recu.id()))).contains(recu);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRelireUnGesteDAtelierSansPosteNiDate() {
    PointageEnAttente recu = recu(
      GesteDAtelier.builder()
        .suivi(SuiviDAtelierId.newId())
        .operateur(new OperateurId(UUID.randomUUID()))
        .type(TypeDEvenementDAtelier.NON_CONFORMITE)
        .poste(Optional.empty())
        .dateDeclaree(Optional.empty()),
      MotifDeMiseEnAttente.ELEMENT_INCONNU,
      Instant.parse("2044-01-06T08:00:00Z")
    );

    inTransaction(() -> pointages.create(recu));

    assertThat(inTransaction(() -> pointages.get(recu.id()))).contains(recu);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRelireUnGesteDAtelierAvecPoste() {
    PointageEnAttente recu = recu(
      GesteDAtelier.builder()
        .suivi(SuiviDAtelierId.newId())
        .operateur(new OperateurId(UUID.randomUUID()))
        .type(TypeDEvenementDAtelier.DEBUT)
        .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
        .dateDeclaree(Optional.of(Instant.parse("2044-01-06T07:30:00Z"))),
      MotifDeMiseEnAttente.POSTE_INCONNU,
      Instant.parse("2044-01-06T08:00:00Z")
    );

    inTransaction(() -> pointages.create(recu));

    assertThat(inTransaction(() -> pointages.get(recu.id()))).contains(recu);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRelireUnPointageApplique() {
    PointageEnAttente recu = recuDe(new OperateurId(UUID.randomUUID()), Instant.parse("2044-01-07T08:00:00Z"));
    inTransaction(() -> pointages.create(recu));
    PointageEnAttente applique = recu.traite(new Application(AUTEUR_LEROY, Instant.parse("2044-01-08T09:00:00Z")));

    inTransaction(() -> pointages.update(applique));

    assertThat(inTransaction(() -> pointages.get(recu.id()))).contains(applique);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRelireUnPointageEcarte() {
    PointageEnAttente recu = recuDe(new OperateurId(UUID.randomUUID()), Instant.parse("2044-01-07T09:00:00Z"));
    inTransaction(() -> pointages.create(recu));
    PointageEnAttente ecarte = recu.traite(new Ecart(AUTEUR_LEROY, Instant.parse("2044-01-08T09:00:00Z"), new MotifDEcart("Doublon")));

    inTransaction(() -> pointages.update(ecarte));

    assertThat(inTransaction(() -> pointages.get(recu.id()))).contains(ecarte);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserDeCreerDeuxFoisLeMemePointage() {
    PointageEnAttente recu = recuDe(new OperateurId(UUID.randomUUID()), Instant.parse("2044-01-09T08:00:00Z"));
    inTransaction(() -> pointages.create(recu));

    assertThatThrownBy(() -> inTransaction(() -> pointages.create(recu))).isExactlyInstanceOf(PointageEnAttenteDejaExistantException.class);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRefuserDeMettreAJourUnPointageInconnu() {
    PointageEnAttente inconnu = recuDe(new OperateurId(UUID.randomUUID()), Instant.parse("2044-01-09T09:00:00Z"));

    assertThatThrownBy(() -> inTransaction(() -> pointages.update(inconnu))).isExactlyInstanceOf(
      PointageEnAttenteIntrouvableException.class
    );
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNePasTrouverUnPointageInconnu() {
    assertThat(inTransaction(() -> pointages.get(PointageEnAttenteId.newId()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRetrouverLesPointagesDUnIdentifiantDuPupitre() {
    UUID evenement = UUID.randomUUID();
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    PointageEnAttente pause = recu(
      evenement,
      new GesteDePresence(operateur, TypeDEvenementDePresence.PAUSE, Optional.empty()),
      MotifDeMiseEnAttente.IDENTIFIANT_REUTILISE,
      Instant.parse("2044-01-10T08:00:00Z")
    );
    PointageEnAttente depart = recu(
      evenement,
      new GesteDePresence(operateur, TypeDEvenementDePresence.DEPART, Optional.empty()),
      MotifDeMiseEnAttente.IDENTIFIANT_REUTILISE,
      Instant.parse("2044-01-10T09:00:00Z")
    );
    inTransaction(() -> pointages.create(pause));
    inTransaction(() -> pointages.create(depart));

    assertThat(inTransaction(() -> pointages.parEvenementDuPupitre(evenement))).containsExactlyInAnyOrder(pause, depart);
    assertThat(inTransaction(() -> pointages.parEvenementDuPupitre(UUID.randomUUID()))).isEmpty();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldListerLesPointagesNonTraitesDUnOperateurLePlusRecentDAbord() {
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    PointageEnAttente matin = recu(
      new GesteDePresence(operateur, TypeDEvenementDePresence.PAUSE, Optional.empty()),
      MotifDeMiseEnAttente.OPERATEUR_INCONNU,
      Instant.parse("2044-01-11T08:00:00Z")
    );
    PointageEnAttente soir = recu(
      new GesteDePresence(operateur, TypeDEvenementDePresence.DEPART, Optional.empty()),
      MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE,
      Instant.parse("2044-01-11T17:00:00Z")
    );
    PointageEnAttente traite = recuDe(operateur, Instant.parse("2044-01-11T12:00:00Z")).traite(
      new Application(AUTEUR_LEROY, Instant.parse("2044-01-12T09:00:00Z"))
    );
    PointageEnAttente autre = recuDe(new OperateurId(UUID.randomUUID()), Instant.parse("2044-01-11T13:00:00Z"));
    for (PointageEnAttente pointage : List.of(matin, soir, traite, autre)) {
      inTransaction(() -> pointages.create(pointage));
    }

    Page<PointageEnAttente> tous = liste(operateur, Optional.empty(), new Pageable(0, 10));
    Page<PointageEnAttente> horsSequence = liste(operateur, Optional.of(MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE), new Pageable(0, 10));
    Page<PointageEnAttente> premier = liste(operateur, Optional.empty(), new Pageable(0, 1));

    assertThat(tous.content()).containsExactly(soir, matin);
    assertThat(horsSequence.content()).containsExactly(soir);
    assertThat(premier.content()).containsExactly(soir);
    assertThat(premier.totalElementsCount()).isEqualTo(2);
  }

  /**
   * Deux pointages recus au meme instant : l'identifiant les departage, sans quoi la page 2 pourrait repeter la 1.
   */
  @Test
  @WithTenant("impeccmold")
  void shouldDepartagerParIdentifiantDeuxPointagesRecusEnsemble() {
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    Instant ensemble = Instant.parse("2044-01-13T08:00:00Z");
    PointageEnAttente premier = recu(PointageEnAttenteId.newId(), operateur, ensemble);
    PointageEnAttente second = recu(PointageEnAttenteId.newId(), operateur, ensemble);
    inTransaction(() -> pointages.create(premier));
    inTransaction(() -> pointages.create(second));

    List<PointageEnAttente> attendus = premier.id().compareTo(second.id()) < 0 ? List.of(premier, second) : List.of(second, premier);
    assertThat(liste(operateur, Optional.empty(), new Pageable(0, 10)).content()).containsExactlyElementsOf(attendus);
  }

  private Page<PointageEnAttente> liste(OperateurId operateur, Optional<MotifDeMiseEnAttente> motif, Pageable pageable) {
    return inTransaction(() -> pointages.list(new CriteresDePointageEnAttente(Optional.of(operateur), motif), pageable));
  }

  private static PointageEnAttente recuDe(OperateurId operateur, Instant reception) {
    return recu(
      new GesteDePresence(operateur, TypeDEvenementDePresence.PAUSE, Optional.empty()),
      MotifDeMiseEnAttente.OPERATEUR_INCONNU,
      reception
    );
  }

  private static PointageEnAttente recu(PointageEnAttenteId id, OperateurId operateur, Instant reception) {
    return PointageEnAttente.builder()
      .id(id)
      .evenementDuPupitre(UUID.randomUUID())
      .geste(new GesteDePresence(operateur, TypeDEvenementDePresence.PAUSE, Optional.empty()))
      .motif(MotifDeMiseEnAttente.OPERATEUR_INCONNU)
      .auteur(AUTEUR_DUPONT)
      .dateDeReception(reception)
      .traitement(Optional.empty());
  }

  private static PointageEnAttente recu(GesteEnAttente geste, MotifDeMiseEnAttente motif, Instant reception) {
    return recu(UUID.randomUUID(), geste, motif, reception);
  }

  private static PointageEnAttente recu(UUID evenement, GesteEnAttente geste, MotifDeMiseEnAttente motif, Instant reception) {
    return PointageEnAttente.builder()
      .id(PointageEnAttenteId.newId())
      .evenementDuPupitre(evenement)
      .geste(geste)
      .motif(motif)
      .auteur(AUTEUR_DUPONT)
      .dateDeReception(reception)
      .traitement(Optional.empty());
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
