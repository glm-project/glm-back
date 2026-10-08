package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.LigneDeCout;
import com.glm.glmback.coutderevient.domain.NatureDOperation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(
  description = """
  Une ligne du rapport : tout ce qui a ete fait sur l'element a une meme nature d'operation.

  La nature vient du poste de travail, jamais de la personne, et elle a ete copiee au moment de la saisie : un poste
  requalifie depuis ne requalifie pas les heures deja passees. Elle est absente quand le pointage n'a engage aucun
  poste, ce qui est le comportement nominal d'une entreprise sans parc machine.
  """
)
record RestLigneDeCout(
  @Schema(description = "Nature de l'operation, absente pour un pointage sans poste.", example = "Fraisage") String nature,
  @Schema(description = "Premier debut comptabilise, derniere fin certaine de cette nature si elle existe.") RestPeriode periode,
  @Schema(description = "Temps passe, bon travail et non conformite separes.") RestTempsPasse temps,
  @Schema(description = "Les periodes de reprise terminees et certaines, datees, dans l'ordre.") List<RestPeriode> nonConformites,
  @Schema(description = "Periodes terminees automatiquement, signalant une anomalie active.") List<RestPeriode> finsAutomatiques,
  @Schema(description = "Cout de la ligne, somme de montants deja au centime.") RestCout cout,
  @Schema(
    description = "Les pointages de la ligne dans l'ordre ou ils ont commence, de quoi justifier son cout.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestPointageDeCout> pointages
) {
  static RestLigneDeCout from(LigneDeCout ligne, AnnuaireDuCout annuaire) {
    return new RestLigneDeCout(
      ligne.nature().map(NatureDOperation::value).orElse(null),
      RestPeriode.from(ligne.periode()),
      RestTempsPasse.from(ligne.temps()),
      ligne.nonConformites().stream().map(RestPeriode::from).toList(),
      ligne.finsAutomatiques().stream().map(RestPeriode::from).toList(),
      RestCout.from(ligne.cout()),
      ligne
        .pointages()
        .stream()
        .map(pointage -> RestPointageDeCout.from(pointage, annuaire))
        .toList()
    );
  }
}
