package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.application.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.*;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class ApercusDeResolutionTest {

  @Test
  void shouldPreparerLAnnulationSansEcrireEtAuthentifierLesMemesConsequences() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transition = passageEnTravailDe(travail).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(transition);
    var adresse = new AdresseDossierConflit(suivi.id(), transition.id());
    var acte = new ActeDeResolution.Annulation(
      new AnnulationAEnregistrer(suivi.id(), transition.id(), AUTEUR_LEROY, MOTIF_ERREUR_DE_SAISIE)
    );
    var repository = mock(SuiviDAtelierRepository.class);
    when(repository.get(suivi.id())).thenReturn(Optional.of(suivi));
    var references = mock(ReferencesDApercu.class);
    when(references.issue(any())).thenReturn("reference-opaque");
    var preparation = PreparationDesActes.builder()
      .repository(repository)
      .elements(mock(ElementsEngageables.class))
      .operateurs(mock(OperateursConnus.class))
      .postes(mock(PostesConnus.class))
      .habilitations(mock(Habilitations.class))
      .empreintes((apres, evaluation) -> "consequences");
    var maintenant = LE_10_MAI_2026_A_12H.plusSeconds(3600);
    var service = ApercusDeResolution.builder()
      .suivis(repository)
      .preparation(preparation)
      .references(references)
      .clock(() -> maintenant)
      .validite(() -> Duration.ofMinutes(15));
    UUID commande = UUID.randomUUID();

    var apercu = service.apercu(commande, adresse, suivi.revision(), acte, CONTEXTE_LEROY_IMPECCMOLD);

    assertThat(apercu).isNotNull();
    assertThat(apercu.apres().kind()).isEqualTo(EtatDAdresseDossier.ANCRE_ANNULEE);
    assertThat(apercu.apres().activites())
      .singleElement()
      .satisfies(activite -> assertThat(activite.fin()).isEmpty());
    assertThat(apercu.reference().opaque()).isEqualTo("reference-opaque");
    assertThat(apercu.reference().preuve().commande()).isEqualTo(commande);
    assertThat(apercu.reference().preuve().contexte()).isEqualTo(CONTEXTE_LEROY_IMPECCMOLD);
    assertThat(apercu.reference().preuve().acte()).isEqualTo(acte);
    assertThat(apercu.reference().preuve().evenement()).isEmpty();
    assertThat(apercu.reference().preuve().evaluation()).isEqualTo(maintenant);
    assertThat(apercu.reference().preuve().expireLe()).isEqualTo(maintenant.plusSeconds(900));
    assertThat(apercu.reference().preuve().empreinteConsequences()).isEqualTo("consequences");
    assertThat(suivi.journal().evenement(transition.id()))
      .get()
      .satisfies(fait -> assertThat(fait.estAnnule()).isFalse());
    verify(repository).get(suivi.id());
    verifyNoMoreInteractions(repository);
    verify(references).issue(apercu.reference().preuve());
  }
}
