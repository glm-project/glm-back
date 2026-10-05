package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDElementEngage;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import com.glm.glmback.atelier.domain.gestionanomalies.AnomaliesDAtelierCriteria;
import com.glm.glmback.atelier.domain.gestionanomalies.FinAutomatiqueEnListe;
import com.glm.glmback.atelier.domain.gestionanomalies.FinsAutomatiquesDAtelier;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
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
 * Les fins automatiques se jugent entierement en SQL sur la projection des activites : sans fin reelle, hors des
 * activites a resoudre, echeance atteinte a l'instant d'evaluation, borne comprise. Aucun journal n'est rejoue.
 */
@Repository
class JpaFinsAutomatiquesDAtelier implements FinsAutomatiquesDAtelier {

  private static final String LIGNES = """
    with filtre as (
      select activite.ouverture_id as ouvrant, activite.id as activite, suivi.id as suivi, suivi.revision as revision,
        suivi.element_id as element_id, suivi.element_nom as element_nom, suivi.element_type as element_type,
        activite.operateur_id as operateur_id, activite.poste_id as poste_id,
        activite.debut as debut, activite.echeance as echeance
      from activite_d_atelier activite
      join suivi_d_atelier suivi on suivi.id = activite.suivi_id
      left join operateur on operateur.id = activite.operateur_id
      where activite.fin is null and not activite.a_resoudre and activite.echeance <= :evaluation
        and (lower(coalesce(operateur.prenom || ' ' || operateur.nom, '')) like :operateur escape '\\'
        or cast(activite.operateur_id as varchar) like :operateur escape '\\')
        and (lower(suivi.element_nom) like :element escape '\\' or cast(suivi.element_id as varchar) like :element escape '\\')
    ), page as (
      select * from filtre order by debut, suivi, ouvrant limit :taille offset :position
    )
    select compte.total, page.* from (select count(*) as total from filtre) compte
    left join page on true order by page.debut, page.suivi, page.ouvrant
    """;

  private final EntityManager entities;

  JpaFinsAutomatiquesDAtelier(EntityManager entities) {
    this.entities = entities;
  }

  @Override
  public Page<FinAutomatiqueEnListe> list(AnomaliesDAtelierCriteria criteria, Instant evaluation, Pageable pageable) {
    List<Tuple> lignes = lignes(criteria, evaluation, pageable);
    return Page.<FinAutomatiqueEnListe>builder()
      .content(
        lignes
          .stream()
          .filter(ligne -> ligne.get("ouvrant") != null)
          .map(JpaFinsAutomatiquesDAtelier::from)
          .toList()
      )
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(((Number) lignes.getFirst().get("total")).longValue());
  }

  @SuppressWarnings("unchecked")
  private List<Tuple> lignes(AnomaliesDAtelierCriteria criteria, Instant evaluation, Pageable pageable) {
    return entities
      .createNativeQuery(LIGNES, Tuple.class)
      .setParameter("evaluation", new ExactInstantConverter().convertToDatabaseColumn(evaluation))
      .setParameter("element", RechercheLitterale.motif(criteria.element()))
      .setParameter("operateur", RechercheLitterale.motif(criteria.operateur()))
      .setParameter("taille", pageable.size())
      .setParameter("position", pageable.offset())
      .getResultList();
  }

  private static FinAutomatiqueEnListe from(Tuple ligne) {
    var instants = new ExactInstantConverter();
    return FinAutomatiqueEnListe.builder()
      .adresse(
        new AdresseDossierAnomalie(
          new SuiviDAtelierId(ligne.get("suivi", UUID.class)),
          new EvenementDAtelierId(ligne.get("ouvrant", UUID.class))
        )
      )
      .revision(new RevisionDuSuivi(((Number) ligne.get("revision")).longValue()))
      .element(
        new ElementEngage(
          new ElementEngageId(ligne.get("element_id", UUID.class)),
          new NomDElement(ligne.get("element_nom", String.class)),
          TypeDElementEngage.valueOf(ligne.get("element_type", String.class))
        )
      )
      .cle(
        new CleDActivite(
          new OperateurId(ligne.get("operateur_id", UUID.class)),
          Optional.ofNullable(ligne.get("poste_id", UUID.class)).map(PosteDeTravailId::new)
        )
      )
      .activite(new ActiviteId(ligne.get("activite", UUID.class)))
      .debut(instants.convertToEntityAttribute(ligne.get("debut", BigDecimal.class)))
      .echeance(instants.convertToEntityAttribute(ligne.get("echeance", BigDecimal.class)));
  }
}
