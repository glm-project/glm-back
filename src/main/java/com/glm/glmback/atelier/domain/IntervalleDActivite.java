package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Du temps affecte a un operateur, un poste de travail et une categorie, produit par le repli du journal.
 *
 * <p>
 * Un intervalle sans fin est encore en cours : le repli se fait sans horloge, seule une sommation aurait besoin de
 * savoir quand on est.
 * </p>
 */
public record IntervalleDActivite(
  EvenementDAtelierId evenement,
  Operateur operateur,
  Optional<PosteDeTravail> poste,
  Optional<NatureDOperation> nature,
  CategorieDActivite categorie,
  Instant debut,
  Optional<Instant> fin
) {
  public IntervalleDActivite {
    Assert.notNull("evenement", evenement);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("categorie", categorie);
    Assert.notNull("debut", debut);
    Assert.notNull("fin", fin);
    fin.ifPresent(date -> Assert.field("fin", date).afterOrAt(debut));
  }

  static IntervalleDActiviteEvenementBuilder builder() {
    return evenement ->
      operateur ->
        poste -> nature -> categorie -> debut -> fin -> new IntervalleDActivite(evenement, operateur, poste, nature, categorie, debut, fin);
  }

  /**
   * Le meme intervalle reduit a la fenetre de presence donnee, s'il en reste quelque chose.
   */
  public Optional<IntervalleDActivite> reduitA(FenetreDePresence fenetre) {
    return fenetre
      .intersection(debut, fin)
      .map(part -> new IntervalleDActivite(evenement, operateur, poste, nature, categorie, part.debut(), part.fin()));
  }

  public boolean estOuvert() {
    return fin.isEmpty();
  }

  public CleDActivite cle() {
    return new CleDActivite(operateur, poste);
  }

  interface IntervalleDActiviteEvenementBuilder {
    IntervalleDActiviteOperateurBuilder evenement(EvenementDAtelierId evenement);
  }

  interface IntervalleDActiviteOperateurBuilder {
    IntervalleDActivitePosteBuilder operateur(Operateur operateur);
  }

  interface IntervalleDActivitePosteBuilder {
    IntervalleDActiviteNatureBuilder poste(Optional<PosteDeTravail> poste);
  }

  interface IntervalleDActiviteNatureBuilder {
    IntervalleDActiviteCategorieBuilder nature(Optional<NatureDOperation> nature);
  }

  interface IntervalleDActiviteCategorieBuilder {
    IntervalleDActiviteDebutBuilder categorie(CategorieDActivite categorie);
  }

  interface IntervalleDActiviteDebutBuilder {
    IntervalleDActiviteFinBuilder debut(Instant debut);
  }

  interface IntervalleDActiviteFinBuilder {
    IntervalleDActivite fin(Optional<Instant> fin);
  }
}
