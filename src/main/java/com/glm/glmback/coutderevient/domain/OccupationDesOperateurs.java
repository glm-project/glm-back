package com.glm.glmback.coutderevient.domain;

import java.util.List;
import java.util.Set;

/** Activites de ces operateurs recouvrant ou touchant la periode, tous elements confondus.
 * Les relais adjacents peuvent appartenir a une seule fenetre de partage. */
@FunctionalInterface
public interface OccupationDesOperateurs {
  List<ActiviteInterpretee> activites(Set<OperateurId> operateurs, Periode periode);
}
