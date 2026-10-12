package com.glm.glmback.elementdefabrication.infrastructure.secondary;

import com.glm.glmback.elementdefabrication.domain.AuteurDeLaCloture;
import com.glm.glmback.elementdefabrication.domain.EtatEnAtelier;
import com.glm.glmback.elementdefabrication.domain.PassageClos;
import com.glm.glmback.elementdefabrication.domain.PassageEnAtelier;
import com.glm.glmback.elementdefabrication.domain.PassageOuvert;
import com.glm.glmback.elementdefabrication.domain.SuiviId;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule du suivi d'atelier d'un element : ce contexte n'en retient que ce que ses ecrans montrent, sans
 * importer l'atelier.
 */
@Entity
@Immutable
@Table(name = "suivi_d_atelier")
class SuiviDuProduitEntity {

  @Id
  private UUID id;

  private UUID elementId;

  @Convert(converter = ExactInstantConverter.class)
  private Instant engagementDate;

  private String clotureAuteur;

  @Convert(converter = ExactInstantConverter.class)
  private Instant clotureDateDeSurvenue;

  protected SuiviDuProduitEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }

  boolean estCloture() {
    return clotureDateDeSurvenue != null;
  }

  PassageEnAtelier passage(EtatEnAtelier etat) {
    if (estCloture()) {
      return new PassageClos(new SuiviId(id), engagementDate, clotureDateDeSurvenue, new AuteurDeLaCloture(clotureAuteur));
    }
    return new PassageOuvert(new SuiviId(id), engagementDate, etat);
  }
}
