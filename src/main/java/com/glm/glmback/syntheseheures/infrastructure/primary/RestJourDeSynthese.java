package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.JourDeSynthese;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Schema(
  description = """
  Un jour du calendrier de l'entreprise, son journal brut, sa duree de presence et son temps operationnel.

  Les sept jours sont toujours rendus, meme vides : un trou obligerait le lecteur a deviner s'il manque une journee
  ou si l'operateur n'etait pas la.
  """
)
record RestJourDeSynthese(
  @Schema(description = "Date du jour, dans le fuseau de l'entreprise.", example = "2026-05-11") LocalDate jour,
  @Schema(
    description = """
    Le journal brut de ce jour : les pointages de presence, et tous les pointages d'element non annules de l'operateur
    dates de ce jour, meme hors de toute journee. Dans l'ordre des heures ; a instant egal, l'arrivee, puis les
    pointages d'element, puis le depart ; deux elements pointes au meme instant se departagent par l'identifiant de
    l'element, et un meme element garde l'ordre de son journal.
    """
  )
  List<RestPointage> pointages,
  @Schema(description = "Duree de presence pointee du jour, de l'arrivee au depart, pause comprise.", example = "PT8H") Duration duree,
  @Schema(
    description = """
    Duree presumee du jour : le temps d'une journee sans depart, abandonnee au-dela de l'amplitude maximale, entre son
    arrivee et son dernier fait connu. A confirmer par une regularisation du depart.
    """,
    example = "PT3H"
  )
  Duration dureePresumee,
  @Schema(
    description = """
    Temps operationnel pointe du jour : la somme des periodes de travail closes de ce jour, reduites a la presence. Il se
    cumule par element, et peut donc depasser la presence. Un travail en cours ne compte rien.
    """,
    example = "PT13H40M",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration dureeOperationnelle,
  @Schema(
    description = "Temps operationnel presume du jour : le travail borne par la fin presumee d'une journee abandonnee.",
    example = "PT0S",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration dureeOperationnellePresumee
) {
  static RestJourDeSynthese from(JourDeSynthese jour) {
    return new RestJourDeSynthese(
      jour.jour(),
      jour.pointages().stream().map(RestPointage::from).toList(),
      jour.duree(),
      jour.dureePresumee(),
      jour.dureeOperationnelle(),
      jour.dureeOperationnellePresumee()
    );
  }
}
