package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.ActivitePointable;
import com.glm.glmback.pupitre.domain.CategorieDActivite;
import com.glm.glmback.pupitre.domain.CleDActivite;
import com.glm.glmback.pupitre.domain.OperateurId;
import com.glm.glmback.pupitre.domain.OuvertureDActivite;
import com.glm.glmback.pupitre.domain.PosteDeTravailId;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Lecture de la projection maintenue par l'atelier, sans reinterpreter les pointages. */
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
  private CategorieDActivite categorie;

  private Instant debut;
  private Instant echeance;
  private Instant fin;

  @Column(name = "a_resoudre")
  private boolean aResoudre;

  protected ActiviteDuPupitreEntity() {}

  UUID suiviId() {
    return suiviId;
  }

  ActivitePointable toDomain() {
    return new ActivitePointable(
      new CleDActivite(new OperateurId(operateurId), Optional.ofNullable(posteId).map(PosteDeTravailId::new)),
      categorie,
      new OuvertureDActivite(id, debut, echeance)
    );
  }
}
