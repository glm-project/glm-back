package com.glm.glmback.pupitre.domain;

import java.util.List;

/**
 * Les categories de produit de l'entreprise courante, dans l'ordre choisi par le gestionnaire.
 *
 * <p>
 * Le pupitre range ses tuiles par categorie et dans cet ordre : c'est l'adapter qui trie, par rang puis par code, le
 * domaine ne connaissant pas le rang. Aucune pagination, pour la meme raison que {@link OperateursDuPupitre}.
 * </p>
 */
@FunctionalInterface
public interface CategoriesDuPupitre {
  List<CategorieDElement> toutes();
}
