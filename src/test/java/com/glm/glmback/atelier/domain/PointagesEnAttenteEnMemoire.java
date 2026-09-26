package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PointagesEnAttenteEnMemoire implements PointagesEnAttente {

  private final Map<PointageEnAttenteId, PointageEnAttente> pointages = new ConcurrentHashMap<>();

  @Override
  public PointageEnAttente create(PointageEnAttente pointage) {
    if (pointages.putIfAbsent(pointage.id(), pointage) != null) {
      throw new PointageEnAttenteDejaExistantException(pointage.id());
    }

    return pointage;
  }

  @Override
  public PointageEnAttente update(PointageEnAttente pointage) {
    if (!pointages.containsKey(pointage.id())) {
      throw new PointageEnAttenteIntrouvableException(pointage.id());
    }
    pointages.put(pointage.id(), pointage);

    return pointage;
  }

  @Override
  public Optional<PointageEnAttente> get(PointageEnAttenteId id) {
    return Optional.ofNullable(pointages.get(id));
  }

  @Override
  public List<PointageEnAttente> parEvenementDuPupitre(UUID evenement) {
    return pointages
      .values()
      .stream()
      .filter(pointage -> pointage.evenementDuPupitre().equals(evenement))
      .toList();
  }

  @Override
  public Page<PointageEnAttente> list(CriteresDePointageEnAttente criteres, Pageable pageable) {
    List<PointageEnAttente> retenus = pointages
      .values()
      .stream()
      .filter(criteres::matches)
      .sorted(
        Comparator.comparing(PointageEnAttente::dateDeReception, Comparator.<Instant>reverseOrder()).thenComparing(PointageEnAttente::id)
      )
      .toList();

    return Page.<PointageEnAttente>builder()
      .content(retenus.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(retenus.size());
  }

  public List<PointageEnAttente> tous() {
    return List.copyOf(pointages.values());
  }
}
