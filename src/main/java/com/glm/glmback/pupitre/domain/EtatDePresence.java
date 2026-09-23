package com.glm.glmback.pupitre.domain;

/**
 * L'etat de presence d'un operateur, tel que le pupitre l'affiche.
 *
 * <p>
 * C'est l'expression propre a ce contexte de ce que l'atelier appelle du meme nom, comme {@code OperateurId},
 * {@code Nom} et {@code Prenom} : {@code atelier} est annote {@code BusinessContext} et ne s'importe pas. Seules les
 * valeurs sont reprises — l'automate de transition, lui, reste chez l'atelier, seul a ecrire.
 * </p>
 *
 * <p>
 * Le pupitre s'en sert pour n'offrir que les gestes de presence legaux, y compris hors ligne : proposer une pause a un
 * operateur deja en pause ne peut qu'aboutir a un refus du serveur, que l'ecran d'atelier ne decouvrirait qu'a la
 * reconnexion.
 * </p>
 */
public enum EtatDePresence {
  ABSENT,
  PRESENT,
  EN_PAUSE,
}
