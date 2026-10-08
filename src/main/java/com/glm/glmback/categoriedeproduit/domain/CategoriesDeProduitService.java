package com.glm.glmback.categoriedeproduit.domain;

import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CategoriesDeProduitService {

  private final CategorieDeProduitRepository repository;
  private final CategoriesUtilisees usages;

  public CategoriesDeProduitService(CategorieDeProduitRepository repository, CategoriesUtilisees usages) {
    this.repository = repository;
    this.usages = usages;
  }

  /**
   * Une categorie nouvelle se range en dernier : l'ordre deja choisi par l'entreprise ne bouge pas.
   */
  public CategorieDeProduit create(CodeDeCategorie code) {
    if (repository.get(code).isPresent()) {
      throw new CategorieDejaExistanteException(code);
    }

    return repository.create(new CategorieDeProduit(code, repository.dernierRang().map(Rang::suivant).orElseGet(Rang::premier)));
  }

  /**
   * L'ordre est donne en entier, jamais par deplacements relatifs : il doit citer chaque categorie de l'entreprise une
   * fois et une seule, faute de quoi deux rangs pourraient se confondre ou une categorie rester sans place choisie.
   */
  public void reordonne(List<CodeDeCategorie> ordre) {
    if (new HashSet<>(ordre).size() != ordre.size() || ordre.size() != repository.compte()) {
      throw new OrdreIncompletException();
    }
    List<CategorieDeProduit> categories = ordre
      .stream()
      .map(code -> repository.get(code).orElseThrow(OrdreIncompletException::new))
      .toList();

    Rang rang = Rang.premier();
    for (CategorieDeProduit categorie : categories) {
      repository.update(categorie.deplace(rang));
      rang = rang.suivant();
    }
  }

  /**
   * Chaque categorie dit si des produits y sont ranges, pour que l'ecran ne propose pas une suppression vouee au refus.
   * Les usages de la page sont lus en une fois.
   */
  public Page<CategorieDeProduitListee> list(Pageable pageable) {
    Page<CategorieDeProduit> page = repository.list(pageable);
    Set<CodeDeCategorie> utilisees = usages.utiliseesParmi(page.content().stream().map(CategorieDeProduit::code).toList());

    return new Page<>(
      page
        .content()
        .stream()
        .map(categorie -> new CategorieDeProduitListee(categorie, utilisees.contains(categorie.code())))
        .toList(),
      page.currentPage(),
      page.pageSize(),
      page.totalElementsCount()
    );
  }

  /**
   * Une categorie qui range deja des produits ne se supprime pas : leur nom porte son code, et ils resteraient ranges
   * dans une famille disparue.
   */
  public void delete(CodeDeCategorie code) {
    if (repository.get(code).isEmpty()) {
      throw new CategorieIntrouvableException(code);
    }
    if (usages.estUtilisee(code)) {
      throw new CategorieUtiliseeException(code);
    }
    repository.delete(code);
  }
}
