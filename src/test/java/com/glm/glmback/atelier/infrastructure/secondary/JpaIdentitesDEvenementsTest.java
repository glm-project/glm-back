package com.glm.glmback.atelier.infrastructure.secondary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.application.AgregatDEvenement;
import com.glm.glmback.atelier.application.EmpreinteDEvenement;
import com.glm.glmback.atelier.application.NatureDeGesteDuPupitre;
import com.glm.glmback.atelier.application.TypeDAgregatDEvenement;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@UnitTest
class JpaIdentitesDEvenementsTest {

  private final EntityManager entityManager = Mockito.mock(EntityManager.class);
  private final Query query = Mockito.mock(Query.class);
  private final JpaIdentitesDEvenements identites = new JpaIdentitesDEvenements(entityManager);

  @Test
  void shouldReserveAnUnknownIdentity() {
    configureQuery(1);

    assertThat(identites.reserve(UUID.randomUUID(), empreinte(Optional.empty())).estUnRejeu()).isFalse();
  }

  @Test
  void shouldReplayTheSameIdentityAndRejectOtherContents() {
    UUID agregat = UUID.randomUUID();
    Instant date = Instant.parse("2040-01-01T08:00:00Z");
    configureQuery(0);
    given(query.getResultList()).willReturn(java.util.Collections.singletonList(ligne(true, date, agregat)));

    assertThat(identites.reserve(UUID.randomUUID(), empreinte(Optional.of(date))).agregat()).contains(
      new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, agregat)
    );
    assertThatThrownBy(() -> identites.reserve(UUID.randomUUID(), empreinte(Optional.empty()))).isExactlyInstanceOf(
      IdentifiantDEvenementReutiliseException.class
    );
  }

  @Test
  void shouldRejectANonReplayableIdentityAndExposeBothOutcomesOfServerReservation() {
    configureQuery(0, 1, 0);
    given(query.getResultList()).willReturn(java.util.Collections.singletonList(ligne(false, Instant.EPOCH, UUID.randomUUID())));

    assertThatThrownBy(() -> identites.reserve(UUID.randomUUID(), empreinte(Optional.of(Instant.EPOCH)))).isExactlyInstanceOf(
      IdentifiantDEvenementReutiliseException.class
    );
    assertThat(identites.reserveHorsPupitre(UUID.randomUUID())).isTrue();
    assertThat(identites.reserveHorsPupitre(UUID.randomUUID())).isFalse();
  }

  @Test
  void shouldAssociateAnIdentity() {
    configureQuery(1);

    identites.associe(UUID.randomUUID(), new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, UUID.randomUUID()));

    then(query).should().executeUpdate();
  }

  private void configureQuery(int... resultats) {
    given(entityManager.createNativeQuery(anyString())).willReturn(query);
    given(query.setParameter(anyInt(), any())).willReturn(query);
    if (resultats.length == 1) {
      given(query.executeUpdate()).willReturn(resultats[0]);
      return;
    }
    Integer[] suivants = java.util.Arrays.stream(resultats).skip(1).boxed().toArray(Integer[]::new);
    given(query.executeUpdate()).willReturn(resultats[0], suivants);
  }

  private static EmpreinteDEvenement empreinte(Optional<Instant> date) {
    return new EmpreinteDEvenement(
      NatureDeGesteDuPupitre.ARRIVEE,
      Optional.empty(),
      UUID.fromString("00000000-0000-0000-0000-000000000001"),
      "ARRIVEE",
      Optional.empty(),
      date
    );
  }

  private static Object[] ligne(boolean rejouable, Instant date, UUID agregat) {
    return new Object[] {
      rejouable,
      "ARRIVEE",
      null,
      UUID.fromString("00000000-0000-0000-0000-000000000001"),
      "ARRIVEE",
      null,
      true,
      "JOURNEE_DE_TRAVAIL",
      agregat,
      date,
    };
  }
}
