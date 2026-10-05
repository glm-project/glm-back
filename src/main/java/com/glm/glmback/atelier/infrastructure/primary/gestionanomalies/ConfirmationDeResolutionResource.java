package com.glm.glmback.atelier.infrastructure.primary.gestionanomalies;

import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.application.gestionanomalies.ConfirmerLesActes;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/suivis/{suivi}/confirmations-de-resolution")
@Tag(name = "Atelier - anomalies de pointage")
class ConfirmationDeResolutionResource {

  private final ConfirmerLesActes confirmations;
  private final SuivisDAtelierApplicationService atelier;

  ConfirmationDeResolutionResource(ConfirmerLesActes confirmations, SuivisDAtelierApplicationService atelier) {
    this.confirmations = confirmations;
    this.atelier = atelier;
  }

  @PostMapping
  @Operation(summary = "Confirmer une proposition explicite une seule fois et rendre son recu avec le dossier courant")
  RestConfirmationDeResolution confirme(@PathVariable UUID suivi, @Valid @RequestBody RestConfirmationAEnregistrer demande) {
    var contexte = GestionnaireConnecte.get();
    var resultat = confirmations.confirmer(new SuiviDAtelierId(suivi), demande.toDomain(contexte.gestionnaire().auteur()), contexte);
    return RestConfirmationDeResolution.from(resultat, atelier.annuairePour(resultat.dossier().lecture().suivi()));
  }

  @GetMapping("/{commande}")
  @Operation(
    summary = "Verifier si une confirmation dispose d un recu durable et rendre le dossier courant",
    description = "NON_ATTESTEE indique seulement qu aucun recu n est visible. La commande peut encore etre en cours ; cette reponse ne prouve pas son echec."
  )
  RestConfirmationDeResolution verifie(@PathVariable UUID suivi, @PathVariable UUID commande) {
    return confirmations
      .verifier(new SuiviDAtelierId(suivi), commande, GestionnaireConnecte.get())
      .map(resultat -> RestConfirmationDeResolution.from(resultat, atelier.annuairePour(resultat.dossier().lecture().suivi())))
      .orElseGet(RestConfirmationDeResolution.NonAttestee::new);
  }
}
