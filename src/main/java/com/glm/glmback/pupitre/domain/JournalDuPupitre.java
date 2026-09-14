package com.glm.glmback.pupitre.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Le journal d'un element engage, replie pour ne rendre que ses activites ouvertes.
 *
 * <p>
 * C'est le quatrieme rejeu du repli du journal d'atelier dans ce projet, apres {@code feuilledetemps} et
 * {@code coutderevient} : {@code atelier} est annote {@code BusinessContext} et ne s'importe pas. Le filet qui tient
 * les implementations alignees est le scenario Cucumber, qui pointe par l'API de l'atelier et relit par celle-ci.
 * </p>
 *
 * <p>
 * Une difference assumee avec l'atelier : un pointage qui casse l'automate n'est pas refuse, il est <b>ignore</b>,
 * comme {@code syntheseheures} le fait de la presence. Le cas n'est pas atteignable par l'API — l'atelier valide tout
 * le journal a chaque ecriture — mais un ecran d'atelier ne doit jamais s'eteindre parce qu'un journal est bizarre.
 * </p>
 */
public record JournalDuPupitre(List<EvenementDuPupitre> evenements) {
  public JournalDuPupitre {
    Assert.field("evenements", evenements).notNull().noNullElement();
    evenements = List.copyOf(evenements);
  }

  public static JournalDuPupitre vide() {
    return new JournalDuPupitre(List.of());
  }

  /**
   * Les activites encore ouvertes, une au plus par couple (operateur, poste).
   *
   * <p>
   * L'ordre chronologique des evenements est celui dans lequel l'adapter les a rendus : c'est une regle de lecture du
   * journal, exprimee en SQL une fois pour toutes plutot que retriee a chaque appel.
   * </p>
   */
  public List<ActiviteEnCours> activitesEnCours() {
    return parActivite()
      .entrySet()
      .stream()
      .flatMap(activite -> ouverture(activite.getValue()).activite(activite.getKey()).stream())
      .toList();
  }

  public boolean estVierge() {
    return evenements.isEmpty();
  }

  private Map<CleDActivite, List<EvenementDuPupitre>> parActivite() {
    return evenements.stream().collect(Collectors.groupingBy(EvenementDuPupitre::activite, LinkedHashMap::new, Collectors.toList()));
  }

  private static Ouverture ouverture(List<EvenementDuPupitre> evenements) {
    Ouverture ouverture = Ouverture.absente();

    for (EvenementDuPupitre evenement : evenements) {
      ouverture = ouverture.apres(evenement);
    }

    return ouverture;
  }

  /**
   * L'etat atteint par une activite et l'instant ou elle y est entree.
   *
   * <p>
   * Un evenement que l'automate refuse laisse les deux inchanges : c'est la tolerance de ce contexte, et c'est aussi
   * ce qui garantit que {@code depuis} est renseigne des que l'activite est ouverte.
   * </p>
   */
  private record Ouverture(EtatDActivite etat, Optional<Instant> depuis) {
    static Ouverture absente() {
      return new Ouverture(EtatDActivite.ABSENTE, Optional.empty());
    }

    Ouverture apres(EvenementDuPupitre evenement) {
      return etat
        .apres(evenement.type())
        .map(atteint -> new Ouverture(atteint, Optional.of(evenement.dateDeSurvenue())))
        .orElse(this);
    }

    List<ActiviteEnCours> activite(CleDActivite cle) {
      return etat
        .categorie()
        .map(categorie -> List.of(new ActiviteEnCours(cle, categorie, depuis.orElseThrow())))
        .orElseGet(List::of);
    }
  }
}
