package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

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
import com.glm.glmback.atelier.domain.gestionanomalies.ConflitEnListe;
import com.glm.glmback.atelier.domain.gestionanomalies.ConflitsDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.RepereDeSequence;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.time.infrastructure.secondary.ExactInstantConverter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaConflitsDAtelier implements ConflitsDAtelier {

  private static final String LIGNES = """
    with filtre as (
      select sequence.id as ancre, suivi.id as suivi, suivi.revision as revision,
        suivi.element_id as element_id, suivi.element_nom as element_nom, suivi.element_type as element_type,
        sequence.operateur_id as operateur_id, sequence.poste_id as poste_id,
        premier.date_de_survenue as premier_pointage,
        (select count(*) from pointage_en_conflit where sequence_id = sequence.id) as nombre_pointages
      from sequence_en_conflit sequence
      join suivi_d_atelier suivi on suivi.id = sequence.suivi_id
      join evenement_d_atelier premier on premier.id = sequence.id
      left join operateur on operateur.id = sequence.operateur_id
      where (lower(coalesce(operateur.prenom || ' ' || operateur.nom, '')) like :operateur escape '\\'
        or cast(sequence.operateur_id as varchar) like :operateur escape '\\')
        and (lower(suivi.element_nom) like :element escape '\\' or cast(suivi.element_id as varchar) like :element escape '\\')
    ), page as (
      select * from filtre order by premier_pointage, suivi, ancre limit :taille offset :position
    )
    select compte.total, page.* from (select count(*) as total from filtre) compte
    left join page on true order by page.premier_pointage, page.suivi, page.ancre
    """;

  private final EntityManager entities;

  JpaConflitsDAtelier(EntityManager entities) {
    this.entities = entities;
  }

  @Override
  public Page<ConflitEnListe> list(AnomaliesDAtelierCriteria criteria, Pageable pageable) {
    List<Tuple> lignes = lignes(criteria, pageable);
    return Page.<ConflitEnListe>builder()
      .content(
        lignes
          .stream()
          .filter(ligne -> ligne.get("ancre") != null)
          .map(JpaConflitsDAtelier::from)
          .toList()
      )
      .currentPage(pageable.page())
      .pageSize(pageable.size())
      .totalElementsCount(((Number) lignes.getFirst().get("total")).longValue());
  }

  @SuppressWarnings("unchecked")
  private List<Tuple> lignes(AnomaliesDAtelierCriteria criteria, Pageable pageable) {
    return entities
      .createNativeQuery(LIGNES, Tuple.class)
      .setParameter("element", RechercheLitterale.motif(criteria.element()))
      .setParameter("operateur", RechercheLitterale.motif(criteria.operateur()))
      .setParameter("taille", pageable.size())
      .setParameter("position", pageable.offset())
      .getResultList();
  }

  private static ConflitEnListe from(Tuple ligne) {
    return ConflitEnListe.builder()
      .adresse(
        new AdresseDossierAnomalie(
          new SuiviDAtelierId(ligne.get("suivi", UUID.class)),
          new EvenementDAtelierId(ligne.get("ancre", UUID.class))
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
      .repere(
        new RepereDeSequence(
          new ExactInstantConverter().convertToEntityAttribute(ligne.get("premier_pointage", BigDecimal.class)),
          ((Number) ligne.get("nombre_pointages")).intValue()
        )
      );
  }
}
