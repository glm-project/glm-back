package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.CoutHoraire;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.NatureDOperation;
import com.glm.glmback.atelier.domain.TauxHoraire;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.shared.authentication.application.HourlyRatesAuthorization;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(
  description = """
  Un evenement du journal d'un element engage.

  L'horodatage est bitemporel : heure du fait et heure de son enregistrement. Une pause s'y lit par une fin,
  puis une ouverture a la reprise.
  """
)
public record RestEvenementDAtelier(
  @Schema(description = "Identifiant de l'evenement.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(
    description = """
    Nature du fait. DEBUT, NON_CONFORMITE et FIN sont les gestes que le pupitre pointe ; la reprise du travail apres une non
    conformite se pointe DEBUT. RETRAIT et CHANGEMENT_DE_POSTE sont des corrections du gestionnaire, qui ciblent une
    activite (`activiteVisee`), sont datees du moment de la correction et ne se pointent pas.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  TypeDEvenementDAtelier type,
  @Schema(
    description = """
    Identite de l'activite qu'ouvre un debut ou une non conformite, absente pour une fin. C'est l'identifiant du
    pointage ouvrant.
    """
  )
  UUID activite,
  @Schema(
    description = """
    Identite de l'activite qu'un fait du gestionnaire cible : la fin d'une regularisation ou d'un ajout, un retrait, un
    changement de poste. Absente pour un pointage.
    """
  )
  UUID activiteVisee,
  @Schema(
    description = "Identite brute de l'operateur, conservee meme si la fiche est absente.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  UUID operateurId,
  @Schema(description = "Operateur dont le temps est affecte, absent si la fiche n'est plus resolue au referentiel.")
  RestOperateur operateur,
  @Schema(description = "Identite brute du poste, absente uniquement lorsqu'aucun poste n'a ete pointe.") UUID posteId,
  @Schema(description = "Poste de travail, toujours facultatif.") RestPosteDeTravail poste,
  @Schema(description = "Nature de l'operation, recopiee du poste a la saisie. Simple axe d'agregation.") String nature,
  @Schema(
    description = "Cout horaire du poste, copie a la saisie. Absent si le poste n'est pas valorise ou si aucun poste n'est fourni. Reserve au GESTIONNAIRE, absent pour les autres roles.",
    example = "45.50"
  )
  BigDecimal coutHoraire,
  @Schema(
    description = "Taux horaire de l'operateur, copie a la saisie. Absent si l'operateur n'est pas valorise. Reserve au GESTIONNAIRE, absent pour les autres roles.",
    example = "22.00"
  )
  BigDecimal tauxHoraire,
  @Schema(description = "Utilisateur ayant saisi l'evenement.", example = "dupont", requiredMode = Schema.RequiredMode.REQUIRED)
  String auteur,
  @Schema(description = "Heure metier a laquelle le fait a eu lieu.", requiredMode = Schema.RequiredMode.REQUIRED) Instant dateDeSurvenue,
  @Schema(description = "Heure a laquelle la saisie a ete enregistree.", requiredMode = Schema.RequiredMode.REQUIRED)
  Instant dateDEnregistrement,
  @Schema(
    description = """
    Vrai pour tout fait que le gestionnaire ecrit : la fin d'une regularisation, l'ouverture et la fin d'une activite qu'il
    ajoute, un retrait, un changement de poste.
    Un pointage ne l'est jamais, meme rejoue hors ligne avec l'heure de son geste : une saisie differee se lit a l'ecart
    entre dateDeSurvenue et dateDEnregistrement.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  boolean estUneRegularisation
) {
  public static RestEvenementDAtelier from(EvenementDAtelier evenement, AnnuaireDAtelier annuaire) {
    return new RestEvenementDAtelier(
      evenement.id().uuid(),
      evenement.type(),
      evenement.activite().map(ActiviteId::uuid).orElse(null),
      evenement.activiteVisee().map(ActiviteId::uuid).orElse(null),
      evenement.operateur().uuid(),
      RestOperateur.resolu(annuaire, evenement.operateur()),
      evenement
        .poste()
        .map(poste -> poste.uuid())
        .orElse(null),
      RestPosteDeTravail.resolu(annuaire, evenement.poste()),
      evenement.nature().map(NatureDOperation::libelle).orElse(null),
      HourlyRatesAuthorization.disclose(evenement.coutHoraire().map(CoutHoraire::value)),
      HourlyRatesAuthorization.disclose(evenement.tauxHoraire().map(TauxHoraire::value)),
      evenement.auteur().value(),
      evenement.dateDeSurvenue(),
      evenement.dateDEnregistrement(),
      evenement.estUneRegularisation()
    );
  }
}
