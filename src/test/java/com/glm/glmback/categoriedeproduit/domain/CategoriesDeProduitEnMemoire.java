package com.glm.glmback.categoriedeproduit.domain;

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
final class CategoriesDeProduitEnMemoire implements CategorieDeProduitRepository {

  private final Map<CodeDeCategorie, CategorieDeProduit> categories = new ConcurrentHashMap<>();

  @Override
  public CategorieDeProduit create(CategorieDeProduit categorie) {
    CategorieDeProduit existante = categories.putIfAbsent(categorie.code(), categorie);
    if (existante != null) {
      throw new CategorieDejaExistanteException(categorie.code());
    }

    return categorie;
  }

  @Override
  public CategorieDeProduit update(CategorieDeProduit categorie) {
    CategorieDeProduit precedente = categories.replace(categorie.code(), categorie);
    if (precedente == null) {
      throw new CategorieIntrouvableException(categorie.code());
    }

    return categorie;
  }

  @Override
  public void delete(CodeDeCategorie code) {
    CategorieDeProduit supprimee = categories.remove(code);
    if (supprimee == null) {
      throw new CategorieIntrouvableException(code);
    }
  }

  @Override
  public Optional<CategorieDeProduit> get(CodeDeCategorie code) {
    return Optional.ofNullable(categories.get(code));
  }

  @Override
  public Optional<Rang> dernierRang() {
    return categories.values().stream().map(CategorieDeProduit::rang).max(Comparator.comparingInt(Rang::value));
  }

  @Override
  public long compte() {
    return categories.size();
  }

  @Override
  public Page<CategorieDeProduit> list(Pageable pageable) {
    List<CategorieDeProduit> triees = categories.values().stream().sorted(parRang()).toList();

    return Page.<CategorieDeProduit>builder()
      .content(triees.stream().skip(pageable.offset()).limit(pageable.size()).toList())
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(triees.size());
  }

  private static Comparator<CategorieDeProduit> parRang() {
    return Comparator.comparingInt((CategorieDeProduit categorie) -> categorie.rang().value()).thenComparing(categorie ->
      categorie.code().value()
    );
  }
}
