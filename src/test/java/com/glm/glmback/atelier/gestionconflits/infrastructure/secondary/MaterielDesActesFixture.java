package com.glm.glmback.atelier.gestionconflits.infrastructure.secondary;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.LibelleDePoste;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.gestionconflits.domain.ActeDeResolution;
import java.math.BigDecimal;
import java.util.Optional;

final class MaterielDesActesFixture {

  private MaterielDesActesFixture() {}

  static SuiviDAtelier suiviAvecTransitionSurFraiseuse() {
    var debut = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    return suiviDAtelierEngage().enregistre(debut).enregistre(passageEnTravailDe(debut).a(LE_10_MAI_2026_A_12H));
  }

  static ActeDeResolution correctionDeTransitionEnFin(SuiviDAtelier suivi) {
    var fin = RegularisationAEnregistrer.builder()
      .suivi(suivi.id())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activiteVisee(suivi.journal().evenements().getFirst().activite())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(LE_10_MAI_2026_A_12H);
    return new ActeDeResolution.Correction(
      new CorrectionAEnregistrer(suivi.journal().evenements().getLast().id(), MOTIF_ERREUR_DE_SAISIE, fin),
      "2026-05-10T12:00:00Z"
    );
  }

  static OperateurConnu dupontAvecTaux(BigDecimal taux) {
    return OperateurConnu.builder().id(OPERATEUR_ID_DUPONT).nom(NOM_DUPONT).prenom(PRENOM_JEAN).tauxHoraire(taux);
  }

  static PosteConnu fraiseuseAvecCout(BigDecimal cout) {
    return PosteConnu.builder().id(POSTE_ID_FRAISEUSE_1).libelle(LIBELLE_FRAISEUSE_1).nature(NATURE_FRAISAGE).coutHoraire(cout);
  }

  static PosteConnu fraiseuseAvecNature(NatureDOperation nature) {
    return PosteConnu.builder()
      .id(POSTE_ID_FRAISEUSE_1)
      .libelle(LIBELLE_FRAISEUSE_1)
      .nature(nature)
      .coutHoraire(COUT_HORAIRE_FRAISEUSE_1.value());
  }

  static OperateurConnu dupontRenomme() {
    return OperateurConnu.builder().id(OPERATEUR_ID_DUPONT).nom(NOM_MARTIN).prenom(PRENOM_PAUL).tauxHoraire(TAUX_HORAIRE_DUPONT.value());
  }

  static PosteConnu fraiseuseRenommee() {
    return PosteConnu.builder()
      .id(POSTE_ID_FRAISEUSE_1)
      .libelle(new LibelleDePoste("Machine A"))
      .nature(NATURE_FRAISAGE)
      .coutHoraire(COUT_HORAIRE_FRAISEUSE_1.value());
  }
}
