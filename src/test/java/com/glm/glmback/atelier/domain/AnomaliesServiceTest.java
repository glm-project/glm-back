package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class AnomaliesServiceTest {

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_11_MAI_2026_A_9H);
  private final AtomicReference<AmplitudeMaximale> seuil = new AtomicReference<>(AMPLITUDE_MAXIMALE_13H);
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final AnomaliesService anomalies = new AnomaliesService(journees, seuil::get, maintenant::get);

  /**
   * E2 : la journee de lundi sans depart apparait mardi, puis disparait une fois le depart regularise.
   */
  @Test
  void shouldListerPuisOublierUneJourneeSansDepartRegularisee() {
    JourneeDeTravail lundi = journees.create(journeeDeDupontOuverteA7H());

    assertThat(toutes().content()).containsExactly(new AnomalieDePresence(TypeDAnomalie.JOURNEE_SANS_DEPART, lundi));

    journees.update(lundi.enregistre(departRegulariseParLeroyA(LE_10_MAI_2026_A_17H)));

    assertThat(toutes().content()).isEmpty();
  }

  /**
   * E4 : une journee fermee a 16 h reste en anomalie, meme regularisee : c'est au gestionnaire de trancher.
   */
  @Test
  void shouldListerUneAmplitudeExcessive() {
    JourneeDeTravail seize = journees.create(
      journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_10_MAI_2026_A_7H.plus(Duration.ofHours(16))))
    );

    assertThat(toutes().content()).containsExactly(new AnomalieDePresence(TypeDAnomalie.AMPLITUDE_EXCESSIVE, seize));
  }

  @Test
  void shouldNeRienListerSansAnomalie() {
    journees.create(journeeDeDupontDe7HA17HAvecPauseDeMidi());
    maintenant.set(LE_10_MAI_2026_A_17H);
    journees.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_MARTIN).enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_8H))
    );

    assertThat(toutes().content()).isEmpty();
    assertThat(toutes().totalElementsCount()).isZero();
  }

  @Test
  void shouldListerLaPlusRecenteDAbord() {
    JourneeDeTravail lundi = journees.create(journeeDeDupontOuverteA7H());
    JourneeDeTravail mardi = journees.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_DUPONT).enregistre(arriveeDeDupontA(LE_11_MAI_2026_A_7H))
    );
    maintenant.set(LE_11_MAI_2026_A_20H.plusSeconds(3600));

    assertThat(toutes().content()).extracting(AnomalieDePresence::journee).containsExactly(mardi, lundi);
  }

  @Test
  void shouldPaginer() {
    journees.create(journeeDeDupontOuverteA7H());
    journees.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_MARTIN).enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_8H))
    );

    Page<AnomalieDePresence> premiere = anomalies.list(Optional.empty(), Optional.empty(), new Pageable(0, 1));

    assertThat(premiere.content()).hasSize(1);
    assertThat(premiere.totalElementsCount()).isEqualTo(2);
  }

  @Test
  void shouldFiltrerParOperateurEtParType() {
    journees.create(journeeDeDupontOuverteA7H());
    JourneeDeTravail deMartin = journees.create(
      JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), OPERATEUR_ID_MARTIN)
        .enregistre(arriveeDeDupontA(LE_10_MAI_2026_A_7H))
        .enregistre(departDeDupontA(LE_10_MAI_2026_A_7H.plus(Duration.ofHours(16))))
    );

    assertThat(anomalies.list(Optional.of(OPERATEUR_ID_MARTIN), Optional.empty(), firstPageOfTen()).content())
      .extracting(AnomalieDePresence::journee)
      .containsExactly(deMartin);
    assertThat(anomalies.list(Optional.empty(), Optional.of(TypeDAnomalie.JOURNEE_SANS_DEPART), firstPageOfTen()).content())
      .extracting(AnomalieDePresence::type)
      .containsExactly(TypeDAnomalie.JOURNEE_SANS_DEPART);
  }

  /**
   * E8 : un seuil ramene a 10 h fait apparaitre une journee de 11 h, sans rien stocker.
   */
  @Test
  void shouldJugerAvecLeSeuilCourant() {
    journees.create(journeeDeDupontOuverteA7H().enregistre(departDeDupontA(LE_10_MAI_2026_A_17H.plus(Duration.ofHours(1)))));
    assertThat(toutes().content()).isEmpty();

    seuil.set(AMPLITUDE_MAXIMALE_10H);

    assertThat(toutes().content()).extracting(AnomalieDePresence::type).containsExactly(TypeDAnomalie.AMPLITUDE_EXCESSIVE);
  }

  private Page<AnomalieDePresence> toutes() {
    return anomalies.list(Optional.empty(), Optional.empty(), firstPageOfTen());
  }
}
