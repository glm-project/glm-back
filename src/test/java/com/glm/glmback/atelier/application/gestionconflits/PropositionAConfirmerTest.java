package com.glm.glmback.atelier.application.gestionconflits;

import static com.glm.glmback.atelier.application.gestionconflits.ResolutionFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.gestionconflits.ActeDeResolution;
import com.glm.glmback.atelier.domain.gestionconflits.AdresseDossierConflit;
import com.glm.glmback.atelier.domain.gestionconflits.PropositionInvalideException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@UnitTest
class PropositionAConfirmerTest {

  @Test
  void shouldReconnaitreLaMemeDemandeDeConfirmation() {
    var proposition = propositionDAnnulationDeTransition(suiviAvecTransitionDeMemeCategorie());
    assertThat(proposition.memeDemandeQue(proposition)).isTrue();
  }

  @ParameterizedTest
  @EnumSource(ChangementDeDemande.class)
  void shouldDistinguerChaqueChampDeLaDemande(ChangementDeDemande changement) {
    var originale = propositionDeCorrectionDeTransition(suiviAvecTransitionDeMemeCategorie());
    var autreActe = new ActeDeResolution.Regularisation(
      ((ActeDeResolution.Correction) originale.acte()).commande().remplacement(),
      "2026-05-10T14:00:00.123456789+02:00"
    );
    var reprise = PropositionAConfirmer.builder()
      .commande(changement == ChangementDeDemande.COMMANDE ? UUID.randomUUID() : originale.commande())
      .adresse(
        changement == ChangementDeDemande.ADRESSE
          ? new AdresseDossierConflit(originale.adresse().suivi(), EvenementDAtelierId.newId())
          : originale.adresse()
      )
      .revision(changement == ChangementDeDemande.REVISION ? new RevisionDuSuivi(1) : originale.revision())
      .acte(changement == ChangementDeDemande.ACTE ? autreActe : originale.acte())
      .evenement(changement == ChangementDeDemande.EVENEMENT ? Optional.of(EvenementDAtelierId.newId()) : originale.evenement())
      .empreinteConsequences(changement == ChangementDeDemande.EMPREINTE ? "consequences-modifiees" : originale.empreinteConsequences());
    assertThat(originale.memeDemandeQue(reprise)).isFalse();
  }

  @Test
  void shouldRefuserUneIdentiteProspectivePourLAnnulation() {
    var proposition = propositionDAnnulationDeTransition(suiviAvecTransitionDeMemeCategorie());
    assertThatThrownBy(() ->
      PropositionAConfirmer.builder()
        .commande(proposition.commande())
        .adresse(proposition.adresse())
        .revision(proposition.revision())
        .acte(proposition.acte())
        .evenement(Optional.of(EvenementDAtelierId.newId()))
        .empreinteConsequences(proposition.empreinteConsequences())
    ).isExactlyInstanceOf(PropositionInvalideException.class);
  }

  @ParameterizedTest
  @EnumSource(ActeCreateur.class)
  void shouldExigerUneIdentiteProspectiveDistincteDeLaCommande(ActeCreateur type) {
    var suivi = suiviAvecTransitionDeMemeCategorie();
    var proposition =
      type == ActeCreateur.CORRECTION ? propositionDeCorrectionDeTransition(suivi) : propositionDeRegularisationDeFin(suivi);
    assertThatThrownBy(() ->
      PropositionAConfirmer.builder()
        .commande(proposition.commande())
        .adresse(proposition.adresse())
        .revision(proposition.revision())
        .acte(proposition.acte())
        .evenement(Optional.empty())
        .empreinteConsequences(proposition.empreinteConsequences())
    ).isExactlyInstanceOf(PropositionInvalideException.class);
    assertThatThrownBy(() ->
      PropositionAConfirmer.builder()
        .commande(proposition.commande())
        .adresse(proposition.adresse())
        .revision(proposition.revision())
        .acte(proposition.acte())
        .evenement(Optional.of(new EvenementDAtelierId(proposition.commande())))
        .empreinteConsequences(proposition.empreinteConsequences())
    ).isExactlyInstanceOf(PropositionInvalideException.class);
  }

  @Test
  void shouldRefuserUnActeDestineAUnAutreSuivi() {
    var originale = propositionDAnnulationDeTransition(suiviAvecTransitionDeMemeCategorie());
    var autre = propositionDAnnulationDeTransition(suiviAvecTransitionDeMemeCategorie());
    assertThatThrownBy(() ->
      PropositionAConfirmer.builder()
        .commande(originale.commande())
        .adresse(originale.adresse())
        .revision(originale.revision())
        .acte(autre.acte())
        .evenement(Optional.empty())
        .empreinteConsequences(originale.empreinteConsequences())
    ).isExactlyInstanceOf(PropositionInvalideException.class);
  }

  private enum ChangementDeDemande {
    COMMANDE,
    ADRESSE,
    REVISION,
    ACTE,
    EVENEMENT,
    EMPREINTE,
  }

  private enum ActeCreateur {
    CORRECTION,
    REGULARISATION,
  }
}
