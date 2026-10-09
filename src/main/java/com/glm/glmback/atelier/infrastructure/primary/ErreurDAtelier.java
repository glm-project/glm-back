package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.shared.error.infrastructure.primary.ProblemCode;
import org.springframework.http.HttpStatus;

enum ErreurDAtelier implements ProblemCode {
  SUIVI_D_ATELIER_INTROUVABLE(HttpStatus.NOT_FOUND, "suivi d'atelier introuvable"),
  FIN_AUTOMATIQUE_INTROUVABLE(HttpStatus.NOT_FOUND, "fin automatique introuvable"),
  ELEMENT_DE_FABRICATION_INTROUVABLE(HttpStatus.NOT_FOUND, "element de fabrication introuvable"),
  OPERATEUR_INTROUVABLE(HttpStatus.NOT_FOUND, "operateur introuvable"),
  POSTE_DE_TRAVAIL_INTROUVABLE(HttpStatus.NOT_FOUND, "poste de travail introuvable"),
  ACTIVITE_VISEE_INTROUVABLE(HttpStatus.NOT_FOUND, "activite visee introuvable"),
  OPERATEUR_NON_HABILITE(HttpStatus.CONFLICT, "operateur non habilite"),
  ACTIVITE_VISEE_INCOHERENTE(HttpStatus.CONFLICT, "activite visee incoherente"),
  ELEMENT_DEJA_ENGAGE(HttpStatus.CONFLICT, "element deja engage"),
  SUIVI_D_ATELIER_CLOTURE(HttpStatus.CONFLICT, "suivi d'atelier cloture"),
  EVENEMENT_ANTERIEUR_A_L_ENGAGEMENT(HttpStatus.CONFLICT, "evenement anterieur a l'engagement"),
  ACTIVITE_NON_ECHUE(HttpStatus.CONFLICT, "activite non echue"),
  ACTIVITE_DEJA_REGULARISEE(HttpStatus.CONFLICT, "activite deja regularisee"),
  FIN_AVANT_DEBUT(HttpStatus.CONFLICT, "fin avant debut"),
  FIN_APRES_BORNE(HttpStatus.CONFLICT, "fin apres borne"),
  SAISIE_CONCURRENTE(HttpStatus.CONFLICT, "saisie concurrente"),
  POINTAGE_IGNORE(HttpStatus.CONFLICT, "pointage ignore"),
  DATE_DE_SURVENUE_FUTURE(HttpStatus.BAD_REQUEST, "date de survenue future");

  private final HttpStatus status;
  private final String title;

  ErreurDAtelier(HttpStatus status, String title) {
    this.status = status;
    this.title = title;
  }

  @Override
  public String context() {
    return "atelier";
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
