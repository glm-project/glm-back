package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ArriveeAEnregistrer;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageDePresenceAEnregistrer;
import com.glm.glmback.atelier.domain.PointagesEnAttente;
import com.glm.glmback.atelier.domain.PointagesSignales;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ce que le pupitre recoit quand sa journee est abandonnee ou son arrivee redondante : jamais un refus.
 */
@UnitTest
class JourneeAbandonneeDuPupitreTest {

  private final JourneeDeTravailRepository journees = Mockito.mock(JourneeDeTravailRepository.class);
  private final IdentitesDEvenements identites = Mockito.mock(IdentitesDEvenements.class);
  private final JourneeDeTravail ouverteA7H = journeeDeDupontOuverteA7H();

  /**
   * L'arrivee absorbee se rend comme un rejeu : 200 et la journee en cours. Son identifiant est associe a cette
   * journee, pour qu'un rejeu ulterieur rende exactement la meme chose.
   */
  @Test
  void shouldRendreLArriveeRedondanteCommeUnRejeu() {
    // GIVEN
    JourneesDeTravailApplicationService service = service(LE_10_MAI_2026_A_12H);
    EvenementDePresenceId geste = EvenementDePresenceId.newId();

    // WHEN
    ResultatDEcriture<JourneeDeTravail> resultat = service.arriveDuPupitre(
      new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, Optional.empty(), geste)
    );

    // THEN
    assertThat(resultat.rejeu()).isTrue();
    assertThat(resultat.agregat()).contains(ouverteA7H);
    then(identites)
      .should()
      .associe(geste.uuid(), new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, ouverteA7H.id().uuid()));
    then(journees).should(never()).create(any());
    then(journees).should(never()).update(any());
  }

  /**
   * L'arrivee implicite d'un geste tardif prend une identite serveur, reservee comme celle d'une regularisation : une
   * collision avec un geste du pupitre en fait tirer une autre.
   */
  @Test
  void shouldReserverUneIdentiteServeurPourLArriveeImplicite() {
    // GIVEN
    AtomicReference<UUID> refusee = new AtomicReference<>();
    given(identites.reserveHorsPupitre(any())).willAnswer(invocation -> !refusee.compareAndSet(null, invocation.getArgument(0)));
    JourneesDeTravailApplicationService service = service(LE_11_MAI_2026_A_9H15);
    EvenementDePresenceId geste = EvenementDePresenceId.newId();

    // WHEN
    ResultatDEcriture<JourneeDeTravail> resultat = service.pointeDuPupitre(
      new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.DEPART, Optional.empty(), geste)
    );

    // THEN
    JourneeDeTravail nouvelle = resultat.agregat().orElseThrow();
    assertThat(resultat.rejeu()).isFalse();
    assertThat(nouvelle.id()).isNotEqualTo(ouverteA7H.id());
    assertThat(nouvelle.journal().evenements().getFirst().id().uuid()).isNotEqualTo(refusee.get()).isNotEqualTo(geste.uuid());
    then(identites).should().associe(geste.uuid(), new AgregatDEvenement(TypeDAgregatDEvenement.JOURNEE_DE_TRAVAIL, nouvelle.id().uuid()));
    then(journees).should().create(nouvelle);
  }

  private JourneesDeTravailApplicationService service(Instant maintenant) {
    OperateursConnus operateurs = Mockito.mock(OperateursConnus.class);
    given(operateurs.existe(OPERATEUR_ID_DUPONT)).willReturn(true);
    given(journees.getEnCoursPour(OPERATEUR_ID_DUPONT)).willReturn(Optional.of(ouverteA7H));
    given(journees.create(any())).willAnswer(invocation -> invocation.getArgument(0));
    given(identites.reserve(any(), any())).willReturn(ReservationDEvenement.inedite());

    return new JourneesDeTravailApplicationService(
      journees,
      operateurs,
      Mockito.mock(PostesConnus.class),
      () -> AMPLITUDE_MAXIMALE_13H,
      Mockito.mock(PointagesSignales.class),
      Mockito.mock(PointagesEnAttente.class),
      () -> maintenant,
      identites,
      new TransactionTemplate(Mockito.mock(PlatformTransactionManager.class))
    );
  }
}
