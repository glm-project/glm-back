package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Nom;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.Prenom;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des operateurs.
 *
 * <p>
 * L'atelier lit la table du contexte voisin sans jamais importer son code, annote {@code @BusinessContext}. Rien n'en
 * est copie dans le journal, qui ne stocke que l'identifiant : l'identite est relue a chaque lecture, pour qu'une
 * correction s'affiche partout.
 * </p>
 */
@Entity
@Immutable
@Table(name = "operateur")
class OperateurConnuEntity {

  @Id
  private UUID id;

  private String nom;

  private String prenom;

  protected OperateurConnuEntity() {
    // Constructeur requis par JPA.
  }

  OperateurConnu toDomain() {
    return new OperateurConnu(new OperateurId(id), new Nom(nom), new Prenom(prenom));
  }
}
