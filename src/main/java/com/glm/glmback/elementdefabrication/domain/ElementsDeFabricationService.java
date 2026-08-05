package com.glm.glmback.elementdefabrication.domain;

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
    Annee annee = Annee.of(maintenant);

    ElementDeFabrication cree = switch (toCreate) {
      case OrdreDeFabricationToCreate ordre -> OrdreDeFabrication.builder()
        .id(ElementDeFabricationId.newId())
        .prefixe(prefixes.prefixeDOrdreDeFabrication())
        .compteur(compteur.prochainNumeroDOrdreDeFabrication(annee))
        .titre(ordre.titre().value())
        .description(ordre.description().value())
        .dateDeCreation(maintenant)
        .dateDeModification(maintenant);
      case ProduitToCreate produit -> Produit.builder()
        .id(ElementDeFabricationId.newId())
        .prefixe(prefixes.prefixeDeProduit().value())
        .annee(annee.value())
        .compteur(compteur.prochainNumeroDeProduit(annee))
        .titre(produit.titre().value())
        .description(produit.description().value())
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
        .prefixe(ordre.nom().prefixe())
        .compteur(ordre.nom().compteur())
        .titre(toUpdate.titre().value())
        .description(toUpdate.description().value())
        .dateDeCreation(ordre.dateDeCreation())
        .dateDeModification(maintenant);
      case Produit produit -> Produit.builder()
        .id(produit.id())
        .prefixe(produit.nom().prefixe().value())
        .annee(produit.nom().annee().value())
        .compteur(produit.nom().compteur())
        .titre(toUpdate.titre().value())
        .description(toUpdate.description().value())
        .dateDeCreation(produit.dateDeCreation())
        .dateDeModification(maintenant);
    };

    return repository.update(modifie);
  }

  public void delete(ElementDeFabricationId id) {
    repository.delete(id);
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
