package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.Nom;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.Prenom;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule de la table des operateurs.
 *
 * <p>
 * L'atelier lit la table du contexte voisin sans jamais importer son code, annote {@code @BusinessContext}. Rien n'en
 * est copie dans le journal a partir de l'identite, qui ne stocke que l'identifiant : l'identite est relue a chaque
 * lecture, pour qu'une correction s'affiche partout. Le taux horaire, lui, est recopie sur l'evenement au moment de
 * la saisie.
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

  @Column(name = "taux_horaire", precision = 10, scale = 2)
  private BigDecimal tauxHoraire;

  protected OperateurConnuEntity() {
    // Constructeur requis par JPA.
  }

  OperateurConnu toDomain() {
    return OperateurConnu.builder().id(new OperateurId(id)).nom(new Nom(nom)).prenom(new Prenom(prenom)).tauxHoraire(tauxHoraire);
  }
}
