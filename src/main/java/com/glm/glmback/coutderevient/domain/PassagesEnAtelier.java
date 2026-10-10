package com.glm.glmback.coutderevient.domain;

import java.util.List;

/** Les passages en atelier d'un element, lus sur les suivis de l'atelier. */
@FunctionalInterface
public interface PassagesEnAtelier {
  List<PassageEnAtelier> passages(ElementId element);
}
