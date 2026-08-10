package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * La suite ordonnee des evenements d'un element engage, et les invariants de sequence qui la gouvernent.
 *
 * <p>
 * Le journal se trie par date de survenue et se valide en entier a chaque construction : une insertion retroactive ou
 * une annulation rejoue tout le chemin, donc une correction incoherente est refusee au lieu de produire un etat
 * absurde. Les evenements annules restent presents, pour la trace, mais sont ecartes du repli.
 * </p>
 */
public record JournalDAtelier(List<EvenementDAtelier> evenements) {
  private static final Comparator<EvenementDAtelier> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(EvenementDAtelier::dateDeSurvenue)
    .thenComparing(EvenementDAtelier::dateDEnregistrement)
    .thenComparing(EvenementDAtelier::id);

  private static final Comparator<IntervalleDActivite> PAR_DEBUT = Comparator.comparing(IntervalleDActivite::debut).thenComparing(
    IntervalleDActivite::evenement
  );

  public JournalDAtelier {
    Assert.field("evenements", evenements).notNull().noNullElement();
    evenements = evenements.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList();
    valide(evenements);
  }

  public static JournalDAtelier vide() {
    return new JournalDAtelier(List.of());
  }

  public JournalDAtelier enregistre(EvenementDAtelier evenement) {
    return new JournalDAtelier(Stream.concat(evenements.stream(), Stream.of(evenement)).toList());
  }

  public JournalDAtelier annule(EvenementDAtelierId id, Annulation annulation) {
    exige(id);

    return new JournalDAtelier(
      evenements
        .stream()
        .map(evenement -> evenement.id().equals(id) ? evenement.annule(annulation) : evenement)
        .toList()
    );
  }

  /**
   * Annule un evenement et lui substitue sa version corrigee, en validant la seule sequence finale.
   *
   * <p>
   * Enchainer une annulation puis une insertion ferait passer le journal par un etat intermediaire que l'automate
   * refuserait a raison : annuler un debut y laisserait une fin orpheline. C'est ce qui justifie l'acte unique.
   * </p>
   */
  public JournalDAtelier corrige(EvenementDAtelierId id, Annulation annulation, EvenementDAtelier remplacant) {
    return new JournalDAtelier(remplace(id, evenement -> Stream.of(evenement.annule(annulation), remplacant)));
  }

  public Optional<EvenementDAtelier> evenement(EvenementDAtelierId id) {
    return evenements
      .stream()
      .filter(evenement -> evenement.id().equals(id))
      .findFirst();
  }

  public List<EvenementDAtelier> actifs() {
    return actifs(evenements);
  }

  public List<IntervalleDActivite> intervalles(Optional<Instant> fermetureFinale) {
    return intervalles(evenements, fermetureFinale);
  }

  private void exige(EvenementDAtelierId id) {
    evenement(id).orElseThrow(() -> new EvenementDAtelierIntrouvableException(id));
  }

  private List<EvenementDAtelier> remplace(EvenementDAtelierId id, Function<EvenementDAtelier, Stream<EvenementDAtelier>> remplacement) {
    EvenementDAtelier cible = evenement(id).orElseThrow(() -> new EvenementDAtelierIntrouvableException(id));

    return evenements
      .stream()
      .flatMap(evenement -> evenement.equals(cible) ? remplacement.apply(evenement) : Stream.of(evenement))
      .toList();
  }

  private static void valide(List<EvenementDAtelier> evenements) {
    intervalles(evenements, Optional.empty());
  }

  private static List<EvenementDAtelier> actifs(List<EvenementDAtelier> evenements) {
    return evenements
      .stream()
      .filter(evenement -> !evenement.estAnnule())
      .toList();
  }

  private static List<IntervalleDActivite> intervalles(List<EvenementDAtelier> evenements, Optional<Instant> fermetureFinale) {
    return actifs(evenements)
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

    return intervalles;
  }

  private static IntervalleDActivite intervalle(EvenementDAtelier evenement, CategorieDActivite categorie, Optional<Instant> fin) {
    return IntervalleDActivite.builder()
      .evenement(evenement.id())
      .operateur(evenement.operateur())
      .poste(evenement.poste())
      .nature(evenement.nature())
      .categorie(categorie)
      .debut(evenement.dateDeSurvenue())
      .fin(fin);
  }
}
