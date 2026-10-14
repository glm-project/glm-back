package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * La suite ordonnee des evenements d'un element engage.
 *
 * <p>
 * Le journal se trie par date de survenue et se relit en entier : une insertion retroactive rejoue tout le chemin.
 * Les gestes sont lus par {@link SequenceDActivites}, la seule interpretation du journal, puis les corrections du
 * gestionnaire s'appliquent aux activites lues (voir {@link #activites}).
 * </p>
 *
 * <p>
 * A heure metier egale, la fin passe avant l'ouverture, puis l'identifiant departage : jamais la date
 * d'enregistrement, qui ferait dependre le journal de l'ordre de reception. L'ordre est sans ambiguite parce que le
 * journal ne porte jamais une fin et l'ouverture qu'elle fermerait a la meme heure : la regle de reception ignore une
 * fin du pupitre a l'heure du debut de l'activite qu'elle fermerait (ANTERIEUR), et la regularisation comme l'ajout
 * d'une activite refusent une fin qui n'est pas posterieure au debut de son activite (fin-avant-debut). Cette garantie
 * vient de ces trois controles, pas du journal, qui ne refuse rien.
 * </p>
 */
public record JournalDAtelier(List<EvenementDAtelier> evenements) {
  static final Comparator<EvenementDAtelier> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(EvenementDAtelier::dateDeSurvenue)
    .thenComparing(evenement -> evenement.type().ouvreUneActivite())
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
    return new JournalDAtelier(Stream.concat(evenements.stream(), Stream.of(evenement)).toList());
  }

  /**
   * Les activites que le journal donne, la cloture refermant a son heure celle qui reste en cours.
   *
   * <p>
   * Deux lectures se suivent. La premiere ({@link SequenceDActivites}) lit les gestes : elle donne les activites et lie
   * chaque fin a l'activite qu'elle ferme, retirees ou deplacees comprises, sans regarder une correction. La seconde
   * applique les corrections du gestionnaire aux activites lues : celles qu'un retrait cible disparaissent, celles qu'un
   * changement de poste cible comptent sur le poste du dernier changement du journal.
   * </p>
   */
  public List<Activite> activites(Optional<Instant> cloture) {
    Set<ActiviteId> retirees = viseesPar(TypeDEvenementDAtelier.RETRAIT).keySet();
    Map<ActiviteId, EvenementDAtelier> changements = viseesPar(TypeDEvenementDAtelier.CHANGEMENT_DE_POSTE);

    return SequenceDActivites.activites(
      evenements
        .stream()
        .filter(evenement -> evenement.type().estUnGeste())
        .toList(),
      cloture
    )
      .stream()
      .filter(activite -> !retirees.contains(activite.id()))
      .map(activite -> Optional.ofNullable(changements.get(activite.id())).map(activite::surLePosteDe).orElse(activite))
      .sorted(PAR_DEBUT)
      .toList();
  }

  /**
   * Les corrections de ce type, par activite visee ; pour une activite corrigee plusieurs fois, la derniere du journal.
   */
  private Map<ActiviteId, EvenementDAtelier> viseesPar(TypeDEvenementDAtelier type) {
    return evenements
      .stream()
      .filter(evenement -> evenement.type() == type)
      .collect(
        Collectors.toMap(evenement -> evenement.activiteVisee().orElseThrow(), Function.identity(), (premiere, derniere) -> derniere)
      );
  }
}
