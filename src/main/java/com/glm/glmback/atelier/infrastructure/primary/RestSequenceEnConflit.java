package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.SequenceEnConflit;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(
  description = """
  Des pointages d'un meme operateur sur un meme poste qui se contredisent : une fin ou une transition qui vise une
  activite deja remplacee ou terminee, pas encore ouverte ou annulee, ou une transition vers sa propre categorie.

  Rien n'est choisi a la place du gestionnaire : les activites de la sequence sont a resoudre, ni en cours ni terminees,
  sans fin ni duree, et l'echeance ne les termine pas. Il la resout en corrigeant ou en annulant les faits concernes ;
  elle disparait au recalcul des que les faits redeviennent coherents. Elle se deduit du journal a chaque lecture et
  n'est jamais stockee.
  """
)
record RestSequenceEnConflit(
  @Schema(description = "Operateur des pointages en conflit, absent si la fiche n'est plus resolue au referentiel.")
  RestOperateur operateur,
  @Schema(description = "Poste de travail des pointages en conflit, facultatif.") RestPosteDeTravail poste,
  @Schema(
    description = """
    Les activites a resoudre, par l'identite de leur pointage ouvrant d'origine, dans l'ordre de leur ouverture : aucune
    fin ni transition ne doit plus les viser. Vide quand le seul geste en conflit vise une ouverture annulee.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<UUID> activites,
  @Schema(
    description = """
    Les pointages de la sequence, dans l'ordre du journal : les gestes contradictoires et ceux qui ouvrent ou visent ses
    activites. Un pointage publie dont l'identifiant figure ici est enregistre et conserve en conflit, pas refuse.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<UUID> pointages
) {
  static RestSequenceEnConflit from(SequenceEnConflit sequence, AnnuaireDAtelier annuaire) {
    return new RestSequenceEnConflit(
      RestOperateur.resolu(annuaire, sequence.operateur()),
      RestPosteDeTravail.resolu(annuaire, sequence.poste()),
      sequence.activites().stream().map(ActiviteId::uuid).toList(),
      sequence.pointages().stream().map(EvenementDAtelierId::uuid).toList()
    );
  }
}
