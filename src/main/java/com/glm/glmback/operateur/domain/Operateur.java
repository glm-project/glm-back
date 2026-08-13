package com.glm.glmback.operateur.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import java.util.Set;

/**
 * Une personne qui pointe, et les postes sur lesquels elle est habilitee.
 *
 * <p>
 * Ses metiers ne sont pas declares ici : ils se deduisent des natures de ses postes, via {@link ProfilDOperateur}. Un
 * operateur habilite sur un poste de soudure et sur un tour est soudeur et tourneur sans que personne ne l'ait saisi.
 * </p>
 */
public record Operateur(OperateurId id, Nom nom, Prenom prenom, Optional<Matricule> matricule, Set<PosteHabilitableId> postes) {
  public Operateur {
    Assert.notNull("id", id);
    Assert.notNull("nom", nom);
    Assert.notNull("prenom", prenom);
    Assert.notNull("matricule", matricule);
    Assert.field("postes", postes).notNull().noNullElement();

    postes = Set.copyOf(postes);
  }

  public static OperateurIdBuilder builder() {
    return id -> nom -> prenom -> matricule -> postes -> new Operateur(id, nom, prenom, Matricule.of(matricule), postes);
  }

  public Operateur revise(Nom nom, Prenom prenom, Optional<Matricule> matricule, Set<PosteHabilitableId> postes) {
    return new Operateur(id, nom, prenom, matricule, postes);
  }

  public interface OperateurIdBuilder {
    OperateurNomBuilder id(OperateurId id);
  }

  public interface OperateurNomBuilder {
    OperateurPrenomBuilder nom(Nom nom);
  }

  public interface OperateurPrenomBuilder {
    OperateurMatriculeBuilder prenom(Prenom prenom);
  }

  public interface OperateurMatriculeBuilder {
    OperateurPostesBuilder matricule(String matricule);
  }

  public interface OperateurPostesBuilder {
    Operateur postes(Set<PosteHabilitableId> postes);
  }
}
