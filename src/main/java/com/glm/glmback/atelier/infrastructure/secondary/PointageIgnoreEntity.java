package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.PointageAEnregistrer;
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
 * Une ligne de l'audit des pointages ignores.
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

  private PointageIgnoreEntity(PointageIgnore ignore) {
    PointageAEnregistrer pointage = ignore.pointage();
    id = pointage.evenement().uuid();
    suiviId = pointage.suivi().uuid();
    operateurId = pointage.operateur().uuid();
    posteId = pointage.poste().map(PosteDeTravailId::uuid).orElse(null);
    type = pointage.type();
    dateDeSurvenue = ignore.horodatage().dateDeSurvenue();
    dateDeReception = ignore.horodatage().dateDEnregistrement();
    raison = ignore.verdict().raison();
    dernierAccepteId = ignore.verdict().dernierAccepte().map(EvenementDAtelierId::uuid).orElse(null);
  }

  static PointageIgnoreEntity from(PointageIgnore ignore) {
    return new PointageIgnoreEntity(ignore);
  }
}
