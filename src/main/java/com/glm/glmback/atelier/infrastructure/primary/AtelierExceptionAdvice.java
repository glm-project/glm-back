package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ActiviteViseeIncoherenteException;
import com.glm.glmback.atelier.domain.ActiviteViseeIntrouvableException;
import com.glm.glmback.atelier.domain.DateDeSurvenueFutureException;
import com.glm.glmback.atelier.domain.ElementDejaEngageException;
import com.glm.glmback.atelier.domain.ElementEngageableIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementAvantEngagementException;
import com.glm.glmback.atelier.domain.EvenementDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.EvenementDejaAnnuleException;
import com.glm.glmback.atelier.domain.IdentifiantDEvenementReutiliseException;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurNonHabiliteException;
import com.glm.glmback.atelier.domain.PosteDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SaisieConcurrenteException;
import com.glm.glmback.atelier.domain.SuiviDAtelierClotureException;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.gestionanomalies.ApercuObsoleteException;
import com.glm.glmback.atelier.domain.gestionanomalies.ConfirmationReutiliseeException;
import com.glm.glmback.atelier.domain.gestionanomalies.NatureDAnomalieInvalideException;
import com.glm.glmback.atelier.domain.gestionanomalies.PropositionInvalideException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 20_000)
class AtelierExceptionAdvice {

  @ExceptionHandler(PropositionInvalideException.class)
  ProblemDetail handlePropositionInvalide(PropositionInvalideException e) {
    return ErreurDAtelier.PROPOSITION_INVALIDE.problem(e);
  }

  @ExceptionHandler(NatureDAnomalieInvalideException.class)
  ProblemDetail handleNatureDAnomalieInvalide(NatureDAnomalieInvalideException e) {
    return ErreurDAtelier.NATURE_D_ANOMALIE_INVALIDE.problem(e);
  }

  @ExceptionHandler(ApercuObsoleteException.class)
  ProblemDetail handleApercuObsolete(ApercuObsoleteException e) {
    return ErreurDAtelier.APERCU_OBSOLETE.problem(e);
  }

  @ExceptionHandler(ConfirmationReutiliseeException.class)
  ProblemDetail handleConfirmationReutilisee(ConfirmationReutiliseeException e) {
    return ErreurDAtelier.CONFIRMATION_REUTILISEE.problem(e);
  }

  @ExceptionHandler(SuiviDAtelierIntrouvableException.class)
  ProblemDetail handleSuiviDAtelierIntrouvable(SuiviDAtelierIntrouvableException e) {
    return ErreurDAtelier.SUIVI_D_ATELIER_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(EvenementDAtelierIntrouvableException.class)
  ProblemDetail handleEvenementDAtelierIntrouvable(EvenementDAtelierIntrouvableException e) {
    return ErreurDAtelier.EVENEMENT_D_ATELIER_INTROUVABLE.problem(e);
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

  @ExceptionHandler(ActiviteViseeIntrouvableException.class)
  ProblemDetail handleActiviteViseeIntrouvable(ActiviteViseeIntrouvableException e) {
    return ErreurDAtelier.ACTIVITE_VISEE_INTROUVABLE.problem(e);
  }

  @ExceptionHandler(ActiviteViseeIncoherenteException.class)
  ProblemDetail handleActiviteViseeIncoherente(ActiviteViseeIncoherenteException e) {
    return ErreurDAtelier.ACTIVITE_VISEE_INCOHERENTE.problem(e);
  }

  @ExceptionHandler(OperateurNonHabiliteException.class)
  ProblemDetail handleOperateurNonHabilite(OperateurNonHabiliteException e) {
    return ErreurDAtelier.OPERATEUR_NON_HABILITE.problem(e);
  }

  @ExceptionHandler(ElementDejaEngageException.class)
  ProblemDetail handleElementDejaEngage(ElementDejaEngageException e) {
    return ErreurDAtelier.ELEMENT_DEJA_ENGAGE.problem(e);
  }

  @ExceptionHandler(EvenementDejaAnnuleException.class)
  ProblemDetail handleEvenementDejaAnnule(EvenementDejaAnnuleException e) {
    return ErreurDAtelier.EVENEMENT_DEJA_ANNULE.problem(e);
  }

  @ExceptionHandler(SuiviDAtelierClotureException.class)
  ProblemDetail handleSuiviDAtelierCloture(SuiviDAtelierClotureException e) {
    return ErreurDAtelier.SUIVI_D_ATELIER_CLOTURE.problem(e);
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
