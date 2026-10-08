package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.ActiviteId;
import com.glm.glmback.pupitre.domain.ActiviteSansFin;
import com.glm.glmback.pupitre.domain.CategorieDActivite;
import com.glm.glmback.pupitre.domain.CleDActivite;
import com.glm.glmback.pupitre.domain.OperateurId;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Lecture privee de l'interpretation d'atelier, sans tarif ni repli du journal. */
@Entity
@Immutable
@Table(name = "activite_d_atelier")
class ActiviteDuPupitreEntity {

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
  private CategorieDActivite categorie;

  @Convert(converter = ExactInstantConverter.class)
  private Instant debut;

  @Convert(converter = ExactInstantConverter.class)
  private Instant echeance;

  @Convert(converter = ExactInstantConverter.class)
  private Instant fin;

  @Column(name = "a_resoudre")
  private boolean aResoudre;

  protected ActiviteDuPupitreEntity() {
    // Constructeur requis par JPA.
  }

  UUID suiviId() {
    return suiviId;
  }

  ActiviteSansFin toDomain() {
    return ActiviteSansFin.builder()
      .ouverture(new ActiviteId(id))
      .activite(new CleDActivite(new OperateurId(operateurId), Optional.ofNullable(posteId).map(PosteDeTravailId::new)))
      .categorie(categorie)
      .depuis(debut)
      .echeance(echeance);
  }
}
