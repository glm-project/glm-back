package com.glm.glmback.feuilledetemps.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Les pointages d'un operateur sur un suivi d'atelier, replies en intervalles d'activite.
 *
 * <p>
 * Seconde ecriture du repli de l'atelier, redeclaree ici : ce contexte lit les memes tables sans importer le paquet
 * voisin, annote {@code BusinessContext}. L'operateur etant fixe, l'automate se joue par poste : le meme operateur
 * sur deux postes mene deux activites independantes. L'ordre des ex aequo vient de la requete, qui trie par date de
 * survenue puis par identifiant ; le tri du domaine etant stable, il le conserve.
 * </p>
 *
 * <p>
 * La lecture ne doit jamais echouer sur le journal : une fin sans activite en cours est ignoree, et l'intervalle
 * precedent reste borne par la fin qui l'a vraiment arrete.
 * </p>
 */
public record JournalDAtelier(List<PointageDAtelier> pointages) {
  private static final Comparator<PointageDAtelier> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(PointageDAtelier::dateDeSurvenue);

  private static final Comparator<IntervalleDActivite> PAR_DEBUT = Comparator.comparing(intervalle -> intervalle.plage().debut());

  public JournalDAtelier {
    Assert.field("pointages", pointages).notNull().noNullElement();
    pointages = pointages.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList();
  }

  /**
   * Les intervalles d'activite sur l'element, la fermeture finale refermant ce que personne n'a arrete : c'est la
   * cloture du suivi, au-dela de laquelle plus rien n'a pu etre fait sur l'element.
   */
  public List<IntervalleDActivite> intervalles(ElementId element, Optional<Instant> fermetureFinale) {
    return pointages
      .stream()
      .collect(Collectors.groupingBy(PointageDAtelier::poste, LinkedHashMap::new, Collectors.toList()))
      .values()
      .stream()
      .flatMap(activite -> intervallesDUneActivite(element, activite, fermetureFinale).stream())
      .sorted(PAR_DEBUT)
      .toList();
  }

  private static List<IntervalleDActivite> intervallesDUneActivite(
    ElementId element,
    List<PointageDAtelier> activite,
    Optional<Instant> fermetureFinale
  ) {
    List<PointageDAtelier> retenus = retenus(activite);
    List<IntervalleDActivite> intervalles = new ArrayList<>();
    EtatDActivite etat = EtatDActivite.ABSENTE;

    for (int rang = 0; rang < retenus.size(); rang++) {
      PointageDAtelier pointage = retenus.get(rang);
      etat = etat.apres(pointage.type()).orElseThrow();

      Optional<Instant> fin = rang + 1 < retenus.size() ? Optional.of(retenus.get(rang + 1).dateDeSurvenue()) : fermetureFinale;
      etat.categorie().ifPresent(categorie -> intervalles.add(intervalle(element, pointage, categorie, fin)));
    }

    return List.copyOf(intervalles);
  }

  /**
   * Les seuls pointages que l'automate admet, dans l'ordre : un geste refuse est saute, et l'etat reste celui d'avant.
   */
  private static List<PointageDAtelier> retenus(List<PointageDAtelier> pointages) {
    List<PointageDAtelier> retenus = new ArrayList<>();
    EtatDActivite etat = EtatDActivite.ABSENTE;

    for (PointageDAtelier pointage : pointages) {
      Optional<EtatDActivite> apres = etat.apres(pointage.type());

      if (apres.isPresent()) {
        retenus.add(pointage);
        etat = apres.orElseThrow();
      }
    }

    return retenus;
  }

  private static IntervalleDActivite intervalle(
    ElementId element,
    PointageDAtelier pointage,
    CategorieDActivite categorie,
    Optional<Instant> fin
  ) {
    Activite activite = Activite.builder().element(element).poste(pointage.poste()).nature(pointage.nature()).categorie(categorie);

    return new IntervalleDActivite(activite, new Plage(pointage.dateDeSurvenue(), fin));
  }
}
