package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@UnitTest
class ReservationDIdentiteServeurTest {

  @Test
  void shouldRegulariseWorkWithAFreshIdentityAfterACollision() {
    // GIVEN
    AtomicReference<UUID> refusee = new AtomicReference<>();
    IdentitesDEvenements identites = identitesAvecCollision(refusee);
    SuiviDAtelier suivi = suiviDAtelierEngage();
    SuivisDAtelierApplicationService service = prepareAtelier(suivi, identites);

    // WHEN
    LectureDuSuivi resultat = regulariseTravail(service, suivi);

    // THEN
    assertTravailRegularise(resultat, refusee.get());
  }

  private static IdentitesDEvenements identitesAvecCollision(AtomicReference<UUID> refusee) {
    IdentitesDEvenements identites = Mockito.mock(IdentitesDEvenements.class);
    given(identites.reserveHorsPupitre(any())).willAnswer(invocation -> !refusee.compareAndSet(null, invocation.getArgument(0)));
    return identites;
  }

  @Test
  void shouldReplayClosedFollowUpsWithoutWritingAgain() {
    // GIVEN
    IdentitesDEvenements identites = Mockito.mock(IdentitesDEvenements.class);
    SuiviDAtelierRepository suivis = Mockito.mock(SuiviDAtelierRepository.class);
    SuiviDAtelier suivi = suiviDAtelierEngage().cloture(clotureParLeroyA(LE_10_MAI_2026_A_17H));
    UUID suiviId = suivi.id().uuid();
    prepareRejeux(identites, suiviId);
    given(suivis.get(suivi.id())).willReturn(Optional.of(suivi));
    SuivisDAtelierApplicationService atelier = serviceDAtelier(suivis, identites);

    // WHEN
    List<SuiviDAtelier> suivisRejoues = rejoueAtelier(atelier, suivi);

    // THEN
    assertThat(suivisRejoues).containsExactly(suivi, suivi);
    then(suivis).should(never()).update(any());
  }

  private static PointageAEnregistrer pointage(UUID suivi, OperateurId operateur, Auteur auteur) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(new SuiviDAtelierId(suivi))
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activiteVisee(Optional.empty())
      .operateur(operateur)
      .poste(Optional.empty())
      .auteur(auteur)
      .dateDeSurvenue(Optional.empty())
      .evenement(new EvenementDAtelierId(UUID.randomUUID()));
  }

  private static LectureDuSuivi regulariseTravail(SuivisDAtelierApplicationService service, SuiviDAtelier suivi) {
    return service.regularise(
      RegularisationAEnregistrer.builder()
        .suivi(suivi.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .intention(IntentionDePointage.OUVERTURE)
        .activiteVisee(Optional.empty())
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.empty())
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(LE_10_MAI_2026_A_8H)
    );
  }

  private static void assertTravailRegularise(LectureDuSuivi resultat, UUID identiteRefusee) {
    // Lu le lendemain a 9 h 15, le travail regularise a 8 h a atteint son echeance de 21 h : il est termine.
    assertThat(resultat.evaluation()).isEqualTo(LE_11_MAI_2026_A_9H15);
    assertThat(resultat.etat()).isEqualTo(EtatDAtelier.INTERROMPU);
    assertThat(resultat.suivi().journal().evenements())
      .singleElement()
      .satisfies(debut -> {
        assertThat(identiteRefusee).isNotNull();
        assertThat(debut.id().uuid()).isNotEqualTo(identiteRefusee);
        assertThat(debut.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(debut.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
      });
  }

  private static SuivisDAtelierApplicationService prepareAtelier(SuiviDAtelier suivi, IdentitesDEvenements identites) {
    SuiviDAtelierRepository repository = Mockito.mock(SuiviDAtelierRepository.class);
    given(repository.get(suivi.id())).willReturn(Optional.of(suivi));
    given(repository.update(any())).willAnswer(invocation -> invocation.getArgument(0));
    OperateursConnus operateurs = Mockito.mock(OperateursConnus.class);
    given(operateurs.get(OPERATEUR_ID_DUPONT)).willReturn(Optional.of(OPERATEUR_CONNU_DUPONT));
    return new SuivisDAtelierApplicationService(
      repository,
      Mockito.mock(ElementsEngageables.class),
      operateurs,
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Habilitations.class),
      () -> LE_11_MAI_2026_A_9H15,
      identites,
      new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class))
    );
  }

  private static void prepareRejeux(IdentitesDEvenements identites, UUID suiviId) {
    ReservationDEvenement atelier = ReservationDEvenement.rejeu(new AgregatDEvenement(TypeDAgregatDEvenement.SUIVI_D_ATELIER, suiviId));
    given(identites.reserve(any(), any())).willReturn(atelier);
  }

  private static SuivisDAtelierApplicationService serviceDAtelier(SuiviDAtelierRepository suivis, IdentitesDEvenements identites) {
    return new SuivisDAtelierApplicationService(
      suivis,
      Mockito.mock(ElementsEngageables.class),
      Mockito.mock(OperateursConnus.class),
      Mockito.mock(PostesConnus.class),
      Mockito.mock(Habilitations.class),
      () -> LE_10_MAI_2026_A_17H,
      identites,
      new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class))
    );
  }

  private static List<SuiviDAtelier> rejoueAtelier(SuivisDAtelierApplicationService atelier, SuiviDAtelier suivi) {
    return List.of(
      atelier.pointeDuPupitre(pointage(suivi.id().uuid(), OPERATEUR_ID_DUPONT, AUTEUR_DUPONT)).agregat().suivi(),
      atelier.pointe(pointage(suivi.id().uuid(), OPERATEUR_ID_DUPONT, AUTEUR_DUPONT)).suivi()
    );
  }
}
