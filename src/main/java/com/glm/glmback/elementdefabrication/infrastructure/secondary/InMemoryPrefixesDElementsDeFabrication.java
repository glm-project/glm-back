package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.Prefixe;
import com.glm.glmback.elementdefabrication.domain.PrefixesDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InMemoryPrefixesDElementsDeFabrication implements PrefixesDElementsDeFabrication {

  private static final Map<TypeDElementDeFabrication, Prefixe> PREFIXES = Map.of(
    TypeDElementDeFabrication.ORDRE_DE_FABRICATION,
    new Prefixe("OF"),
    TypeDElementDeFabrication.PRODUIT,
    new Prefixe("PRD")
  );

  @Override
  public Prefixe prefixe(TypeDElementDeFabrication type) {
    return PREFIXES.get(type);
  }
}
