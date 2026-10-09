package com.glm.glmback.naturedetravail.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Set;

public final class NaturesDeTravailService {

  private final NatureDeTravailRepository repository;
  private final NaturesEnUsage usages;

  public NaturesDeTravailService(NatureDeTravailRepository repository, NaturesEnUsage usages) {
    this.repository = repository;
    this.usages = usages;
  }

  public NatureDeTravail declare(LibelleDeNature libelle) {
    verifierLibelleLibre(libelle);

    return repository.create(new NatureDeTravail(NatureDeTravailId.newId(), libelle));
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
   * L'unicite se juge sur la cle, pas sur le libelle : « Soudage » et « soudâge » sont la meme nature.
   */
  private void verifierLibelleLibre(LibelleDeNature libelle) {
    if (repository.idPourCle(libelle.cle()).isPresent()) {
      throw new NatureDejaExistanteException(libelle);
    }
  }
}
