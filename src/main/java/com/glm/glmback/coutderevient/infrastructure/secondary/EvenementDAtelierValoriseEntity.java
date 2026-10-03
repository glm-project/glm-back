package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.CoutHoraire;
import com.glm.glmback.coutderevient.domain.PointageEnConflit;
import com.glm.glmback.coutderevient.domain.TauxHoraire;
import com.glm.glmback.coutderevient.domain.TypeDePointage;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Tarifs captures sur le fait ouvrant actif, jamais relus au referentiel ; et, pour un fait d'une sequence en conflit,
 * ce qu'il disait et quand.
 */
@Entity
@Immutable
@Table(name = "evenement_d_atelier")
class EvenementDAtelierValoriseEntity {

  @Id
  private UUID id;

  @Column(name = "cout_horaire", precision = 10, scale = 2)
  private BigDecimal coutHoraire;

  @Column(name = "taux_horaire", precision = 10, scale = 2)
  private BigDecimal tauxHoraire;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private TypeDePointage type;

  @Convert(converter = ExactInstantConverter.class)
  private Instant dateDeSurvenue;

  protected EvenementDAtelierValoriseEntity() {
    /* Requis par JPA. */
  }

  Optional<CoutHoraire> coutHoraire() {
    return CoutHoraire.of(coutHoraire);
  }

  Optional<TauxHoraire> tauxHoraire() {
    return TauxHoraire.of(tauxHoraire);
  }

  UUID id() {
    return id;
  }

  PointageEnConflit enConflit() {
    return new PointageEnConflit(id, type, dateDeSurvenue);
  }
}
