package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * La suite ordonnee des evenements d'un suivi d'atelier, repliee en intervalles d'activite.
 *
 * <p>
 * Seconde ecriture du repli de l'atelier, redeclaree ici : ce contexte lit les memes tables sans importer le paquet
 * voisin, annote {@code BusinessContext}. Elle en est aussi une version allegee — la correction est l'affaire de
 * l'atelier, les evenements annules sont ecartes des la requete, et rien ne s'ecrit jamais ici.
 * </p>
 *
 * <p>
 * L'automate se joue par cle d'activite : le meme operateur sur deux postes mene deux activites independantes, ce
 * qu'un repli en bloc refuserait. L'ordre des ex aequo vient de la requete, qui trie par date de survenue puis par
 * identifiant ; le tri du domaine etant stable, il le conserve.
 * </p>
 */
public record JournalDAtelier(List<EvenementDAtelier> evenements) {
  private static final Comparator<EvenementDAtelier> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(EvenementDAtelier::dateDeSurvenue);

  private static final Comparator<IntervalleDActivite> PAR_DEBUT = Comparator.comparing(intervalle -> intervalle.plage().debut());

  public JournalDAtelier {
    Assert.field("evenements", evenements).notNull().noNullElement();
    evenements = evenements.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList();
    intervalles(evenements, Optional.empty());
  }

  /**
   * Les intervalles d'activite du journal, la fermeture finale refermant ce que personne n'a arrete : c'est la
   * cloture du suivi, au-dela de laquelle plus rien n'a pu etre fait sur l'element.
   */
  public List<IntervalleDActivite> intervalles(Optional<Instant> fermetureFinale) {
    return intervalles(evenements, fermetureFinale);
  }

  private static List<IntervalleDActivite> intervalles(List<EvenementDAtelier> evenements, Optional<Instant> fermetureFinale) {
    return evenements
      .stream()
      .collect(Collectors.groupingBy(EvenementDAtelier::cle, LinkedHashMap::new, Collectors.toList()))
      .values()
      .stream()
      .flatMap(activite -> intervallesDUneActivite(activite, fermetureFinale).stream())
      .sorted(PAR_DEBUT)
      .toList();
  }

  private static List<IntervalleDActivite> intervallesDUneActivite(List<EvenementDAtelier> evenements, Optional<Instant> fermetureFinale) {
    List<IntervalleDActivite> intervalles = new ArrayList<>();
    EtatDActivite etat = EtatDActivite.ABSENTE;

    for (int rang = 0; rang < evenements.size(); rang++) {
      EvenementDAtelier evenement = evenements.get(rang);
      EtatDActivite avant = etat;
      etat = avant.apres(evenement.type()).orElseThrow(() -> new TransitionDAtelierInterditeException(evenement, avant));

      Optional<Instant> fin = rang + 1 < evenements.size() ? Optional.of(evenements.get(rang + 1).dateDeSurvenue()) : fermetureFinale;
      etat.categorie().ifPresent(categorie -> intervalles.add(intervalle(evenement, categorie, fin)));
    }

    return List.copyOf(intervalles);
  }

  private static IntervalleDActivite intervalle(EvenementDAtelier evenement, CategorieDActivite categorie, Optional<Instant> fin) {
    Activite activite = Activite.builder()
      .operateur(evenement.operateur())
      .poste(evenement.poste())
      .nature(evenement.nature())
      .coutHoraire(evenement.coutHoraire())
      .tauxHoraire(evenement.tauxHoraire())
      .categorie(categorie);

    return new IntervalleDActivite(activite, new Plage(evenement.dateDeSurvenue(), fin));
  }
}
