package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.JourneesDeTravailApplicationService;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.JourneeDeTravail;
import com.glm.glmback.atelier.domain.JourneeDeTravailId;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/journees")
@Tag(
  name = "Atelier - presence des operateurs",
  description = """
  La presence des operateurs : arrivee, pause, reprise, depart.

  Une journee de travail est une venue, bornee par une arrivee et un depart. Ce n'est pas un jour calendaire : le
  contexte ne connait ni fuseau horaire ni date.

  La presence est ecrite une seule fois, ici, et jamais recopiee dans le journal des elements. C'est ce qui permet a un
  seul bouton de pause de scinder tout ce que l'operateur avait en cours, et a une seule regularisation de depart de
  refermer tous ses elements de la journee.
  """
)
class JourneeDeTravailResource {

  private final JourneesDeTravailApplicationService applicationService;

  JourneeDeTravailResource(JourneesDeTravailApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les journees de travail",
    description = "Filtres facultatifs. La periode, quand elle est fournie, porte sur l'heure d'arrivee."
  )
  @ApiResponse(responseCode = "200", description = "La page demandee, triee par debut descendant.")
  Page<RestJourneeDeTravail> list(
    @RequestParam(required = false) Instant debut,
    @RequestParam(required = false) Instant fin,
    @RequestParam(required = false) UUID operateur,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<JourneeDeTravail> resultat = applicationService.list(periode(debut, fin), operateur(operateur), new Pageable(page, size));

    return Page.<RestJourneeDeTravail>builder()
      .content(rendus(resultat.content()))
      .currentPage(resultat.currentPage())
      .pageSize(resultat.pageSize())
      .totalElementsCount(resultat.totalElementsCount());
  }

  @PostMapping
  @Operation(
    summary = "Enregistrer une arrivee",
    description = """
    Ouvre la journee de travail de l'operateur.

    Sans dateDeSurvenue, l'arrivee est datee a l'instant present : c'est le geste du matin. Avec, c'est le
    gestionnaire qui saisit apres coup une arrivee jamais pointee.
    """
  )
  @ApiResponse(responseCode = "201", description = "La journee est ouverte.")
  @ApiResponse(responseCode = "200", description = "Le geste identique est rejoue.")
  @ApiResponse(responseCode = "400", description = "Le corps est invalide ou la date de survenue est future.")
  @ApiResponse(responseCode = "404", description = "Aucun operateur ne porte cet identifiant.")
  @ApiResponse(responseCode = "409", description = "Cet operateur a deja une journee ouverte ou l'identifiant est reutilise.")
  ResponseEntity<RestJourneeDeTravail> arrive(@RequestBody @Valid RestArrivee request) {
    var resultat = applicationService.arriveDuPupitre(request.toDomain(AuteurConnecte.get()));
    return ResponseEntity.status(resultat.rejeu() ? HttpStatus.OK : HttpStatus.CREATED).body(rendu(resultat.agregat()));
  }

  @PostMapping("/pointages")
  @Operation(
    summary = "Pointer une pause, une reprise ou un depart",
    description = """
    Vise la journee ouverte de l'operateur, retrouvee par le serveur : aucun identifiant de journee n'est a fournir.

    Un seul appel, quel que soit le nombre d'elements en cours. Ne jamais boucler sur les elements pour repercuter une
    pause : le croisement est fait a la lecture du temps effectif.
    """
  )
  @ApiResponse(responseCode = "201", description = "Le pointage est enregistre.")
  @ApiResponse(responseCode = "200", description = "Le geste identique est rejoue.")
  @ApiResponse(responseCode = "400", description = "Le corps est invalide ou la date de survenue est future.")
  @ApiResponse(responseCode = "404", description = "Cet operateur n'a aucune journee ouverte.")
  @ApiResponse(responseCode = "409", description = "Transition impossible depuis l'etat de presence courant ou identifiant reutilise.")
  ResponseEntity<RestJourneeDeTravail> pointe(@RequestBody @Valid RestPointageDePresence request) {
    var resultat = applicationService.pointeDuPupitre(request.toDomain(AuteurConnecte.get()));
    return ResponseEntity.status(resultat.rejeu() ? HttpStatus.OK : HttpStatus.CREATED).body(rendu(resultat.agregat()));
  }

  @GetMapping("/{id}")
  @Operation(
    summary = "Consulter une journee de travail",
    description = "Expose a la fois l'amplitude et les fenetres : le contexte ne choisit pas laquelle compte pour la paie."
  )
  @ApiResponse(responseCode = "404", description = "Journee introuvable.")
  RestJourneeDeTravail get(@PathVariable UUID id) {
    return rendu(applicationService.get(new JourneeDeTravailId(id)));
  }

  @PostMapping("/{id}/regularisations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Rattraper une presence oubliee",
    description = """
    Le cas type : l'operateur est rentre chez lui sans pointer son depart, et le gestionnaire le saisit le lendemain a
    l'heure reelle. Cette seule regularisation referme aussi tous les elements restes ouverts.
    """
  )
  @ApiResponse(responseCode = "201", description = "La regularisation est enregistree.")
  @ApiResponse(responseCode = "404", description = "Journee introuvable.")
  @ApiResponse(responseCode = "409", description = "Transition impossible depuis l'etat de presence a cet instant.")
  RestJourneeDeTravail regularise(@PathVariable UUID id, @RequestBody @Valid RestRegularisationDePresence request) {
    return rendu(applicationService.regularise(request.toDomain(new JourneeDeTravailId(id), AuteurConnecte.get())));
  }

  @PostMapping("/{id}/evenements/{evenementId}/annulation")
  @Operation(summary = "Annuler une presence saisie en trop", description = "L'evenement reste au journal, porteur de son annulation.")
  @ApiResponse(responseCode = "404", description = "Journee ou evenement introuvable.")
  @ApiResponse(responseCode = "409", description = "Evenement deja annule.")
  RestJourneeDeTravail annule(@PathVariable UUID id, @PathVariable UUID evenementId, @RequestBody @Valid RestAnnulationDEvenement request) {
    return rendu(applicationService.annule(request.toDomain(new JourneeDeTravailId(id), evenementId, AuteurConnecte.get())));
  }

  @PutMapping("/{id}/evenements/{evenementId}")
  @Operation(summary = "Corriger une presence fausse", description = "Une annulation et une regularisation en un seul appel.")
  @ApiResponse(responseCode = "404", description = "Journee ou evenement introuvable.")
  @ApiResponse(responseCode = "409", description = "Evenement deja annule, ou transition impossible.")
  RestJourneeDeTravail corrige(
    @PathVariable UUID id,
    @PathVariable UUID evenementId,
    @RequestBody @Valid RestCorrectionDePresence request
  ) {
    return rendu(applicationService.corrige(request.toDomain(new JourneeDeTravailId(id), evenementId, AuteurConnecte.get())));
  }

  /**
   * La journee rendue avec son operateur resolu : le journal ne stocke qu'un identifiant.
   */
  private RestJourneeDeTravail rendu(JourneeDeTravail journee) {
    return RestJourneeDeTravail.from(journee, applicationService.annuairePour(journee));
  }

  /**
   * Une page entiere resolue en un seul aller par port, jamais un par journee.
   */
  private List<RestJourneeDeTravail> rendus(List<JourneeDeTravail> journees) {
    AnnuaireDAtelier annuaire = applicationService.annuairePourJournees(journees);

    return journees
      .stream()
      .map(journee -> RestJourneeDeTravail.from(journee, annuaire))
      .toList();
  }

  private static Optional<Periode> periode(Instant debut, Instant fin) {
    if (debut == null || fin == null) {
      return Optional.empty();
    }

    return Optional.of(new Periode(debut, fin));
  }

  private static Optional<OperateurId> operateur(UUID operateur) {
    return Optional.ofNullable(operateur).map(OperateurId::new);
  }
}
