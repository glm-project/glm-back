package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AucuneJourneeDeTravailEnCoursException;
import com.glm.glmback.atelier.domain.ElementDejaEngageException;
import com.glm.glmback.atelier.domain.ElementEngageableIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementAvantEngagementException;
import com.glm.glmback.atelier.domain.EvenementDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDePresenceDejaAnnuleException;
import com.glm.glmback.atelier.domain.EvenementDePresenceIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDejaAnnuleException;
import com.glm.glmback.atelier.domain.JourneeDeTravailDejaOuverteException;
import com.glm.glmback.atelier.domain.JourneeDeTravailIntrouvableException;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelierClotureException;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.TransitionDAtelierInterditeException;
import com.glm.glmback.atelier.domain.TransitionDePresenceInterditeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class AtelierExceptionAdvice {

  private static final String MESSAGE_KEY = "message";

  @ExceptionHandler(SuiviDAtelierIntrouvableException.class)
  ProblemDetail handleSuiviDAtelierIntrouvable(SuiviDAtelierIntrouvableException e) {
    return introuvable("suivi d'atelier introuvable", e);
  }

  @ExceptionHandler(JourneeDeTravailIntrouvableException.class)
  ProblemDetail handleJourneeDeTravailIntrouvable(JourneeDeTravailIntrouvableException e) {
    return introuvable("journee de travail introuvable", e);
  }

  @ExceptionHandler(EvenementDAtelierIntrouvableException.class)
  ProblemDetail handleEvenementDAtelierIntrouvable(EvenementDAtelierIntrouvableException e) {
    return introuvable("evenement d'atelier introuvable", e);
  }

  @ExceptionHandler(EvenementDePresenceIntrouvableException.class)
  ProblemDetail handleEvenementDePresenceIntrouvable(EvenementDePresenceIntrouvableException e) {
    return introuvable("evenement de presence introuvable", e);
  }

  @ExceptionHandler(ElementEngageableIntrouvableException.class)
  ProblemDetail handleElementEngageableIntrouvable(ElementEngageableIntrouvableException e) {
    return introuvable("element de fabrication introuvable", e);
  }

  @ExceptionHandler(AucuneJourneeDeTravailEnCoursException.class)
  ProblemDetail handleAucuneJourneeDeTravailEnCours(AucuneJourneeDeTravailEnCoursException e) {
    return introuvable("aucune journee de travail en cours", e);
  }

  @ExceptionHandler(ElementDejaEngageException.class)
  ProblemDetail handleElementDejaEngage(ElementDejaEngageException e) {
    return conflit("element deja engage", e);
  }

  @ExceptionHandler(JourneeDeTravailDejaOuverteException.class)
  ProblemDetail handleJourneeDeTravailDejaOuverte(JourneeDeTravailDejaOuverteException e) {
    return conflit("journee de travail deja ouverte", e);
  }

  @ExceptionHandler(EvenementDejaAnnuleException.class)
  ProblemDetail handleEvenementDejaAnnule(EvenementDejaAnnuleException e) {
    return conflit("evenement deja annule", e);
  }

  @ExceptionHandler(EvenementDePresenceDejaAnnuleException.class)
  ProblemDetail handleEvenementDePresenceDejaAnnule(EvenementDePresenceDejaAnnuleException e) {
    return conflit("evenement de presence deja annule", e);
  }

  @ExceptionHandler(SuiviDAtelierClotureException.class)
  ProblemDetail handleSuiviDAtelierCloture(SuiviDAtelierClotureException e) {
    return conflit("suivi d'atelier cloture", e);
  }

  @ExceptionHandler(TransitionDAtelierInterditeException.class)
  ProblemDetail handleTransitionDAtelierInterdite(TransitionDAtelierInterditeException e) {
    return conflit("transition d'atelier interdite", e);
  }

  @ExceptionHandler(TransitionDePresenceInterditeException.class)
  ProblemDetail handleTransitionDePresenceInterdite(TransitionDePresenceInterditeException e) {
    return conflit("transition de presence interdite", e);
  }

  @ExceptionHandler(EvenementAvantEngagementException.class)
  ProblemDetail handleEvenementAvantEngagement(EvenementAvantEngagementException e) {
    return conflit("evenement anterieur a l'engagement", e);
  }

  @ExceptionHandler(SaisieConcurrenteException.class)
  ProblemDetail handleSaisieConcurrente(SaisieConcurrenteException e) {
    return conflit("saisie concurrente", e);
  }

  private static ProblemDetail introuvable(String titre, RuntimeException e) {
    return probleme(HttpStatus.NOT_FOUND, titre, e);
  }

  private static ProblemDetail conflit(String titre, RuntimeException e) {
    return probleme(HttpStatus.CONFLICT, titre, e);
  }

  private static ProblemDetail probleme(HttpStatus status, String titre, RuntimeException e) {
    var detail = ProblemDetail.forStatus(status);
    detail.setTitle(titre);
    detail.setProperty(MESSAGE_KEY, e.getMessage());

    return detail;
  }
}
