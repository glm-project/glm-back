package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.Application;
import com.glm.glmback.atelier.domain.Ecart;
import com.glm.glmback.atelier.domain.GesteDAtelier;
import com.glm.glmback.atelier.domain.GesteDePresence;
import com.glm.glmback.atelier.domain.MotifDeMiseEnAttente;
import com.glm.glmback.atelier.domain.PointageEnAttente;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.TraitementDuPointage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  Un geste du pupitre qu'on ne sait rattacher a rien, conserve tel quel et hors de tous les calculs. Il sort de la
  liste des que le gestionnaire l'applique ou l'ecarte.
  """
)
record RestPointageEnAttente(
  @Schema(description = "Identifiant attribue par le serveur, pour l'appliquer ou l'ecarter.", requiredMode = Schema.RequiredMode.REQUIRED)
  UUID id,
  @Schema(description = "Identifiant du geste envoye par le pupitre.", requiredMode = Schema.RequiredMode.REQUIRED) UUID evenement,
  @Schema(
    description = """
    OPERATEUR_INCONNU, POSTE_INCONNU, ELEMENT_INCONNU : referentiel du pupitre perime. GESTE_HORS_SEQUENCE : rejoue dans
    le desordre, ou date dans une journee deja fermee. IDENTIFIANT_REUTILISE : identifiant deja pris par un autre geste.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  MotifDeMiseEnAttente motif,
  @Schema(description = "PRESENCE ou ATELIER.", requiredMode = Schema.RequiredMode.REQUIRED) NatureDuGeste nature,
  @Schema(
    description = "ARRIVEE, PAUSE, REPRISE ou DEPART pour la presence ; DEBUT, NON_CONFORMITE ou FIN pour l'atelier.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  String type,
  @Schema(description = "L'identifiant de l'operateur envoye, meme inconnu.", requiredMode = Schema.RequiredMode.REQUIRED) UUID operateurId,
  @Schema(description = "L'operateur, resolu au referentiel ; absent s'il est inconnu.") RestOperateur operateur,
  @Schema(description = "Le suivi vise, pour un geste d'atelier.") UUID suivi,
  @Schema(description = "Le poste envoye, pour un geste d'atelier qui en porte un.") UUID poste,
  @Schema(description = "La date envoyee par le pupitre, absente s'il n'en a pas envoye.") Instant dateDeclaree,
  @Schema(description = "La date a laquelle le geste s'appliquera.", requiredMode = Schema.RequiredMode.REQUIRED) Instant dateDeSurvenue,
  @Schema(description = "La reception par le serveur.", requiredMode = Schema.RequiredMode.REQUIRED) Instant dateDeReception,
  @Schema(description = "Utilisateur connecte au pupitre.", requiredMode = Schema.RequiredMode.REQUIRED) String auteur,
  @Schema(description = "Ce qu'en a fait le gestionnaire ; absent tant qu'il est en attente.") RestTraitement traitement
) {
  static RestPointageEnAttente from(PointageEnAttente pointage, AnnuaireDAtelier annuaire) {
    Geste geste = switch (pointage.geste()) {
      case GesteDePresence presence -> new Geste(NatureDuGeste.PRESENCE, presence.type().name(), null, null);
      case GesteDAtelier atelier -> new Geste(
        NatureDuGeste.ATELIER,
        atelier.type().name(),
        atelier.suivi().uuid(),
        atelier.poste().map(PosteDeTravailId::uuid).orElse(null)
      );
    };

    return new RestPointageEnAttente(
      pointage.id().uuid(),
      pointage.evenementDuPupitre(),
      pointage.motif(),
      geste.nature(),
      geste.type(),
      pointage.geste().operateur().uuid(),
      RestOperateur.resolu(annuaire, pointage.geste().operateur()),
      geste.suivi(),
      geste.poste(),
      pointage.geste().dateDeclaree().orElse(null),
      pointage.dateDeSurvenue(),
      pointage.dateDeReception(),
      pointage.auteur().value(),
      pointage.traitement().map(RestTraitement::from).orElse(null)
    );
  }

  private record Geste(NatureDuGeste nature, String type, UUID suivi, UUID poste) {}

  enum NatureDuGeste {
    PRESENCE,
    ATELIER,
  }

  @Schema(name = "RestTraitementDuPointage", description = "Ce que le gestionnaire a fait d'un pointage en attente.")
  record RestTraitement(
    @Schema(description = "APPLIQUE ou ECARTE.", requiredMode = Schema.RequiredMode.REQUIRED) TypeDeTraitement type,
    @Schema(description = "Le gestionnaire.", requiredMode = Schema.RequiredMode.REQUIRED) String auteur,
    @Schema(description = "L'instant du traitement.", requiredMode = Schema.RequiredMode.REQUIRED) Instant date,
    @Schema(description = "Le motif, pour un pointage ecarte.") String motif
  ) {
    static RestTraitement from(TraitementDuPointage traitement) {
      return switch (traitement) {
        case Application application -> new RestTraitement(
          TypeDeTraitement.APPLIQUE,
          application.auteur().value(),
          application.date(),
          null
        );
        case Ecart ecart -> new RestTraitement(TypeDeTraitement.ECARTE, ecart.auteur().value(), ecart.date(), ecart.motif().value());
      };
    }
  }

  enum TypeDeTraitement {
    APPLIQUE,
    ECARTE,
  }
}
