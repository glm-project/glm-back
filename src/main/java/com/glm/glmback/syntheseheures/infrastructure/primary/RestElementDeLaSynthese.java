package com.glm.glmback.syntheseheures.infrastructure.primary;

import com.glm.glmback.syntheseheures.domain.DescriptionDElement;
import com.glm.glmback.syntheseheures.domain.ElementDeLaSynthese;
import com.glm.glmback.syntheseheures.domain.ReferenceDElement;
import com.glm.glmback.syntheseheures.domain.TypeDElement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Schema(
  name = "RestElementDeLaSynthese",
  description = """
  Un element sur lequel l'operateur a travaille ou pointe dans la semaine, et le temps qu'il y a passe.

  Nom et type viennent du suivi, copies a l'engagement ; reference et description sont relues au referentiel, et
  absentes si l'element a ete supprime. Les durees se cumulent par element : une heure passee sur deux elements compte
  sur chacun.
  """
)
record RestElementDeLaSynthese(
  @Schema(description = "Identifiant de l'element de fabrication.", requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
  @Schema(description = "Type de l'element.", requiredMode = Schema.RequiredMode.REQUIRED) TypeDElement type,
  @Schema(description = "Nom de l'element.", example = "PRD-2026-000015", requiredMode = Schema.RequiredMode.REQUIRED) String nom,
  @Schema(description = "Reference de l'element, relue au referentiel.", example = "1015") String reference,
  @Schema(description = "Description de l'element, relue au referentiel.", example = "Carter de pompe") String description,
  @Schema(
    description = "Duree comptabilisee sur l'element dans la semaine, non conformite comprise.",
    example = "PT15H30M",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration duree,
  @Schema(
    description = "Part comptabilisee de la duree passee en non conformite.",
    example = "PT50M",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  Duration dureeNonConformite,
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
      element.element().type(),
      element.element().nom().value(),
      element.reference().map(ReferenceDElement::value).orElse(null),
      element.description().map(DescriptionDElement::value).orElse(null),
      element.duree(),
      element.dureeNonConformite(),
      element.postes().stream().map(RestPosteDeLElementDeLaSynthese::from).toList()
    );
  }
}
