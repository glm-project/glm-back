package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.DescriptionDElement;
import com.glm.glmback.syntheseheures.domain.ElementDeLaSynthese;
import com.glm.glmback.syntheseheures.domain.ReferenceDElement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(
  name = "RestElementDeLaSynthese",
  description = """
  Un element sur lequel l'operateur a travaille ou pointe dans la semaine, et le temps qu'il y a passe.

  Nom et categorie viennent du suivi, copies a l'engagement ; reference et description sont relues au referentiel, et
  absentes si l'element a ete supprime. Les durees se cumulent par element : une heure passee sur deux elements compte
  sur chacun.
  """
)
record RestElementDeLaSynthese(
  @Schema(description = "Identifiant de l'element de fabrication.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Categorie de l'element.", requiredMode = Schema.RequiredMode.REQUIRED) String type,
  @Schema(description = "Nom de l'element.", example = "PRD-2026-000015", requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  @Schema(description = "Reference de l'element, relue au referentiel.", example = "1015") String reference,
  @Schema(description = "Description de l'element, relue au referentiel.", example = "Carter de pompe") String description,
  @Schema(
    description = "Total sur l'element dans la semaine, NC comprise. Incomplet sans valeur si une activite a resoudre y contribue.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  RestDureeDeSynthese duree,
  @Schema(
    description = "Part de NC. Incomplete sans valeur si une NC a resoudre y contribue ; une contradiction de travail ne masque pas une NC certaine.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  RestDureeDeSynthese dureeNonConformite,
  @Schema(
    description = """
    Un couple par poste et nature distincts, dans l'ordre de premiere apparition, tire du travail comme des pointages :
    tout poste nomme au journal y trouve son libelle. Ce qui est pointe sans poste ni nature ne donne aucun couple.
    """,
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestPosteDeLElementDeLaSynthese> postes
) {
  static RestElementDeLaSynthese from(ElementDeLaSynthese element) {
    return new RestElementDeLaSynthese(
      element.element().id().uuid(),
      element.element().categorie().value(),
      element.element().nom().value(),
      element.reference().map(ReferenceDElement::value).orElse(null),
      element.description().map(DescriptionDElement::value).orElse(null),
      RestDureeDeSynthese.from(element.duree()),
      RestDureeDeSynthese.from(element.dureeNonConformite()),
      element.postes().stream().map(RestPosteDeLElementDeLaSynthese::from).toList()
    );
  }
}
