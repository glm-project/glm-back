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
  void shouldRefuserUneRevisionPerimeeAvantLaPreparation() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transition = passageEnTravailDe(travail).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(transition);
    var repository = mock(SuiviDAtelierRepository.class);
    when(repository.get(suivi.id())).thenReturn(Optional.of(suivi));
    var references = mock(ReferencesDApercu.class);
    var preparation = PreparationDesActes.builder()
      .repository(repository)
      .elements(mock(ElementsEngageables.class))
      .operateurs(mock(OperateursConnus.class))
      .postes(mock(PostesConnus.class))
      .habilitations(mock(Habilitations.class))
      .empreintes((apres, evaluation) -> "consequences");
    var service = ApercusDeResolution.builder()
      .suivis(repository)
      .preparation(preparation)
      .references(references)
      .clock(() -> LE_10_MAI_2026_A_17H)
      .validite(() -> Duration.ofMinutes(15));
    var acte = new ActeDeResolution.Annulation(
      new AnnulationAEnregistrer(suivi.id(), transition.id(), AUTEUR_LEROY, MOTIF_ERREUR_DE_SAISIE)
    );
    assertThatThrownBy(() ->
      service.apercu(
        UUID.randomUUID(),
        new AdresseDossierConflit(suivi.id(), transition.id()),
        new RevisionDuSuivi(9),
        acte,
        CONTEXTE_LEROY_IMPECCMOLD
      )
    ).isExactlyInstanceOf(ApercuObsoleteException.class);
    verifyNoInteractions(references);
    verify(repository).get(suivi.id());
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldPreparerLaFinCorrigeeEtConserverSonIdentifiantDansLaPreuve() {
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nc = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nc).enregistre(fin);
    var repository = mock(SuiviDAtelierRepository.class);
    when(repository.get(suivi.id())).thenReturn(Optional.of(suivi));
    var operateurs = mock(OperateursConnus.class);
    when(operateurs.get(OPERATEUR_ID_DUPONT)).thenReturn(Optional.of(OPERATEUR_CONNU_DUPONT));
    var postes = mock(PostesConnus.class);
    when(postes.get(POSTE_ID_FRAISEUSE_1)).thenReturn(Optional.of(POSTE_CONNU_FRAISEUSE_1));
    var habilitations = mock(Habilitations.class);
    when(habilitations.estHabilite(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1)).thenReturn(true);
    var preparation = PreparationDesActes.builder()
      .repository(repository)
      .elements(mock(ElementsEngageables.class))
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .empreintes((apres, evaluation) -> "fin-corrigee");
    var references = mock(ReferencesDApercu.class);
    when(references.issue(any())).thenReturn("fin-opaque");
    var service = ApercusDeResolution.builder()
      .suivis(repository)
      .preparation(preparation)
      .references(references)
      .clock(() -> LE_10_MAI_2026_A_17H)
      .validite(() -> Duration.ofMinutes(15));
    var remplacement = RegularisationAEnregistrer.builder()
      .suivi(suivi.id())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activiteVisee(nc.activite())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(LE_10_MAI_2026_A_17H);
    var acte = new ActeDeResolution.Correction(
      new CorrectionAEnregistrer(fin.id(), MOTIF_ERREUR_DE_SAISIE, remplacement),
      "2026-05-10T17:00:00Z"
    );

    var apercu = service.apercu(
      UUID.randomUUID(),
      new AdresseDossierConflit(suivi.id(), fin.id()),
      suivi.revision(),
      acte,
      CONTEXTE_LEROY_IMPECCMOLD
    );

    assertThat(apercu.apres().kind()).isEqualTo(EtatDAdresseDossier.ANCRE_ANNULEE);
    assertThat(apercu.apres().activites())
      .extracting(IntervalleDActivite::debut)
      .containsExactly(LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_12H);
    assertThat(apercu.apres().activites()).allSatisfy(activite -> assertThat(activite.aResoudre()).isFalse());
    var id = apercu.reference().preuve().evenement().orElseThrow();
    assertThat(apercu.apres().lecture().suivi().journal().evenement(id))
      .get()
      .satisfies(fait -> assertThat(fait.activiteVisee()).isEqualTo(nc.activite()));
    assertThat(suivi.journal().evenements()).hasSize(3);
    verify(repository).get(suivi.id());
    verifyNoMoreInteractions(repository);
  }

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
