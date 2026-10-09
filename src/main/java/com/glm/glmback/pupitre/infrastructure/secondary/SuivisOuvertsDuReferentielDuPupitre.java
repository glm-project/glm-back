package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.ActiviteSansFin;
import com.glm.glmback.pupitre.domain.SuiviDuPupitre;
import com.glm.glmback.pupitre.domain.SuivisOuvertsDuPupitre;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Les elements encore pointables, leurs activites interpretees et leurs references, lus dans les tables des contextes voisins sans
 * jamais importer leur code.
 *
 * <p>
 * Les suivis non clotures, leurs activites sans fin, l existence de pointages et les
 * references de leurs elements se lisent par ensembles, jamais une requete par element.
 * </p>
 */
@Repository
class SuivisOuvertsDuReferentielDuPupitre implements SuivisOuvertsDuPupitre {

  private final SpringDataSuivisDuPupitreRepository suivis;
  private final SpringDataActivitesDuPupitreRepository activites;
  private final SpringDataElementsDuPupitreRepository elements;

  SuivisOuvertsDuReferentielDuPupitre(
    SpringDataSuivisDuPupitreRepository suivis,
    SpringDataElementsDuPupitreRepository elements,
    SpringDataActivitesDuPupitreRepository activites
  ) {
    this.suivis = suivis;
    this.elements = elements;
    this.activites = activites;
  }

  @Override
  public List<SuiviDuPupitre> tous() {
    List<SuiviDuPupitreEntity> ouverts = suivis.ouverts();
    Set<UUID> identites = ouverts.stream().map(SuiviDuPupitreEntity::id).collect(Collectors.toSet());
    Map<UUID, List<ActiviteSansFin>> sansFin = activites
      .sansFinDesSuivis(identites)
      .stream()
      .collect(
        Collectors.groupingBy(
          ActiviteDuPupitreEntity::suiviId,
          LinkedHashMap::new,
          Collectors.mapping(ActiviteDuPupitreEntity::toDomain, Collectors.toList())
        )
      );
    Set<UUID> pointes = suivis.suivisPointes(identites);
    Map<UUID, String> references = references(ouverts);

    return ouverts
      .stream()
      .map(suivi ->
        suivi
          .toDomain(references.get(suivi.elementId()))
          .activites(sansFin.getOrDefault(suivi.id(), List.of()))
          .dejaPointe(pointes.contains(suivi.id()))
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
