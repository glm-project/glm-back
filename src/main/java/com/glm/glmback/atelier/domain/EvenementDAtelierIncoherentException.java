package com.glm.glmback.atelier.domain;

/**
 * Un fait dont le type et les activites qu'il ouvre ou cible ne s'accordent pas : un debut ou une non conformite ouvre
 * une activite et porte sa duree maximale, une fin n'en ouvre aucune, et seule la fin d'une regularisation porte une
 * cible. Le contrat HTTP ne laisse pas passer ces combinaisons : c'est un defaut, jamais un cas d'usage.
 */
public final class EvenementDAtelierIncoherentException extends RuntimeException {

  public EvenementDAtelierIncoherentException(TypeDEvenementDAtelier type, OrigineDuPointage origine) {
    super(
      (
        "Le fait %s d'origine %s est incoherent : un debut ou une non conformite ouvre une activite et porte sa duree "
        + "maximale, une fin n'en ouvre aucune, et seule la fin d'une regularisation porte une cible"
      ).formatted(type, origine)
    );
  }
}
