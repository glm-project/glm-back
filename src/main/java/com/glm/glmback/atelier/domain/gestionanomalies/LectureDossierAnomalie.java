package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.Activite;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.DiagnosticDeConflit;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.RaisonDuConflit;
import com.glm.glmback.atelier.domain.SequenceEnConflit;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LectureDossierAnomalie(AdresseDossierAnomalie adresse, LectureDuSuivi lecture, Set<ActiviteId> concernees) {
  public LectureDossierAnomalie {
    Assert.notNull("adresse", adresse);
    Assert.notNull("lecture", lecture);
    Assert.field("activites concernees", concernees).notNull().noNullElement();
    concernees = Set.copyOf(concernees);
  }

  public LectureDossierAnomalie(AdresseDossierAnomalie adresse, LectureDuSuivi lecture) {
    this(
      adresse,
      lecture,
      lecture
        .conflits()
        .stream()
        .filter(sequence -> sequence.pointages().contains(adresse.pointage()))
        .findFirst()
        .map(sequence -> Set.copyOf(sequence.activites()))
        .orElseGet(() ->
          activiteEchueOuverteParLAncre(adresse, lecture)
            .map(activite -> Set.of(activite.id()))
            .orElse(Set.of())
        )
    );
  }

  /** L'activite que l'ancre ouvre et qu'aucune fin reelle n'a terminee avant son echeance, a l'instant d'evaluation. */
  private static Optional<Activite> activiteEchueOuverteParLAncre(AdresseDossierAnomalie adresse, LectureDuSuivi lecture) {
    return lecture
      .suivi()
      .activites()
      .stream()
      .filter(activite -> activite.ouvrant().id().equals(adresse.pointage()))
      .filter(activite -> activite.a(lecture.evaluation()).finAutomatique())
      .findFirst();
  }

  /**
   * Le meme dossier, relu apres un acte. Seul le dossier d'une sequence en conflit voit s'elargir ses activites
   * concernees : les faits que l'acte ajoute les rattachent. Celui d'une fin automatique garde l'activite de son
   * ancre, identifiee par son {@link ActiviteId} d'origine, que la correction de l'ouvrant conserve : l'activite qu'un
   * geste tardif corrige ouvre a son tour, elle, a sa propre adresse, et son echeance n'est pas celle de ce dossier.
   */
  public LectureDossierAnomalie apresActe(LectureDuSuivi apres) {
    if (sequence().isEmpty()) {
      return new LectureDossierAnomalie(adresse, apres, concernees);
    }
    Set<ActiviteId> apresActe = Stream.concat(
      concernees.stream(),
      apres
        .suivi()
        .journal()
        .evenements()
        .stream()
        .filter(fait -> lecture.suivi().journal().evenement(fait.id()).filter(fait::equals).isEmpty())
        .flatMap(fait -> Stream.concat(fait.activite().stream(), fait.activiteVisee().stream()))
    ).collect(Collectors.toSet());
    return new LectureDossierAnomalie(adresse, apres, apresActe);
  }

  public List<IntervalleDActivite> activites() {
    return lecture
      .suivi()
      .activites()
      .stream()
      .filter(activite -> concernees.contains(activite.id()))
      .map(activite -> activite.a(lecture.evaluation()))
      .toList();
  }

  public Optional<PerimetreDeDossier> perimetre() {
    return lecture
      .suivi()
      .journal()
      .evenement(adresse.pointage())
      .map(ancre -> {
        List<EvenementDAtelier> faits = lecture
          .suivi()
          .journal()
          .evenements()
          .stream()
          .filter(
            fait ->
              fait.id().equals(adresse.pointage())
              || fait.activite().filter(concernees::contains).isPresent()
              || fait.activiteVisee().filter(concernees::contains).isPresent()
          )
          .toList();
        return new PerimetreDeDossier(
          ancre.cle(),
          faits
            .stream()
            .flatMap(fait -> fait.activite().stream())
            .distinct()
            .toList(),
          faits.stream().map(EvenementDAtelier::id).toList()
        );
      });
  }

  public boolean enConflit() {
    return perimetre()
      .map(perimetre ->
        lecture
          .conflits()
          .stream()
          .anyMatch(sequence -> sequence.pointages().stream().anyMatch(perimetre.pointages()::contains))
      )
      .orElse(false);
  }

  public EtatDAdresseDossier kind() {
    Optional<EvenementDAtelier> pointage = lecture.suivi().journal().evenement(adresse.pointage());
    if (pointage.isEmpty()) {
      return EtatDAdresseDossier.INTROUVABLE;
    }
    if (pointage.orElseThrow().estAnnule()) {
      return EtatDAdresseDossier.ANCRE_ANNULEE;
    }
    if (sequence().isPresent()) {
      return EtatDAdresseDossier.EN_CONFLIT;
    }
    return activiteEchueOuverteParLAncre(adresse, lecture).isPresent()
      ? EtatDAdresseDossier.FIN_AUTOMATIQUE
      : EtatDAdresseDossier.SANS_ANOMALIE;
  }

  /** Vrai si une activite concernee reste terminee automatiquement, quel que soit l'etat de l'adresse. */
  public boolean finAutomatique() {
    return activites().stream().anyMatch(IntervalleDActivite::finAutomatique);
  }

  public Optional<SequenceEnConflit> sequence() {
    return lecture
      .conflits()
      .stream()
      .filter(conflit -> conflit.pointages().contains(adresse.pointage()))
      .findFirst();
  }

  public List<PropositionDeResolution> choix() {
    return Stream.concat(choixDeConflit().stream(), choixDeFinAutomatique().stream()).toList();
  }

  /**
   * Une seule proposition guide la fin automatique, d'apres les gestes tardifs qui visent l'activite. Une transition
   * tardive se corrige d'abord : toute fin regularisee la contredirait. Sans transition, une fin tardive se corrige,
   * la plus tardive seule, car corriger une autre laisserait la suivante viser une activite deja terminee. Sans geste
   * tardif, la fin se regularise a l'heure que le gestionnaire saisira.
   */
  private List<PropositionDeResolution> choixDeFinAutomatique() {
    if (kind() != EtatDAdresseDossier.FIN_AUTOMATIQUE) {
      return List.of();
    }
    var activite = activiteEchueOuverteParLAncre(adresse, lecture).orElseThrow().id();
    var gestes = lecture
      .suivi()
      .journal()
      .evenements()
      .stream()
      .filter(fait -> !fait.estAnnule() && fait.activiteVisee().filter(activite::equals).isPresent())
      .toList();
    return List.of(
      plusTardif(gestes, IntentionDePointage.TRANSITION)
        .map(transition ->
          new PropositionDeResolution(CodeDeProposition.CORRIGER_TRANSITION_TARDIVE, transition.id(), Optional.of(activite))
        )
        .or(() ->
          plusTardif(gestes, IntentionDePointage.FIN).map(fin ->
            new PropositionDeResolution(CodeDeProposition.CORRIGER_FIN_TARDIVE, fin.id(), Optional.of(activite))
          )
        )
        .orElseGet(() -> new PropositionDeResolution(CodeDeProposition.REGULARISER_FIN, adresse.pointage(), Optional.of(activite)))
    );
  }

  private static Optional<EvenementDAtelier> plusTardif(List<EvenementDAtelier> gestes, IntentionDePointage intention) {
    return gestes
      .stream()
      .filter(geste -> geste.intention() == intention)
      .max(Comparator.comparing(EvenementDAtelier::dateDeSurvenue));
  }

  private List<PropositionDeResolution> choixDeConflit() {
    return diagnostics()
      .stream()
      .flatMap(diagnostic -> {
        var geste = lecture.suivi().journal().evenement(diagnostic.pointage()).orElseThrow();
        if (diagnostic.raison() != RaisonDuConflit.CIBLE_REMPLACEE || geste.intention() != IntentionDePointage.FIN) {
          return Stream.<PropositionDeResolution>empty();
        }
        return diagnostic
          .cible()
          .termineePar()
          .flatMap(lecture.suivi().journal()::evenement)
          .filter(terminant -> terminant.intention() == IntentionDePointage.TRANSITION)
          .map(transition ->
            List.of(
              new PropositionDeResolution(CodeDeProposition.RATTACHER_FIN_A_ACTIVITE_REMPLACANTE, geste.id(), transition.activite()),
              new PropositionDeResolution(CodeDeProposition.ANNULER_TRANSITION, transition.id(), Optional.empty())
            )
          )
          .orElse(List.of())
          .stream();
      })
      .toList();
  }

  public List<ConflitEnListe> continuations() {
    var suivi = lecture.suivi();
    return lecture
      .conflits()
      .stream()
      .filter(conflit -> !conflit.pointages().contains(adresse.pointage()))
      .map(conflit ->
        ConflitEnListe.builder()
          .adresse(new AdresseDossierAnomalie(suivi.id(), conflit.pointages().getFirst()))
          .revision(suivi.revision())
          .element(suivi.element())
          .cle(conflit.cle())
          .repere(
            new RepereDeSequence(
              suivi.journal().evenement(conflit.pointages().getFirst()).orElseThrow().dateDeSurvenue(),
              conflit.pointages().size()
            )
          )
      )
      .toList();
  }

  public List<DiagnosticDeConflit> diagnostics() {
    return sequence()
      .map(conflit ->
        lecture
          .suivi()
          .journal()
          .diagnostics(lecture.suivi().cloture().map(Cloture::dateDeSurvenue))
          .stream()
          .filter(diagnostic -> conflit.pointages().contains(diagnostic.pointage()))
          .toList()
      )
      .orElse(List.of());
  }
}
