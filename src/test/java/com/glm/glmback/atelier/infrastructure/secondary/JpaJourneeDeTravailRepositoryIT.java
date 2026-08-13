package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.EvenementDePresence;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailCriteria;
import com.glm.glmback.atelier.domain.JourneeDeTravailDejaOuverteException;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailIntrouvableException;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Meme discipline que pour les suivis : le schema etant partage, chaque test se donne son propre operateur, qui borne
 * a lui seul tout ce qu'il liste.
 */
@IntegrationTest
class JpaJourneeDeTravailRepositoryIT {

  private static final String IMPECCMOLD = "impeccmold";
  private static final String KATILYS = "katilys";
  private static final AtomicLong COMPTEUR = new AtomicLong();

  @Autowired
  private JourneeDeTravailRepository journees;

  @Autowired
  private TransactionTemplate transactions;

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUneJourneeOuverte() {
    OperateurId operateur = operateurDeTest();
    JourneeDeTravail ouverte = journeeOuverteA(operateur, Instant.parse("2041-01-05T07:00:00Z"));

    inTransaction(() -> journees.create(ouverte));

    JourneeDeTravail relue = inTransaction(() -> journees.get(ouverte.id())).orElseThrow();
    assertThat(relue).isEqualTo(ouverte);
    assertThat(relue.etat()).isEqualTo(EtatDePresence.PRESENT);
    assertThat(relue.amplitude()).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRelireUneJourneeCompleteAvecSonAmplitudeEtSesFenetres() {
    OperateurId operateur = operateurDeTest();
    Instant arrivee = Instant.parse("2041-01-06T07:00:00Z");
    JourneeDeTravail complete = journeeCompleteA(operateur, arrivee);

    inTransaction(() -> journees.create(complete));

    JourneeDeTravail relue = inTransaction(() -> journees.get(complete.id())).orElseThrow();
    assertThat(relue).isEqualTo(complete);
    assertThat(relue.amplitude()).contains(new Periode(arrivee, arrivee.plusSeconds(36000)));
    assertThat(relue.fenetres()).hasSize(2);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldConserverUnEvenementDePresenceAnnuleAuJournal() {
    OperateurId operateur = operateurDeTest();
    Instant arrivee = Instant.parse("2041-01-07T07:00:00Z");
    EvenementDePresence pointage = presence(TypeDEvenementDePresence.ARRIVEE, arrivee);
    JourneeDeTravail ouverte = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), operateur).enregistre(pointage);
    inTransaction(() -> journees.create(ouverte));

    JourneeDeTravail annulee = ouverte.annule(
      pointage.id(),
      new Annulation(AUTEUR_LEROY, arrivee.plusSeconds(7200), MOTIF_ERREUR_DE_SAISIE)
    );
    inTransaction(() -> journees.update(annulee));

    JourneeDeTravail relue = inTransaction(() -> journees.get(ouverte.id())).orElseThrow();
    assertThat(relue).isEqualTo(annulee);
    assertThat(relue.journal().evenements()).hasSize(1);
    assertThat(relue.debut()).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRefuserDeCreerDeuxFoisLaMemeIdentite() {
    JourneeDeTravail ouverte = journeeOuverteA(operateurDeTest(), Instant.parse("2041-01-08T07:00:00Z"));
    inTransaction(() -> journees.create(ouverte));

    assertThatThrownBy(() -> inTransaction(() -> journees.create(ouverte))).isExactlyInstanceOf(JourneeDeTravailDejaOuverteException.class);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRefuserDeMettreAJourUneJourneeInconnue() {
    JourneeDeTravail inconnue = journeeOuverteA(operateurDeTest(), Instant.parse("2041-01-09T07:00:00Z"));

    assertThatThrownBy(() -> inTransaction(() -> journees.update(inconnue))).isExactlyInstanceOf(
      JourneeDeTravailIntrouvableException.class
    );
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNePasTrouverUneJourneeInconnue() {
    assertThat(inTransaction(() -> journees.get(JourneeDeTravailId.newId()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldTrouverLaJourneeEnCoursDUnOperateur() {
    OperateurId operateur = operateurDeTest();
    JourneeDeTravail ouverte = journeeOuverteA(operateur, Instant.parse("2041-02-05T07:00:00Z"));
    inTransaction(() -> journees.create(ouverte));

    assertThat(inTransaction(() -> journees.getEnCoursPour(operateur))).contains(ouverte);
    assertThat(inTransaction(() -> journees.getEnCoursPour(operateurDeTest()))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldNePasTrouverDeJourneeEnCoursApresLeDepart() {
    OperateurId operateur = operateurDeTest();
    inTransaction(() -> journees.create(journeeCompleteA(operateur, Instant.parse("2041-02-06T07:00:00Z"))));

    assertThat(inTransaction(() -> journees.getEnCoursPour(operateur))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldTrouverLaJourneeContenantUnInstant() {
    OperateurId operateur = operateurDeTest();
    Instant arrivee = Instant.parse("2041-02-07T07:00:00Z");
    JourneeDeTravail complete = journeeCompleteA(operateur, arrivee);
    inTransaction(() -> journees.create(complete));

    assertThat(inTransaction(() -> journees.journeeContenant(operateur, arrivee))).contains(complete);
    assertThat(inTransaction(() -> journees.journeeContenant(operateur, arrivee.plusSeconds(18000)))).contains(complete);
    assertThat(inTransaction(() -> journees.journeeContenant(operateur, arrivee.plusSeconds(36000)))).contains(complete);
    assertThat(inTransaction(() -> journees.journeeContenant(operateur, arrivee.minusSeconds(1)))).isEmpty();
    assertThat(inTransaction(() -> journees.journeeContenant(operateur, arrivee.plusSeconds(36001)))).isEmpty();
    assertThat(inTransaction(() -> journees.journeeContenant(operateurDeTest(), arrivee))).isEmpty();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldTrouverLaJourneeEncoreOuverteContenantUnInstantTardif() {
    OperateurId operateur = operateurDeTest();
    Instant arrivee = Instant.parse("2041-02-08T07:00:00Z");
    JourneeDeTravail ouverte = journeeOuverteA(operateur, arrivee);
    inTransaction(() -> journees.create(ouverte));

    assertThat(inTransaction(() -> journees.journeeContenant(operateur, arrivee.plusSeconds(864000)))).contains(ouverte);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListerLesJourneesDUnOperateurDuPlusRecentAuPlusAncien() {
    OperateurId operateur = operateurDeTest();
    JourneeDeTravail ancienne = journeeOuverteA(operateur, Instant.parse("2041-03-05T07:00:00Z"));
    JourneeDeTravail recente = journeeOuverteA(operateur, Instant.parse("2041-03-06T07:00:00Z"));
    inTransaction(() -> journees.create(ancienne));
    inTransaction(() -> journees.create(recente));

    Page<JourneeDeTravail> page = inTransaction(() -> journees.list(criteres(Optional.empty(), operateur), firstPageOfTen()));

    assertThat(page.content()).containsExactly(recente, ancienne);
    assertThat(page.totalElementsCount()).isEqualTo(2);
  }

  /**
   * Une journee dont tous les evenements ont ete annules n'a plus de debut. Le double en memoire la replie sur
   * {@code Instant.MIN} et la place donc en dernier ; sans un tri qui le dise, PostgreSQL ouvrirait la liste avec.
   */
  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListerEnDernierUneJourneeSansDebut() {
    OperateurId operateur = operateurDeTest();
    Instant arrivee = Instant.parse("2041-03-07T07:00:00Z");
    EvenementDePresence pointage = presence(TypeDEvenementDePresence.ARRIVEE, arrivee);
    JourneeDeTravail sansDebut = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), operateur).enregistre(pointage);
    JourneeDeTravail datee = journeeOuverteA(operateur, arrivee.plusSeconds(86400));
    inTransaction(() -> journees.create(sansDebut));
    inTransaction(() -> journees.create(datee));
    JourneeDeTravail annulee = sansDebut.annule(
      pointage.id(),
      new Annulation(AUTEUR_LEROY, arrivee.plusSeconds(7200), MOTIF_ERREUR_DE_SAISIE)
    );
    inTransaction(() -> journees.update(annulee));

    Page<JourneeDeTravail> page = inTransaction(() -> journees.list(criteres(Optional.empty(), operateur), firstPageOfTen()));

    assertThat(page.content()).containsExactly(datee, annulee);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldFiltrerLesJourneesParPeriodeDeDebut() {
    OperateurId operateur = operateurDeTest();
    Instant lundi = Instant.parse("2041-04-05T07:00:00Z");
    Instant mardi = Instant.parse("2041-04-06T07:00:00Z");
    inTransaction(() -> journees.create(journeeOuverteA(operateur, lundi)));
    JourneeDeTravail duMardi = journeeOuverteA(operateur, mardi);
    inTransaction(() -> journees.create(duMardi));

    Page<JourneeDeTravail> page = inTransaction(() ->
      journees.list(criteres(Optional.of(new Periode(mardi, mardi)), operateur), firstPageOfTen())
    );

    assertThat(page.content()).containsExactly(duMardi);
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldListerSansAucunFiltre() {
    inTransaction(() -> journees.create(journeeOuverteA(operateurDeTest(), Instant.parse("2041-04-07T07:00:00Z"))));

    Page<JourneeDeTravail> page = inTransaction(() ->
      journees.list(new JourneeDeTravailCriteria(Optional.empty(), Optional.empty()), firstPageOfTen())
    );

    assertThat(page.totalElementsCount()).isPositive();
  }

  @Test
  @WithTenant(IMPECCMOLD)
  void shouldRefuserUneSaisieCalculeeSurUnJournalPerime() {
    OperateurId operateur = operateurDeTest();
    Instant arrivee = Instant.parse("2041-05-05T07:00:00Z");
    JourneeDeTravail ouverte = journeeOuverteA(operateur, arrivee);
    inTransaction(() -> journees.create(ouverte));

    JourneeDeTravail premiereSaisie = ouverte.enregistre(presence(TypeDEvenementDePresence.PAUSE, arrivee.plusSeconds(18000)));
    JourneeDeTravail secondeSaisie = ouverte.enregistre(presence(TypeDEvenementDePresence.DEPART, arrivee.plusSeconds(36000)));
    inTransaction(() -> journees.update(premiereSaisie));

    assertThatThrownBy(() -> inTransaction(() -> journees.update(secondeSaisie))).isExactlyInstanceOf(SaisieConcurrenteException.class);
  }

  @Test
  void shouldNePasLireLaJourneeDUneAutreEntreprise() {
    OperateurId operateur = operateurDeTest();
    JourneeDeTravail ouverte = journeeOuverteA(operateur, Instant.parse("2041-06-05T07:00:00Z"));

    TenantSecurityContexts.authenticateOn(IMPECCMOLD);
    inTransaction(() -> journees.create(ouverte));

    TenantSecurityContexts.authenticateOn(KATILYS);
    Optional<JourneeDeTravail> chezKatilys = inTransaction(() -> journees.get(ouverte.id()));
    Page<JourneeDeTravail> pageChezKatilys = inTransaction(() -> journees.list(criteres(Optional.empty(), operateur), firstPageOfTen()));

    assertThat(chezKatilys).isEmpty();
    assertThat(pageChezKatilys.content()).isEmpty();
  }

  private static JourneeDeTravailCriteria criteres(Optional<Periode> periode, OperateurId operateur) {
    return new JourneeDeTravailCriteria(periode, Optional.of(operateur));
  }

  private static OperateurId operateurDeTest() {
    return new OperateurId(new UUID(COMPTEUR.incrementAndGet(), 0));
  }

  private static JourneeDeTravail journeeOuverteA(OperateurId operateur, Instant arrivee) {
    return JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), operateur).enregistre(presence(TypeDEvenementDePresence.ARRIVEE, arrivee));
  }

  private static JourneeDeTravail journeeCompleteA(OperateurId operateur, Instant arrivee) {
    return journeeOuverteA(operateur, arrivee)
      .enregistre(presence(TypeDEvenementDePresence.PAUSE, arrivee.plusSeconds(18000)))
      .enregistre(presence(TypeDEvenementDePresence.REPRISE, arrivee.plusSeconds(21600)))
      .enregistre(presence(TypeDEvenementDePresence.DEPART, arrivee.plusSeconds(36000)));
  }

  private static EvenementDePresence presence(TypeDEvenementDePresence type, Instant date) {
    return EvenementDePresence.builder()
      .id(EvenementDePresenceId.newId())
      .type(type)
      .auteur(AUTEUR_DUPONT)
      .horodatage(Horodatage.saisiA(date));
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
