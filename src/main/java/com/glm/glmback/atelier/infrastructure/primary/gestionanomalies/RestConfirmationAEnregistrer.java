package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.gestionanomalies.PropositionAConfirmer;
import com.glm.glmback.atelier.domain.Auteur;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Optional;
import java.util.UUID;

record RestConfirmationAEnregistrer(
  @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID commande,
  @NotNull @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestAdresseDossierAnomalie adresse,
  @NotNull @PositiveOrZero @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long revision,
  @NotNull @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RestActeDeResolution acte,
  @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String empreinteConsequences,
  UUID evenement
) {
  PropositionAConfirmer toDomain(Auteur auteur) {
    var suivi = new SuiviDAtelierId(adresse.suivi());
    return PropositionAConfirmer.builder()
      .commande(commande)
      .adresse(new AdresseDossierAnomalie(suivi, new EvenementDAtelierId(adresse.pointage())))
      .revision(new RevisionDuSuivi(revision))
      .acte(acte.toDomain(suivi, auteur))
      .evenement(Optional.ofNullable(evenement).map(EvenementDAtelierId::new))
      .empreinteConsequences(empreinteConsequences);
  }
}
