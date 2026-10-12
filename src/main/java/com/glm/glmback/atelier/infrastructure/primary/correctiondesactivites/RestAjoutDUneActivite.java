package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.correctiondesactivites.AjoutDUneActivite;
import com.glm.glmback.atelier.domain.correctiondesactivites.PlageDAjout;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Schema(
  description = """
  Une activite oubliee que le gestionnaire ajoute a un suivi, du debut a la fin qu'il donne.

  Elle s'ecrit en deux faits d'origine regularisation : une ouverture (un debut, ou une non conformite selon la
  categorie) a l'heure du debut, et une fin a l'heure de la fin qui vise l'activite. La nature d'operation, le cout
  horaire et le taux horaire sont recopies du poste et de l'operateur, comme pour un pointage ; la duree maximale
  d'activite en vigueur est copiee sur l'ouverture. La fin peut tomber le lendemain du debut : le chevauchement se verifie
  sur toute l'activite, sur la cle (operateur, poste) de ce suivi.
  """
)
record RestAjoutDUneActivite(
  @Schema(
    description = """
    Identifiant de la saisie, genere par le client une fois par saisie et conserve d'un renvoi a l'autre. C'est aussi
    l'identifiant de l'ouverture, donc de l'activite ajoutee. Un identifiant deja present au journal est un renvoi : il
    repond 200, sans rien ecrire, avant toute regle.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  UUID id,

  @Schema(description = "Identifiant de l'operateur dont le temps est affecte.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID operateur,

  @Schema(description = "Identifiant du poste de travail, sur lequel l'operateur doit etre habilite. Facultatif, comme pour un pointage.")
  UUID poste,

  @Schema(
    description = "TRAVAIL ou NON_CONFORMITE : le type de l'activite, qui decide du fait d'ouverture (DEBUT ou NON_CONFORMITE).",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  CategorieDActivite categorie,

  @Schema(
    description = "Heure metier a laquelle l'activite a commence : pas avant l'engagement du suivi.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  Instant debut,

  @Schema(
    description = """
    Heure metier a laquelle l'activite a fini : posterieure au debut, ni dans le futur, ni apres la cloture du suivi. Elle
    peut tomber le lendemain du debut.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  @NotNull
  Instant fin
) {
  AjoutDUneActivite toDomain(SuiviDAtelierId suivi, Auteur auteur) {
    return AjoutDUneActivite.builder()
      .suivi(suivi)
      .evenement(new EvenementDAtelierId(id))
      .operateur(new OperateurId(operateur))
      .poste(Optional.ofNullable(poste).map(PosteDeTravailId::new))
      .categorie(categorie)
      .plage(new PlageDAjout(debut, fin))
      .auteur(auteur);
  }
}
