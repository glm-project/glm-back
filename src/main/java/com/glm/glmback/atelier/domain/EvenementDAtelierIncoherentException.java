package com.glm.glmback.atelier.domain;

/**
 * Un fait dont le type et les activites qu'il ouvre ou cible ne s'accordent pas : un debut ou une non conformite ouvre
 * une activite et porte sa duree maximale, une fin n'en ouvre aucune et ne porte de cible que lorsqu'elle est regularisee, et un retrait ou un changement de poste
 * est un acte du gestionnaire qui vise toujours une activite. Le contrat HTTP ne laisse pas passer ces combinaisons : c'est un defaut, jamais un cas d'usage.
 */
public final class EvenementDAtelierIncoherentException extends RuntimeException {

  public EvenementDAtelierIncoherentException(TypeDEvenementDAtelier type, OrigineDuPointage origine) {
    super(
      (
        "Le fait %s d'origine %s est incoherent : un debut ou une non conformite ouvre une activite et porte sa duree "
        + "maximale, une fin n'en ouvre aucune et ne porte de cible que regularisee, "
        + "et un retrait ou un changement de poste est un acte du gestionnaire qui vise toujours une activite"
      ).formatted(type, origine)
    );
  }
}
