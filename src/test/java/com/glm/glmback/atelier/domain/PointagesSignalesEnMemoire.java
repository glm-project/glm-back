package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class PointagesSignalesEnMemoire implements PointagesSignales {

  private final Map<PointageSignaleId, PointageSignale> signalements = new ConcurrentHashMap<>();

  @Override
  public PointageSignale create(PointageSignale pointage) {
    if (signalements.putIfAbsent(pointage.id(), pointage) != null) {
      throw new PointageSignaleDejaExistantException(pointage.id());
    }

    return pointage;
  }

  @Override
  public PointageSignale update(PointageSignale pointage) {
    if (!signalements.containsKey(pointage.id())) {
      throw new PointageSignaleIntrouvableException(pointage.id());
    }
    signalements.put(pointage.id(), pointage);

    return pointage;
  }

  @Override
  public Optional<PointageSignale> get(PointageSignaleId id) {
    return Optional.ofNullable(signalements.get(id));
  }

  @Override
  public Page<PointageSignale> list(CriteresDePointageSignale criteres, Pageable pageable) {
    List<PointageSignale> retenus = signalements
      .values()
      .stream()
      .filter(criteres::matches)
      .sorted(
        Comparator.comparing(
          (PointageSignale pointage) -> pointage.horodatage().dateDeSurvenue(),
          Comparator.<java.time.Instant>reverseOrder()
        ).thenComparing(pointage -> pointage.id().uuid())
      )
      .toList();

    return Page.<PointageSignale>builder()
      .content(retenus.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(retenus.size());
  }

  List<PointageSignale> tous() {
    return List.copyOf(signalements.values());
  }
}
