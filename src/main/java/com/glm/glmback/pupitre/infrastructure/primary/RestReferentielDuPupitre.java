package com.glm.glmback.pupitre.infrastructure.primary;

import com.glm.glmback.pupitre.domain.CategorieDElement;
import com.glm.glmback.pupitre.domain.ReferentielDuPupitre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
  name = "RestReferentielDuPupitre",
  description = """
  Tout ce que le pupitre met en cache pour continuer a fonctionner sans reseau, en un seul appel.

  Operateurs, habilitations, elements pointables et categories de produit sont relus dans une transaction unique, sans pagination.
  Les activites interpretees par atelier sont lues dans sa projection et leur expiration est jugee a genereLe.
  Les requetes successives ne garantissent pas un instantane face aux ecritures concurrentes.
  """
)
record RestReferentielDuPupitre(
  @Schema(
    description = "Instant ou le serveur a produit la reponse et evalue l'expiration. Il ne garantit pas un instantane commun face aux ecritures concurrentes.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Instant genereLe,
  @Schema(description = "Les operateurs designables, tries par nom puis prenom.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestOperateurDuPupitre> operateurs,
  @Schema(description = "Les elements pointables, tries par nom.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<RestSuiviDuPupitre> suivis,
  @Schema(
    description = "Les codes des categories de produit de l'entreprise, dans l'ordre choisi par le gestionnaire. Vide tant qu'aucune n'est declaree.",
    requiredMode = Schema.RequiredMode.REQUIRED,
    example = "[\"MOULE\", \"OF\"]"
  )
  List<String> categories,
  @Schema(
    description = """
    La duree maximale d'une activite, au format ISO 8601, telle que le gestionnaire l'a fixee (treize heures tant qu'il
    n'a rien fixe) : l'echeance d'une activite est son debut plus cette duree. Une activite que rien n'a terminee a cette
    echeance se termine automatiquement ; le pupitre hors ligne la lit ici plutot que de la coder.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED,
    example = "PT13H"
  )
  String dureeMaximaleDActivite
) {
  static RestReferentielDuPupitre from(ReferentielDuPupitre referentiel) {
    return new RestReferentielDuPupitre(
      referentiel.genereLe(),
      referentiel.operateurs().stream().map(RestOperateurDuPupitre::from).toList(),
      referentiel
        .suivis()
        .stream()
        .map(suivi -> RestSuiviDuPupitre.from(suivi, referentiel.genereLe()))
        .toList(),
      referentiel.categories().stream().map(CategorieDElement::value).toList(),
      referentiel.dureeMaximaleDActivite().value().toString()
    );
  }
}
