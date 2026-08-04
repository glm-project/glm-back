package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.Annee;
import com.glm.glmback.elementdefabrication.domain.CompteurDElementsDeFabrication;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCompteurDElementsDeFabrication implements CompteurDElementsDeFabrication {

  private final Map<Annee, AtomicLong> ordresDeFabrication = new ConcurrentHashMap<>();
  private final Map<Annee, AtomicLong> produits = new ConcurrentHashMap<>();

  @Override
  public long prochainNumeroDOrdreDeFabrication(Annee annee) {
    return prochainNumero(ordresDeFabrication, annee);
  }

  @Override
  public long prochainNumeroDeProduit(Annee annee) {
    return prochainNumero(produits, annee);
  }

  private static long prochainNumero(Map<Annee, AtomicLong> compteurs, Annee annee) {
    return compteurs.computeIfAbsent(annee, nouvelleAnnee -> new AtomicLong()).incrementAndGet();
  }
}
