package com.glm.glmback.postedetravail.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doublure de test du port de persistance : elle laisse les tests du domaine se passer d'une base.
 */
final class PostesDeTravailEnMemoire implements PosteDeTravailRepository {

  private final Map<PosteDeTravailId, PosteDeTravail> postes = new ConcurrentHashMap<>();

  @Override
  public PosteDeTravail create(PosteDeTravail poste) {
    PosteDeTravail existant = postes.putIfAbsent(poste.id(), poste);
    if (existant != null) {
      throw new PosteDeTravailDejaExistantException(poste.id());
    }

    return poste;
  }

  @Override
  public PosteDeTravail update(PosteDeTravail poste) {
    PosteDeTravail precedent = postes.replace(poste.id(), poste);
    if (precedent == null) {
      throw new PosteDeTravailIntrouvableException(poste.id());
    }

    return poste;
  }

  @Override
  public void delete(PosteDeTravailId id) {
    PosteDeTravail supprime = postes.remove(id);
    if (supprime == null) {
      throw new PosteDeTravailIntrouvableException(id);
    }
  }

  @Override
  public Optional<PosteDeTravail> get(PosteDeTravailId id) {
    return Optional.ofNullable(postes.get(id));
  }

  @Override
  public Optional<PosteDeTravailId> idPourLibelle(Libelle libelle) {
    return postes
      .values()
      .stream()
      .filter(poste -> poste.libelle().equals(libelle))
      .findFirst()
      .map(PosteDeTravail::id);
  }

  @Override
  public Page<PosteDeTravail> list(PosteDeTravailCriteria criteria, Pageable pageable) {
    List<PosteDeTravail> filtres = postes.values().stream().filter(criteria::matches).sorted(parLibelle()).toList();

    return Page.<PosteDeTravail>builder()
      .content(filtres.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(filtres.size());
  }

  private static Comparator<PosteDeTravail> parLibelle() {
    return Comparator.comparing((PosteDeTravail poste) -> poste.libelle().value()).thenComparing(PosteDeTravail::id);
  }
}
