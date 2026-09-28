package com.glm.glmback.feuilledetemps.infrastructure.secondary;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'un passage en atelier, reduite a l'element qu'il porte et a la cloture qui referme son
 * journal.
 *
 * <p>
 * Le nommage des colonnes reste implicite, comme dans l'entite d'ecriture de l'atelier : deux entites qui donnent a
 * la meme colonne physique deux noms logiques differents empechent Hibernate de demarrer.
 * </p>
 */
@Entity
@Immutable
@Table(name = "suivi_d_atelier")
class SuiviDeLaFeuilleDeTempsEntity {

  @Id
  private UUID id;

  private UUID elementId;

  private Instant clotureDateDeSurvenue;

  protected SuiviDeLaFeuilleDeTempsEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }

  UUID elementId() {
    return elementId;
  }

  Optional<Instant> cloture() {
    return Optional.ofNullable(clotureDateDeSurvenue);
  }
}
