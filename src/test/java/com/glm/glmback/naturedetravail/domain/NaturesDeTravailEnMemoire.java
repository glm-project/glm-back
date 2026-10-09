package com.glm.glmback.naturedetravail.domain;

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
final class NaturesDeTravailEnMemoire implements NatureDeTravailRepository {

  private final Map<NatureDeTravailId, NatureDeTravail> natures = new ConcurrentHashMap<>();

  @Override
  public NatureDeTravail create(NatureDeTravail nature) {
    NatureDeTravail existante = natures.putIfAbsent(nature.id(), nature);
    if (existante != null) {
      throw new NatureDeTravailDejaCreeeException(nature.id());
    }

    return nature;
  }

  @Override
  public NatureDeTravail update(NatureDeTravail nature) {
    NatureDeTravail precedente = natures.replace(nature.id(), nature);
    if (precedente == null) {
      throw new NatureIntrouvableException(nature.id());
    }

    return nature;
  }

  @Override
  public void delete(NatureDeTravailId id) {
    NatureDeTravail supprimee = natures.remove(id);
    if (supprimee == null) {
      throw new NatureIntrouvableException(id);
    }
  }

  @Override
  public Optional<NatureDeTravail> get(NatureDeTravailId id) {
    return Optional.ofNullable(natures.get(id));
  }

  @Override
  public Optional<NatureDeTravailId> idPourCle(CleDeNature cle) {
    return natures
      .values()
      .stream()
      .filter(nature -> nature.libelle().cle().equals(cle))
      .map(NatureDeTravail::id)
      .findFirst();
  }

  @Override
  public Page<NatureDeTravail> list(Pageable pageable) {
    List<NatureDeTravail> triees = natures.values().stream().sorted(parCle()).toList();

    return Page.<NatureDeTravail>builder()
      .content(triees.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(triees.size());
  }

  private static Comparator<NatureDeTravail> parCle() {
    return Comparator.comparing((NatureDeTravail nature) -> nature.libelle().cle().value()).thenComparing(NatureDeTravail::id);
  }
}
