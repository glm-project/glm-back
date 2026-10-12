package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.CategorieDActivite;
import com.glm.glmback.syntheseheures.domain.EtatDActivite;
import com.glm.glmback.syntheseheures.domain.LigneDuJournal;
import com.glm.glmback.syntheseheures.domain.MoisCalendaire;
import com.glm.glmback.syntheseheures.domain.NatureDOperation;
import com.glm.glmback.syntheseheures.domain.OperateurConnu;
import com.glm.glmback.syntheseheures.domain.PeriodeCalendaire;
import com.glm.glmback.syntheseheures.domain.PosteConnu;
import com.glm.glmback.syntheseheures.domain.SemaineCalendaire;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Les mots des exports du rapport d'activite, communs au classeur et au PDF. */
final class LibellesDuRapportDActivite {

  static final String TITRE = "Rapport d'activité";
  static final String SANS_POSTE = "Sans poste";
  static final String FIN_AUTOMATIQUE = "fin automatique";
  static final String PRESENCE = "Le temps pointé mesure le temps passé sur les éléments, pas la présence.";

  private static final Locale FRANCAIS = Locale.FRENCH;
  private static final DateTimeFormatter JOUR_ET_MOIS = DateTimeFormatter.ofPattern("d MMMM", FRANCAIS);
  private static final DateTimeFormatter JOUR_MOIS_ET_ANNEE = DateTimeFormatter.ofPattern("d MMMM yyyy", FRANCAIS);
  private static final DateTimeFormatter MOIS_ET_ANNEE = DateTimeFormatter.ofPattern("MMMM yyyy", FRANCAIS);

  private LibellesDuRapportDActivite() {}

  static String operateur(OperateurConnu operateur) {
    return "%s %s".formatted(operateur.prenom().value(), operateur.nom().value());
  }

  /** Semaine 38 · du 14 au 20 septembre 2026, ou Septembre 2026. */
  static String periode(PeriodeCalendaire periode) {
    return switch (periode) {
      case SemaineCalendaire semaine -> "Semaine %d · du %s au %s".formatted(
        semaine.numero(),
        debutDeSemaine(semaine.premierJour(), semaine.dernierJour()),
        semaine.dernierJour().format(JOUR_MOIS_ET_ANNEE)
      );
      case MoisCalendaire mois -> majuscule(mois.premierJour().format(MOIS_ET_ANNEE));
    };
  }

  /** Le debut ne repete que ce qui change : du 14 au 20 septembre, du 28 septembre au 4 octobre. */
  private static String debutDeSemaine(LocalDate lundi, LocalDate dimanche) {
    if (lundi.getYear() != dimanche.getYear()) {
      return lundi.format(JOUR_MOIS_ET_ANNEE);
    }
    if (lundi.getMonth() != dimanche.getMonth()) {
      return lundi.format(JOUR_ET_MOIS);
    }
    return String.valueOf(lundi.getDayOfMonth());
  }

  static String jourCourt(LocalDate jour) {
    return jour.getDayOfWeek().getDisplayName(TextStyle.SHORT, FRANCAIS);
  }

  static String poste(Optional<PosteConnu> poste) {
    return poste.map(connu -> connu.libelle().value()).orElse(SANS_POSTE);
  }

  static String nature(Optional<NatureDOperation> nature) {
    return nature.map(NatureDOperation::value).orElse(SANS_POSTE);
  }

  static String categorie(CategorieDActivite categorie) {
    return switch (categorie) {
      case TRAVAIL -> "Travail";
      case NON_CONFORMITE -> "Non-conformité";
    };
  }

  static List<String> mentions(LigneDuJournal ligne) {
    List<String> mentions = new ArrayList<>();
    if (ligne.etat() == EtatDActivite.TERMINEE_AUTOMATIQUEMENT) {
      mentions.add(FIN_AUTOMATIQUE);
    }
    if (ligne.etat() == EtatDActivite.EN_COURS) {
      mentions.add("en cours");
    }
    if (ligne.simultanee()) {
      mentions.add("en même temps qu'un autre pointage");
    }
    return List.copyOf(mentions);
  }

  /** Lefèvre Martin, semaine 38 : activite-lefevre-martin-S38-2026. Sans accent ni espace, pour tout systeme. */
  static String nomDeFichier(OperateurConnu operateur, PeriodeCalendaire periode) {
    String identite = Normalizer.normalize("%s-%s".formatted(operateur.nom().value(), operateur.prenom().value()), Normalizer.Form.NFD)
      .replaceAll("\\p{M}", "")
      .toLowerCase(Locale.ROOT)
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "");
    return "activite-%s-%s".formatted(identite, suffixe(periode));
  }

  private static String suffixe(PeriodeCalendaire periode) {
    return switch (periode) {
      case SemaineCalendaire semaine -> "S%02d-%d".formatted(semaine.numero(), semaine.annee());
      case MoisCalendaire mois -> "%d-%02d".formatted(mois.annee(), mois.mois());
    };
  }

  private static String majuscule(String texte) {
    return texte.substring(0, 1).toUpperCase(FRANCAIS) + texte.substring(1);
  }
}
