package com.glm.glmback.elementdefabrication.domain;

public final class ReferenceDejaUtiliseeException extends RuntimeException {

  public ReferenceDejaUtiliseeException(Reference reference) {
    super("La reference %s est deja utilisee par un autre element de fabrication".formatted(reference.value()));
  }
}
