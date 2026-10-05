package com.glm.glmback.atelier.infrastructure.secondary.gestionanomalies;

import static com.glm.glmback.atelier.application.gestionanomalies.ResolutionFixture.*;
import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.atelier.domain.gestionanomalies.TypeDActeDeResolution.*;

import com.glm.glmback.atelier.application.gestionanomalies.PropositionAConfirmer;
import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.MotifDAnnulation;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.ActeDeResolution;
import com.glm.glmback.atelier.domain.gestionanomalies.TypeDActeDeResolution;
import java.time.Instant;
import java.util.Optional;

public final class ReprisesDActesFixture {

  private static final MotifDAnnulation MOTIF_POINTAGE_EN_DOUBLE = new MotifDAnnulation("Pointage en double");

  private ReprisesDActesFixture() {}

  enum ChampDeDemande {
    MOTIF,
    POINTAGE,
    TYPE,
    INTENTION,
    CIBLE,
    OPERATEUR,
    POSTE,
    INSTANT,
    REPRESENTATION_DE_L_INSTANT,
  }

  enum CasDeRejeu {
    ANNULATION_MOTIF(ANNULATION, ChampDeDemande.MOTIF),
    ANNULATION_POINTAGE(ANNULATION, ChampDeDemande.POINTAGE),
    CORRECTION_MOTIF(CORRECTION, ChampDeDemande.MOTIF),
    CORRECTION_POINTAGE(CORRECTION, ChampDeDemande.POINTAGE),
    CORRECTION_TYPE(CORRECTION, ChampDeDemande.TYPE),
    CORRECTION_INTENTION(CORRECTION, ChampDeDemande.INTENTION),
    CORRECTION_CIBLE(CORRECTION, ChampDeDemande.CIBLE),
    CORRECTION_OPERATEUR(CORRECTION, ChampDeDemande.OPERATEUR),
    CORRECTION_POSTE(CORRECTION, ChampDeDemande.POSTE),
    CORRECTION_INSTANT(CORRECTION, ChampDeDemande.INSTANT),
    CORRECTION_REPRESENTATION_DE_L_INSTANT(CORRECTION, ChampDeDemande.REPRESENTATION_DE_L_INSTANT),
    REGULARISATION_TYPE(REGULARISATION, ChampDeDemande.TYPE),
    REGULARISATION_INTENTION(REGULARISATION, ChampDeDemande.INTENTION),
    REGULARISATION_CIBLE(REGULARISATION, ChampDeDemande.CIBLE),
    REGULARISATION_OPERATEUR(REGULARISATION, ChampDeDemande.OPERATEUR),
    REGULARISATION_POSTE(REGULARISATION, ChampDeDemande.POSTE),
    REGULARISATION_INSTANT(REGULARISATION, ChampDeDemande.INSTANT),
    REGULARISATION_REPRESENTATION_DE_L_INSTANT(REGULARISATION, ChampDeDemande.REPRESENTATION_DE_L_INSTANT);

    private final TypeDActeDeResolution kind;
    private final ChampDeDemande champ;

    CasDeRejeu(TypeDActeDeResolution kind, ChampDeDemande champ) {
      this.kind = kind;
      this.champ = champ;
    }

    PropositionAConfirmer originale(SuiviDAtelier suivi) {
      var proposition = switch (kind) {
        case ANNULATION -> propositionDAnnulationDeTransition(suivi);
        case CORRECTION -> propositionDeCorrectionDeTransition(suivi);
        case REGULARISATION -> propositionDeRegularisationDeFin(suivi);
      };
      if (kind == ANNULATION) {
        return proposition;
      }
      var instant = "2026-05-10T16:00:00.123456789+02:00";
      var ouverture = champ == ChampDeDemande.OPERATEUR || champ == ChampDeDemande.POSTE;
      var fait = RegularisationAEnregistrer.builder()
        .suivi(suivi.id())
        .type(TypeDEvenementDAtelier.NON_CONFORMITE)
        .intention(ouverture ? IntentionDePointage.OUVERTURE : IntentionDePointage.TRANSITION)
        .activiteVisee(ouverture ? Optional.empty() : suivi.journal().evenements().getFirst().activite())
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.empty())
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(Instant.parse(instant));
      var acte =
        kind == CORRECTION
          ? new ActeDeResolution.Correction(
              new CorrectionAEnregistrer(proposition.adresse().pointage(), MOTIF_ERREUR_DE_SAISIE, fait),
              instant
            )
          : new ActeDeResolution.Regularisation(fait, instant);
      return avecActe(new PropositionAvecActe(proposition, acte));
    }

    PropositionAConfirmer modifiee(DemandeSurSuivi donnees) {
      var proposition = donnees.proposition();
      var suivi = donnees.suivi();
      var acte = switch (proposition.acte()) {
        case ActeDeResolution.Annulation annulation -> new ActeDeResolution.Annulation(
          new AnnulationAEnregistrer(
            suivi.id(),
            champ == ChampDeDemande.POINTAGE ? suivi.journal().evenements().getFirst().id() : annulation.commande().evenement(),
            annulation.commande().auteur(),
            champ == ChampDeDemande.MOTIF ? MOTIF_POINTAGE_EN_DOUBLE : annulation.commande().motif()
          )
        );
        case ActeDeResolution.Correction correction -> new ActeDeResolution.Correction(
          new CorrectionAEnregistrer(
            champ == ChampDeDemande.POINTAGE ? suivi.journal().evenements().getFirst().id() : correction.commande().evenement(),
            champ == ChampDeDemande.MOTIF ? MOTIF_POINTAGE_EN_DOUBLE : correction.commande().motif(),
            faitModifie(new FaitSurSuivi(correction.commande().remplacement(), suivi))
          ),
          instantModifie(correction.instant())
        );
        case ActeDeResolution.Regularisation regularisation -> new ActeDeResolution.Regularisation(
          faitModifie(new FaitSurSuivi(regularisation.commande(), suivi)),
          instantModifie(regularisation.instant())
        );
      };
      return avecActe(new PropositionAvecActe(proposition, acte));
    }

    private RegularisationAEnregistrer faitModifie(FaitSurSuivi donnees) {
      var fait = donnees.fait();
      return RegularisationAEnregistrer.builder()
        .suivi(fait.suivi())
        .type(champ == ChampDeDemande.TYPE ? TypeDEvenementDAtelier.DEBUT : fait.type())
        .intention(champ == ChampDeDemande.INTENTION ? IntentionDePointage.OUVERTURE : fait.intention())
        .activiteVisee(
          switch (champ) {
            case INTENTION -> Optional.empty();
            case CIBLE -> donnees.suivi().journal().evenements().getLast().activite();
            default -> fait.activiteVisee();
          }
        )
        .operateur(champ == ChampDeDemande.OPERATEUR ? OPERATEUR_ID_MARTIN : fait.operateur())
        .poste(champ == ChampDeDemande.POSTE ? Optional.of(POSTE_ID_FRAISEUSE_1) : fait.poste())
        .auteur(fait.auteur())
        .dateDeSurvenue(champ == ChampDeDemande.INSTANT ? fait.dateDeSurvenue().plusSeconds(60) : fait.dateDeSurvenue());
    }

    private String instantModifie(String instant) {
      return switch (champ) {
        case INSTANT -> Instant.parse(instant).plusSeconds(60).toString();
        case REPRESENTATION_DE_L_INSTANT -> Instant.parse(instant).toString();
        default -> instant;
      };
    }
  }

  private static PropositionAConfirmer avecActe(PropositionAvecActe donnees) {
    var proposition = donnees.proposition();
    return PropositionAConfirmer.builder()
      .commande(proposition.commande())
      .adresse(proposition.adresse())
      .revision(proposition.revision())
      .acte(donnees.acte())
      .evenement(proposition.evenement())
      .empreinteConsequences(proposition.empreinteConsequences());
  }

  record DemandeSurSuivi(PropositionAConfirmer proposition, SuiviDAtelier suivi) {}

  private record FaitSurSuivi(RegularisationAEnregistrer fait, SuiviDAtelier suivi) {}

  private record PropositionAvecActe(PropositionAConfirmer proposition, ActeDeResolution acte) {}
}
