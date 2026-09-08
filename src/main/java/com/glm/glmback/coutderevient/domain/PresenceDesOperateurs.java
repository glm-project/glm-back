package com.glm.glmback.coutderevient.domain;

import java.util.List;
import java.util.Set;

/**
 * Les venues de ces operateurs sur la periode couverte par le rapport.
 */
@FunctionalInterface
public interface PresenceDesOperateurs {
  List<PresenceDUnOperateur> presences(Set<OperateurId> operateurs, Periode periode);
}
