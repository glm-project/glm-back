package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * L'interpretation des faits d'une meme cle d'activite — un operateur sur un poste de travail, pour un element engage.
 *
 * <p>
 * Les faits actifs arrivent dans l'ordre du journal, et au plus une activite est en cours sur la cle. Une ouverture
 * termine l'activite en cours a son heure (relance) et en ouvre une nouvelle. Une transition termine l'activite qu'elle
 * vise et ouvre une activite distincte, de l'autre categorie. Une fin termine l'activite qu'elle vise, et elle seule :
 * un geste ne touche jamais une autre activite que sa cible. La cloture, a defaut, ferme l'activite restee en cours.
 * </p>
 *
 * <p>
 * L'echeance gouverne l'interpretation sans aucun instant de lecture, sur les seules heures metier. Une activite que
 * rien n'a terminee a son echeance cesse d'etre en cours pour les faits qui suivent : elle garde sa borne automatique,
 * qu'une relance ne prolonge pas, ni la cloture. Un geste pointe au plus tard a l'echeance de sa cible la termine a son
 * heure ; pointe apres, une fin est conservee sans effet, et une transition ouvre sa nouvelle activite a son heure. Seul
 * un acte du gestionnaire, fin ou transition regularisee, termine une activite au-dela de son echeance.
 * </p>
 *
 * <p>
 * Aucun fait n'est jamais refuse ici. Un geste qui contredit les autres faits est conserve, et la sequence est en
 * conflit : sa cible est deja remplacee ou deja terminee par une fin reelle a son heure, pas encore ouverte, ou annulee ;
 * la transition vise sa propre categorie, une cible echue alors qu'une autre activite est en cours, ou une cible que seul
 * le gestionnaire a prolongee au-dela de son echeance. Une cible seulement echue ne contredit rien.
 * </p>
 *
 * <p>
 * Une contradiction couvre la zone qui va du debut de la cible a l'heure du geste. Sont a resoudre la cible, l'activite
 * qu'ouvre le geste et toute activite de la cle qui chevauche cette zone ; les autres gardent leur interpretation. Les
 * contradictions qui partagent une activite, ou une cible annulee, forment une meme sequence en conflit, dont les
 * pointages sont les gestes contradictoires et tout fait actif qui ouvre ou vise l'une de ses activites. Le resultat ne
 * depend que de l'ensemble des faits, jamais de leur ordre de reception.
 * </p>
 */
final class SequenceDActivites {

  private final List<EvenementDAtelier> actifs;
  private final Map<ActiviteId, Instant> regularisations;
  private final Map<ActiviteId, Instant> debuts = new HashMap<>();
  private final Map<ActiviteId, Activite> activites = new LinkedHashMap<>();
  private final Set<ActiviteId> expirees = new HashSet<>();
  private final List<Contradiction> contradictions = new ArrayList<>();
  private Optional<ActiviteId> courante = Optional.empty();
  private Optional<Instant> cloture = Optional.empty();

  private SequenceDActivites(List<EvenementDAtelier> faits) {
    actifs = faits
      .stream()
      .filter(fait -> !fait.estAnnule())
      .toList();
    regularisations = actifs
      .stream()
      .filter(EvenementDAtelier::estUneRegularisation)
      .filter(fait -> fait.activiteVisee().isPresent())
      .collect(
        Collectors.toMap(
          fait -> fait.activiteVisee().orElseThrow(),
          EvenementDAtelier::dateDeSurvenue,
          BinaryOperator.maxBy(Comparator.naturalOrder())
        )
      );
    Stream.concat(faits.stream().filter(EvenementDAtelier::estAnnule), actifs.stream()).forEach(fait ->
      fait.activite().ifPresent(activite -> debuts.put(activite, fait.dateDeSurvenue()))
    );
  }

  /**
   * Les activites que les faits actifs de la cle interpretent, dans l'ordre de leur ouverture. Les faits annules n'y
   * comptent pas : ils ne servent qu'a situer la contradiction d'un geste qui vise leur ouverture.
   */
  static List<Activite> activites(List<EvenementDAtelier> faits, Optional<Instant> cloture) {
    return interpretation(faits, cloture).activitesInterpretees();
  }

  /**
   * Les sequences en conflit que les faits actifs de la cle laissent a resoudre, dans l'ordre de leur premier pointage.
   */
  static List<SequenceEnConflit> conflits(List<EvenementDAtelier> faits, Optional<Instant> cloture) {
    return interpretation(faits, cloture).sequencesEnConflit();
  }

  private static SequenceDActivites interpretation(List<EvenementDAtelier> faits, Optional<Instant> cloture) {
    SequenceDActivites sequence = new SequenceDActivites(faits);
    sequence.actifs.forEach(sequence::interprete);
    sequence.clot(cloture);

    return sequence;
  }

  private void interprete(EvenementDAtelier fait) {
    expireAvant(fait.dateDeSurvenue());
    if (fait.intention() == IntentionDePointage.OUVERTURE) {
      ouvre(fait);
    } else if (fait.intention() == IntentionDePointage.TRANSITION) {
      transite(fait);
    } else {
      arrete(fait);
    }
  }

  /**
   * L'activite courante dont la limite tombe avant l'heure du fait n'est plus vivante : elle garde sa borne
   * automatique, sans fin reelle.
   */
  private void expireAvant(Instant heure) {
    courante
      .filter(activite -> limite(activites.get(activite)).isBefore(heure))
      .ifPresent(activite -> {
        expirees.add(activite);
        courante = Optional.empty();
      });
  }

  /**
   * Une ouverture termine a son heure l'activite en cours sur la cle, s'il y en a une : c'est la relance.
   */
  private void ouvre(EvenementDAtelier ouvrant) {
    courante.ifPresent(activite -> termine(activite, ouvrant.dateDeSurvenue()));
    Activite ouverte = Activite.ouvertePar(ouvrant);
    activites.put(ouverte.id(), ouverte);
    courante = Optional.of(ouverte.id());
  }

  /**
   * Une transition remplace a son heure l'activite en cours, qu'elle vise ; si sa cible a seulement expire, sans
   * qu'aucune autre activite ne l'ait suivie, elle n'ouvre que la nouvelle activite, a son heure, et la cible garde sa
   * borne automatique. Contradictoire, elle ouvre tout de meme son activite, a resoudre.
   */
  private void transite(EvenementDAtelier transition) {
    ActiviteId visee = transition.activiteVisee().orElseThrow();
    if (contredit(transition, visee)) {
      contradictions.add(contradiction(transition, visee));
    }
    ouvre(transition);
  }

  private boolean contredit(EvenementDAtelier transition, ActiviteId visee) {
    Optional<Activite> cible = Optional.ofNullable(activites.get(visee));
    if (cible.filter(activite -> activite.ouvrant().type() == transition.type()).isPresent()) {
      return true;
    }

    if (courante.filter(visee::equals).isPresent()) {
      return !peutTerminer(cible.orElseThrow(), transition);
    }

    return !expirees.contains(visee) || courante.isPresent();
  }

  /**
   * Une fin termine a son heure l'activite en cours qu'elle vise. Pointee apres l'echeance de sa cible, elle est
   * conservee sans effet : la cible garde sa borne automatique, ou la fin que le gestionnaire a regularisee. Une fin
   * dont la cible n'est ni en cours ni echue contredit le journal.
   */
  private void arrete(EvenementDAtelier fin) {
    ActiviteId visee = fin.activiteVisee().orElseThrow();
    if (courante.filter(visee::equals).isPresent()) {
      if (peutTerminer(activites.get(visee), fin)) {
        termine(visee, fin.dateDeSurvenue());
        courante = Optional.empty();
      }
      return;
    }

    if (!expirees.contains(visee)) {
      contradictions.add(contradiction(fin, visee));
    }
  }

  /**
   * La cloture termine a son heure l'activite encore vivante ; elle ne prolonge jamais une activite deja echue.
   */
  private void clot(Optional<Instant> cloture) {
    this.cloture = cloture;
    cloture.ifPresent(date -> {
      expireAvant(date);
      courante.ifPresent(activite -> termine(activite, date));
    });
  }

  private void termine(ActiviteId activite, Instant heure) {
    activites.computeIfPresent(activite, (identite, terminee) -> terminee.termineeA(heure));
  }

  /**
   * La contradiction d'un geste avec sa cible couvre ce qui separe le debut de la cible de l'heure du geste ; la
   * cible dont aucune ouverture n'est connue sur la cle la reduit a l'heure du geste.
   */
  private Contradiction contradiction(EvenementDAtelier geste, ActiviteId visee) {
    Instant heure = geste.dateDeSurvenue();
    Instant debut = debuts.getOrDefault(visee, heure);

    return debut.isBefore(heure) ? new Contradiction(geste, visee, debut, heure) : new Contradiction(geste, visee, heure, debut);
  }

  /**
   * L'instant jusqu'auquel une activite reste vivante : son echeance, ou plus tard le dernier geste du gestionnaire qui
   * la termine ou la remplace. Seule une regularisation peut etablir une fin reelle au-dela de l'echeance.
   */
  private Instant limite(Activite activite) {
    Instant echeance = activite.echeance().value();

    return Optional.ofNullable(regularisations.get(activite.id())).filter(echeance::isBefore).orElse(echeance);
  }

  /**
   * L'instant ou l'activite cesse d'occuper la cle : sa fin, sa limite si elle a expire, aucun si elle vit encore
   * apres le dernier fait.
   */
  private Optional<Instant> borne(Activite activite) {
    if (activite.fin().isPresent()) {
      return activite.fin();
    }

    return expirees.contains(activite.id()) ? Optional.of(limite(activite)) : Optional.empty();
  }

  private List<Activite> activitesInterpretees() {
    Set<ActiviteId> aResoudre = groupes()
      .stream()
      .flatMap(groupe -> groupe.cibles().stream())
      .collect(Collectors.toSet());

    return activites
      .values()
      .stream()
      .map(activite -> aResoudre.contains(activite.id()) ? activite.enConflit(finAuPlusTard(activite)) : activite)
      .toList();
  }

  private Instant finAuPlusTard(Activite activite) {
    Instant limite = limite(activite);
    return cloture.filter(limite::isAfter).orElse(limite);
  }

  private List<SequenceEnConflit> sequencesEnConflit() {
    List<EvenementDAtelierId> ordre = actifs.stream().map(EvenementDAtelier::id).toList();

    return groupes()
      .stream()
      .map(this::sequence)
      .sorted(Comparator.comparingInt(sequence -> ordre.indexOf(sequence.pointages().getFirst())))
      .toList();
  }

  private SequenceEnConflit sequence(GroupeContradictoire groupe) {
    return new SequenceEnConflit(
      actifs.getFirst().cle(),
      activites.keySet().stream().filter(groupe.cibles()::contains).toList(),
      actifs.stream().filter(groupe::concerne).map(EvenementDAtelier::id).toList()
    );
  }

  /**
   * Les contradictions regroupees en sequences : deux contradictions qui laissent une meme activite a resoudre, ou qui
   * visent une meme cible annulee, appartiennent a la meme sequence.
   */
  private List<GroupeContradictoire> groupes() {
    List<GroupeContradictoire> groupes = new ArrayList<>();
    contradictions.forEach(contradiction -> {
      GroupeContradictoire nouveau = new GroupeContradictoire(concernees(contradiction), List.of(contradiction.geste().id()));
      List<GroupeContradictoire> lies = groupes
        .stream()
        .filter(groupe -> !Collections.disjoint(groupe.cibles(), nouveau.cibles()))
        .toList();
      groupes.removeAll(lies);
      groupes.add(Stream.concat(lies.stream(), Stream.of(nouveau)).reduce(GroupeContradictoire::fusionne).orElseThrow());
    });

    return groupes;
  }

  /**
   * La cible d'une contradiction, l'activite qu'ouvre son geste, et les activites de la cle qui chevauchent sa zone.
   */
  private Set<ActiviteId> concernees(Contradiction contradiction) {
    Set<ActiviteId> concernees = new LinkedHashSet<>(List.of(contradiction.visee()));
    contradiction.geste().activite().ifPresent(concernees::add);
    activites
      .values()
      .stream()
      .filter(activite -> contradiction.chevauche(activite.debut(), borne(activite)))
      .map(Activite::id)
      .forEach(concernees::add);

    return concernees;
  }

  /**
   * Un geste pointe ne termine sa cible qu'au plus tard a son echeance, un geste exactement a l'echeance l'emportant
   * sur la fin automatique. Seul un acte du gestionnaire peut la terminer au-dela.
   */
  private static boolean peutTerminer(Activite cible, EvenementDAtelier geste) {
    return geste.estUneRegularisation() || !geste.dateDeSurvenue().isAfter(cible.echeance().value());
  }

  private record Contradiction(EvenementDAtelier geste, ActiviteId visee, Instant debut, Instant fin) {
    /**
     * Vrai si une activite occupant la cle de ce debut a cette borne recouvre la zone de la contradiction, bornes
     * exclues : celle qui finit a son debut, ou commence a sa fin, ne la chevauche pas.
     */
    boolean chevauche(Instant debutDeLActivite, Optional<Instant> borneDeLActivite) {
      return debutDeLActivite.isBefore(fin) && borneDeLActivite.map(debut::isBefore).orElse(true);
    }
  }

  /**
   * Des contradictions d'une meme sequence : les identites qu'elles laissent a resoudre, cibles annulees comprises, et
   * leurs gestes.
   */
  private record GroupeContradictoire(Set<ActiviteId> cibles, List<EvenementDAtelierId> gestes) {
    GroupeContradictoire fusionne(GroupeContradictoire autre) {
      Set<ActiviteId> reunies = new LinkedHashSet<>(cibles);
      reunies.addAll(autre.cibles());

      return new GroupeContradictoire(reunies, Stream.concat(gestes.stream(), autre.gestes().stream()).toList());
    }

    boolean concerne(EvenementDAtelier fait) {
      return (
        gestes.contains(fait.id())
        || fait.activite().filter(cibles::contains).isPresent()
        || fait.activiteVisee().filter(cibles::contains).isPresent()
      );
    }
  }
}
