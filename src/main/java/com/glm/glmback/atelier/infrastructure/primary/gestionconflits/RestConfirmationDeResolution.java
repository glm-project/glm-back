package com.glm.glmback.atelier.infrastructure.primary.gestionconflits;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.glm.glmback.atelier.application.gestionconflits.ResultatDActe;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes(
  {
    @JsonSubTypes.Type(value = RestConfirmationDeResolution.Enregistree.class, name = "ENREGISTREE"),
    @JsonSubTypes.Type(value = RestConfirmationDeResolution.NonAttestee.class, name = "NON_ATTESTEE"),
  }
)
@Schema(
  discriminatorProperty = "kind",
  discriminatorMapping = {
    @DiscriminatorMapping(value = "ENREGISTREE", schema = RestConfirmationDeResolution.Enregistree.class),
    @DiscriminatorMapping(value = "NON_ATTESTEE", schema = RestConfirmationDeResolution.NonAttestee.class),
  },
  oneOf = { RestConfirmationDeResolution.Enregistree.class, RestConfirmationDeResolution.NonAttestee.class }
)
sealed interface RestConfirmationDeResolution {
  static RestConfirmationDeResolution from(ResultatDActe resultat, AnnuaireDAtelier annuaire) {
    return new Enregistree(RestRecuDActe.from(resultat.recu()), RestDossierConflit.from(resultat.dossier(), annuaire));
  }

  @Schema(name = "RestConfirmationEnregistree")
  record Enregistree(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestRecuDActe recu,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestDossierConflit dossier
  ) implements RestConfirmationDeResolution {}

  @Schema(name = "RestConfirmationNonAttestee", description = "Aucun recu visible ; l absence ne prouve pas l echec de la commande.")
  record NonAttestee() implements RestConfirmationDeResolution {}
}
