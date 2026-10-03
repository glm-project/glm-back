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
 * La suite ordonnee des evenements d'un element engage.
 *
 * <p>
 * Le journal se trie par date de survenue et se reinterprete en entier a chaque lecture : une insertion retroactive,
 * une annulation ou une correction rejoue tout le chemin. Il ne refuse jamais une sequence : des faits qui se
 * contredisent sont conserves, et leur sequence est en conflit jusqu'a ce que le gestionnaire la resolve. Les
 * evenements annules restent presents, pour la trace, mais sont ecartes du repli. Les faits de chaque cle d'activite
 * sont interpretes par {@link SequenceDActivites}.
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

  private static final Comparator<Activite> PAR_DEBUT = Comparator.comparing(Activite::debut).thenComparing(activite ->
    activite.ouvrant().id()
  );

  public JournalDAtelier {
    Assert.field("evenements", evenements).notNull().noNullElement();
    evenements = evenements.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList();
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
   * Annule un evenement et lui substitue sa version corrigee, en un seul acte.
   *
   * <p>
   * Le remplacant d'un ouvrant reprend l'activite qu'il ouvrait, pour que les gestes qui la visent y restent
   * rattaches : c'est ce qui distingue la correction d'une annulation suivie d'une regularisation, qui ouvrirait une
   * autre activite et laisserait ces gestes en conflit.
   * </p>
   */
  public JournalDAtelier corrige(EvenementDAtelierId id, Annulation annulation, EvenementDAtelier remplacant) {
    EvenementDAtelier corrige = evenement(id).orElseThrow(() -> new EvenementDAtelierIntrouvableException(id));
    EvenementDAtelier enPlace = remplacant.enRemplacementDe(corrige);
    List<EvenementDAtelier> corriges = remplace(corrige, evenement -> Stream.of(evenement.annule(annulation), enPlace));
    exigeLActiviteVisee(corriges, enPlace);
    enPlace
      .activite()
      .ifPresent(activite ->
        corriges
          .stream()
          .filter(fait -> !fait.estAnnule())
          .filter(fait -> fait.activiteVisee().filter(activite::equals).isPresent())
          .forEach(fait -> exigeLActiviteVisee(corriges, fait))
      );

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

  /**
   * Les activites que les faits actifs de chaque cle interpretent, la cloture refermant a son heure celle qui reste en
   * cours.
   */
  public List<Activite> activites(Optional<Instant> cloture) {
    return parCle()
      .flatMap(faits -> SequenceDActivites.activites(faits, cloture).stream())
      .sorted(PAR_DEBUT)
      .toList();
  }

  /**
   * Les sequences en conflit que les faits actifs de chaque cle laissent a resoudre.
   */
  public List<SequenceEnConflit> conflits(Optional<Instant> cloture) {
    return parCle()
      .flatMap(faits -> SequenceDActivites.conflits(faits, cloture).stream())
      .toList();
  }

  public List<DiagnosticDeConflit> diagnostics(Optional<Instant> cloture) {
    return parCle()
      .flatMap(faits -> SequenceDActivites.diagnostics(faits, cloture).stream())
      .toList();
  }

  /**
   * Les faits de chaque cle, annules compris : un geste qui vise une ouverture annulee se situe par elle.
   */
  private Stream<List<EvenementDAtelier>> parCle() {
    return evenements
      .stream()
      .collect(Collectors.groupingBy(EvenementDAtelier::cle, LinkedHashMap::new, Collectors.toList()))
      .values()
      .stream();
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

  private static List<EvenementDAtelier> actifs(List<EvenementDAtelier> evenements) {
    return evenements
      .stream()
      .filter(evenement -> !evenement.estAnnule())
      .toList();
  }
}
