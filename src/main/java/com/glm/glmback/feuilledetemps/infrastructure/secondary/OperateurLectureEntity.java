package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import com.glm.glmback.feuilledetemps.domain.Nom;
import com.glm.glmback.feuilledetemps.domain.OperateurConnu;
import com.glm.glmback.feuilledetemps.domain.OperateurId;
import com.glm.glmback.feuilledetemps.domain.Prenom;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des operateurs, restreinte a l'identite affichee sur une feuille de temps.
 *
 * <p>
 * Ni taux horaire ni habilitation : ce contexte affiche du temps, il ne le valorise pas et n'autorise rien. Comme
 * l'atelier, il lit la table du voisin sans importer son code, annote {@code BusinessContext}.
 * </p>
 */
@Entity
@Immutable
@Table(name = "operateur")
class OperateurLectureEntity {

  @Id
  private UUID id;

  private String nom;

  private String prenom;

  protected OperateurLectureEntity() {
    // Constructeur requis par JPA.
  }

  OperateurConnu toDomain() {
    return new OperateurConnu(new OperateurId(id), new Nom(nom), new Prenom(prenom));
  }
}
