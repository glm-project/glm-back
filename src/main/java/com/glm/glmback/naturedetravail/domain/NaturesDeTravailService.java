package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.List;
import java.util.Set;

public final class NaturesDeTravailService {

  private final NatureDeTravailRepository repository;
  private final NaturesEnUsage usages;

  public NaturesDeTravailService(NatureDeTravailRepository repository, NaturesEnUsage usages) {
    this.repository = repository;
    this.usages = usages;
  }

  public NatureDeTravail declare(LibelleDeNature libelle) {
    NatureDeTravailId id = NatureDeTravailId.newId();
    verifierLibelleLibre(id, libelle);

    return repository.create(new NatureDeTravail(id, libelle));
  }

  /**
   * Une nature peut reprendre sa propre cle : changer la casse ou les accents de son libelle reste permis.
   */
  public NatureDeTravailListee renomme(NatureDeTravailId id, LibelleDeNature libelle) {
    NatureDeTravail existante = repository.get(id).orElseThrow(() -> new NatureIntrouvableException(id));
    verifierLibelleLibre(id, libelle);

    return new NatureDeTravailListee(repository.update(existante.renomme(libelle)), !usages.utiliseesParmi(List.of(id)).isEmpty());
  }

  /**
   * Chaque nature dit si elle sert deja, pour que l'ecran ne propose pas une suppression vouee au refus. Les usages de
   * la page sont lus en une fois.
   */
  public Page<NatureDeTravailListee> list(Pageable pageable) {
    Page<NatureDeTravail> page = repository.list(pageable);
    Set<NatureDeTravailId> utilisees = usages.utiliseesParmi(page.content().stream().map(NatureDeTravail::id).toList());

    return new Page<>(
      page
        .content()
        .stream()
        .map(nature -> new NatureDeTravailListee(nature, utilisees.contains(nature.id())))
        .toList(),
      page.currentPage(),
      page.pageSize(),
      page.totalElementsCount()
    );
  }

  /**
   * Une nature qui sert deja ne se supprime pas : un poste la porte, ou des heures pointees la citent.
   */
  public void delete(NatureDeTravailId id) {
    if (repository.get(id).isEmpty()) {
      throw new NatureIntrouvableException(id);
    }
    if (usages.estUtilisee(id)) {
      throw new NatureUtiliseeException(id);
    }
    repository.delete(id);
  }

  /**
   * L'unicite se juge sur la cle, pas sur le libelle : « Soudage » et « soudâge » sont la meme nature. A la creation,
   * l'identifiant vient d'etre tire et ne peut detenir aucune cle ; au renommage, la nature ne se heurte pas a elle-meme.
   */
  private void verifierLibelleLibre(NatureDeTravailId id, LibelleDeNature libelle) {
    repository
      .idPourCle(libelle.cle())
      .filter(detenteur -> !detenteur.equals(id))
      .ifPresent(detenteur -> {
        throw new NatureDejaExistanteException(libelle);
      });
  }
}
