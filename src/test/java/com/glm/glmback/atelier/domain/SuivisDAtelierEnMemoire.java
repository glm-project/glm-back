package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class SuivisDAtelierEnMemoire implements SuiviDAtelierRepository {

  private final Map<SuiviDAtelierId, SuiviDAtelier> suivis = new ConcurrentHashMap<>();

  @Override
  public SuiviDAtelier create(SuiviDAtelier suivi) {
    if (suivis.containsKey(suivi.id())) {
      throw new SuiviDAtelierDejaExistantException(suivi.id());
    }
    suivis.put(suivi.id(), suivi);

    return suivi;
  }

  @Override
  public SuiviDAtelier update(SuiviDAtelier suivi) {
    if (!suivis.containsKey(suivi.id())) {
      throw new SuiviDAtelierIntrouvableException(suivi.id());
    }
    suivis.put(suivi.id(), suivi);

    return suivi;
  }

  @Override
  public Optional<SuiviDAtelier> get(SuiviDAtelierId id) {
    return Optional.ofNullable(suivis.get(id));
  }

  @Override
  public Optional<SuiviDAtelier> getEnCoursPour(ElementEngageId element) {
    return suivis
      .values()
      .stream()
      .filter(suivi -> !suivi.estCloture())
      .filter(suivi -> suivi.element().id().equals(element))
      .findFirst();
  }

  @Override
  public Page<SuiviDAtelier> list(SuiviDAtelierCriteria criteria, Pageable pageable) {
    List<SuiviDAtelier> retenus = suivis.values().stream().filter(criteria::matches).sorted(parDateDEngagementDescendante()).toList();

    return Page.<SuiviDAtelier>builder()
      .content(retenus.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(retenus.size());
  }

  private static Comparator<SuiviDAtelier> parDateDEngagementDescendante() {
    return Comparator.comparing((SuiviDAtelier suivi) -> suivi.engagement().date(), Comparator.<Instant>reverseOrder()).thenComparing(
      SuiviDAtelier::id
    );
  }
}
