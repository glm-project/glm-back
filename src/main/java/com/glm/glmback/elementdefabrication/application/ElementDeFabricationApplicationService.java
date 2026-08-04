package com.glm.glmback.elementdefabrication.application;

import com.glm.glmback.elementdefabrication.domain.CompteurDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.Description;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationCriteria;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationRepository;
import com.glm.glmback.elementdefabrication.domain.ElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.Fiche;
import com.glm.glmback.elementdefabrication.domain.Nom;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabrication;
import com.glm.glmback.elementdefabrication.domain.OrdreDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.Periode;
import com.glm.glmback.elementdefabrication.domain.PrefixesDElementsDeFabrication;
import com.glm.glmback.elementdefabrication.domain.Produit;
import com.glm.glmback.elementdefabrication.domain.ProduitId;
import com.glm.glmback.elementdefabrication.domain.Titre;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ElementDeFabricationApplicationService {

  private final ElementDeFabricationRepository repository;
  private final ElementsDeFabrication elements;
  private final Clock clock;

  public ElementDeFabricationApplicationService(
    ElementDeFabricationRepository repository,
    CompteurDElementsDeFabrication compteur,
    PrefixesDElementsDeFabrication prefixes,
    Clock clock
  ) {
    this.repository = repository;
    this.elements = new ElementsDeFabrication(compteur, prefixes);
    this.clock = clock;
  }

  @Secured("ROLE_ADMIN")
  @Transactional
  public ElementDeFabrication creerOrdreDeFabrication(Optional<Nom> nom, Titre titre, Description description) {
    return repository.create(elements.ordreDeFabrication(OrdreDeFabricationId.newId(), nom, ficheAMaintenant(titre, description)));
  }

  @Secured("ROLE_ADMIN")
  @Transactional
  public ElementDeFabrication creerProduit(Optional<Nom> nom, Titre titre, Description description) {
    return repository.create(elements.produit(ProduitId.newId(), nom, ficheAMaintenant(titre, description)));
  }

  @Secured({ "ROLE_USER", "ROLE_ADMIN" })
  @Transactional(readOnly = true)
  public ElementDeFabrication obtenir(ElementDeFabricationId id) {
    return repository.get(id).orElseThrow(() -> new ElementDeFabricationIntrouvableException(id));
  }

  @Secured({ "ROLE_USER", "ROLE_ADMIN" })
  @Transactional(readOnly = true)
  public Page<ElementDeFabrication> lister(Periode periode, Pageable pageable) {
    return repository.list(new ElementDeFabricationCriteria(periode), pageable);
  }

  @Secured("ROLE_ADMIN")
  @Transactional
  public ElementDeFabrication modifier(ElementDeFabricationId id, Titre titre, Description description) {
    ElementDeFabrication existant = obtenir(id);
    Instant maintenant = clock.instant();

    ElementDeFabrication modifie = switch (existant) {
      case OrdreDeFabrication ordre -> OrdreDeFabrication.builder()
        .id(ordre.id())
        .nom(ordre.nom())
        .titre(titre)
        .description(description)
        .dateDeCreation(ordre.dateDeCreation())
        .dateDeModification(maintenant);
      case Produit produit -> Produit.builder()
        .id(produit.id())
        .nom(produit.nom())
        .titre(titre)
        .description(description)
        .dateDeCreation(produit.dateDeCreation())
        .dateDeModification(maintenant);
    };

    return repository.update(modifie);
  }

  @Secured("ROLE_ADMIN")
  @Transactional
  public void supprimer(ElementDeFabricationId id) {
    repository.delete(id);
  }

  private Fiche ficheAMaintenant(Titre titre, Description description) {
    Instant maintenant = clock.instant();

    return Fiche.builder().titre(titre).description(description).dateDeCreation(maintenant).dateDeModification(maintenant);
  }
}
