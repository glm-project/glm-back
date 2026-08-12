package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * La suite ordonnee des pointages de presence d'un operateur, et les invariants de sequence qui la gouvernent.
 *
 * <p>
 * Meme patron que {@link JournalDAtelier} : tri par date de survenue, validation integrale a chaque construction, et
 * les evenements annules restent presents pour la trace mais sont ecartes du repli. Une arrivee oubliee se regularise
 * donc a l'heure ou l'operateur est reellement entre, et le calcul des heures repart de zero.
 * </p>
 */
public record JournalDePresence(List<EvenementDePresence> evenements) {
  private static final Comparator<EvenementDePresence> PAR_ORDRE_CHRONOLOGIQUE = Comparator.comparing(EvenementDePresence::dateDeSurvenue)
    .thenComparing(EvenementDePresence::dateDEnregistrement)
    .thenComparing(EvenementDePresence::id);

  public JournalDePresence {
    Assert.field("evenements", evenements).notNull().noNullElement();
    evenements = evenements.stream().sorted(PAR_ORDRE_CHRONOLOGIQUE).toList();
    fenetres(evenements);
  }

  public static JournalDePresence vide() {
    return new JournalDePresence(List.of());
  }

  public JournalDePresence enregistre(EvenementDePresence evenement) {
    return new JournalDePresence(Stream.concat(evenements.stream(), Stream.of(evenement)).toList());
  }

  public JournalDePresence annule(EvenementDePresenceId id, Annulation annulation) {
    exige(id);

    return new JournalDePresence(
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
   * Une heure d'arrivee fausse ne se rattrape pas en deux temps : annuler l'arrivee laisserait un depart orphelin, que
   * l'automate refuserait a raison.
   * </p>
   */
  public JournalDePresence corrige(EvenementDePresenceId id, Annulation annulation, EvenementDePresence remplacant) {
    return new JournalDePresence(remplace(id, evenement -> Stream.of(evenement.annule(annulation), remplacant)));
  }

  public List<FenetreDePresence> fenetres() {
    return fenetres(evenements);
  }

  public EtatDePresence etat() {
    EtatDePresence etat = EtatDePresence.ABSENT;

    for (EvenementDePresence evenement : actifs(evenements)) {
      etat = etat.apres(evenement.type()).orElseThrow();
    }

    return etat;
  }

  public Optional<Instant> debut() {
    return actifs(evenements).stream().findFirst().map(EvenementDePresence::dateDeSurvenue);
  }

  /**
   * L'amplitude de la journee, de l'arrivee au depart : le temps passe dans les murs, pauses comprises.
   */
  public Optional<Periode> amplitude() {
    List<EvenementDePresence> actifs = actifs(evenements);

    return actifs
      .stream()
      .reduce((premier, dernier) -> dernier)
      .filter(dernier -> dernier.type() == TypeDEvenementDePresence.DEPART)
      .map(dernier -> new Periode(actifs.getFirst().dateDeSurvenue(), dernier.dateDeSurvenue()));
  }

  private void exige(EvenementDePresenceId id) {
    evenements
      .stream()
      .filter(evenement -> evenement.id().equals(id))
      .findFirst()
      .orElseThrow(() -> new EvenementDePresenceIntrouvableException(id));
  }

  private List<EvenementDePresence> remplace(
    EvenementDePresenceId id,
    Function<EvenementDePresence, Stream<EvenementDePresence>> remplacement
  ) {
    exige(id);

    return evenements
      .stream()
      .flatMap(evenement -> evenement.id().equals(id) ? remplacement.apply(evenement) : Stream.of(evenement))
      .toList();
  }

  private static List<EvenementDePresence> actifs(List<EvenementDePresence> evenements) {
    return evenements
      .stream()
      .filter(evenement -> !evenement.estAnnule())
      .toList();
  }

  private static List<FenetreDePresence> fenetres(List<EvenementDePresence> evenements) {
    List<EvenementDePresence> actifs = actifs(evenements);
    List<FenetreDePresence> fenetres = new ArrayList<>();
    EtatDePresence etat = EtatDePresence.ABSENT;

    for (int rang = 0; rang < actifs.size(); rang++) {
      EvenementDePresence evenement = actifs.get(rang);
      EtatDePresence avant = etat;
      etat = avant.apres(evenement.type()).orElseThrow(() -> new TransitionDePresenceInterditeException(evenement, avant));

      if (etat == EtatDePresence.PRESENT) {
        Optional<Instant> fin = rang + 1 < actifs.size() ? Optional.of(actifs.get(rang + 1).dateDeSurvenue()) : Optional.empty();
        fenetres.add(new FenetreDePresence(evenement.dateDeSurvenue(), fin));
      }
    }

    return fenetres;
  }
}
