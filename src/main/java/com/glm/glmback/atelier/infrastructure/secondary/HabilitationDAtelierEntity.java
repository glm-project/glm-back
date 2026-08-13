package com.glm.glmback.atelier.infrastructure.secondary;

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
 * La table porte une cle composite ; l'identifiant JPA declare ici ne sert qu'a satisfaire la specification. Aucune
 * lecture par identite n'est faite : le seul acces passe par l'existence du couple operateur / poste.
 * </p>
 */
@Entity
@Immutable
@Table(name = "operateur_poste")
class HabilitationDAtelierEntity {

  @Id
  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  protected HabilitationDAtelierEntity() {
    // Constructeur requis par JPA.
  }
}
