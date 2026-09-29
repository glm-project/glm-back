package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.ActiviteId;
import com.glm.glmback.pupitre.domain.CleDActivite;
import com.glm.glmback.pupitre.domain.OperateurId;
import com.glm.glmback.pupitre.domain.PointageId;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import com.glm.glmback.pupitre.domain.SequenceEnConflitDuPupitre;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Lecture privee des sequences derivees et ecrites exclusivement par atelier. */
@Entity
@Immutable
@Table(name = "sequence_en_conflit")
class SequenceEnConflitDuPupitreEntity {

  @Id
  private UUID id;

  @Column(name = "suivi_id")
  private UUID suiviId;

  @Column(name = "operateur_id")
  private UUID operateurId;

  @Column(name = "poste_id")
  private UUID posteId;

  @ElementCollection
  @CollectionTable(name = "pointage_en_conflit", joinColumns = @JoinColumn(name = "sequence_id"))
  @Column(name = "evenement_id")
  @OrderColumn(name = "ordre")
  private List<UUID> pointages;

  protected SequenceEnConflitDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  UUID id() {
    return id;
  }

  UUID suiviId() {
    return suiviId;
  }

  SequenceEnConflitDuPupitre toDomain(List<ActiviteId> activites) {
    return new SequenceEnConflitDuPupitre(
      new CleDActivite(new OperateurId(operateurId), Optional.ofNullable(posteId).map(PosteDeTravailId::new)),
      activites,
      pointages.stream().map(PointageId::new).toList()
    );
  }
}
