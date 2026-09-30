package com.glm.glmback.atelier.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.AgregatDEvenement;
import com.glm.glmback.atelier.application.EmpreinteDEvenement;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.NatureDeGesteDuPupitre;
import com.glm.glmback.atelier.application.ReservationDEvenement;
import com.glm.glmback.atelier.application.ResultatDEcriture;
import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.application.TypeDAgregatDEvenement;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.operateur.domain.OperateurRepository;
import com.glm.glmback.operateur.domain.OperateursFixture;
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

  private static final UUID OPERATEUR = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID SUIVI = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID CIBLE = UUID.fromString("00000000-0000-0000-0000-000000000003");
  private static final UUID AUTRE_CIBLE = UUID.fromString("00000000-0000-0000-0000-000000000004");

  @MockitoBean
  private Clock clock;

  @Autowired
  private IdentitesDEvenements identites;

  @Autowired
  private TransactionTemplate transactions;

  @Autowired
  private SuivisDAtelierApplicationService atelier;

  @Autowired
  private SuiviDAtelierRepository suivis;

  @Autowired
  private OperateurRepository operateurs;

  @Test
  @WithTenant("impeccmold")
  void shouldReserveThenReplayTheSameFingerprint() {
    // GIVEN
    UUID evenement = UUID.randomUUID();
    UUID suivi = UUID.randomUUID();
    EmpreinteDEvenement empreinte = finDatee(Optional.of(Instant.parse("2042-01-01T08:00:00.123456789Z")));

    AgregatDEvenement agregat = suiviIdentifiePar(suivi);
    // WHEN
    ReservationDEvenement premiere = reserveEtAssocie(evenement, empreinte, agregat);
    ReservationDEvenement rejeu = inTransaction(() -> identites.reserve(evenement, empreinte));

    // THEN
    assertThat(premiere.estUnRejeu()).isFalse();
    assertThat(rejeu.agregat()).contains(agregat);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldPersistOnlyOneTargetedFinishWhenItsRetryOverlapsTheFirstTransaction() throws Exception {
    // GIVEN
    given(clock.now()).willReturn(LE_11_MAI_2026_A_9H15);
    SuiviDAtelier suivi = prepareActiviteOuverte();
    PointageAEnregistrer fin = finA17H(suivi);

    try (RejeuConcurrent envois = new RejeuConcurrent()) {
      // WHEN
      var premiere = envois.enregistreSansValider(fin);
      var seconde = envois.rejouePendantLaPremiereTransaction(fin);
      envois.validePremiereTransaction();

      var initial = premiere.get(5, TimeUnit.SECONDS);
      var rejeu = seconde.get(5, TimeUnit.SECONDS);

      // THEN
      assertThat(rejeu.agregat().suivi().id()).isEqualTo(suivi.id());
      assertUneSeuleFinPersiste(initial, rejeu, fin);
    }
  }

  private SuiviDAtelier prepareActiviteOuverte() {
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    // La fiche et le fait ouvrant portent la meme identite, sans arrivee ni poste.
    var fiche = OperateursFixture.operateurDeRejeuSansPoste(new com.glm.glmback.operateur.domain.OperateurId(OPERATEUR_ID_DUPONT.uuid()));
    inTransaction(() -> operateurs.create(fiche));
    return inTransaction(() -> suivis.create(suiviDAtelierEngage().enregistre(debut)));
  }

  private static PointageAEnregistrer finA17H(SuiviDAtelier suivi) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(suivi.id())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activiteVisee(Optional.of(new ActiviteId(suivi.journal().evenements().getFirst().id().uuid())))
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .dateDeSurvenue(Optional.of(LE_10_MAI_2026_A_17H))
      .evenement(EvenementDAtelierId.newId());
  }

  private void assertUneSeuleFinPersiste(
    ResultatDEcriture<LectureDuSuivi> initial,
    ResultatDEcriture<LectureDuSuivi> rejeu,
    PointageAEnregistrer fin
  ) {
    assertThat(initial.rejeu()).isFalse();
    assertThat(rejeu.rejeu()).isTrue();
    SuiviDAtelier relu = atelier.get(initial.agregat().suivi().id()).suivi();
    assertThat(relu.journal().evenements()).hasSize(2);
    var evenement = relu.journal().evenements().getLast();
    assertThat(evenement.id()).isEqualTo(fin.evenement());
    assertThat(evenement.activiteVisee()).isEqualTo(fin.activiteVisee());
    assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H);
    assertThat(evenement.dateDEnregistrement()).isEqualTo(initial.agregat().suivi().journal().evenements().getLast().dateDEnregistrement());
    assertThat(rejeu.agregat().suivi()).isEqualTo(relu);
  }

  private final class RejeuConcurrent implements AutoCloseable {

    private final CountDownLatch ecriture = new CountDownLatch(1);
    private final CountDownLatch validation = new CountDownLatch(1);
    private final CountDownLatch tentative = new CountDownLatch(1);
    private final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    private final RequestAttributes requete = RequestContextHolder.getRequestAttributes();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    Future<ResultatDEcriture<LectureDuSuivi>> enregistreSansValider(PointageAEnregistrer fin) {
      return executor.submit(() -> avecAuthentification(authentication, requete, () -> inTransaction(() -> pointeEtAttendValidation(fin))));
    }

    Future<ResultatDEcriture<LectureDuSuivi>> rejouePendantLaPremiereTransaction(PointageAEnregistrer fin) {
      attend(ecriture);
      var rejeu = executor.submit(() -> avecAuthentification(authentication, requete, () -> tenteRejeu(fin)));
      attend(tentative);
      assertAttendLaValidation(rejeu);
      return rejeu;
    }

    void validePremiereTransaction() {
      validation.countDown();
    }

    private ResultatDEcriture<LectureDuSuivi> pointeEtAttendValidation(PointageAEnregistrer fin) {
      var resultat = atelier.pointeDuPupitre(fin);
      ecriture.countDown();
      attend(validation);
      return resultat;
    }

    private ResultatDEcriture<LectureDuSuivi> tenteRejeu(PointageAEnregistrer fin) {
      tentative.countDown();
      return atelier.pointeDuPupitre(fin);
    }

    private void assertAttendLaValidation(Future<ResultatDEcriture<LectureDuSuivi>> rejeu) {
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
    Throwable refus = catchThrowable(() -> inTransaction(() -> identites.reserve(evenement, finDatee(Optional.empty()))));

    // THEN
    assertThat(refus).isExactlyInstanceOf(IdentifiantDEvenementReutiliseException.class);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldDistinguishAnAbsentDateFromAPresentDate() {
    // GIVEN
    UUID evenement = UUID.randomUUID();
    reserveEtAssocie(evenement, finDatee(Optional.empty()), suiviIdentifiePar(UUID.randomUUID()));

    // WHEN
    Throwable refus = catchThrowable(() -> inTransaction(() -> identites.reserve(evenement, finDatee(Optional.of(Instant.EPOCH)))));

    // THEN
    assertThat(refus).isExactlyInstanceOf(IdentifiantDEvenementReutiliseException.class);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRetainEveryNanosecondOfTheSuppliedDate() {
    UUID evenement = UUID.randomUUID();
    Instant date = Instant.parse("2042-01-01T08:00:00.123456789Z");
    reserveEtAssocie(evenement, finDatee(Optional.of(date)), suiviIdentifiePar(UUID.randomUUID()));

    assertThatThrownBy(() ->
      inTransaction(() -> identites.reserve(evenement, finDatee(Optional.of(date.plusNanos(1)))))
    ).isExactlyInstanceOf(IdentifiantDEvenementReutiliseException.class);
    assertThat(inTransaction(() -> identites.reserve(evenement, finDatee(Optional.of(date)))).estUnRejeu()).isTrue();
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRollBackAReservationWhenTheWriteFails() {
    UUID evenement = UUID.randomUUID();
    EmpreinteDEvenement empreinte = finDatee(Optional.empty());

    assertThatThrownBy(() ->
      inTransaction(() -> {
        identites.reserve(evenement, empreinte);
        throw new IllegalStateException("ecriture refusee");
      })
    ).isExactlyInstanceOf(IllegalStateException.class);
    assertThat(reserveEtAssocie(evenement, empreinte, suiviIdentifiePar(UUID.randomUUID())).estUnRejeu()).isFalse();
  }

  /**
   * Le rejeu d'un geste d'atelier porte la meme intention et la meme cible que son premier envoi : il est reconnu.
   */
  @Test
  @WithTenant("impeccmold")
  void shouldReplayTheSameTargetedGesture() {
    // GIVEN
    UUID evenement = UUID.randomUUID();
    AgregatDEvenement suivi = suiviIdentifiePar(UUID.randomUUID());
    reserveEtAssocie(evenement, finVisant(CIBLE), suivi);

    // WHEN
    ReservationDEvenement rejeu = inTransaction(() -> identites.reserve(evenement, finVisant(CIBLE)));

    // THEN
    assertThat(rejeu.agregat()).contains(suivi);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejectTheSameIdentityWithAnotherTarget() {
    // GIVEN
    UUID evenement = UUID.randomUUID();
    reserveEtAssocie(evenement, finVisant(CIBLE), suiviIdentifiePar(UUID.randomUUID()));

    // WHEN
    Throwable refus = catchThrowable(() -> inTransaction(() -> identites.reserve(evenement, finVisant(AUTRE_CIBLE))));

    // THEN
    assertThat(refus).isExactlyInstanceOf(IdentifiantDEvenementReutiliseException.class);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRejectTheSameIdentityWithAnotherIntention() {
    // GIVEN
    UUID evenement = UUID.randomUUID();
    reserveEtAssocie(
      evenement,
      gesteDAtelier("DEBUT", Optional.of("TRANSITION"), Optional.of(CIBLE)),
      suiviIdentifiePar(UUID.randomUUID())
    );

    // WHEN
    Throwable refus = catchThrowable(() ->
      inTransaction(() -> identites.reserve(evenement, gesteDAtelier("DEBUT", Optional.of("OUVERTURE"), Optional.empty())))
    );

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
    AgregatDEvenement premier = suiviIdentifiePar(UUID.randomUUID());
    AgregatDEvenement second = suiviIdentifiePar(UUID.randomUUID());
    EmpreinteDEvenement empreinte = finDatee(Optional.empty());
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

  private static AgregatDEvenement suiviIdentifiePar(UUID id) {
    return new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, id);
  }

  private static EmpreinteDEvenement finDatee(Optional<Instant> date) {
    return EmpreinteDEvenement.builder()
      .nature(NatureDeGesteDuPupitre.POINTAGE_D_ATELIER)
      .suivi(Optional.of(SUIVI))
      .operateur(OPERATEUR)
      .type("FIN")
      .intention(Optional.of("FIN"))
      .activiteVisee(Optional.of(CIBLE))
      .poste(Optional.empty())
      .dateDeSurvenue(date);
  }

  private static EmpreinteDEvenement finVisant(UUID cible) {
    return gesteDAtelier("FIN", Optional.of("FIN"), Optional.of(cible));
  }

  private static EmpreinteDEvenement gesteDAtelier(String type, Optional<String> intention, Optional<UUID> cible) {
    return EmpreinteDEvenement.builder()
      .nature(NatureDeGesteDuPupitre.POINTAGE_D_ATELIER)
      .suivi(Optional.of(SUIVI))
      .operateur(OPERATEUR)
      .type(type)
      .intention(intention)
      .activiteVisee(cible)
      .poste(Optional.empty())
      .dateDeSurvenue(Optional.of(Instant.parse("2042-01-01T12:00:00Z")));
  }

  private <T> T inTransaction(java.util.function.Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
