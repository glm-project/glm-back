package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Locale;
import java.util.Optional;

/**
 * Un texte cherche dans le nom, la reference et la description d'un element, sans tenir compte de la casse.
 */
public record Recherche(String value) {
  public Recherche {
    Assert.notBlank("recherche", value);
    value = value.strip().toLowerCase(Locale.ROOT);
  }

  public boolean trouveDans(ElementDeFabrication element) {
    return (
      contient(element.nom().value())
      || element.reference().map(Reference::value).filter(this::contient).isPresent()
      || element.description().map(Description::value).filter(this::contient).isPresent()
    );
  }

  private boolean contient(String texte) {
    return texte.toLowerCase(Locale.ROOT).contains(value);
  }

  /** Un texte vide ou blanc ne cherche rien. */
  public static Optional<Recherche> de(Optional<String> texte) {
    return texte.filter(saisi -> !saisi.isBlank()).map(Recherche::new);
  }
}
