package com.glm.glmback.atelier.application.gestionconflits;

import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.ElementsEngageables;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PostesConnus;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.atelier.domain.SuivisDAtelierService;
import com.glm.glmback.atelier.domain.gestionconflits.ActeDeResolution;
import java.time.Instant;
import java.util.Optional;

public final class PreparationDesActes {

  private final SuiviDAtelierRepository repository;
  private final ElementsEngageables elements;
  private final OperateursConnus operateurs;
  private final PostesConnus postes;
  private final Habilitations habilitations;
  private final EmpreintesDesConsequences empreintes;

  private PreparationDesActes(
    SuiviDAtelierRepository repository,
    ElementsEngageables elements,
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    EmpreintesDesConsequences empreintes
  ) {
    this.repository = repository;
    this.elements = elements;
    this.operateurs = operateurs;
    this.postes = postes;
    this.habilitations = habilitations;
    this.empreintes = empreintes;
  }

  public static RepositoryBuilder builder() {
    return repository ->
      elements ->
        operateurs ->
          postes ->
            habilitations -> empreintes -> new PreparationDesActes(repository, elements, operateurs, postes, habilitations, empreintes);
  }

  public ActePrepare prepare(
    SuiviDAtelier suivi,
    ActeDeResolution acte,
    Optional<EvenementDAtelierId> evenement,
    Auteur auteur,
    Instant maintenant
  ) {
    var service = SuivisDAtelierService.builder()
      .repository(repository)
      .elements(elements)
      .operateurs(operateurs)
      .postes(postes)
      .habilitations(habilitations)
      .clock(() -> maintenant);
    if (acte instanceof ActeDeResolution.Annulation annulation) {
      var commande = annulation.commande();
      var apres = service.prepareAnnulation(
        suivi,
        new AnnulationAEnregistrer(commande.suivi(), commande.evenement(), auteur, commande.motif())
      );
      return new ActePrepare(apres, empreintes.calcule(apres, maintenant));
    }
    if (acte instanceof ActeDeResolution.Regularisation regularisation) {
      var apres = service.prepareRegularisation(suivi, avecAuteur(regularisation.commande(), auteur), evenement.orElseThrow());
      return new ActePrepare(apres, empreintes.calcule(apres, maintenant));
    }
    var correction = (ActeDeResolution.Correction) acte;
    var remplacement = avecAuteur(correction.commande().remplacement(), auteur);
    var apres = service.prepareCorrection(
      suivi,
      new CorrectionAEnregistrer(correction.commande().evenement(), correction.commande().motif(), remplacement),
      evenement.orElseThrow()
    );
    return new ActePrepare(apres, empreintes.calcule(apres, maintenant));
  }

  private RegularisationAEnregistrer avecAuteur(RegularisationAEnregistrer commande, Auteur auteur) {
    return RegularisationAEnregistrer.builder()
      .suivi(commande.suivi())
      .type(commande.type())
      .intention(commande.intention())
      .activiteVisee(commande.activiteVisee())
      .operateur(commande.operateur())
      .poste(commande.poste())
      .auteur(auteur)
      .dateDeSurvenue(commande.dateDeSurvenue());
  }

  public interface RepositoryBuilder {
    ElementsBuilder repository(SuiviDAtelierRepository value);
  }

  public interface ElementsBuilder {
    OperateursBuilder elements(ElementsEngageables value);
  }

  public interface OperateursBuilder {
    PostesBuilder operateurs(OperateursConnus value);
  }

  public interface PostesBuilder {
    HabilitationsBuilder postes(PostesConnus value);
  }

  public interface HabilitationsBuilder {
    EmpreintesBuilder habilitations(Habilitations value);
  }

  public interface EmpreintesBuilder {
    PreparationDesActes empreintes(EmpreintesDesConsequences value);
  }
}
