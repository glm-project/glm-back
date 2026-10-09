package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * La suite ordonnee des evenements d'un element engage.
 *
 * <p>
 * Le journal se trie par date de survenue et se relit en entier : une insertion retroactive rejoue tout le chemin.
 * Les faits de chaque cle d'activite sont lus par {@link SequenceDActivites}, la seule interpretation du journal.
 * </p>
 *
 * <p>
 * A heure metier egale, la fin passe avant l'ouverture, puis l'identifiant departage : jamais la date
 * d'enregistrement, qui ferait dependre le journal de l'ordre de reception. L'ordre est sans ambiguite parce que le
 * journal ne porte jamais une fin et l'ouverture qu'elle fermerait a la meme heure : la regle de reception ignore une
 * fin du pupitre a l'heure du debut de l'activite qu'elle fermerait (ANTERIEUR), et la regularisation refuse une fin
 * qui n'est pas posterieure au debut de son activite (fin-avant-debut). Cette garantie vient de ces deux controles, pas
 * du journal, qui ne refuse rien.
 * </p>
 */
public record JournalDAtelier(List<EvenementDAtelier> evenements) {
  private static final Comparator<EvenementDAtelier> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(EvenementDAtelier::dateDeSurvenue)
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
   * Les activites que les faits de chaque cle donnent, la cloture refermant a son heure celle qui reste en cours.
   */
  public List<Activite> activites(Optional<Instant> cloture) {
    return parCle()
      .flatMap(faits -> SequenceDActivites.activites(faits, cloture).stream())
      .sorted(PAR_DEBUT)
      .toList();
  }

  /**
   * Les faits de chaque cle.
   */
  private Stream<List<EvenementDAtelier>> parCle() {
    return evenements
      .stream()
      .collect(Collectors.groupingBy(EvenementDAtelier::cle, LinkedHashMap::new, Collectors.toList()))
      .values()
      .stream();
  }
}
