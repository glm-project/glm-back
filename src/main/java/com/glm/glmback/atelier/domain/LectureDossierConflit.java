package com.glm.glmback.atelier.domain;

import java.util.List;
import java.util.Optional;

public record LectureDossierConflit(AdresseDossierConflit adresse, LectureDuSuivi lecture) {
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
