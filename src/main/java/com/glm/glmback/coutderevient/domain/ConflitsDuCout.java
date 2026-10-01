package com.glm.glmback.coutderevient.domain;

import java.util.List;
import java.util.Set;

/** Conflits de l'element et conflits susceptibles de modifier le partage humain. */
public interface ConflitsDuCout {
  List<SequenceEnConflit> deLElement(ElementId element);
  List<SequenceEnConflit> desOperateurs(Set<OperateurId> operateurs);
}
