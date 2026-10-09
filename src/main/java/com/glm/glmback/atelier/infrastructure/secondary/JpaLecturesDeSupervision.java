package com.glm.glmback.atelier.infrastructure.secondary;

import com.glm.glmback.atelier.domain.ActiviteDeSupervision;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.CategorieDElement;
import com.glm.glmback.atelier.domain.DescriptionDActiviteDeSupervision;
import com.glm.glmback.atelier.domain.Echeance;
import com.glm.glmback.atelier.domain.ElementDeSupervision;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.LectureDeSupervision;
import com.glm.glmback.atelier.domain.LecturesDeSupervision;
import com.glm.glmback.atelier.domain.LibelleDePoste;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.Nom;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurDeSupervision;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeSupervision;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
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
    List<OperateurDeSupervision> operateurs = readOperateurs();
    List<Tuple> activites = readActivites();
    return LectureDeSupervision.builder()
      .evaluation(evaluation)
      .operateurs(operateurs)
      .activites(
        activites
          .stream()
          .map(this::toDescription)
          .map(description -> ActiviteDeSupervision.a(description, evaluation))
          .toList()
      );
  }

  private List<OperateurDeSupervision> readOperateurs() {
    List<?> rows = entities.createNativeQuery("select id, nom, prenom from operateur order by id", Tuple.class).getResultList();
    List<?> habilitations = entities
      .createNativeQuery(
        "select distinct op.operateur_id, n.libelle as nature from operateur_poste op join poste_de_travail p on p.id = op.poste_id join nature_de_travail n on n.id = p.nature_id order by op.operateur_id, n.libelle",
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
    return rows
      .stream()
      .map(Tuple.class::cast)
      .map(row -> toOperateur(row, metiers))
      .toList();
  }

  private List<Tuple> readActivites() {
    return entities
      .createQuery(
        """
        select a.id as id, a.operateurId as operateurId, a.categorie as categorie, a.debut as debut, a.echeance as echeance,
        s.elementId as elementId, s.elementNom as elementNom, s.elementCategorie as elementCategorie, e.reference as reference,
          a.posteId as posteId, p.libelle as posteLibelle, a.nature as nature
        from ActiviteDAtelierEntity a join a.suivi s
          left join ElementEngageableEntity e on e.id = s.elementId
          left join PosteConnuEntity p on p.id = a.posteId
        where a.fin is null order by a.debut, a.id
        """,
        Tuple.class
      )
      .getResultList();
  }

  private DescriptionDActiviteDeSupervision toDescription(Tuple row) {
    ElementEngage element = new ElementEngage(
      new ElementEngageId(row.get("elementId", UUID.class)),
      new NomDElement(row.get("elementNom", String.class)),
      new CategorieDElement(row.get("elementCategorie", String.class))
    );
    return DescriptionDActiviteDeSupervision.builder()
      .id(new ActiviteId(row.get("id", UUID.class)))
      .operateur(new OperateurId(row.get("operateurId", UUID.class)))
      .element(new ElementDeSupervision(element, Optional.ofNullable(row.get("reference", String.class))))
      .poste(toPoste(row))
      .categorie(row.get("categorie", CategorieDActivite.class))
      .debut(row.get("debut", Instant.class))
      .echeance(new Echeance(row.get("echeance", Instant.class)));
  }

  private Optional<PosteDeSupervision> toPoste(Tuple row) {
    return Optional.ofNullable(row.get("posteId", UUID.class)).map(id ->
      new PosteDeSupervision(
        new PosteDeTravailId(id),
        new LibelleDePoste(row.get("posteLibelle", String.class)),
        Optional.ofNullable(row.get("nature", String.class)).map(NatureDOperation::new)
      )
    );
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
