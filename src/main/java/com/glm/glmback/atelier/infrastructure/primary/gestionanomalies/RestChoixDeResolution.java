package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.CodeDeProposition;
import com.glm.glmback.atelier.domain.gestionanomalies.LectureDossierAnomalie;
import com.glm.glmback.atelier.domain.gestionanomalies.PropositionDeResolution;
import com.glm.glmback.atelier.domain.gestionanomalies.TypeDActeDeResolution;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Une hypothese structuree issue du diagnostic, sans motif et sans selection par defaut.")
final class RestChoixDeResolution {

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final CodeDeProposition code;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final TypeDActeDeResolution kind;

  @JsonProperty
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private final UUID pointage;

  @JsonProperty
  @Schema(
    description = "Le fait prerempli de l'acte : sans instant pour REGULARISER_FIN, que le gestionnaire date ; avec l'instant du geste tardif pour une correction.",
    oneOf = { RestFaitDeResolution.class, RestFaitARegulariser.class }
  )
  private final Object fait;

  private RestChoixDeResolution(PropositionDeResolution proposition, LectureDossierAnomalie dossier) {
    code = proposition.code();
    kind = code.kind();
    pointage = proposition.pointage().uuid();
    if (code == CodeDeProposition.REGULARISER_FIN) {
      var ouvrant = dossier.lecture().suivi().journal().evenement(proposition.pointage()).orElseThrow();
      fait = new RestFaitARegulariser(
        TypeDEvenementDAtelier.FIN,
        IntentionDePointage.FIN,
        proposition.activiteVisee().orElseThrow().uuid(),
        ouvrant.operateur().uuid(),
        ouvrant.poste().map(PosteDeTravailId::uuid).orElse(null)
      );
    } else if (kind == TypeDActeDeResolution.CORRECTION) {
      var source = dossier.lecture().suivi().journal().evenement(proposition.pointage()).orElseThrow();
      fait = new RestFaitDeResolution(
        source.type(),
        source.intention(),
        proposition.activiteVisee().orElseThrow().uuid(),
        source.operateur().uuid(),
        source.poste().map(PosteDeTravailId::uuid).orElse(null),
        source.dateDeSurvenue().toString()
      );
    } else {
      fait = null;
    }
  }

  static RestChoixDeResolution from(PropositionDeResolution proposition, LectureDossierAnomalie dossier) {
    return new RestChoixDeResolution(proposition, dossier);
  }
}
