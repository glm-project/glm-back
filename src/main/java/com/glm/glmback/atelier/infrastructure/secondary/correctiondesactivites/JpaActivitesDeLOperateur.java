package com.glm.glmback.atelier.infrastructure.secondary.correctiondesactivites;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.CategorieDElement;
import com.glm.glmback.atelier.domain.Echeance;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.NatureDOperationId;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.correctiondesactivites.ActiviteProjetee;
import com.glm.glmback.atelier.domain.correctiondesactivites.ActivitesDeLOperateur;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Les activites d'un operateur qui touchent une plage, lues sur la projection de l'atelier : celles dont le debut
 * precede la fin de la plage et dont la fin, reelle ou a defaut l'echeance, suit son debut. Une activite en cours est
 * donc lue avec son echeance comme borne, puisque le pupitre la tient ouverte au plus jusque-la. Aucun journal n'est
 * rejoue : la projection est ecrite depuis le journal apres les retraits et les changements de poste.
 */
@Repository
class JpaActivitesDeLOperateur implements ActivitesDeLOperateur {

  private static final String ACTIVITES = """
    select activite.id as id, activite.suivi_id as suivi, suivi.element_id as element_id, suivi.element_nom as element_nom,
      suivi.element_categorie as element_categorie, activite.poste_id as poste_id, activite.nature_id as nature_id,
      (select referentiel.libelle from nature_de_travail referentiel where referentiel.id = activite.nature_id) as nature,
      activite.categorie as categorie,
      activite.debut as debut, activite.echeance as echeance, activite.fin as fin
    from activite_d_atelier activite
    join suivi_d_atelier suivi on suivi.id = activite.suivi_id
    where activite.operateur_id = :operateur and activite.debut < :finExclue and coalesce(activite.fin, activite.echeance) > :debut
    order by activite.debut, activite.id
    """;

  private final EntityManager entities;

  JpaActivitesDeLOperateur(EntityManager entities) {
    this.entities = entities;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<ActiviteProjetee> touchant(OperateurId operateur, Instant debut, Instant finExclue) {
    ExactInstantConverter instants = new ExactInstantConverter();

    return (
      (List<Tuple>) entities
        .createNativeQuery(ACTIVITES, Tuple.class)
        .setParameter("operateur", operateur.uuid())
        .setParameter("debut", instants.convertToDatabaseColumn(debut))
        .setParameter("finExclue", instants.convertToDatabaseColumn(finExclue))
        .getResultList()
    ).stream()
      .map(ligne -> from(ligne, instants))
      .toList();
  }

  private static ActiviteProjetee from(Tuple ligne, ExactInstantConverter instants) {
    return ActiviteProjetee.builder()
      .id(new ActiviteId(ligne.get("id", UUID.class)))
      .suivi(new SuiviDAtelierId(ligne.get("suivi", UUID.class)))
      .element(
        new ElementEngage(
          new ElementEngageId(ligne.get("element_id", UUID.class)),
          new NomDElement(ligne.get("element_nom", String.class)),
          new CategorieDElement(ligne.get("element_categorie", String.class))
        )
      )
      .poste(Optional.ofNullable(ligne.get("poste_id", UUID.class)).map(PosteDeTravailId::new))
      .nature(
        Optional.ofNullable(ligne.get("nature_id", UUID.class)).map(id ->
          new NatureDOperation(new NatureDOperationId(id), ligne.get("nature", String.class))
        )
      )
      .categorie(CategorieDActivite.valueOf(ligne.get("categorie", String.class)))
      .debut(instants.convertToEntityAttribute(ligne.get("debut", BigDecimal.class)))
      .echeance(new Echeance(instants.convertToEntityAttribute(ligne.get("echeance", BigDecimal.class))))
      .fin(Optional.ofNullable(instants.convertToEntityAttribute(ligne.get("fin", BigDecimal.class))));
  }
}
