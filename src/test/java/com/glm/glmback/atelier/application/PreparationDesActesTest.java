package com.glm.glmback.atelier.application;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

@UnitTest
class PreparationDesActesTest {

  @Test
  void shouldPreparerUneRegularisationAvecSonIdentifiantConserveEtSansEcriture() {
    var repository = mock(SuiviDAtelierRepository.class);
    var operateurs = mock(OperateursConnus.class);
    when(operateurs.get(OPERATEUR_ID_DUPONT)).thenReturn(Optional.of(OPERATEUR_CONNU_DUPONT));
    var suivi = suiviDAtelierEngage();
    var commande = RegularisationAEnregistrer.builder()
      .suivi(suivi.id())
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.empty())
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(LE_10_MAI_2026_A_8H);
    var acte = new ActeDeResolution.Regularisation(commande, "2026-05-10T08:00:00Z");
    var preparation = PreparationDesActes.builder()
      .repository(repository)
      .elements(mock(ElementsEngageables.class))
      .operateurs(operateurs)
      .postes(mock(PostesConnus.class))
      .habilitations(mock(Habilitations.class))
      .empreintes((apres, instant) -> "regularisation");
    var evenement = EvenementDAtelierId.newId();

    var resultat = preparation.prepare(suivi, acte, Optional.of(evenement), AUTEUR_MARTIN, LE_10_MAI_2026_A_17H);

    assertThat(resultat.apres().journal().evenement(evenement))
      .get()
      .satisfies(fait -> {
        assertThat(fait.auteur()).isEqualTo(AUTEUR_MARTIN);
        assertThat(fait.estUneRegularisation()).isTrue();
      });
    assertThat(resultat.empreinteConsequences()).isEqualTo("regularisation");
    verifyNoInteractions(repository);
  }

  @Test
  void shouldPreparerLAnnulationEnLaissantLeTravailEnCours() {
    var repository = mock(SuiviDAtelierRepository.class);
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var transition = passageEnTravailDe(travail).a(LE_10_MAI_2026_A_12H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(transition);
    var acte = new ActeDeResolution.Annulation(
      new AnnulationAEnregistrer(suivi.id(), transition.id(), AUTEUR_LEROY, MOTIF_ERREUR_DE_SAISIE)
    );
    var preparation = PreparationDesActes.builder()
      .repository(repository)
      .elements(mock(ElementsEngageables.class))
      .operateurs(mock(OperateursConnus.class))
      .postes(mock(PostesConnus.class))
      .habilitations(mock(Habilitations.class))
      .empreintes((apres, instant) -> "annulation");
    var resultat = preparation.prepare(suivi, acte, Optional.empty(), AUTEUR_MARTIN, LE_10_MAI_2026_A_12H.plusSeconds(3600));

    assertThat(resultat.apres().conflits()).isEmpty();
    assertThat(resultat.apres().activites())
      .singleElement()
      .satisfies(activite -> assertThat(activite.a(LE_10_MAI_2026_A_12H.plusSeconds(3600)).fin()).isEmpty());
    assertThat(resultat.apres().journal().evenement(transition.id()))
      .get()
      .satisfies(fait ->
        assertThat(fait.annulation())
          .get()
          .satisfies(annulation -> assertThat(annulation.auteur()).isEqualTo(AUTEUR_MARTIN))
      );
    verifyNoInteractions(repository);
  }

  @Test
  void shouldPreparerLaCorrectionSansEnregistrerEtAvecLAuteurActuel() {
    var operateurs = mock(OperateursConnus.class);
    var postes = mock(PostesConnus.class);
    var habilitations = mock(Habilitations.class);
    var repository = mock(SuiviDAtelierRepository.class);
    when(operateurs.get(OPERATEUR_ID_DUPONT)).thenReturn(Optional.of(OPERATEUR_CONNU_DUPONT));
    when(postes.get(POSTE_ID_FRAISEUSE_1)).thenReturn(Optional.of(POSTE_CONNU_FRAISEUSE_1));
    when(habilitations.estHabilite(OPERATEUR_ID_DUPONT, POSTE_ID_FRAISEUSE_1)).thenReturn(true);
    var travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    var nc = passageEnNonConformiteDe(travail).a(LE_10_MAI_2026_A_12H);
    var fin = finDe(travail).a(LE_10_MAI_2026_A_17H);
    var suivi = suiviDAtelierEngage().enregistre(travail).enregistre(nc).enregistre(fin);

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
    var preparation = PreparationDesActes.builder()
      .repository(repository)
      .elements(mock(ElementsEngageables.class))
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .empreintes((apres, evaluation) -> "empreinte-canonique");
    var event = EvenementDAtelierId.newId();
    var resultat = preparation.prepare(suivi, acte, Optional.of(event), AUTEUR_MARTIN, LE_10_MAI_2026_A_17H);
    assertThat(resultat.apres().conflits()).isEmpty();
    assertThat(resultat.apres().journal().evenement(event))
      .get()
      .satisfies(fait -> {
        assertThat(fait.auteur()).isEqualTo(AUTEUR_MARTIN);
        assertThat(fait.activiteVisee()).isEqualTo(nc.activite());
      });
    assertThat(resultat.apres().journal().evenement(fin.id()))
      .get()
      .satisfies(fait ->
        assertThat(fait.annulation())
          .get()
          .satisfies(annulation -> assertThat(annulation.auteur()).isEqualTo(AUTEUR_MARTIN))
      );
    verifyNoInteractions(repository);
    assertThat(resultat.empreinteConsequences()).isEqualTo("empreinte-canonique");
  }
}
