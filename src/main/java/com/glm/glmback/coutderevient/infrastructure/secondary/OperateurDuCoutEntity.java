package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.NomDOperateur;
import com.glm.glmback.coutderevient.domain.OperateurId;
import com.glm.glmback.coutderevient.domain.OperateurNomme;
import com.glm.glmback.coutderevient.domain.PrenomDOperateur;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des operateurs, restreinte a l'identite que le detail d'un pointage affiche.
 *
 * <p>
 * Ni taux horaire ni habilitation : le taux valorise vient du fait ouvrant, fige a la saisie. Ce contexte lit la table
 * du voisin sans importer son code, annote {@code BusinessContext}.
 * </p>
 */
@Entity
@Immutable
@Table(name = "operateur")
class OperateurDuCoutEntity {

  @Id
  private UUID id;

  private String nom;

  private String prenom;

  protected OperateurDuCoutEntity() {
    // Constructeur requis par JPA.
  }

  OperateurNomme toDomain() {
    return new OperateurNomme(new OperateurId(id), new PrenomDOperateur(prenom), new NomDOperateur(nom));
  }
}
