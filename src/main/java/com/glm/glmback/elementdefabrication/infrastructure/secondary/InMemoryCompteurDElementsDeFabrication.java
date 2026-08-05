package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.Annee;
import com.glm.glmback.elementdefabrication.domain.CompteurDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.TypeDElementDeFabrication;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCompteurDElementsDeFabrication implements CompteurDElementsDeFabrication {

  private final Map<CleDeCompteur, AtomicLong> compteurs = new ConcurrentHashMap<>();

  @Override
  public long prochainNumero(TypeDElementDeFabrication type, Annee annee) {
    return compteurs.computeIfAbsent(new CleDeCompteur(type, annee), nouvelleCle -> new AtomicLong()).incrementAndGet();
  }

  private record CleDeCompteur(TypeDElementDeFabrication type, Annee annee) {}
}
