package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.LectureDeSupervision;
import com.glm.glmback.atelier.domain.LecturesDeSupervision;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.Nom;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurDeSupervision;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.Prenom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    List<?> habilitations = entities
      .createNativeQuery(
        "select distinct op.operateur_id, p.nature from operateur_poste op join poste_de_travail p on p.id = op.poste_id order by op.operateur_id, p.nature",
        Tuple.class
      )
      .getResultList();
    Map<UUID, List<NatureDOperation>> metiers = habilitations
      .stream()
      .map(Tuple.class::cast)
      .collect(
        Collectors.groupingBy(
          row -> row.get("operateur_id", UUID.class),
          Collectors.mapping(row -> new NatureDOperation(row.get("nature", String.class)), Collectors.toList())
        )
      );
    List<OperateurDeSupervision> operateurs = rows
      .stream()
      .map(Tuple.class::cast)
      .map(row -> toOperateur(row, metiers))
      .toList();
    return new LectureDeSupervision(evaluation, operateurs);
  }

  private OperateurDeSupervision toOperateur(Tuple row, Map<UUID, List<NatureDOperation>> metiers) {
    OperateurConnu operateur = OperateurConnu.builder()
      .id(new OperateurId(row.get("id", UUID.class)))
      .nom(new Nom(row.get("nom", String.class)))
      .prenom(new Prenom(row.get("prenom", String.class)))
      .tauxHoraire(null);
    return new OperateurDeSupervision(operateur, metiers.getOrDefault(operateur.id().uuid(), List.of()));
  }
}
