package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;

public class ElementsDeFabrication {

  private final CompteurDElementsDeFabrication compteur;
  private final PrefixesDElementsDeFabrication prefixes;

  public ElementsDeFabrication(CompteurDElementsDeFabrication compteur, PrefixesDElementsDeFabrication prefixes) {
    Assert.notNull("compteur", compteur);
    Assert.notNull("prefixes", prefixes);

    this.compteur = compteur;
    this.prefixes = prefixes;
  }

  public OrdreDeFabrication ordreDeFabrication(OrdreDeFabricationId id, Optional<Nom> nom, Fiche fiche) {
    Nom nomRetenu = nom.orElseGet(() -> nomDOrdreDeFabrication(fiche));

    return OrdreDeFabrication.builder()
      .id(id)
      .nom(nomRetenu)
      .titre(fiche.titre())
      .description(fiche.description())
      .dateDeCreation(fiche.dateDeCreation())
      .dateDeModification(fiche.dateDeModification());
  }

  public Produit produit(ProduitId id, Optional<Nom> nom, Fiche fiche) {
    Nom nomRetenu = nom.orElseGet(() -> nomDeProduit(fiche));

    return Produit.builder()
      .id(id)
      .nom(nomRetenu)
      .titre(fiche.titre())
      .description(fiche.description())
      .dateDeCreation(fiche.dateDeCreation())
      .dateDeModification(fiche.dateDeModification());
  }

  private Nom nomDOrdreDeFabrication(Fiche fiche) {
    Annee annee = Annee.de(fiche.dateDeCreation());

    return Nom.de(prefixes.prefixeDOrdreDeFabrication(), annee, compteur.prochainNumeroDOrdreDeFabrication(annee));
  }

  private Nom nomDeProduit(Fiche fiche) {
    Annee annee = Annee.de(fiche.dateDeCreation());

    return Nom.de(prefixes.prefixeDeProduit(), annee, compteur.prochainNumeroDeProduit(annee));
  }
}
