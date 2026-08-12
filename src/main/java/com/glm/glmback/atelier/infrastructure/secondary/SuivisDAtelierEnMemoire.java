package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierCriteria;
import com.glm.glmback.atelier.domain.SuiviDAtelierDejaExistantException;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuiviDAtelierRepository;
import com.glm.glmback.shared.multitenancy.application.CurrentTenant;
import com.glm.glmback.shared.multitenancy.domain.Tenant;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Persistance en memoire des suivis d'atelier, en attendant l'adapter JPA.
 *
 * <p>
 * Le stock est partitionne par entreprise, comme le seront les schemas PostgreSQL : une entreprise ne doit jamais
 * voir les suivis d'une autre, et un adapter provisoire qui relacherait cette garantie donnerait au developpeur front
 * une API qui ment sur son isolation.
 * </p>
 */
@Repository
class SuivisDAtelierEnMemoire implements SuiviDAtelierRepository {

  private final Map<Tenant, Map<SuiviDAtelierId, SuiviDAtelier>> stocks = new ConcurrentHashMap<>();

  @Override
  public SuiviDAtelier create(SuiviDAtelier suivi) {
    Map<SuiviDAtelierId, SuiviDAtelier> suivis = stock();
    if (suivis.containsKey(suivi.id())) {
      throw new SuiviDAtelierDejaExistantException(suivi.id());
    }
    suivis.put(suivi.id(), suivi);

    return suivi;
  }

  @Override
  public SuiviDAtelier update(SuiviDAtelier suivi) {
    Map<SuiviDAtelierId, SuiviDAtelier> suivis = stock();
    if (!suivis.containsKey(suivi.id())) {
      throw new SuiviDAtelierIntrouvableException(suivi.id());
    }
    suivis.put(suivi.id(), suivi);

    return suivi;
  }

  @Override
  public Optional<SuiviDAtelier> get(SuiviDAtelierId id) {
    return Optional.ofNullable(stock().get(id));
  }

  @Override
  public Optional<SuiviDAtelier> getEnCoursPour(ElementEngageId element) {
    return stock()
      .values()
      .stream()
      .filter(suivi -> !suivi.estCloture())
      .filter(suivi -> suivi.element().id().equals(element))
      .findFirst();
  }

  @Override
  public Page<SuiviDAtelier> list(SuiviDAtelierCriteria criteria, Pageable pageable) {
    List<SuiviDAtelier> retenus = stock().values().stream().filter(criteria::matches).sorted(parDateDEngagementDescendante()).toList();

    return Page.<SuiviDAtelier>builder()
      .content(retenus.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(retenus.size());
  }

  private Map<SuiviDAtelierId, SuiviDAtelier> stock() {
    return stocks.computeIfAbsent(CurrentTenant.tenant(), tenant -> new ConcurrentHashMap<>());
  }

  private static Comparator<SuiviDAtelier> parDateDEngagementDescendante() {
    return Comparator.comparing((SuiviDAtelier suivi) -> suivi.engagement().date(), Comparator.<Instant>reverseOrder()).thenComparing(
      SuiviDAtelier::id
    );
  }
}
