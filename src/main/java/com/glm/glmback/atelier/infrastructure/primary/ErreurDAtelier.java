package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDAtelier implements ProblemCode {
  SUIVI_D_ATELIER_INTROUVABLE(HttpStatus.NOT_FOUND, "suivi d'atelier introuvable"),
  JOURNEE_DE_TRAVAIL_INTROUVABLE(HttpStatus.NOT_FOUND, "journee de travail introuvable"),
  EVENEMENT_D_ATELIER_INTROUVABLE(HttpStatus.NOT_FOUND, "evenement d'atelier introuvable"),
  EVENEMENT_DE_PRESENCE_INTROUVABLE(HttpStatus.NOT_FOUND, "evenement de presence introuvable"),
  ELEMENT_DE_FABRICATION_INTROUVABLE(HttpStatus.NOT_FOUND, "element de fabrication introuvable"),
  OPERATEUR_INTROUVABLE(HttpStatus.NOT_FOUND, "operateur introuvable"),
  POSTE_DE_TRAVAIL_INTROUVABLE(HttpStatus.NOT_FOUND, "poste de travail introuvable"),
  AUCUNE_JOURNEE_DE_TRAVAIL_EN_COURS(HttpStatus.NOT_FOUND, "aucune journee de travail en cours"),
  OPERATEUR_NON_HABILITE(HttpStatus.CONFLICT, "operateur non habilite"),
  ELEMENT_DEJA_ENGAGE(HttpStatus.CONFLICT, "element deja engage"),
  JOURNEE_DE_TRAVAIL_DEJA_OUVERTE(HttpStatus.CONFLICT, "journee de travail deja ouverte"),
  EVENEMENT_DEJA_ANNULE(HttpStatus.CONFLICT, "evenement deja annule"),
  EVENEMENT_DE_PRESENCE_DEJA_ANNULE(HttpStatus.CONFLICT, "evenement de presence deja annule"),
  SUIVI_D_ATELIER_CLOTURE(HttpStatus.CONFLICT, "suivi d'atelier cloture"),
  TRANSITION_D_ATELIER_INTERDITE(HttpStatus.CONFLICT, "transition d'atelier interdite"),
  TRANSITION_DE_PRESENCE_INTERDITE(HttpStatus.CONFLICT, "transition de presence interdite"),
  EVENEMENT_ANTERIEUR_A_L_ENGAGEMENT(HttpStatus.CONFLICT, "evenement anterieur a l'engagement"),
  SAISIE_CONCURRENTE(HttpStatus.CONFLICT, "saisie concurrente");

  private final HttpStatus status;
  private final String title;

  ErreurDAtelier(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public HttpStatus status() {
    return status;
  }

  @Override
  public String title() {
    return title;
  }
}
