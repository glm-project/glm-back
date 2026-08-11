package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.domain.Clock;
import java.time.Instant;
import java.util.Optional;

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
    ElementDeFabricationId id = ElementDeFabricationId.newId();
    verifierReferenceLibre(id, toCreate.reference());

    return repository.create(
      ElementDeFabrication.builder()
        .id(id)
        .type(toCreate.type())
        .nom(nom(toCreate.type(), Annee.of(maintenant)))
        .reference(toCreate.reference().map(Reference::value).orElse(null))
        .description(toCreate.description().map(Description::value).orElse(null))
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
    ElementDeFabrication existant = get(toUpdate.id());
    verifierReferenceLibre(existant.id(), toUpdate.reference());

    return repository.update(existant.revise(toUpdate.reference(), toUpdate.description(), clock.now()));
  }

  public void delete(ElementDeFabricationId id) {
    repository.delete(id);
  }

  private Nom nom(TypeDElementDeFabrication type, Annee annee) {
    return Nom.of(prefixes.prefixe(type), annee, compteur.prochainNumero(type, annee));
  }

  /**
   * Un seul chemin sert la creation et la modification : a la creation l'identifiant vient d'etre tire,
   * l'egalite ne peut pas se produire ; a la modification, l'element qui conserve sa propre reference ne
   * se heurte pas a lui-meme.
   */
  private void verifierReferenceLibre(ElementDeFabricationId id, Optional<Reference> reference) {
    reference
      .flatMap(repository::idPourReference)
      .filter(detenteur -> !detenteur.equals(id))
      .ifPresent(detenteur -> {
        throw new ReferenceDejaUtiliseeException(reference.orElseThrow());
      });
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
