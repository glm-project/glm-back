package com.glm.glmback.atelier.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ArriveeAEnregistrer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import com.glm.glmback.shared.time.domain.Clock;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@UnitTest
class ReservationDIdentiteServeurTest {

  @Test
  void shouldRetryTheServerIdentityReservationForPresenceAndWorkshop() throws Exception {
    IdentitesDEvenements identites = Mockito.mock(IdentitesDEvenements.class);
    given(identites.reserveHorsPupitre(any())).willReturn(false, true, false, true);

    assertThat(
      reserve(
        new JourneesDeTravailApplicationService(
          Mockito.mock(JourneeDeTravailRepository.class),
          Mockito.mock(OperateursConnus.class),
          Mockito.mock(PostesConnus.class),
          Mockito.mock(Clock.class),
          identites
        )
      )
    ).isInstanceOf(UUID.class);
    assertThat(
      reserve(
        new SuivisDAtelierApplicationService(
          Mockito.mock(SuiviDAtelierRepository.class),
          Mockito.mock(JourneeDeTravailRepository.class),
          Mockito.mock(ElementsEngageables.class),
          Mockito.mock(OperateursConnus.class),
          Mockito.mock(PostesConnus.class),
          Mockito.mock(Habilitations.class),
          Mockito.mock(Clock.class),
          identites
        )
      )
    ).isInstanceOf(UUID.class);

    then(identites).should(times(4)).reserveHorsPupitre(any());
  }

  @Test
  void shouldReadTheOriginalAggregateForAReplayWithoutCallingTheDomainTransition() {
    IdentitesDEvenements identites = Mockito.mock(IdentitesDEvenements.class);
    JourneeDeTravailRepository journees = Mockito.mock(JourneeDeTravailRepository.class);
    SuiviDAtelierRepository suivis = Mockito.mock(SuiviDAtelierRepository.class);
    JourneeDeTravail journee = Mockito.mock(JourneeDeTravail.class);
    SuiviDAtelier suivi = Mockito.mock(SuiviDAtelier.class);
    UUID journeeId = UUID.randomUUID();
    UUID suiviId = UUID.randomUUID();
    given(journees.get(new JourneeDeTravailId(journeeId))).willReturn(Optional.of(journee));
    given(suivis.get(new SuiviDAtelierId(suiviId))).willReturn(Optional.of(suivi));
    given(identites.reserve(any(), any())).willReturn(
      ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journeeId)),
      ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journeeId)),
      ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journeeId)),
      ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journeeId)),
      ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suiviId)),
      ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suiviId))
    );
    JourneesDeTravailApplicationService presence = new JourneesDeTravailApplicationService(
      journees,
      Mockito.mock(OperateursConnus.class),
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Clock.class),
      identites
    );
    SuivisDAtelierApplicationService atelier = new SuivisDAtelierApplicationService(
      suivis,
      journees,
      Mockito.mock(ElementsEngageables.class),
      Mockito.mock(OperateursConnus.class),
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Habilitations.class),
      Mockito.mock(Clock.class),
      identites
    );
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    Auteur auteur = new Auteur("pupitre");

    assertThat(
      presence
        .arriveDuPupitre(new ArriveeAEnregistrer(operateur, auteur, Optional.empty(), new EvenementDePresenceId(UUID.randomUUID())))
        .agregat()
    ).isSameAs(journee);
    assertThat(
      presence.arrive(new ArriveeAEnregistrer(operateur, auteur, Optional.empty(), new EvenementDePresenceId(UUID.randomUUID())))
    ).isSameAs(journee);
    assertThat(
      presence.pointe(
        new PointageDePresenceAEnregistrer(
          operateur,
          auteur,
          TypeDEvenementDePresence.PAUSE,
          Optional.empty(),
          new EvenementDePresenceId(UUID.randomUUID())
        )
      )
    ).isSameAs(journee);
    assertThat(
      presence
        .pointeDuPupitre(
          new PointageDePresenceAEnregistrer(
            operateur,
            auteur,
            TypeDEvenementDePresence.PAUSE,
            Optional.empty(),
            new EvenementDePresenceId(UUID.randomUUID())
          )
        )
        .agregat()
    ).isSameAs(journee);
    assertThat(atelier.pointeDuPupitre(pointage(suiviId, operateur, auteur)).agregat()).isSameAs(suivi);
    assertThat(atelier.pointe(pointage(suiviId, operateur, auteur))).isSameAs(suivi);
    then(journees).should(never()).create(any());
    then(journees).should(never()).update(any());
    then(suivis).should(never()).update(any());
  }

  private static Object reserve(Object service) throws Exception {
    Method methode = service.getClass().getDeclaredMethod("reserveIdentiteServeur");
    methode.setAccessible(true);
    return methode.invoke(service);
  }

  private static PointageAEnregistrer pointage(UUID suivi, OperateurId operateur, Auteur auteur) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(new SuiviDAtelierId(suivi))
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(operateur)
      .poste(Optional.empty())
      .auteur(auteur)
      .dateDeSurvenue(Optional.empty())
      .evenement(new EvenementDAtelierId(UUID.randomUUID()));
  }
}
