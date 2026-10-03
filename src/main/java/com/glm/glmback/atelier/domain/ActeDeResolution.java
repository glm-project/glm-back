package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;

public sealed interface ActeDeResolution {
  TypeDActeDeResolution kind();

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
