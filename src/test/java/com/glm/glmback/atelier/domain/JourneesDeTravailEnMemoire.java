package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class JourneesDeTravailEnMemoire implements JourneeDeTravailRepository {

  private final Map<JourneeDeTravailId, JourneeDeTravail> journees = new ConcurrentHashMap<>();

  @Override
  public JourneeDeTravail create(JourneeDeTravail journee) {
    if (journees.containsKey(journee.id())) {
      throw new JourneeDeTravailDejaOuverteException(journee.operateur());
    }
    journees.put(journee.id(), journee);

    return journee;
  }

  @Override
  public JourneeDeTravail update(JourneeDeTravail journee) {
    if (!journees.containsKey(journee.id())) {
      throw new JourneeDeTravailIntrouvableException(journee.id());
    }
    journees.put(journee.id(), journee);

    return journee;
  }

  @Override
  public Optional<JourneeDeTravail> get(JourneeDeTravailId id) {
    return Optional.ofNullable(journees.get(id));
  }

  @Override
  public Optional<JourneeDeTravail> getEnCoursPour(Operateur operateur) {
    return journees
      .values()
      .stream()
      .filter(journee -> journee.operateur().equals(operateur))
      .filter(JourneeDeTravail::estEnCours)
      .findFirst();
  }

  @Override
  public Optional<JourneeDeTravail> journeeContenant(Operateur operateur, Instant instant) {
    return journees
      .values()
      .stream()
      .filter(journee -> journee.operateur().equals(operateur))
      .filter(journee -> journee.contient(instant))
      .findFirst();
  }

  @Override
  public Page<JourneeDeTravail> list(JourneeDeTravailCriteria criteria, Pageable pageable) {
    List<JourneeDeTravail> retenues = journees.values().stream().filter(criteria::matches).sorted(parDebutDescendant()).toList();

    return Page.<JourneeDeTravail>builder()
      .content(retenues.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(retenues.size());
  }

  private static Comparator<JourneeDeTravail> parDebutDescendant() {
    return Comparator.comparing(
      (JourneeDeTravail journee) -> journee.debut().orElse(Instant.MIN),
      Comparator.<Instant>reverseOrder()
    ).thenComparing(JourneeDeTravail::id);
  }
}
