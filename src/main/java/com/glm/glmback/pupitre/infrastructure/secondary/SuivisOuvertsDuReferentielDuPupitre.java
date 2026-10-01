package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.ActivitePointable;
import com.glm.glmback.pupitre.domain.SituationDuSuivi;
import com.glm.glmback.pupitre.domain.SuiviDuPupitre;
import com.glm.glmback.pupitre.domain.SuivisOuvertsDuPupitre;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Les elements encore pointables, leurs activites interpretees et leurs references, lus dans les tables des
 * contextes voisins sans jamais importer leur code.
 *
 * <p>
 * Quatre requetes groupees : les suivis non clotures, l'existence de pointages actifs, les activites pointables
 * a l'instant du referentiel et les references. Aucun journal complet ni appel par element.
 * </p>
 */
@Repository
class SuivisOuvertsDuReferentielDuPupitre implements SuivisOuvertsDuPupitre {

  private final SpringDataSuivisDuPupitreRepository suivis;
  private final SpringDataActivitesDuPupitreRepository activites;
  private final SpringDataElementsDuPupitreRepository elements;

  SuivisOuvertsDuReferentielDuPupitre(
    SpringDataSuivisDuPupitreRepository suivis,
    SpringDataActivitesDuPupitreRepository activites,
    SpringDataElementsDuPupitreRepository elements
  ) {
    this.suivis = suivis;
    this.activites = activites;
    this.elements = elements;
  }

  @Override
  public List<SuiviDuPupitre> tous(Instant evaluation) {
    List<SuiviDuPupitreEntity> ouverts = suivis.ouverts();
    Set<UUID> identites = ouverts.stream().map(SuiviDuPupitreEntity::id).collect(Collectors.toSet());
    Set<UUID> pointes = suivis.avecPointages(identites);
    Map<UUID, List<ActivitePointable>> courantes = activites
      .desSuivis(identites, evaluation)
      .stream()
      .collect(
        Collectors.groupingBy(
          ActiviteDuPupitreEntity::suiviId,
          LinkedHashMap::new,
          Collectors.mapping(ActiviteDuPupitreEntity::toDomain, Collectors.toList())
        )
      );
    Map<UUID, String> references = references(ouverts);

    return ouverts
      .stream()
      .map(suivi ->
        suivi.toDomain(
          new SituationDuSuivi(courantes.getOrDefault(suivi.id(), List.of()), pointes.contains(suivi.id())),
          references.get(suivi.elementId())
        )
      )
      .toList();
  }

  /**
   * Un element supprime du referentiel n'a plus de ligne : son suivi garde son nom, copie a l'engagement, et perd sa
   * reference. C'est aussi pour cela que la reference ne peut pas etre jointe une fois pour toutes.
   */
  private Map<UUID, String> references(List<SuiviDuPupitreEntity> ouverts) {
    Set<UUID> identites = ouverts.stream().map(SuiviDuPupitreEntity::elementId).collect(Collectors.toSet());

    return elements
      .findAllById(identites)
      .stream()
      .filter(element -> element.reference() != null)
      .collect(Collectors.toMap(ElementDuPupitreEntity::id, ElementDuPupitreEntity::reference));
  }
}
