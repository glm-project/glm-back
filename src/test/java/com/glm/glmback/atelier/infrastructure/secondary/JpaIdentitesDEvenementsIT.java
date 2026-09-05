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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
    // GIVEN
    UUID evenement = UUID.randomUUID();
    UUID journee = UUID.randomUUID();
    EmpreinteDEvenement empreinte = arrivee(Optional.of(Instant.parse("2042-01-01T08:00:00.123456789Z")));

    AgregatDEvenement agregat = journeeIdentifieePar(journee);
    // WHEN
    ReservationDEvenement premiere = reserveEtAssocie(evenement, empreinte, agregat);
    ReservationDEvenement rejeu = inTransaction(() -> identites.reserve(evenement, empreinte));

    // THEN
    assertThat(premiere.estUnRejeu()).isFalse();
    assertThat(rejeu.agregat()).contains(agregat);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldPersistOnlyOneDepartureWhenItsRetryOverlapsTheFirstTransaction() throws Exception {
    // GIVEN
    given(clock.now()).willReturn(LE_11_MAI_2026_A_9H15);
    JourneeDeTravail journee = prepareJourneeOuverte();
    PointageDePresenceAEnregistrer depart = departA17H(journee);

    try (RejeuConcurrent envois = new RejeuConcurrent()) {
      // WHEN
      var premiere = envois.enregistreSansValider(depart);
      var seconde = envois.rejouePendantLaPremiereTransaction(depart);
      envois.validePremiereTransaction();

      var initial = premiere.get(5, TimeUnit.SECONDS);
      var rejeu = seconde.get(5, TimeUnit.SECONDS);

      // THEN
      assertThat(rejeu.agregat().id()).isEqualTo(journee.id());
      assertUnSeulDepartPersiste(initial, rejeu, depart);
    }
  }

  private JourneeDeTravail prepareJourneeOuverte() {
    JourneeDeTravail journee = JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), new OperateurId(UUID.randomUUID())).enregistre(
      arriveeDeDupontA(LE_10_MAI_2026_A_7H)
    );
    return inTransaction(() -> journees.create(journee));
  }

  private static PointageDePresenceAEnregistrer departA17H(JourneeDeTravail journee) {
    return new PointageDePresenceAEnregistrer(
      journee.operateur(),
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.DEPART,
      Optional.of(LE_10_MAI_2026_A_17H),
      EvenementDePresenceId.newId()
    );
  }

  private void assertUnSeulDepartPersiste(
    ResultatDEcriture<JourneeDeTravail> initial,
    ResultatDEcriture<JourneeDeTravail> rejeu,
    PointageDePresenceAEnregistrer depart
  ) {
    assertThat(initial.rejeu()).isFalse();
    assertThat(rejeu.rejeu()).isTrue();
    JourneeDeTravail relue = presence.get(initial.agregat().id());
    assertThat(relue.journal().evenements()).hasSize(2);
    var evenement = relue.journal().evenements().getLast();
    assertThat(evenement.id()).isEqualTo(depart.evenement());
    assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H);
    assertThat(evenement.dateDEnregistrement()).isEqualTo(initial.agregat().journal().evenements().getLast().dateDEnregistrement());
    assertThat(rejeu.agregat()).isEqualTo(relue);
  }

  private final class RejeuConcurrent implements AutoCloseable {

    private final CountDownLatch ecriture = new CountDownLatch(1);
    private final CountDownLatch validation = new CountDownLatch(1);
    private final CountDownLatch tentative = new CountDownLatch(1);
    private final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    private final RequestAttributes requete = RequestContextHolder.getRequestAttributes();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<ResultatDEcriture<JourneeDeTravail>> enregistreSansValider(PointageDePresenceAEnregistrer depart) {
      return executor.submit(() ->
        avecAuthentification(authentication, requete, () -> inTransaction(() -> pointeEtAttendValidation(depart)))
      );
    }

    Future<ResultatDEcriture<JourneeDeTravail>> rejouePendantLaPremiereTransaction(PointageDePresenceAEnregistrer depart) {
      attend(ecriture);
      var rejeu = executor.submit(() -> avecAuthentification(authentication, requete, () -> tenteRejeu(depart)));
      attend(tentative);
      assertAttendLaValidation(rejeu);
      return rejeu;
    }

    void validePremiereTransaction() {
      validation.countDown();
    }

    private ResultatDEcriture<JourneeDeTravail> pointeEtAttendValidation(PointageDePresenceAEnregistrer depart) {
      var resultat = presence.pointeDuPupitre(depart);
      ecriture.countDown();
      attend(validation);
      return resultat;
    }

    private ResultatDEcriture<JourneeDeTravail> tenteRejeu(PointageDePresenceAEnregistrer depart) {
      tentative.countDown();
      return presence.pointeDuPupitre(depart);
    }

    private void assertAttendLaValidation(Future<ResultatDEcriture<JourneeDeTravail>> rejeu) {
      assertThatThrownBy(() -> rejeu.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
    }

    @Override
    public void close() {
      validePremiereTransaction();
      executor.close();
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
    // GIVEN
    UUID evenement = UUID.randomUUID();
    inTransaction(() -> {
      identites.reserveHorsPupitre(evenement);
      return null;
    });

    // WHEN
    Throwable refus = catchThrowable(() -> inTransaction(() -> identites.reserve(evenement, arrivee(Optional.empty()))));

    // THEN
    assertThat(refus).isExactlyInstanceOf(IdentifiantDEvenementReutiliseException.class);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldDistinguishAnAbsentDateFromAPresentDate() {
    // GIVEN
    UUID evenement = UUID.randomUUID();
    reserveEtAssocie(evenement, arrivee(Optional.empty()), journeeIdentifieePar(UUID.randomUUID()));

    // WHEN
    Throwable refus = catchThrowable(() -> inTransaction(() -> identites.reserve(evenement, arrivee(Optional.of(Instant.EPOCH)))));

    // THEN
    assertThat(refus).isExactlyInstanceOf(IdentifiantDEvenementReutiliseException.class);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldNeverAllocateAServerIdentityTwice() {
    // GIVEN
    UUID evenement = UUID.randomUUID();

    // WHEN
    boolean premiere = inTransaction(() -> identites.reserveHorsPupitre(evenement));
    boolean seconde = inTransaction(() -> identites.reserveHorsPupitre(evenement));

    // THEN
    assertThat(premiere).isTrue();
    assertThat(seconde).isFalse();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldKeepTheSameIdentityIndependentBetweenTenants() {
    // GIVEN
    UUID evenement = UUID.randomUUID();
    AgregatDEvenement premier = journeeIdentifieePar(UUID.randomUUID());
    AgregatDEvenement second = journeeIdentifieePar(UUID.randomUUID());
    EmpreinteDEvenement empreinte = arrivee(Optional.empty());
    reserveEtAssocie(evenement, empreinte, premier);

    // WHEN
    TenantSecurityContexts.authenticateOn("katilys");
    try {
      ReservationDEvenement reservation = reserveEtAssocie(evenement, empreinte, second);
      ReservationDEvenement rejeu = inTransaction(() -> identites.reserve(evenement, empreinte));

      // THEN
      assertThat(reservation.estUnRejeu()).isFalse();
      assertThat(rejeu.agregat()).contains(second);
    } finally {
      TenantSecurityContexts.authenticateOn("impeccmold");
    }
    assertThat(inTransaction(() -> identites.reserve(evenement, empreinte)).agregat()).contains(premier);
  }

  private ReservationDEvenement reserveEtAssocie(UUID evenement, EmpreinteDEvenement empreinte, AgregatDEvenement agregat) {
    return inTransaction(() -> {
      ReservationDEvenement reservation = identites.reserve(evenement, empreinte);
      identites.associe(evenement, agregat);
      return reservation;
    });
  }

  private static AgregatDEvenement journeeIdentifieePar(UUID id) {
    return new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, id);
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
