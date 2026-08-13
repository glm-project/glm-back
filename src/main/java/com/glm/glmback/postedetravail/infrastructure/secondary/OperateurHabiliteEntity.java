package com.glm.glmback.postedetravail.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des habilitations.
 *
 * <p>
 * Ce contexte lit la table du contexte voisin sans jamais importer son code, annote {@code @BusinessContext} : il n'en
 * retient que le poste reference, la seule chose dont il a besoin pour refuser une suppression.
 * </p>
 *
 * <p>
 * La table porte une cle composite ; l'identifiant JPA declare ici ne sert qu'a satisfaire la specification. Aucune
 * lecture par identite n'est faite : le seul acces passe par {@code existsByPosteId}, qui interroge la colonne du
 * poste.
 * </p>
 */
@Entity
@Immutable
@Table(name = "operateur_poste")
class OperateurHabiliteEntity {

  @Id
  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  protected OperateurHabiliteEntity() {
    // Constructeur requis par JPA.
  }
}
