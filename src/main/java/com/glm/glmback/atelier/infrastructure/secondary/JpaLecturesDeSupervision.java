package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.LectureDeSupervision;
import com.glm.glmback.atelier.domain.LecturesDeSupervision;
import com.glm.glmback.atelier.domain.Nom;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurDeSupervision;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.Prenom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaLecturesDeSupervision implements LecturesDeSupervision {

  private final EntityManager entities;

  JpaLecturesDeSupervision(EntityManager entities) {
    this.entities = entities;
  }

  @Override
  public LectureDeSupervision read(Instant evaluation) {
    List<?> rows = entities.createNativeQuery("select id, nom, prenom from operateur order by id", Tuple.class).getResultList();
    List<OperateurDeSupervision> operateurs = rows.stream().map(Tuple.class::cast).map(this::toOperateur).toList();
    return new LectureDeSupervision(evaluation, operateurs);
  }

  private OperateurDeSupervision toOperateur(Tuple row) {
    OperateurConnu operateur = OperateurConnu.builder()
      .id(new OperateurId(row.get("id", UUID.class)))
      .nom(new Nom(row.get("nom", String.class)))
      .prenom(new Prenom(row.get("prenom", String.class)))
      .tauxHoraire(null);
    return new OperateurDeSupervision(operateur, List.of());
  }
}
