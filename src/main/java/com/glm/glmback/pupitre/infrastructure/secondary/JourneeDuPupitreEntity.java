package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.EtatDePresence;
import com.glm.glmback.pupitre.domain.OperateurId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "journee_de_travail")
class JourneeDuPupitreEntity {

  @Id
  private UUID id;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private EtatDePresence etat;

  private Instant debut;

  protected JourneeDuPupitreEntity() {}

  OperateurId operateur() {
    return new OperateurId(operateurId);
  }

  EtatDePresence etat() {
    return etat;
  }
}
