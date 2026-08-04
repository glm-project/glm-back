package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;

public final class ElementsDeFabricationService {

  private final ElementDeFabricationRepository repository;
  private final CompteurDElementsDeFabrication compteur;
  private final PrefixesDElementsDeFabrication prefixes;
  private final Clock clock;

  private ElementsDeFabricationService(
    ElementDeFabricationRepository repository,
    CompteurDElementsDeFabrication compteur,
    PrefixesDElementsDeFabrication prefixes,
    Clock clock
  ) {
    Assert.notNull("repository", repository);
    Assert.notNull("compteur", compteur);
    Assert.notNull("prefixes", prefixes);
    Assert.notNull("clock", clock);

    this.repository = repository;
    this.compteur = compteur;
    this.prefixes = prefixes;
    this.clock = clock;
  }

  public static ElementsDeFabricationServiceRepositoryBuilder builder() {
    return repository -> compteur -> prefixes -> clock -> new ElementsDeFabricationService(repository, compteur, prefixes, clock);
  }

  public ElementDeFabrication create(ElementDeFabricationToCreate toCreate) {
    Instant maintenant = clock.now();
    Annee annee = Annee.de(maintenant);

    ElementDeFabrication cree = switch (toCreate) {
      case OrdreDeFabricationToCreate ordre -> OrdreDeFabrication.builder()
        .id(OrdreDeFabricationId.newId())
        .nom(nomDOrdreDeFabrication(annee))
        .titre(ordre.titre())
        .description(ordre.description())
        .dateDeCreation(maintenant)
        .dateDeModification(maintenant);
      case ProduitToCreate produit -> Produit.builder()
        .id(ProduitId.newId())
        .nom(nomDeProduit(annee))
        .titre(produit.titre())
        .description(produit.description())
        .dateDeCreation(maintenant)
        .dateDeModification(maintenant);
    };

    return repository.create(cree);
  }

  public ElementDeFabrication get(ElementDeFabricationId id) {
    return repository.get(id).orElseThrow(() -> new ElementDeFabricationIntrouvableException(id));
  }

  public Page<ElementDeFabrication> list(Periode periode, Pageable pageable) {
    return repository.list(new ElementDeFabricationCriteria(periode), pageable);
  }

  public ElementDeFabrication update(ElementDeFabricationToUpdate toUpdate) {
    ElementDeFabrication existant = get(toUpdate.id());
    Instant maintenant = clock.now();

    ElementDeFabrication modifie = switch (existant) {
      case OrdreDeFabrication ordre -> OrdreDeFabrication.builder()
        .id(ordre.id())
        .nom(ordre.nom())
        .titre(toUpdate.titre())
        .description(toUpdate.description())
        .dateDeCreation(ordre.dateDeCreation())
        .dateDeModification(maintenant);
      case Produit produit -> Produit.builder()
        .id(produit.id())
        .nom(produit.nom())
        .titre(toUpdate.titre())
        .description(toUpdate.description())
        .dateDeCreation(produit.dateDeCreation())
        .dateDeModification(maintenant);
    };

    return repository.update(modifie);
  }

  public void delete(ElementDeFabricationId id) {
    repository.delete(id);
  }

  private Nom nomDOrdreDeFabrication(Annee annee) {
    return Nom.of(prefixes.prefixeDOrdreDeFabrication(), annee, compteur.prochainNumeroDOrdreDeFabrication(annee));
  }

  private Nom nomDeProduit(Annee annee) {
    return Nom.of(prefixes.prefixeDeProduit(), annee, compteur.prochainNumeroDeProduit(annee));
  }

  public interface ElementsDeFabricationServiceRepositoryBuilder {
    ElementsDeFabricationServiceCompteurBuilder repository(ElementDeFabricationRepository repository);
  }

  public interface ElementsDeFabricationServiceCompteurBuilder {
    ElementsDeFabricationServicePrefixesBuilder compteur(CompteurDElementsDeFabrication compteur);
  }

  public interface ElementsDeFabricationServicePrefixesBuilder {
    ElementsDeFabricationServiceClockBuilder prefixes(PrefixesDElementsDeFabrication prefixes);
  }

  public interface ElementsDeFabricationServiceClockBuilder {
    ElementsDeFabricationService clock(Clock clock);
  }
}
