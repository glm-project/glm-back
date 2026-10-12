package com.glm.glmback.atelier.infrastructure.secondary.correctiondesactivites;

import com.glm.glmback.atelier.domain.CategorieDElement;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.correctiondesactivites.SuiviPresent;
import com.glm.glmback.atelier.domain.correctiondesactivites.SuivisPresents;
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
 * Les suivis presents a l'atelier sur une plage, lus sur la table des suivis : engages avant la fin de la plage, et non
 * clotures avant son debut. Un suivi cloture depuis y figure donc, avec sa cloture.
 */
@Repository
class JpaSuivisPresents implements SuivisPresents {

  private static final String SUIVIS = """
    select suivi.id as id, suivi.element_id as element_id, suivi.element_nom as element_nom,
      suivi.element_categorie as element_categorie, suivi.engagement_date as engage_le,
      suivi.cloture_date_de_survenue as cloture_le
    from suivi_d_atelier suivi
    where suivi.engagement_date < :finExclue
      and (suivi.cloture_date_de_survenue is null or suivi.cloture_date_de_survenue >= :debut)
    order by suivi.engagement_date, suivi.id
    """;

  private final EntityManager entities;

  JpaSuivisPresents(EntityManager entities) {
    this.entities = entities;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<SuiviPresent> entre(Instant debut, Instant finExclue) {
    ExactInstantConverter instants = new ExactInstantConverter();

    return (
      (List<Tuple>) entities
        .createNativeQuery(SUIVIS, Tuple.class)
        .setParameter("debut", instants.convertToDatabaseColumn(debut))
        .setParameter("finExclue", instants.convertToDatabaseColumn(finExclue))
        .getResultList()
    ).stream()
      .map(ligne -> from(ligne, instants))
      .toList();
  }

  private static SuiviPresent from(Tuple ligne, ExactInstantConverter instants) {
    return SuiviPresent.builder()
      .id(new SuiviDAtelierId(ligne.get("id", UUID.class)))
      .element(
        new ElementEngage(
          new ElementEngageId(ligne.get("element_id", UUID.class)),
          new NomDElement(ligne.get("element_nom", String.class)),
          new CategorieDElement(ligne.get("element_categorie", String.class))
        )
      )
      .engageLe(instants.convertToEntityAttribute(ligne.get("engage_le", BigDecimal.class)))
      .clotureLe(Optional.ofNullable(instants.convertToEntityAttribute(ligne.get("cloture_le", BigDecimal.class))));
  }
}
