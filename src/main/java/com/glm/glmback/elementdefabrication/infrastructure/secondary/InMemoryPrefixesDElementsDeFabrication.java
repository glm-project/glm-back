package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.Prefixe;
import com.glm.glmback.elementdefabrication.domain.PrefixesDElementsDeFabrication;
import org.springframework.stereotype.Component;

@Component
public class InMemoryPrefixesDElementsDeFabrication implements PrefixesDElementsDeFabrication {

  private static final Prefixe ORDRE_DE_FABRICATION = new Prefixe("OF");
  private static final Prefixe PRODUIT = new Prefixe("PRD");

  @Override
  public Prefixe prefixeDOrdreDeFabrication() {
    return ORDRE_DE_FABRICATION;
  }

  @Override
  public Prefixe prefixeDeProduit() {
    return PRODUIT;
  }
}
