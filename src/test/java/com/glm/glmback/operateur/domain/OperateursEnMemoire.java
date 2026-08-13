package com.glm.glmback.operateur.domain;

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
final class OperateursEnMemoire implements OperateurRepository {

  private final Map<OperateurId, Operateur> operateurs = new ConcurrentHashMap<>();

  @Override
  public Operateur create(Operateur operateur) {
    Operateur existant = operateurs.putIfAbsent(operateur.id(), operateur);
    if (existant != null) {
      throw new OperateurDejaExistantException(operateur.id());
    }

    return operateur;
  }

  @Override
  public Operateur update(Operateur operateur) {
    Operateur precedent = operateurs.replace(operateur.id(), operateur);
    if (precedent == null) {
      throw new OperateurIntrouvableException(operateur.id());
    }

    return operateur;
  }

  @Override
  public void delete(OperateurId id) {
    Operateur supprime = operateurs.remove(id);
    if (supprime == null) {
      throw new OperateurIntrouvableException(id);
    }
  }

  @Override
  public Optional<Operateur> get(OperateurId id) {
    return Optional.ofNullable(operateurs.get(id));
  }

  @Override
  public Optional<OperateurId> idPourIdentite(Nom nom, Prenom prenom) {
    return operateurs
      .values()
      .stream()
      .filter(operateur -> operateur.nom().equals(nom) && operateur.prenom().equals(prenom))
      .findFirst()
      .map(Operateur::id);
  }

  @Override
  public Optional<OperateurId> idPourMatricule(Matricule matricule) {
    return operateurs
      .values()
      .stream()
      .filter(operateur -> operateur.matricule().filter(matricule::equals).isPresent())
      .findFirst()
      .map(Operateur::id);
  }

  @Override
  public Page<Operateur> list(OperateurCriteria criteria, Pageable pageable) {
    List<Operateur> filtres = operateurs.values().stream().filter(criteria::matches).sorted(parIdentite()).toList();

    return Page.<Operateur>builder()
      .content(filtres.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(filtres.size());
  }

  private static Comparator<Operateur> parIdentite() {
    return Comparator.comparing((Operateur operateur) -> operateur.nom().value())
      .thenComparing(operateur -> operateur.prenom().value())
      .thenComparing(Operateur::id);
  }
}
