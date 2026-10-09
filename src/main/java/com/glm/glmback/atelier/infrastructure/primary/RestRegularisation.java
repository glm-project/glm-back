package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  La fin d'une activite echue, regularisee par le gestionnaire a l'heure ou elle a reellement eu lieu.

  L'operateur, le poste et le type du fait se deduisent de l'activite : la saisie ne les porte pas. L'evenement produit
  est une fin qui vise l'activite, porte `dateDeSurvenue` (fournie ici) et `dateDEnregistrement` (l'instant courant), et
  reste une regularisation (`estUneRegularisation`) meme saisie a l'heure du fait : c'est l'acte qui la fait, jamais
  l'ecart entre les deux dates.
  """
)
record RestRegularisation(
  @Schema(
    description = """
    Identifiant de la saisie, genere par le client une fois par saisie et conserve d'un renvoi a l'autre. Un identifiant
    deja present au journal est un renvoi : il repond 200, sans rien ecrire, avant toute regle.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  UUID id,

  @Schema(
    description = "L'activite dont la fin est regularisee, designee par l'identifiant de son pointage ouvrant d'origine.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  UUID activite,

  @Schema(
    description = """
    Heure metier a laquelle la fin a reellement eu lieu : ni dans le futur, ni avant le debut de l'activite, ni apres sa
    borne (`borneDeFin` du dossier). Elle peut depasser l'echeance de l'activite.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  Instant dateDeSurvenue
) {
  RegularisationAEnregistrer toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .evenement(new EvenementDAtelierId(id))
      .activite(new ActiviteId(activite))
      .auteur(auteur)
      .dateDeSurvenue(dateDeSurvenue);
  }
}
