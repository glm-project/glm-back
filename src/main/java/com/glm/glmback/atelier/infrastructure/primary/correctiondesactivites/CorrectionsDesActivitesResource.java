package com.glm.glmback.atelier.infrastructure.primary.correctiondesactivites;

import com.glm.glmback.atelier.application.correctiondesactivites.CorrectionsDesActivitesApplicationService;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.correctiondesactivites.CorrectionTraitee;
import com.glm.glmback.atelier.infrastructure.primary.AuteurConnecte;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/suivis")
@Tag(
  name = "Atelier - correction des activites",
  description = """
  Le gestionnaire corrige la journee d'un operateur : il ajoute une activite oubliee, retire une activite pointee a tort,
  ou change son poste. Les trois corrections lui sont reservees ; la page de la journee les lit par
  `GET /api/atelier/operateurs/{operateurId}/journees/{jour}`.

  Une correction n'efface rien et ne modifie aucun fait du journal : elle y ajoute des faits d'origine regularisation, qui
  portent chacun leur auteur et leur horodatage. Un ajout ecrit une ouverture et une fin datees de l'heure qu'elles
  etablissent ; un retrait (`RETRAIT`) et un changement de poste (`CHANGEMENT_DE_POSTE`) sont dates du moment de la
  correction, visent leur activite par `activiteVisee`, et ne peuvent suivre la cloture du suivi. Aucune ne passe par la
  regle de reception : celle-ci juge ensuite les pointages sur le journal tel qu'il est apres les corrections. Le releve
  des heures, la supervision et le cout de revient sont recalcules pour chaque jour que l'activite touche.

  Chaque correction porte un identifiant de saisie, genere une fois par saisie par le client : un identifiant deja present
  au journal est un renvoi, qui repond 200 sans rien ecrire, avant toute autre regle. Une activite corrigeable est terminee
  (fin pointee, fin automatique ou fin regularisee) et son suivi n'est pas cloture ; une activite en cours se refuse
  (`activite-en-cours`), une activite retiree est introuvable (`activite-visee-introuvable`). Une saisie perimee, parce
  qu'un pointage s'est glisse depuis la lecture, repond `saisie-concurrente` : le client relit la journee et recommence.
  """
)
class CorrectionsDesActivitesResource {

  private final CorrectionsDesActivitesApplicationService applicationService;

  CorrectionsDesActivitesResource(CorrectionsDesActivitesApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @PostMapping("/{id}/activites")
  @Operation(
    summary = "Ajouter une activite oubliee",
    description = """
    Ajoute au suivi une activite que l'operateur a menee sans la pointer : une ouverture a l'heure du debut et une fin a
    l'heure de la fin, d'origine regularisation. La fin peut tomber le lendemain du debut. La nature d'operation est
    recopiee du poste, le taux horaire de l'operateur.

    Refus, dans l'ordre : suivi (404 `suivi-d-atelier-introuvable`), operateur (404 `operateur-introuvable`) ou poste (404
    `poste-de-travail-introuvable`) introuvable, operateur non habilite sur ce poste (409 `operateur-non-habilite`), fin
    qui ne suit pas le debut (409 `fin-avant-debut`), fin dans le futur (400 `date-de-survenue-future`), debut anterieur
    a l'engagement (409 `evenement-anterieur-a-l-engagement`), fin posterieure a la cloture (409
    `evenement-posterieur-a-la-cloture`), chevauchement d'une autre activite de la meme cle sur toute l'activite (409
    `chevauchement`). `saisie-concurrente` (409) demande de relire la journee.
    """
  )
  @ApiResponse(responseCode = "201", description = "L'activite est ajoutee au journal.", content = @Content)
  @ApiResponse(
    responseCode = "200",
    description = "Renvoi : l'evenement de cet identifiant figure deja au journal, rien n'est ecrit de plus.",
    content = @Content
  )
  @ApiResponse(
    responseCode = "400",
    description = "Le corps est invalide, ou la fin est dans le futur (`date-de-survenue-future`).",
    content = @Content
  )
  @ApiResponse(responseCode = "404", description = "Suivi, operateur ou poste de travail introuvable.", content = @Content)
  @ApiResponse(
    responseCode = "409",
    description = """
    Operateur non habilite sur ce poste, fin qui ne suit pas le debut, debut anterieur a l'engagement, fin posterieure a la
    cloture, chevauchement d'une autre activite de la cle, ou saisie concurrente (la journee est a relire).
    """,
    content = @Content
  )
  ResponseEntity<Void> ajoute(@PathVariable UUID id, @RequestBody @Valid RestAjoutDUneActivite request) {
    return reponse(applicationService.ajoute(request.toDomain(new SuiviDAtelierId(id), AuteurConnecte.get())));
  }

  @PostMapping("/{id}/activites/{activite}/poste")
  @Operation(
    summary = "Changer le poste d'une activite",
    description = """
    L'activite compte desormais sur le nouveau poste : la nature d'operation et le cout horaire sont ceux du nouveau poste,
    recopies a la correction. Le poste d'une fin automatique change aussi : son anomalie reste ouverte, sur la nouvelle cle.

    Refus, dans l'ordre : suivi (404 `suivi-d-atelier-introuvable`) ou activite (404 `activite-visee-introuvable`)
    introuvable, activite en cours (409 `activite-en-cours`), poste introuvable (404 `poste-de-travail-introuvable`),
    operateur non habilite sur le nouveau poste (409 `operateur-non-habilite`), meme poste que l'activite (409
    `meme-poste`), chevauchement sur la nouvelle cle (409 `chevauchement`), changement posterieur a la cloture (409
    `evenement-posterieur-a-la-cloture`). `saisie-concurrente` (409) demande de relire la journee.
    """
  )
  @ApiResponse(responseCode = "201", description = "Le changement de poste est porte au journal.", content = @Content)
  @ApiResponse(
    responseCode = "200",
    description = "Renvoi : l'evenement de cet identifiant figure deja au journal, rien n'est ecrit de plus.",
    content = @Content
  )
  @ApiResponse(responseCode = "400", description = "Le corps est invalide.", content = @Content)
  @ApiResponse(responseCode = "404", description = "Suivi, activite ou poste de travail introuvable.", content = @Content)
  @ApiResponse(
    responseCode = "409",
    description = """
    Activite en cours, operateur non habilite sur le nouveau poste, meme poste, chevauchement sur la nouvelle cle,
    changement posterieur a la cloture, ou saisie concurrente (la journee est a relire).
    """,
    content = @Content
  )
  ResponseEntity<Void> changeLePoste(
    @PathVariable UUID id,
    @PathVariable UUID activite,
    @RequestBody @Valid RestChangementDePoste request
  ) {
    return reponse(
      applicationService.changeLePoste(request.toDomain(new SuiviDAtelierId(id), new ActiviteId(activite), AuteurConnecte.get()))
    );
  }

  @PostMapping("/{id}/activites/{activite}/retrait")
  @Operation(
    summary = "Retirer une activite",
    description = """
    Ecarte l'activite de toutes les lectures du journal : releve des heures, supervision et cout de revient sont recalcules
    pour chaque jour qu'elle touchait. Le retrait d'une fin automatique en fait disparaitre l'anomalie. Aucune habilitation
    n'est verifiee : on retire une activite pointee sur un poste dont l'operateur a perdu l'habilitation.

    Refus, dans l'ordre : suivi (404 `suivi-d-atelier-introuvable`) ou activite (404 `activite-visee-introuvable`, y compris
    une activite deja retiree) introuvable, activite en cours (409 `activite-en-cours`), retrait posterieur a la cloture
    (409 `evenement-posterieur-a-la-cloture`). `saisie-concurrente` (409) demande de relire la journee.
    """
  )
  @ApiResponse(responseCode = "201", description = "Le retrait est porte au journal.", content = @Content)
  @ApiResponse(
    responseCode = "200",
    description = "Renvoi : l'evenement de cet identifiant figure deja au journal, rien n'est ecrit de plus.",
    content = @Content
  )
  @ApiResponse(responseCode = "400", description = "Le corps est invalide.", content = @Content)
  @ApiResponse(responseCode = "404", description = "Suivi ou activite introuvable.", content = @Content)
  @ApiResponse(
    responseCode = "409",
    description = "Activite en cours, retrait posterieur a la cloture, ou saisie concurrente (la journee est a relire).",
    content = @Content
  )
  ResponseEntity<Void> retire(@PathVariable UUID id, @PathVariable UUID activite, @RequestBody @Valid RestRetrait request) {
    return reponse(applicationService.retire(request.toDomain(new SuiviDAtelierId(id), new ActiviteId(activite), AuteurConnecte.get())));
  }

  private static ResponseEntity<Void> reponse(CorrectionTraitee traitee) {
    return ResponseEntity.status(traitee.rejeu() ? HttpStatus.OK : HttpStatus.CREATED).build();
  }
}
