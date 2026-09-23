package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;

/**
 * Ce que le pupitre retient d'un operateur : de quoi le designer, et sur quoi il peut pointer.
 *
 * <p>
 * L'etat de presence en fait partie : le pupitre n'offre que les gestes de presence que l'atelier acceptera, et il
 * doit pouvoir le faire hors ligne, sur le seul contenu de son cache.
 * </p>
 *
 * <p>
 * Pas de taux horaire, contrairement a {@code GET /api/operateurs} : un ecran d'atelier partage n'a aucune raison de
 * recevoir ce que {@code coutderevient} reserve au gestionnaire. Pas de natures non plus, le pupitre n'agrege rien.
 * </p>
 */
public record OperateurDuPupitre(
  OperateurId id,
  Nom nom,
  Prenom prenom,
  Optional<Matricule> matricule,
  EtatDePresence etat,
  List<PosteHabilite> postes
) {
  public OperateurDuPupitre {
    Assert.notNull("id de l'operateur", id);
    Assert.notNull("nom", nom);
    Assert.notNull("prenom", prenom);
    Assert.notNull("matricule", matricule);
    Assert.notNull("etat de presence", etat);
    Assert.field("postes", postes).notNull().noNullElement();
    postes = List.copyOf(postes);
  }

  /**
   * Publique pour la seule raison admise : la relecture depuis la persistance vit dans
   * {@code infrastructure/secondary}.
   */
  public static OperateurDuPupitreIdBuilder builder() {
    return id ->
      nom -> prenom -> matricule -> etat -> postes -> new OperateurDuPupitre(id, nom, prenom, Matricule.of(matricule), etat, postes);
  }

  public interface OperateurDuPupitreIdBuilder {
    OperateurDuPupitreNomBuilder id(OperateurId id);
  }

  public interface OperateurDuPupitreNomBuilder {
    OperateurDuPupitrePrenomBuilder nom(Nom nom);
  }

  public interface OperateurDuPupitrePrenomBuilder {
    OperateurDuPupitreMatriculeBuilder prenom(Prenom prenom);
  }

  public interface OperateurDuPupitreMatriculeBuilder {
    OperateurDuPupitreEtatBuilder matricule(String matricule);
  }

  public interface OperateurDuPupitreEtatBuilder {
    OperateurDuPupitrePostesBuilder etat(EtatDePresence etat);
  }

  public interface OperateurDuPupitrePostesBuilder {
    OperateurDuPupitre postes(List<PosteHabilite> postes);
  }
}
