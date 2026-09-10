package com.glm.glmback.syntheseheures.infrastructure.secondary;

import com.glm.glmback.syntheseheures.domain.EvenementDePresence;
import com.glm.glmback.syntheseheures.domain.TypeDEvenementDePresence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Vue en lecture seule d'un pointage de presence, telle que la synthese des heures la lit.
 *
 * <p>
 * {@code annulationDate} n'est jamais exposee au domaine : seule sa nullite compte, pour ecarter des la requete les
 * evenements annules. Nommee {@code ...SyntheseEntity} pour ne pas entrer en conflit avec le bean JPA homonyme de
 * {@code feuilledetemps} (et celui, deja renomme, de {@code coutderevient}) — trois contextes lisent la meme table.
 * </p>
 */
@Entity
@Immutable
@Table(name = "evenement_de_presence")
class EvenementDePresenceSyntheseEntity {

  @Id
  private UUID id;

  @Column(name = "journee_id")
  private UUID journeeId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDEvenementDePresence type;

  private Instant dateDeSurvenue;

  private Instant annulationDate;

  protected EvenementDePresenceSyntheseEntity() {
    // Constructeur requis par JPA.
  }

  UUID journeeId() {
    return journeeId;
  }

  EvenementDePresence toDomain() {
    return new EvenementDePresence(type, dateDeSurvenue);
  }
}
