package com.glm.glmback.atelier.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.atelier.application.AgregatDEvenement;
import com.glm.glmback.atelier.application.EmpreinteDEvenement;
import com.glm.glmback.atelier.application.IdentitesDEvenements;
import com.glm.glmback.atelier.application.NatureDeGesteDuPupitre;
import com.glm.glmback.atelier.application.ReservationDEvenement;
import com.glm.glmback.atelier.application.TypeDAgregatDEvenement;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
class JpaIdentitesDEvenementsIT {

  @Autowired
  private IdentitesDEvenements identites;

  @Autowired
  private TransactionTemplate transactions;

  @Test
  @WithTenant("impeccmold")
  void shouldReserveThenReplayTheSameFingerprint() {
    UUID evenement = UUID.randomUUID();
    UUID journee = UUID.randomUUID();
    EmpreinteDEvenement empreinte = arrivee(Optional.of(Instant.parse("2042-01-01T08:00:00Z")));

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

  private static EmpreinteDEvenement arrivee(Optional<Instant> date) {
    return new EmpreinteDEvenement(
      NatureDeGesteDuPupitre.ARRIVEE,
      Optional.empty(),
      UUID.fromString("00000000-0000-0000-0000-000000000001"),
      "ARRIVEE",
      Optional.empty(),
      date
    );
  }

  private <T> T inTransaction(java.util.function.Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
