package com.glm.glmback.coutderevient.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Tout ce que ces operateurs ont fait, <strong>tous elements confondus</strong>.
 *
 * <p>
 * C'est ce qui distingue ce port du precedent, et c'est la seule facon de connaitre le diviseur : un nouveau pointage
 * sur un second element change la part deja attribuee au premier. Le temps reparti traverse les agregats, il ne peut
 * donc etre qu'une projection.
 * </p>
 *
 * <p>
 * La borne haute suffit a ecarter ce qui est arrive apres l'element : toute activite qui le recouvre a commence avant
 * sa fin. Aucune borne basse en revanche, sans quoi une activite ouverte avant l'element et fermee apres lui
 * echapperait aux deux.
 * </p>
 */
@FunctionalInterface
public interface OccupationDesOperateurs {
  List<SuiviDuTravail> suivis(Set<OperateurId> operateurs, Instant avant);
}
