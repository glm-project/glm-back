package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.domain.ArriveeAEnregistrer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDePresenceId;
import com.glm.glmback.atelier.domain.OperateurId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Schema(
  description = """
  L'arrivee, qui ouvre la journee de travail d'un operateur.

  L'operateur qui s'identifie le matin et le gestionnaire qui saisit apres coup une arrivee jamais pointee passent par
  le meme acte : seule `dateDeSurvenue` change.
  """
)
record RestArrivee(
  @Schema(description = "Identifiant durable du geste, genere par le pupitre.", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  UUID id,

  @Schema(description = "Identifiant de l'operateur qui arrive.", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull UUID operateur,

  @Schema(description = "Heure metier de l'arrivee. Absente, elle vaut l'instant present.") Instant dateDeSurvenue
) {
  ArriveeAEnregistrer toDomain(Auteur auteur) {
    return new ArriveeAEnregistrer(new OperateurId(operateur), auteur, Optional.ofNullable(dateDeSurvenue), new EvenementDePresenceId(id));
  }
}
