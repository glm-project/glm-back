package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
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
 * absurde. Les evenements annules restent presents, pour la trace, mais sont ecartes du repli. Les faits actifs de
 * chaque cle d'activite sont interpretes par {@link SequenceDActivites}.
 * </p>
 *
 * <p>
 * A heure metier egale, la fin passe avant la transition, la transition avant l'ouverture, puis l'identifiant
 * departage : jamais la date d'enregistrement, qui ferait dependre le journal de l'ordre de reception.
 * </p>
 *
 * <p>
 * Tout geste qui s'y inscrit vise une activite de ce journal et de sa propre cle : une activite qu'aucun pointage n'y
 * a ouverte est introuvable, celle d'un autre operateur ou d'un autre poste est incoherente. L'une et l'autre sont
 * refusees avant toute interpretation.
 * </p>
 */
public record JournalDAtelier(List<EvenementDAtelier> evenements) {
  private static final Comparator<EvenementDAtelier> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(EvenementDAtelier::dateDeSurvenue)
    .thenComparingInt(evenement -> evenement.intention().rangAHeureEgale())
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
    List<EvenementDAtelier> enregistres = Stream.concat(evenements.stream(), Stream.of(evenement)).toList();
    exigeLActiviteVisee(enregistres, evenement);

    return new JournalDAtelier(enregistres);
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
   * Enchainer une annulation puis une insertion ferait passer le journal par un etat intermediaire que l'interpretation
   * refuserait a raison : annuler un debut y laisserait une fin orpheline. C'est ce qui justifie l'acte unique. Le
   * remplacant d'un ouvrant reprend l'activite qu'il ouvrait, pour que les gestes qui la visent y restent rattaches.
   * </p>
   */
  public JournalDAtelier corrige(EvenementDAtelierId id, Annulation annulation, EvenementDAtelier remplacant) {
    EvenementDAtelier corrige = evenement(id).orElseThrow(() -> new EvenementDAtelierIntrouvableException(id));
    EvenementDAtelier enPlace = remplacant.enRemplacementDe(corrige);
    List<EvenementDAtelier> corriges = remplace(corrige, evenement -> Stream.of(evenement.annule(annulation), enPlace));
    exigeLActiviteVisee(corriges, enPlace);

    return new JournalDAtelier(corriges);
  }

  /**
   * Refuse un geste qui vise une activite absente de ce journal, ou ouverte sur une autre cle que la sienne.
   */
  public void exigeLActiviteViseePar(EvenementDAtelier geste) {
    exigeLActiviteVisee(Stream.concat(evenements.stream(), Stream.of(geste)).toList(), geste);
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

  private List<EvenementDAtelier> remplace(EvenementDAtelier corrige, Function<EvenementDAtelier, Stream<EvenementDAtelier>> remplacement) {
    return evenements
      .stream()
      .flatMap(evenement -> evenement.equals(corrige) ? remplacement.apply(evenement) : Stream.of(evenement))
      .toList();
  }

  private static void exigeLActiviteVisee(List<EvenementDAtelier> evenements, EvenementDAtelier geste) {
    geste
      .activiteVisee()
      .ifPresent(visee -> {
        EvenementDAtelier ouvrant = ouvrantDe(evenements, visee).orElseThrow(() -> new ActiviteViseeIntrouvableException(geste, visee));
        if (!ouvrant.cle().equals(geste.cle())) {
          throw new ActiviteViseeIncoherenteException(geste, visee);
        }
      });
  }

  /**
   * L'ouvrant actif de l'activite, ou a defaut celui, annule, qui l'avait ouverte.
   */
  private static Optional<EvenementDAtelier> ouvrantDe(List<EvenementDAtelier> evenements, ActiviteId activite) {
    List<EvenementDAtelier> ouvrants = evenements
      .stream()
      .filter(evenement -> evenement.activite().filter(activite::equals).isPresent())
      .toList();

    return ouvrants
      .stream()
      .filter(ouvrant -> !ouvrant.estAnnule())
      .findFirst()
      .or(() -> ouvrants.stream().findFirst());
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
      .flatMap(faits -> SequenceDActivites.intervalles(faits, fermetureFinale).stream())
      .sorted(PAR_DEBUT)
      .toList();
  }
}
