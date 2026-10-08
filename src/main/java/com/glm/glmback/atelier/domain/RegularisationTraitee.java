package com.glm.glmback.atelier.domain;

/**
 * Le suivi apres une regularisation, et si elle n'etait qu'un renvoi : l'evenement figurait deja au journal.
 */
public record RegularisationTraitee(SuiviDAtelier suivi, boolean rejeu) {}
