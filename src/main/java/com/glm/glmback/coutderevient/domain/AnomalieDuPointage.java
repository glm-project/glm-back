package com.glm.glmback.coutderevient.domain;

/**
 * Ce qui rend un pointage suspect ou incomplet dans le detail d'une ligne.
 *
 * <p>
 * Une fin automatique est comptee mais signalee : aucune fin n'a ete pointee. Un pointage a resoudre n'a ni duree ni
 * montant. Un partage inconnu laisse la machine chiffree mais pas la main d'oeuvre, qu'un pointage a resoudre de
 * l'operateur sur un autre poste empeche de partager.
 * </p>
 */
public enum AnomalieDuPointage {
  FIN_AUTOMATIQUE,
  A_RESOUDRE,
  PARTAGE_INCONNU,
}
