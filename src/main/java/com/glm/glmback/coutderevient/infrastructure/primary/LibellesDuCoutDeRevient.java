package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.Activite;
import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.CategorieDActivite;
import com.glm.glmback.coutderevient.domain.LigneDeCout;
import com.glm.glmback.coutderevient.domain.NatureDOperation;
import com.glm.glmback.coutderevient.domain.StatutDeLElement;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Les mots communs aux exports du cout de revient, pour qu'un classeur et un PDF disent la meme chose. */
final class LibellesDuCoutDeRevient {

  static final String SANS_POSTE = "Sans poste";

  private static final DateTimeFormatter CLOTURE = DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRENCH);

  private LibellesDuCoutDeRevient() {}

  /**
   * Un element termine dit quand, a l'heure de l'entreprise ; un element en cours fait du compte rendu une
   * photographie a l'instant de generation.
   */
  static String statut(StatutDeLElement statut, ZoneId fuseau) {
    return statut
      .termineLe()
      .map(cloture -> "Terminé le " + CLOTURE.format(cloture.atZone(fuseau)))
      .orElse("En cours");
  }

  static String nature(LigneDeCout ligne) {
    return ligne.nature().map(NatureDOperation::value).orElse(SANS_POSTE);
  }

  /** Un nom absent du referentiel laisse l'identifiant seul. */
  static String poste(Activite activite, AnnuaireDuCout annuaire) {
    return activite
      .poste()
      .map(poste ->
        annuaire
          .poste(poste)
          .map(nomme -> nomme.libelle().value())
          .orElse(poste.uuid().toString())
      )
      .orElse(SANS_POSTE);
  }

  static String operateur(Activite activite, AnnuaireDuCout annuaire) {
    return annuaire
      .operateur(activite.operateur())
      .map(nomme -> nomme.prenom().value() + " " + nomme.nom().value())
      .orElse(activite.operateur().uuid().toString());
  }

  static String categorie(CategorieDActivite categorie) {
    return switch (categorie) {
      case TRAVAIL -> "Travail";
      case NON_CONFORMITE -> "Non-conformité";
    };
  }
}
