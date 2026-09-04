package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AucuneJourneeDeTravailEnCoursException;
import com.glm.glmback.atelier.domain.DateDeSurvenueFutureException;
import com.glm.glmback.atelier.domain.ElementDejaEngageException;
import com.glm.glmback.atelier.domain.ElementEngageableIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementAvantEngagementException;
import com.glm.glmback.atelier.domain.EvenementDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDePresenceDejaAnnuleException;
import com.glm.glmback.atelier.domain.EvenementDePresenceIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDejaAnnuleException;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import com.glm.glmback.atelier.domain.JourneeDeTravailDejaOuverteException;
import com.glm.glmback.atelier.domain.JourneeDeTravailIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurNonHabiliteException;
import com.glm.glmback.atelier.domain.PosteDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelierClotureException;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.TransitionDAtelierInterditeException;
import com.glm.glmback.atelier.domain.TransitionDePresenceInterditeException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class AtelierExceptionAdvice {

  @ExceptionHandler(SuiviDAtelierIntrouvableException.class)
  ProblemDetail handleSuiviDAtelierIntrouvable(SuiviDAtelierIntrouvableException e) {
    return ErreurDAtelier.SUIVI_D_ATELIER_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(JourneeDeTravailIntrouvableException.class)
  ProblemDetail handleJourneeDeTravailIntrouvable(JourneeDeTravailIntrouvableException e) {
    return ErreurDAtelier.JOURNEE_DE_TRAVAIL_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(EvenementDAtelierIntrouvableException.class)
  ProblemDetail handleEvenementDAtelierIntrouvable(EvenementDAtelierIntrouvableException e) {
    return ErreurDAtelier.EVENEMENT_D_ATELIER_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(EvenementDePresenceIntrouvableException.class)
  ProblemDetail handleEvenementDePresenceIntrouvable(EvenementDePresenceIntrouvableException e) {
    return ErreurDAtelier.EVENEMENT_DE_PRESENCE_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(ElementEngageableIntrouvableException.class)
  ProblemDetail handleElementEngageableIntrouvable(ElementEngageableIntrouvableException e) {
    return ErreurDAtelier.ELEMENT_DE_FABRICATION_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(OperateurDAtelierIntrouvableException.class)
  ProblemDetail handleOperateurDAtelierIntrouvable(OperateurDAtelierIntrouvableException e) {
    return ErreurDAtelier.OPERATEUR_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(PosteDAtelierIntrouvableException.class)
  ProblemDetail handlePosteDAtelierIntrouvable(PosteDAtelierIntrouvableException e) {
    return ErreurDAtelier.POSTE_DE_TRAVAIL_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(OperateurNonHabiliteException.class)
  ProblemDetail handleOperateurNonHabilite(OperateurNonHabiliteException e) {
    return ErreurDAtelier.OPERATEUR_NON_HABILITE.problem(e);
  }

  @ExceptionHandler(AucuneJourneeDeTravailEnCoursException.class)
  ProblemDetail handleAucuneJourneeDeTravailEnCours(AucuneJourneeDeTravailEnCoursException e) {
    return ErreurDAtelier.AUCUNE_JOURNEE_DE_TRAVAIL_EN_COURS.problem(e);
  }

  @ExceptionHandler(ElementDejaEngageException.class)
  ProblemDetail handleElementDejaEngage(ElementDejaEngageException e) {
    return ErreurDAtelier.ELEMENT_DEJA_ENGAGE.problem(e);
  }

  @ExceptionHandler(JourneeDeTravailDejaOuverteException.class)
  ProblemDetail handleJourneeDeTravailDejaOuverte(JourneeDeTravailDejaOuverteException e) {
    return ErreurDAtelier.JOURNEE_DE_TRAVAIL_DEJA_OUVERTE.problem(e);
  }

  @ExceptionHandler(EvenementDejaAnnuleException.class)
  ProblemDetail handleEvenementDejaAnnule(EvenementDejaAnnuleException e) {
    return ErreurDAtelier.EVENEMENT_DEJA_ANNULE.problem(e);
  }

  @ExceptionHandler(EvenementDePresenceDejaAnnuleException.class)
  ProblemDetail handleEvenementDePresenceDejaAnnule(EvenementDePresenceDejaAnnuleException e) {
    return ErreurDAtelier.EVENEMENT_DE_PRESENCE_DEJA_ANNULE.problem(e);
  }

  @ExceptionHandler(SuiviDAtelierClotureException.class)
  ProblemDetail handleSuiviDAtelierCloture(SuiviDAtelierClotureException e) {
    return ErreurDAtelier.SUIVI_D_ATELIER_CLOTURE.problem(e);
  }

  @ExceptionHandler(TransitionDAtelierInterditeException.class)
  ProblemDetail handleTransitionDAtelierInterdite(TransitionDAtelierInterditeException e) {
    return ErreurDAtelier.TRANSITION_D_ATELIER_INTERDITE.problem(e);
  }

  @ExceptionHandler(TransitionDePresenceInterditeException.class)
  ProblemDetail handleTransitionDePresenceInterdite(TransitionDePresenceInterditeException e) {
    return ErreurDAtelier.TRANSITION_DE_PRESENCE_INTERDITE.problem(e);
  }

  @ExceptionHandler(EvenementAvantEngagementException.class)
  ProblemDetail handleEvenementAvantEngagement(EvenementAvantEngagementException e) {
    return ErreurDAtelier.EVENEMENT_ANTERIEUR_A_L_ENGAGEMENT.problem(e);
  }

  @ExceptionHandler(SaisieConcurrenteException.class)
  ProblemDetail handleSaisieConcurrente(SaisieConcurrenteException e) {
    return ErreurDAtelier.SAISIE_CONCURRENTE.problem(e);
  }

  @ExceptionHandler(IdentifiantDEvenementReutiliseException.class)
  ProblemDetail handleIdentifiantDEvenementReutilise(IdentifiantDEvenementReutiliseException e) {
    return ErreurDAtelier.IDENTIFIANT_EVENEMENT_REUTILISE.problem(e);
  }

  @ExceptionHandler(DateDeSurvenueFutureException.class)
  ProblemDetail handleDateDeSurvenueFuture(DateDeSurvenueFutureException e) {
    return ErreurDAtelier.DATE_DE_SURVENUE_FUTURE.problem(e);
  }
}
