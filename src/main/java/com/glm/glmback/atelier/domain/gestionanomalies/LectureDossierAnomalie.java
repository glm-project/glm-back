package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.Activite;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Cloture;
import com.glm.glmback.atelier.domain.DiagnosticDeConflit;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.SequenceEnConflit;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
