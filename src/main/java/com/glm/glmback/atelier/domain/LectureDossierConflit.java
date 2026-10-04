package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LectureDossierConflit(AdresseDossierConflit adresse, LectureDuSuivi lecture, Set<ActiviteId> concernees) {
  public LectureDossierConflit {
    Assert.notNull("adresse", adresse);
    Assert.notNull("lecture", lecture);
    Assert.field("activites concernees", concernees).notNull().noNullElement();
    concernees = Set.copyOf(concernees);
  }

  public LectureDossierConflit(AdresseDossierConflit adresse, LectureDuSuivi lecture) {
    this(
      adresse,
      lecture,
      lecture
        .conflits()
        .stream()
        .filter(sequence -> sequence.pointages().contains(adresse.pointage()))
        .findFirst()
        .map(sequence -> Set.copyOf(sequence.activites()))
        .orElse(Set.of())
    );
  }

  public LectureDossierConflit apresActe(LectureDuSuivi apres) {
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
    return new LectureDossierConflit(adresse, apres, apresActe);
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
    return sequence().isPresent() ? EtatDAdresseDossier.EN_CONFLIT : EtatDAdresseDossier.HORS_CONFLIT;
  }

  public Optional<SequenceEnConflit> sequence() {
    return lecture
      .conflits()
      .stream()
      .filter(conflit -> conflit.pointages().contains(adresse.pointage()))
      .findFirst();
  }

  public List<PropositionDeResolution> choix() {
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
          .adresse(new AdresseDossierConflit(suivi.id(), conflit.pointages().getFirst()))
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
