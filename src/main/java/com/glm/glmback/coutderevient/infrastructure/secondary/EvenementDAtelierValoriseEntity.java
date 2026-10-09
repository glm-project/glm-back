package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.CoutHoraire;
import com.glm.glmback.coutderevient.domain.TauxHoraire;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Tarifs captures sur le fait ouvrant, jamais relus au referentiel. */
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

  protected EvenementDAtelierValoriseEntity() {
    /* Requis par JPA. */
  }

  Optional<CoutHoraire> coutHoraire() {
    return CoutHoraire.of(coutHoraire);
  }

  Optional<TauxHoraire> tauxHoraire() {
    return TauxHoraire.of(tauxHoraire);
  }
}
