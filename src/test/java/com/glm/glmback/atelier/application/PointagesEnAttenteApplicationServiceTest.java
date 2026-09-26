package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.Application;
import com.glm.glmback.atelier.domain.Ecart;
import com.glm.glmback.atelier.domain.GesteDAtelier;
import com.glm.glmback.atelier.domain.GesteDePresence;
import com.glm.glmback.atelier.domain.GesteEnAttente;
import com.glm.glmback.atelier.domain.MotifDEcart;
import com.glm.glmback.atelier.domain.MotifDeMiseEnAttente;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PointageEnAttente;
import com.glm.glmback.atelier.domain.PointageEnAttenteId;
import com.glm.glmback.atelier.domain.PointagesEnAttenteEnMemoire;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDePresence;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@UnitTest
class PointagesEnAttenteApplicationServiceTest {

  private final PointagesEnAttenteEnMemoire pointages = new PointagesEnAttenteEnMemoire();
  private final OperateursConnus operateurs = Mockito.mock(OperateursConnus.class);
  private final JourneesDeTravailApplicationService presence = Mockito.mock(JourneesDeTravailApplicationService.class);
  private final SuivisDAtelierApplicationService atelier = Mockito.mock(SuivisDAtelierApplicationService.class);
  private final PointagesEnAttenteApplicationService service = new PointagesEnAttenteApplicationService(
    pointages,
    () -> LE_11_MAI_2026_A_9H15,
    operateurs,
    Mockito.mock(PostesConnus.class),
    presence,
    atelier
  );

  /**
   * Un geste de presence s'applique a la date du pupitre, au nom du gestionnaire.
   */
  @Test
  void shouldAppliquerUnGesteDePresence() {
    GesteDePresence pause = new GesteDePresence(OPERATEUR_ID_DUPONT, TypeDEvenementDePresence.PAUSE, Optional.of(LE_10_MAI_2026_A_12H));
    PointageEnAttente recu = enAttente(pause);

    PointageEnAttente applique = service.applique(recu.id(), AUTEUR_LEROY);

    then(presence).should().applique(pause, LE_10_MAI_2026_A_12H, AUTEUR_LEROY);
    assertThat(applique.traitement()).contains(new Application(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15));
  }

  @Test
  void shouldAppliquerUnGesteDAtelierParUneRegularisation() {
    GesteDAtelier debut = GesteDAtelier.builder()
      .suivi(SuiviDAtelierId.newId())
      .operateur(OPERATEUR_ID_MARTIN)
      .type(TypeDEvenementDAtelier.DEBUT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .dateDeclaree(Optional.empty());
    PointageEnAttente recu = enAttente(debut);

    service.applique(recu.id(), AUTEUR_LEROY);

    then(atelier).should().regularise(debut.regularisation(AUTEUR_LEROY, LE_10_MAI_2026_A_13H));
    then(presence).shouldHaveNoInteractions();
  }

  @Test
  void shouldLaisserEnAttenteUnGesteDontLApplicationEstRefusee() {
    GesteDePresence arrivee = new GesteDePresence(OPERATEUR_ID_DUPONT, TypeDEvenementDePresence.ARRIVEE, Optional.empty());
    PointageEnAttente recu = enAttente(arrivee);
    given(presence.applique(any(), any(), any())).willThrow(new OperateurDAtelierIntrouvableException(OPERATEUR_ID_DUPONT));

    assertThatThrownBy(() -> service.applique(recu.id(), AUTEUR_LEROY)).isExactlyInstanceOf(OperateurDAtelierIntrouvableException.class);
    assertThat(pointages.get(recu.id())).contains(recu);
  }

  @Test
  void shouldEcarterUnGeste() {
    PointageEnAttente recu = enAttente(new GesteDePresence(OPERATEUR_ID_DUPONT, TypeDEvenementDePresence.DEPART, Optional.empty()));
    MotifDEcart motif = new MotifDEcart("Doublon");

    PointageEnAttente ecarte = service.ecarte(recu.id(), AUTEUR_LEROY, motif);

    assertThat(ecarte.traitement()).contains(new Ecart(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, motif));
    then(presence).shouldHaveNoInteractions();
  }

  @Test
  void shouldListerEtResoudreLesOperateurs() {
    PointageEnAttente recu = enAttente(new GesteDePresence(OPERATEUR_ID_DUPONT, TypeDEvenementDePresence.DEPART, Optional.empty()));
    given(operateurs.parIds(Set.of(OPERATEUR_ID_DUPONT))).willReturn(List.of(OPERATEUR_CONNU_DUPONT));

    List<PointageEnAttente> liste = service.list(Optional.of(OPERATEUR_ID_DUPONT), Optional.empty(), firstPageOfTen()).content();
    AnnuaireDAtelier annuaire = service.annuairePour(liste);

    assertThat(liste).containsExactly(recu);
    assertThat(annuaire.operateur(OPERATEUR_ID_DUPONT)).contains(OPERATEUR_CONNU_DUPONT);
  }

  private PointageEnAttente enAttente(GesteEnAttente geste) {
    return pointages.create(
      PointageEnAttente.builder()
        .id(PointageEnAttenteId.newId())
        .evenementDuPupitre(UUID.randomUUID())
        .geste(geste)
        .motif(MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE)
        .auteur(AUTEUR_DUPONT)
        .dateDeReception(LE_10_MAI_2026_A_13H)
        .traitement(Optional.empty())
    );
  }
}
