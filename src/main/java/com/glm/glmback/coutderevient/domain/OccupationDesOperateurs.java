package com.glm.glmback.coutderevient.domain;

import java.util.List;
import java.util.Set;

/** Activites de ces operateurs recouvrant la periode, tous elements confondus. */
@FunctionalInterface
public interface OccupationDesOperateurs {
  List<ActiviteInterpretee> activites(Set<OperateurId> operateurs, Periode periode);
}
