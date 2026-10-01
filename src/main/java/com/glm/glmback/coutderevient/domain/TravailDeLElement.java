package com.glm.glmback.coutderevient.domain;

import java.util.List;

/** Activites interpretees de tous les passages de l'element en atelier. */
@FunctionalInterface
public interface TravailDeLElement {
  List<ActiviteInterpretee> activites(ElementId element);
}
