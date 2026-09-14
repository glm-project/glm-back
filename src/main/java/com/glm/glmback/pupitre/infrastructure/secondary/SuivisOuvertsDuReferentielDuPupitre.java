package com.glm.glmback.pupitre.infrastructure.secondary;

import com.glm.glmback.pupitre.domain.EvenementDuPupitre;
import com.glm.glmback.pupitre.domain.JournalDuPupitre;
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
 * Les elements encore pointables, leurs journaux et leurs references, lus dans les tables des contextes voisins sans
 * jamais importer leur code.
 *
 * <p>
 * Trois requetes, jamais une par element : les suivis non clotures, puis leurs journaux d'un seul coup, puis les
 * references de leurs elements. Les evenements annules sont ecartes des le SQL — les rapporter pour les filtrer
 * ensuite ferait porter au domaine une correction qui ne le regarde pas.
 * </p>
 */
@Repository
class SuivisOuvertsDuReferentielDuPupitre implements SuivisOuvertsDuPupitre {

  private final SpringDataSuivisDuPupitreRepository suivis;
  private final SpringDataEvenementsDuPupitreRepository evenements;
  private final SpringDataElementsDuPupitreRepository elements;

  SuivisOuvertsDuReferentielDuPupitre(
    SpringDataSuivisDuPupitreRepository suivis,
    SpringDataEvenementsDuPupitreRepository evenements,
    SpringDataElementsDuPupitreRepository elements
  ) {
    this.suivis = suivis;
    this.evenements = evenements;
    this.elements = elements;
  }

  @Override
  public List<SuiviDuPupitre> tous() {
    List<SuiviDuPupitreEntity> ouverts = suivis.ouverts();
    Map<UUID, JournalDuPupitre> journaux = journaux(ouverts);
    Map<UUID, String> references = references(ouverts);

    return ouverts
      .stream()
      .map(suivi -> suivi.toDomain(journaux.getOrDefault(suivi.id(), JournalDuPupitre.vide()), references.get(suivi.elementId())))
      .toList();
  }

  private Map<UUID, JournalDuPupitre> journaux(List<SuiviDuPupitreEntity> ouverts) {
    Set<UUID> identites = ouverts.stream().map(SuiviDuPupitreEntity::id).collect(Collectors.toSet());

    return evenements
      .desSuivis(identites)
      .stream()
      .collect(
        Collectors.groupingBy(
          EvenementDuPupitreEntity::suiviId,
          LinkedHashMap::new,
          Collectors.mapping(EvenementDuPupitreEntity::toDomain, Collectors.<EvenementDuPupitre>toList())
        )
      )
      .entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, journal -> new JournalDuPupitre(journal.getValue())));
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
