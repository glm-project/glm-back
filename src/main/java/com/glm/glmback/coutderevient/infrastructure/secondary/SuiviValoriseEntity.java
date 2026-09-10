package com.glm.glmback.coutderevient.infrastructure.secondary;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'un passage en atelier, reduite a ce qui referme son journal.
 *
 * <p>
 * La colonne {@code etat} n'est pas lue : c'est une projection de l'atelier, et le journal reste la source de verite.
 * Seule la date de cloture compte ici, comme fermeture finale des activites que personne n'a arretees.
 * </p>
 *
 * <p>
 * Le nommage des colonnes reste implicite, comme dans l'entite d'ecriture de l'atelier : deux entites qui donnent a
 * la meme colonne physique deux noms logiques differents — l'un implicite, l'autre explicite — empechent Hibernate de
 * demarrer.
 * </p>
 */
@Entity
@Immutable
@Table(name = "suivi_d_atelier")
class SuiviValoriseEntity {

  @Id
  private UUID id;

  private UUID elementId;

  private Instant clotureDateDeSurvenue;

  protected SuiviValoriseEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }

  Optional<Instant> cloture() {
    return Optional.ofNullable(clotureDateDeSurvenue);
  }
}
