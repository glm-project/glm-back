package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.AgregatDEvenement;
import com.glm.glmback.atelier.application.EmpreinteDEvenement;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.JourneesDeTravailApplicationService;
import com.glm.glmback.atelier.application.NatureDeGesteDuPupitre;
import com.glm.glmback.atelier.application.ReservationDEvenement;
import com.glm.glmback.atelier.application.ResultatDEcriture;
import com.glm.glmback.atelier.application.TypeDAgregatDEvenement;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.TenantSecurityContexts;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@IntegrationTest
class JpaIdentitesDEvenementsIT {

  @MockitoBean
  private Clock clock;

  @Autowired
  private IdentitesDEvenements identites;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private JourneesDeTravailApplicationService presence;

  @Autowired
  private JourneeDeTravailRepository journees;

  @Test
  @WithTenant("impeccmold")
  void shouldReserveThenReplayTheSameFingerprint() {
    UUID evenement = UUID.randomUUID();
    UUID journee = UUID.randomUUID();
    EmpreinteDEvenement empreinte = arrivee(Optional.of(Instant.parse("2042-01-01T08:00:00.123456789Z")));

    ReservationDEvenement premiere = inTransaction(() -> {
      ReservationDEvenement reservation = identites.reserve(evenement, empreinte);
      identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journee));
      return reservation;
    });
    ReservationDEvenement rejeu = inTransaction(() -> identites.reserve(evenement, empreinte));

    assertThat(premiere.estUnRejeu()).isFalse();
    assertThat(rejeu.agregat()).contains(new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journee));
  }

  @Test
  @WithTenant("impeccmold")
  void shouldPersistOnlyOneDepartureWhenItsRetryOverlapsTheFirstTransaction() throws Exception {
    given(clock.now()).willReturn(LE_11_MAI_2026_A_9H15);
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    JourneeDeTravail journee = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), operateur).enregistre(
      arriveeDeDupontA(LE_10_MAI_2026_A_7H)
    );
    inTransaction(() -> journees.create(journee));
    PointageDePresenceAEnregistrer depart = new PointageDePresenceAEnregistrer(
      operateur,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.DEPART,
      Optional.of(LE_10_MAI_2026_A_17H),
      EvenementDePresenceId.newId()
    );
    CountDownLatch ecriture = new CountDownLatch(1);
    CountDownLatch validation = new CountDownLatch(1);
    CountDownLatch tentative = new CountDownLatch(1);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    RequestAttributes requete = RequestContextHolder.getRequestAttributes();

    try (var executor = Executors.newFixedThreadPool(2)) {
      var premiere = executor.submit(() ->
        avecAuthentification(authentication, requete, () ->
          inTransaction(() -> {
            ResultatDEcriture<JourneeDeTravail> resultat = presence.pointeDuPupitre(depart);
            ecriture.countDown();
            attend(validation);
            return resultat;
          })
        )
      );
      try {
        attend(ecriture);
        var seconde = executor.submit(() ->
          avecAuthentification(authentication, requete, () -> {
            tentative.countDown();
            return presence.pointeDuPupitre(depart);
          })
        );
        attend(tentative);
        assertThatThrownBy(() -> seconde.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        validation.countDown();

        ResultatDEcriture<JourneeDeTravail> initial = premiere.get(5, TimeUnit.SECONDS);
        ResultatDEcriture<JourneeDeTravail> rejeu = seconde.get(5, TimeUnit.SECONDS);
        assertThat(initial.rejeu()).isFalse();
        assertThat(rejeu.rejeu()).isTrue();
        assertThat(rejeu.agregat().id()).isEqualTo(journee.id());
        JourneeDeTravail relue = presence.get(journee.id());
        assertThat(relue.journal().evenements()).hasSize(2);
        var evenement = relue.journal().evenements().getLast();
        assertThat(evenement.id()).isEqualTo(depart.evenement());
        assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H);
        assertThat(evenement.dateDEnregistrement()).isEqualTo(initial.agregat().journal().evenements().getLast().dateDEnregistrement());
        assertThat(rejeu.agregat()).isEqualTo(relue);
      } finally {
        validation.countDown();
      }
    }
  }

  private static void attend(CountDownLatch signal) {
    try {
      assertThat(signal.await(5, TimeUnit.SECONDS)).as("The concurrent transaction reached its rendezvous").isTrue();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
  }

  private static <T> T avecAuthentification(Authentication authentication, RequestAttributes requete, Supplier<T> action) {
    var contexte = SecurityContextHolder.createEmptyContext();
    contexte.setAuthentication(authentication);
    SecurityContextHolder.setContext(contexte);
    RequestContextHolder.setRequestAttributes(requete);
    try {
      return action.get();
    } finally {
      SecurityContextHolder.clearContext();
      RequestContextHolder.resetRequestAttributes();
    }
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejectAnotherFingerprintAndANonReplayableIdentity() {
    UUID evenement = UUID.randomUUID();
    inTransaction(() -> {
      identites.reserveHorsPupitre(evenement);
      return null;
    });

    assertThatThrownBy(() -> inTransaction(() -> identites.reserve(evenement, arrivee(Optional.empty())))).isExactlyInstanceOf(
      IdentifiantDEvenementReutiliseException.class
    );
  }

  @Test
  @WithTenant("impeccmold")
  void shouldDistinguishAnAbsentDateFromAPresentDate() {
    UUID evenement = UUID.randomUUID();
    inTransaction(() -> {
      identites.reserve(evenement, arrivee(Optional.empty()));
      identites.associe(evenement, new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, UUID.randomUUID()));
      return null;
    });

    assertThatThrownBy(() -> inTransaction(() -> identites.reserve(evenement, arrivee(Optional.of(Instant.EPOCH))))).isExactlyInstanceOf(
      IdentifiantDEvenementReutiliseException.class
    );
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNeverAllocateAServerIdentityTwice() {
    UUID evenement = UUID.randomUUID();

    assertThat(inTransaction(() -> identites.reserveHorsPupitre(evenement))).isTrue();
    assertThat(inTransaction(() -> identites.reserveHorsPupitre(evenement))).isFalse();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldKeepTheSameIdentityIndependentBetweenTenants() {
    UUID evenement = UUID.randomUUID();
    AgregatDEvenement premier = new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, UUID.randomUUID());
    AgregatDEvenement second = new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, UUID.randomUUID());
    EmpreinteDEvenement empreinte = arrivee(Optional.empty());
    inTransaction(() -> {
      identites.reserve(evenement, empreinte);
      identites.associe(evenement, premier);
      return null;
    });

    TenantSecurityContexts.authenticateOn("katilys");
    try {
      ReservationDEvenement reservation = inTransaction(() -> {
        ReservationDEvenement resultat = identites.reserve(evenement, empreinte);
        identites.associe(evenement, second);
        return resultat;
      });
      assertThat(reservation.estUnRejeu()).isFalse();
      assertThat(inTransaction(() -> identites.reserve(evenement, empreinte)).agregat()).contains(second);
    } finally {
      TenantSecurityContexts.authenticateOn("impeccmold");
    }
    assertThat(inTransaction(() -> identites.reserve(evenement, empreinte)).agregat()).contains(premier);
  }

  private static EmpreinteDEvenement arrivee(Optional<Instant> date) {
    return EmpreinteDEvenement.builder()
      .nature(NatureDeGesteDuPupitre.ARRIVEE)
      .cible(Optional.empty())
      .operateur(UUID.fromString("00000000-0000-0000-0000-000000000001"))
      .type("ARRIVEE")
      .poste(Optional.empty())
      .dateDeSurvenue(date);
  }

  private <T> T inTransaction(java.util.function.Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
