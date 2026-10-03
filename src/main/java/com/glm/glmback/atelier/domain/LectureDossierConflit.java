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
    Set<ActiviteId> avant = lecture.suivi().activites().stream().map(Activite::id).collect(Collectors.toSet());
    Set<ActiviteId> apresActe = Stream.concat(
      concernees.stream(),
      apres
        .suivi()
        .activites()
        .stream()
        .map(Activite::id)
        .filter(activite -> !avant.contains(activite))
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
