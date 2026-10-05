package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.application.gestionanomalies.ApercusDeResolution;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/suivis")
class ApercuDeResolutionResource {

  private final ApercusDeResolution apercus;
  private final SuivisDAtelierApplicationService suivis;

  ApercuDeResolutionResource(ApercusDeResolution apercus, SuivisDAtelierApplicationService suivis) {
    this.apercus = apercus;
    this.suivis = suivis;
  }

  @PostMapping("/{id}/conflits/{pointage}/apercus")
  @Operation(
    summary = "Previsualiser un acte de resolution",
    description = "Reserve au gestionnaire. Prepare le meme acte que sa confirmation, sans ecrire ni reserver son identifiant. Une revision perimee est refusee ; la proposition devient obsolete si ses consequences changent."
  )
  @ApiResponse(responseCode = "200", description = "L'acte exact, avant/apres, l'adresse et les metadonnees explicites de sa commande.")
  @ApiResponse(responseCode = "400", description = "Acte invalide.")
  @ApiResponse(responseCode = "409", description = "Apercu obsolete ou acte refuse.")
  RestApercuDeResolution apercu(@PathVariable UUID id, @PathVariable UUID pointage, @Valid @RequestBody RestDemandeDApercu demande) {
    var suivi = new SuiviDAtelierId(id);
    var contexte = GestionnaireConnecte.get();
    var apercu = apercus.apercu(
      demande.commande(),
      new AdresseDossierAnomalie(suivi, new EvenementDAtelierId(pointage)),
      new RevisionDuSuivi(demande.revision()),
      demande.acte().toDomain(suivi, contexte.gestionnaire().auteur()),
      contexte
    );
    return RestApercuDeResolution.from(
      apercu,
      suivis.annuairePour(apercu.avant().lecture().suivi()),
      suivis.annuairePour(apercu.apres().lecture().suivi())
    );
  }
}
