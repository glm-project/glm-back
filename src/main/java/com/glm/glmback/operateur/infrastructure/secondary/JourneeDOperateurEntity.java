package com.glm.glmback.operateur.infrastructure.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule des journees de travail.
 *
 * <p>
 * Un operateur peut avoir pointe sa presence sans jamais avoir touche a un element : la presence seule suffit a
 * interdire sa suppression, puisqu'elle est le socle de la paie.
 * </p>
 */
@Entity
@Immutable
@Table(name = "journee_de_travail")
class JourneeDOperateurEntity {

  @Id
  private UUID id;

  @Column(name = "operateur_id")
  private UUID operateurId;

  protected JourneeDOperateurEntity() {
    // Constructeur requis par JPA.
  }
}
