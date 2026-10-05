package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.shared.error.domain.Assert;

public sealed interface ActeDeResolution {
  TypeDActeDeResolution kind();

  default boolean memeDemandeQue(ActeDeResolution autre) {
    var auteur = switch (this) {
      case Annulation annulation -> annulation.commande().auteur();
      case Correction correction -> correction.commande().remplacement().auteur();
      case Regularisation regularisation -> regularisation.commande().auteur();
    };
    return equals(autre.avecAuteur(auteur));
  }

  private ActeDeResolution avecAuteur(Auteur auteur) {
    return switch (this) {
      case Annulation annulation -> new Annulation(
        new AnnulationAEnregistrer(annulation.commande().suivi(), annulation.commande().evenement(), auteur, annulation.commande().motif())
      );
      case Correction correction -> new Correction(
        new CorrectionAEnregistrer(
          correction.commande().evenement(),
          correction.commande().motif(),
          avecAuteur(correction.commande().remplacement(), auteur)
        ),
        correction.instant()
      );
      case Regularisation regularisation -> new Regularisation(avecAuteur(regularisation.commande(), auteur), regularisation.instant());
    };
  }

  private static RegularisationAEnregistrer avecAuteur(RegularisationAEnregistrer commande, Auteur auteur) {
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

  record Annulation(AnnulationAEnregistrer commande) implements ActeDeResolution {
    public Annulation {
      Assert.notNull("annulation", commande);
    }

    @Override
    public TypeDActeDeResolution kind() {
      return TypeDActeDeResolution.ANNULATION;
    }
  }

  record Correction(CorrectionAEnregistrer commande, String instant) implements ActeDeResolution {
    public Correction {
      Assert.notNull("correction", commande);
      Assert.notBlank("instant", instant);
    }

    @Override
    public TypeDActeDeResolution kind() {
      return TypeDActeDeResolution.CORRECTION;
    }
  }

  record Regularisation(RegularisationAEnregistrer commande, String instant) implements ActeDeResolution {
    public Regularisation {
      Assert.notNull("regularisation", commande);
      Assert.notBlank("instant", instant);
    }

    @Override
    public TypeDActeDeResolution kind() {
      return TypeDActeDeResolution.REGULARISATION;
    }
  }
}
