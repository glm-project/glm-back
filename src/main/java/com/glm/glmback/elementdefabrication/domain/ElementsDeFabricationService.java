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

    return repository.create(
      ElementDeFabrication.builder()
        .id(ElementDeFabricationId.newId())
        .type(toCreate.type())
        .nom(nom(toCreate.type(), Annee.of(maintenant)))
        .titre(toCreate.titre().value())
        .description(toCreate.description().value())
        .dateDeCreation(maintenant)
        .dateDeModification(maintenant)
    );
  }

  public ElementDeFabrication get(ElementDeFabricationId id) {
    return repository.get(id).orElseThrow(() -> new ElementDeFabricationIntrouvableException(id));
  }

  public Page<ElementDeFabrication> list(Periode periode, Pageable pageable) {
    return repository.list(new ElementDeFabricationCriteria(periode), pageable);
  }

  public ElementDeFabrication update(ElementDeFabricationToUpdate toUpdate) {
    return repository.update(get(toUpdate.id()).revise(toUpdate.titre(), toUpdate.description(), clock.now()));
  }

  public void delete(ElementDeFabricationId id) {
    repository.delete(id);
  }

  private Nom nom(TypeDElementDeFabrication type, Annee annee) {
    return Nom.of(prefixes.prefixe(type), annee, compteur.prochainNumero(type, annee));
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
