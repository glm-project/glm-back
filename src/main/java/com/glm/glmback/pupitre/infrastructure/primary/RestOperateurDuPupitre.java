package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.EtatDePresence;
import com.glm.glmback.pupitre.domain.Matricule;
import com.glm.glmback.pupitre.domain.OperateurDuPupitre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(
  name = "RestOperateurDuPupitre",
  description = """
  Un operateur designable au pupitre, et les postes sur lesquels il peut pointer.

  Pas de taux horaire, contrairement a GET /api/operateurs : un ecran d'atelier partage n'a aucune raison de recevoir
  ce que le rapport de cout de revient reserve au gestionnaire. Le matricule, lui, est ce que l'operateur tape : il
  designe, il ne prouve rien, et un operateur qui n'en a pas n'est pas designable.
  """
)
record RestOperateurDuPupitre(
  @Schema(
    description = "Identifiant de l'operateur. C'est lui, et lui seul, que le pupitre renvoie en pointant.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID id,
  @Schema(description = "Nom de famille.", example = "Dupont", requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  @Schema(description = "Prenom.", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED) String prenom,
  @Schema(description = "Code tape au pupitre pour se designer, absent si l'entreprise n'en attribue pas.", example = "049")
  String matricule,
  @Schema(
    description = """
    Etat de presence courant, celui de la journee en cours de l'operateur.

    ABSENT vaut pour un operateur sans journee en cours, et ne le retire pas de la liste : elle rend les operateurs
    designables, pas les operateurs presents. C'est cet etat qui dit a l'ecran d'atelier quelles commandes de
    presence sont legales, y compris hors ligne — proposer une pause a un operateur deja en pause ne peut
    qu'aboutir a un refus du serveur.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  EtatDePresence etat,
  @Schema(description = "Postes habilites, tries par libelle.", requiredMode = Schema.RequiredMode.REQUIRED) List<RestPosteDuPupitre> postes
) {
  static RestOperateurDuPupitre from(OperateurDuPupitre operateur) {
    return new RestOperateurDuPupitre(
      operateur.id().uuid(),
      operateur.nom().value(),
      operateur.prenom().value(),
      operateur.matricule().map(Matricule::value).orElse(null),
      operateur.etat(),
      operateur.postes().stream().map(RestPosteDuPupitre::from).toList()
    );
  }
}
