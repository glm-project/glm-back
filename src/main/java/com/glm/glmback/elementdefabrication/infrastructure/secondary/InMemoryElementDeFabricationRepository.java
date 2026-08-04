package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationCriteria;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationDejaExistantException;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationId;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationIntrouvableException;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabricationRepository;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryElementDeFabricationRepository implements ElementDeFabricationRepository {

  private final Map<ElementDeFabricationId, ElementDeFabrication> elements = new ConcurrentHashMap<>();

  @Override
  public ElementDeFabrication create(ElementDeFabrication element) {
    ElementDeFabrication existant = elements.putIfAbsent(element.id(), element);
    if (existant != null) {
      throw new ElementDeFabricationDejaExistantException(element.id());
    }

    return element;
  }

  @Override
  public ElementDeFabrication update(ElementDeFabrication element) {
    ElementDeFabrication precedent = elements.replace(element.id(), element);
    if (precedent == null) {
      throw new ElementDeFabricationIntrouvableException(element.id());
    }

    return element;
  }

  @Override
  public void delete(ElementDeFabricationId id) {
    ElementDeFabrication supprime = elements.remove(id);
    if (supprime == null) {
      throw new ElementDeFabricationIntrouvableException(id);
    }
  }

  @Override
  public Optional<ElementDeFabrication> get(ElementDeFabricationId id) {
    return Optional.ofNullable(elements.get(id));
  }

  @Override
  public Page<ElementDeFabrication> list(ElementDeFabricationCriteria criteria, Pageable pageable) {
    List<ElementDeFabrication> filtres = elements
      .values()
      .stream()
      .filter(criteria::matches)
      .sorted(parDateDeCreationDescendante())
      .toList();

    return Page.<ElementDeFabrication>builder()
      .content(filtres.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(filtres.size());
  }

  private static Comparator<ElementDeFabrication> parDateDeCreationDescendante() {
    return Comparator.comparing(ElementDeFabrication::dateDeCreation, Comparator.<Instant>reverseOrder()).thenComparing(
      ElementDeFabrication::id
    );
  }
}
