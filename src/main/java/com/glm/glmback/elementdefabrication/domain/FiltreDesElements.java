package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

/**
 * Ce que l'ecran demande des elements, quel que soit leur cycle : periode de creation, categorie et texte cherche.
 */
public record FiltreDesElements(Optional<Periode> creation, Optional<Categorie> categorie, Optional<Recherche> recherche) {
  public FiltreDesElements {
    Assert.notNull("creation", creation);
    Assert.notNull("categorie", categorie);
    Assert.notNull("recherche", recherche);
  }

  public static FiltreDesElements aucun() {
    return new FiltreDesElements(Optional.empty(), Optional.empty(), Optional.empty());
  }

  public boolean matches(ElementDeFabrication element) {
    return (
      creation.map(periode -> periode.contains(element.dateDeCreation())).orElse(true)
      && categorie.map(element.categorie()::equals).orElse(true)
      && recherche.map(texte -> texte.trouveDans(element)).orElse(true)
    );
  }
}
