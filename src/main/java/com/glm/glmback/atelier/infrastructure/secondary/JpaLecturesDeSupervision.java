package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.*;
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
import java.util.Optional;
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
    List<Tuple> activites = entities
      .createQuery(
        """
        select a.id as id, a.operateurId as operateurId, a.categorie as categorie, a.debut as debut, a.echeance as echeance,
        s.elementId as elementId, s.elementNom as elementNom, s.elementType as elementType
        from ActiviteDAtelierEntity a join a.suivi s
        where a.aResoudre = false and a.fin is null order by a.debut, a.id
        """,
        Tuple.class
      )
      .getResultList();
    return new LectureDeSupervision(
      evaluation,
      operateurs,
      activites
        .stream()
        .map(this::toDescription)
        .map(description -> ActiviteDeSupervision.a(description, evaluation))
        .toList()
    );
  }

  private DescriptionDActiviteDeSupervision toDescription(Tuple row) {
    ElementEngage element = new ElementEngage(
      new ElementEngageId(row.get("elementId", UUID.class)),
      new NomDElement(row.get("elementNom", String.class)),
      row.get("elementType", TypeDElementEngage.class)
    );
    return DescriptionDActiviteDeSupervision.builder()
      .id(new ActiviteId(row.get("id", UUID.class)))
      .operateur(new OperateurId(row.get("operateurId", UUID.class)))
      .element(new ElementDeSupervision(element, Optional.empty()))
      .poste(Optional.empty())
      .categorie(row.get("categorie", CategorieDActivite.class))
      .debut(row.get("debut", Instant.class))
      .echeance(row.get("echeance", Instant.class));
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
