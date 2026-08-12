package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailCriteria;
import com.glm.glmback.atelier.domain.JourneeDeTravailDejaOuverteException;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.JourneeDeTravailIntrouvableException;
import com.glm.glmback.atelier.domain.JourneeDeTravailRepository;
import com.glm.glmback.atelier.domain.Operateur;
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
 * Persistance en memoire des journees de travail, en attendant l'adapter JPA.
 *
 * <p>
 * Partitionnee par entreprise, pour la meme raison que {@link SuivisDAtelierEnMemoire}.
 * </p>
 */
@Repository
class JourneesDeTravailEnMemoire implements JourneeDeTravailRepository {

  private final Map<Tenant, Map<JourneeDeTravailId, JourneeDeTravail>> stocks = new ConcurrentHashMap<>();

  @Override
  public JourneeDeTravail create(JourneeDeTravail journee) {
    Map<JourneeDeTravailId, JourneeDeTravail> journees = stock();
    if (journees.containsKey(journee.id())) {
      throw new JourneeDeTravailDejaOuverteException(journee.operateur());
    }
    journees.put(journee.id(), journee);

    return journee;
  }

  @Override
  public JourneeDeTravail update(JourneeDeTravail journee) {
    Map<JourneeDeTravailId, JourneeDeTravail> journees = stock();
    if (!journees.containsKey(journee.id())) {
      throw new JourneeDeTravailIntrouvableException(journee.id());
    }
    journees.put(journee.id(), journee);

    return journee;
  }

  @Override
  public Optional<JourneeDeTravail> get(JourneeDeTravailId id) {
    return Optional.ofNullable(stock().get(id));
  }

  @Override
  public Optional<JourneeDeTravail> getEnCoursPour(Operateur operateur) {
    return stock()
      .values()
      .stream()
      .filter(journee -> journee.operateur().equals(operateur))
      .filter(JourneeDeTravail::estEnCours)
      .findFirst();
  }

  @Override
  public Optional<JourneeDeTravail> journeeContenant(Operateur operateur, Instant instant) {
    return stock()
      .values()
      .stream()
      .filter(journee -> journee.operateur().equals(operateur))
      .filter(journee -> journee.contient(instant))
      .findFirst();
  }

  @Override
  public Page<JourneeDeTravail> list(JourneeDeTravailCriteria criteria, Pageable pageable) {
    List<JourneeDeTravail> retenues = stock().values().stream().filter(criteria::matches).sorted(parDebutDescendant()).toList();

    return Page.<JourneeDeTravail>builder()
      .content(retenues.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(retenues.size());
  }

  private Map<JourneeDeTravailId, JourneeDeTravail> stock() {
    return stocks.computeIfAbsent(CurrentTenant.tenant(), tenant -> new ConcurrentHashMap<>());
  }

  private static Comparator<JourneeDeTravail> parDebutDescendant() {
    return Comparator.comparing(
      (JourneeDeTravail journee) -> journee.debut().orElse(Instant.MIN),
      Comparator.<Instant>reverseOrder()
    ).thenComparing(JourneeDeTravail::id);
  }
}
