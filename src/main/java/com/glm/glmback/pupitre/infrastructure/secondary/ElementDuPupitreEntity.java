package com.glm.glmback.pupitre.infrastructure.secondary;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des elements de fabrication, restreinte a leur reference.
 *
 * <p>
 * Le nom n'est pas lu ici : c'est celui copie sur le suivi a l'engagement qui fait foi, pour qu'un element renomme ne
 * reecrive pas l'histoire de l'atelier. La reference suit la regle inverse — c'est un libelle courant, relu a chaque
 * lecture, qui disparait avec la fiche.
 * </p>
 */
@Entity
@Immutable
@Table(name = "element_de_fabrication")
class ElementDuPupitreEntity {

  @Id
  private UUID id;

  private String reference;

  protected ElementDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }

  String reference() {
    return reference;
  }
}
