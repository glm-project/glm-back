package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
import com.glm.glmback.atelier.domain.PointageEnAvance;
import com.glm.glmback.atelier.domain.PointageIgnore;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RaisonDePointageIgnore;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Une ligne de l'audit : un pointage ignore par la regle de reception, ou un pointage en avance.
 *
 * <p>
 * La table n'a ni cle ni contrainte : {@code @Id} ne dit que le seul identifiant que JPA exige, celui du geste, et rien
 * n'en garantit l'unicite en base. L'entite n'est jamais relue, seulement ecrite ; un renvoi se cherche par son
 * identifiant.
 * </p>
 */
@Entity
@Table(name = "pointage_ignore_d_atelier")
class PointageIgnoreEntity {

  @Id
  private UUID id;

  @Column(name = "suivi_id")
  private UUID suiviId;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDEvenementDAtelier type;

  @Convert(converter = ExactInstantConverter.class)
  private Instant dateDeSurvenue;

  @Convert(converter = ExactInstantConverter.class)
  private Instant dateDeReception;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private RaisonDePointageIgnore raison;

  @Column(name = "dernier_accepte_id")
  private UUID dernierAccepteId;

  protected PointageIgnoreEntity() {
    // Constructeur requis par JPA.
  }

  private PointageIgnoreEntity(PointageAEnregistrer pointage) {
    id = pointage.evenement().uuid();
    suiviId = pointage.suivi().uuid();
    operateurId = pointage.operateur().uuid();
    posteId = pointage.poste().map(PosteDeTravailId::uuid).orElse(null);
    type = pointage.type();
  }

  static PointageIgnoreEntity from(PointageIgnore ignore) {
    PointageIgnoreEntity ligne = new PointageIgnoreEntity(ignore.pointage());
    ligne.dateDeSurvenue = ignore.horodatage().dateDeSurvenue();
    ligne.dateDeReception = ignore.horodatage().dateDEnregistrement();
    ligne.raison = ignore.verdict().raison();
    ligne.dernierAccepteId = ignore.verdict().dernierAccepte().map(EvenementDAtelierId::uuid).orElse(null);

    return ligne;
  }

  /**
   * Un pointage en avance n'a ete compare a aucun pointage accepte : sa ligne n'en porte pas.
   */
  static PointageIgnoreEntity from(PointageEnAvance enAvance) {
    PointageIgnoreEntity ligne = new PointageIgnoreEntity(enAvance.pointage());
    ligne.dateDeSurvenue = enAvance.dateDeSurvenue();
    ligne.dateDeReception = enAvance.dateDeReception();
    ligne.raison = enAvance.raison();

    return ligne;
  }
}
