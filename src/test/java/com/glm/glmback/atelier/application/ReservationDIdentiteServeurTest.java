package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ArriveeAEnregistrer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.EtatDePresence;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.RegularisationDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import com.glm.glmback.shared.time.domain.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@UnitTest
class ReservationDIdentiteServeurTest {

  @Test
  void shouldRegulariseADepartureWithAFreshIdentityAfterACollision() {
    AtomicReference<UUID> refusee = new AtomicReference<>();
    IdentitesDEvenements identites = identitesAvecCollision(refusee);
    JourneeDeTravail journee = journeeDeDupontOuverteA7H();
    JourneesDeTravailApplicationService service = preparePresence(journee, identites);

    JourneeDeTravail resultat = regulariseDepart(service, journee);

    assertDepartRegularise(resultat, refusee.get());
  }

  @Test
  void shouldRegulariseWorkWithAFreshIdentityAfterACollision() {
    AtomicReference<UUID> refusee = new AtomicReference<>();
    IdentitesDEvenements identites = identitesAvecCollision(refusee);
    SuiviDAtelier suivi = suiviDAtelierEngage();
    SuivisDAtelierApplicationService service = prepareAtelier(suivi, identites);

    SuiviDAtelier resultat = regulariseTravail(service, suivi);

    assertTravailRegularise(resultat, refusee.get());
  }

  private static IdentitesDEvenements identitesAvecCollision(AtomicReference<UUID> refusee) {
    IdentitesDEvenements identites = Mockito.mock(IdentitesDEvenements.class);
    given(identites.reserveHorsPupitre(any())).willAnswer(invocation -> !refusee.compareAndSet(null, invocation.getArgument(0)));
    return identites;
  }

  @Test
  void shouldReplayCompletedDaysAndClosedFollowUpsWithoutWritingAgain() {
    IdentitesDEvenements identites = Mockito.mock(IdentitesDEvenements.class);
    JourneeDeTravailRepository journees = Mockito.mock(JourneeDeTravailRepository.class);
    SuiviDAtelierRepository suivis = Mockito.mock(SuiviDAtelierRepository.class);
    JourneeDeTravail journee = journeeDeDupontDe7HA17HAvecPauseDeMidi();
    SuiviDAtelier suivi = suiviDAtelierEngage().cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H));
    UUID journeeId = journee.id().uuid();
    UUID suiviId = suivi.id().uuid();
    prepareRejeux(identites, journeeId, suiviId);
    given(journees.get(journee.id())).willReturn(Optional.of(journee));
    given(suivis.get(suivi.id())).willReturn(Optional.of(suivi));
    JourneesDeTravailApplicationService presence = serviceDePresence(journees, identites);
    SuivisDAtelierApplicationService atelier = serviceDAtelier(suivis, journees, identites);
    assertRejeuxDePresence(presence, journee);
    assertRejeuxDAtelier(atelier, suivi);
    then(journees).should(never()).create(any());
    then(journees).should(never()).update(any());
    then(suivis).should(never()).update(any());
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

  private static JourneeDeTravail regulariseDepart(JourneesDeTravailApplicationService service, JourneeDeTravail journee) {
    return service.regularise(
      RegularisationDePresenceAEnregistrer.builder()
        .journee(journee.id())
        .type(TypeDEvenementDePresence.DEPART)
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(LE_10_MAI_2026_A_17H)
    );
  }

  private static void assertDepartRegularise(JourneeDeTravail resultat, UUID identiteRefusee) {
    assertThat(resultat.etat()).isEqualTo(EtatDePresence.ABSENT);
    assertThat(resultat.journal().evenements()).hasSize(2);
    var depart = resultat.journal().evenements().getLast();
    assertThat(identiteRefusee).isNotNull();
    assertThat(depart.id().uuid()).isNotEqualTo(identiteRefusee);
    assertThat(depart.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H);
    assertThat(depart.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
  }

  private static SuiviDAtelier regulariseTravail(SuivisDAtelierApplicationService service, SuiviDAtelier suivi) {
    return service.regularise(
      RegularisationAEnregistrer.builder()
        .suivi(suivi.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.empty())
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(LE_10_MAI_2026_A_8H)
    );
  }

  private static void assertTravailRegularise(SuiviDAtelier resultat, UUID identiteRefusee) {
    assertThat(resultat.etat()).isEqualTo(EtatDAtelier.EN_COURS);
    assertThat(resultat.journal().evenements())
      .singleElement()
      .satisfies(debut -> {
        assertThat(identiteRefusee).isNotNull();
        assertThat(debut.id().uuid()).isNotEqualTo(identiteRefusee);
        assertThat(debut.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(debut.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
      });
  }

  private static JourneesDeTravailApplicationService preparePresence(JourneeDeTravail journee, IdentitesDEvenements identites) {
    JourneeDeTravailRepository repository = Mockito.mock(JourneeDeTravailRepository.class);
    given(repository.get(journee.id())).willReturn(Optional.of(journee));
    given(repository.update(any())).willAnswer(invocation -> invocation.getArgument(0));
    return new JourneesDeTravailApplicationService(
      repository,
      Mockito.mock(OperateursConnus.class),
      Mockito.mock(PostesConnus.class),
      () -> LE_11_MAI_2026_A_9H15,
      identites
    );
  }

  private static SuivisDAtelierApplicationService prepareAtelier(SuiviDAtelier suivi, IdentitesDEvenements identites) {
    SuiviDAtelierRepository repository = Mockito.mock(SuiviDAtelierRepository.class);
    given(repository.get(suivi.id())).willReturn(Optional.of(suivi));
    given(repository.update(any())).willAnswer(invocation -> invocation.getArgument(0));
    OperateursConnus operateurs = Mockito.mock(OperateursConnus.class);
    given(operateurs.get(OPERATEUR_ID_DUPONT)).willReturn(Optional.of(OPERATEUR_CONNU_DUPONT));
    return new SuivisDAtelierApplicationService(
      repository,
      Mockito.mock(JourneeDeTravailRepository.class),
      Mockito.mock(ElementsEngageables.class),
      operateurs,
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Habilitations.class),
      () -> LE_11_MAI_2026_A_9H15,
      identites
    );
  }

  private static void prepareRejeux(IdentitesDEvenements identites, UUID journeeId, UUID suiviId) {
    ReservationDEvenement presence = ReservationDEvenement.rejeu(
      new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, journeeId)
    );
    ReservationDEvenement atelier = ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suiviId));
    given(identites.reserve(any(), any())).willReturn(presence, presence, presence, presence, atelier, atelier);
  }

  private static JourneesDeTravailApplicationService serviceDePresence(
    JourneeDeTravailRepository journees,
    IdentitesDEvenements identites
  ) {
    return new JourneesDeTravailApplicationService(
      journees,
      Mockito.mock(OperateursConnus.class),
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Clock.class),
      identites
    );
  }

  private static SuivisDAtelierApplicationService serviceDAtelier(
    SuiviDAtelierRepository suivis,
    JourneeDeTravailRepository journees,
    IdentitesDEvenements identites
  ) {
    return new SuivisDAtelierApplicationService(
      suivis,
      journees,
      Mockito.mock(ElementsEngageables.class),
      Mockito.mock(OperateursConnus.class),
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Habilitations.class),
      Mockito.mock(Clock.class),
      identites
    );
  }

  private static void assertRejeuxDePresence(JourneesDeTravailApplicationService presence, JourneeDeTravail journee) {
    ArriveeAEnregistrer arrivee = new ArriveeAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      Optional.empty(),
      EvenementDePresenceId.newId()
    );
    PointageDePresenceAEnregistrer pause = new PointageDePresenceAEnregistrer(
      OPERATEUR_ID_DUPONT,
      AUTEUR_DUPONT,
      TypeDEvenementDePresence.PAUSE,
      Optional.empty(),
      EvenementDePresenceId.newId()
    );

    assertThat(presence.arriveDuPupitre(arrivee).agregat()).isEqualTo(journee);
    assertThat(presence.arrive(arrivee)).isEqualTo(journee);
    assertThat(presence.pointe(pause)).isEqualTo(journee);
    assertThat(presence.pointeDuPupitre(pause).agregat()).isEqualTo(journee);
  }

  private static void assertRejeuxDAtelier(SuivisDAtelierApplicationService atelier, SuiviDAtelier suivi) {
    assertThat(atelier.pointeDuPupitre(pointage(suivi.id().uuid(), OPERATEUR_ID_DUPONT, AUTEUR_DUPONT)).agregat()).isEqualTo(suivi);
    assertThat(atelier.pointe(pointage(suivi.id().uuid(), OPERATEUR_ID_DUPONT, AUTEUR_DUPONT))).isEqualTo(suivi);
  }
}
