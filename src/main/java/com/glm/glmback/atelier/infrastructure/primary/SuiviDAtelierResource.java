package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/atelier/suivis")
@Tag(
  name = "Atelier - elements engages",
  description = """
  Le suivi des elements de fabrication mis en atelier.

  Deux publics se partagent ces routes. L'operateur (role USER) consulte le tableau des elements actifs et pointe son
  travail. Le gestionnaire (role GESTIONNAIRE) engage les elements, les cloture et corrige les saisies.

  Rien de ce qui se deduit n'est stocke : etat, activites en cours et temps sont recalcules du journal a chaque lecture.
  """
)
class SuiviDAtelierResource {

  private final SuivisDAtelierApplicationService applicationService;

  SuiviDAtelierResource(SuivisDAtelierApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les elements engages",
    description = """
    Le tableau de l'atelier.

    Tous les filtres sont facultatifs : l'ecran des operateurs veut tous les elements actifs d'un coup, sans notion de
    date. Le parametre etats accepte plusieurs valeurs (?etats=EN_ATTENTE&etats=EN_COURS) ; absent, il ne filtre rien.
    La periode, quand elle est fournie, porte sur la date d'engagement.

    Chaque ligne conserve l'etat et les activites en cours, mais ne contient pas de journal.
    Le journal complet, annules compris, se consulte via GET /api/atelier/suivis/{id}.
    """
  )
  @ApiResponse(responseCode = "200", description = "La page demandee sans les journaux, triee par date d'engagement descendante.")
  RestPage<RestSyntheseDeSuiviDAtelier> list(
    @RequestParam(required = false) Instant debut,
    @RequestParam(required = false) Instant fin,
    @RequestParam(required = false) Set<EtatDAtelier> etats,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<SuiviDAtelier> resultat = applicationService.list(periode(debut, fin), etats(etats), new Pageable(page, size));
    AnnuaireDAtelier annuaire = applicationService.annuairePourSuivis(resultat.content());

    return RestPage.from(resultat, suivi -> RestSyntheseDeSuiviDAtelier.from(suivi, annuaire));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Engager un element en atelier",
    description = """
    Geste metier du back-office, distinct de la creation de l'element : tout ce qui est cree n'est pas forcement a
    faire, et c'est cet acte qui fait apparaitre l'element sur l'ecran des operateurs.

    Le nom et le type de l'element sont copies a cet instant ; l'atelier ne les relira plus.
    """
  )
  @ApiResponse(responseCode = "201", description = "L'element est engage.")
  @ApiResponse(responseCode = "404", description = "Aucun element de fabrication ne porte cet identifiant.")
  @ApiResponse(responseCode = "409", description = "Cet element est deja engage et non cloture.")
  RestSuiviDAtelier engage(@RequestBody @Valid RestEngagement request) {
    return rendu(applicationService.engage(request.toDomain(AuteurConnecte.get())));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consulter un element engage", description = "Le suivi complet, avec son journal, evenements annules compris.")
  @ApiResponse(responseCode = "404", description = "Suivi introuvable.")
  RestSuiviDAtelier get(@PathVariable UUID id) {
    return rendu(applicationService.get(new SuiviDAtelierId(id)));
  }

  @GetMapping("/{id}/temps-effectif")
  @Operation(
    summary = "Lire le temps effectivement passe sur un element",
    description = """
    Les intervalles bruts du journal, ramenes aux fenetres de presence des operateurs.

    C'est ici que la pause de midi scinde un travail en deux et qu'un depart referme ce que l'operateur a oublie
    d'arreter, sans qu'aucun de ces faits ait eu besoin d'etre recopie dans le journal de l'element. Un intervalle
    sans fin est encore en cours.
    """
  )
  @ApiResponse(responseCode = "404", description = "Suivi introuvable.")
  List<RestIntervalleDActivite> tempsEffectif(@PathVariable UUID id) {
    List<IntervalleDActivite> intervalles = applicationService.tempsEffectif(new SuiviDAtelierId(id));
    AnnuaireDAtelier annuaire = applicationService.annuairePourIntervalles(intervalles);

    return intervalles
      .stream()
      .map(intervalle -> RestIntervalleDActivite.from(intervalle, annuaire))
      .toList();
  }

  @PostMapping("/{id}/pointages")
  @Operation(
    summary = "Pointer un debut, une non conformite ou une fin",
    description = """
    Le geste de l'operateur, date a l'instant present.

    Ne jamais pointer ici une pause ou un depart : ce sont des faits de la journee de travail de l'operateur, ecrits
    une seule fois via POST /api/atelier/journees/pointages.
    """
  )
  @ApiResponse(responseCode = "201", description = "Le pointage est enregistre.")
  @ApiResponse(responseCode = "200", description = "Le geste identique est rejoue.")
  @ApiResponse(responseCode = "400", description = "Le corps est invalide ou la date de survenue est future.")
  @ApiResponse(responseCode = "404", description = "Suivi, operateur ou poste de travail introuvable.")
  @ApiResponse(
    responseCode = "409",
    description = "Operateur non habilite sur ce poste, element cloture, transition impossible ou identifiant reutilise."
  )
  ResponseEntity<RestSuiviDAtelier> pointe(@PathVariable UUID id, @RequestBody @Valid RestPointage request) {
    var resultat = applicationService.pointeDuPupitre(request.toDomain(new SuiviDAtelierId(id), AuteurConnecte.get()));
    return ResponseEntity.status(resultat.rejeu() ? HttpStatus.OK : HttpStatus.CREATED).body(rendu(resultat.agregat()));
  }

  @PostMapping("/{id}/regularisations")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Rattraper une saisie oubliee", description = "Premier des trois actes de correction.")
  @ApiResponse(responseCode = "201", description = "La regularisation est enregistree.")
  @ApiResponse(responseCode = "404", description = "Suivi, operateur ou poste de travail introuvable.")
  @ApiResponse(
    responseCode = "409",
    description = "Operateur non habilite sur ce poste, transition impossible, ou evenement anterieur a l'engagement."
  )
  RestSuiviDAtelier regularise(@PathVariable UUID id, @RequestBody @Valid RestRegularisation request) {
    return rendu(applicationService.regularise(request.toDomain(new SuiviDAtelierId(id), AuteurConnecte.get())));
  }

  @PostMapping("/{id}/evenements/{evenementId}/annulation")
  @Operation(summary = "Annuler une saisie en trop", description = "Deuxieme acte de correction. L'evenement reste au journal.")
  @ApiResponse(responseCode = "404", description = "Suivi ou evenement introuvable.")
  @ApiResponse(responseCode = "409", description = "Evenement deja annule.")
  RestSuiviDAtelier annule(@PathVariable UUID id, @PathVariable UUID evenementId, @RequestBody @Valid RestAnnulationDEvenement request) {
    return rendu(applicationService.annule(request.toDomain(new SuiviDAtelierId(id), evenementId, AuteurConnecte.get())));
  }

  @PutMapping("/{id}/evenements/{evenementId}")
  @Operation(
    summary = "Corriger une saisie fausse",
    description = "Troisieme acte : une annulation et une regularisation en un seul appel."
  )
  @ApiResponse(responseCode = "404", description = "Suivi, evenement, operateur ou poste de travail introuvable.")
  @ApiResponse(responseCode = "409", description = "Operateur non habilite sur ce poste, evenement deja annule, ou transition impossible.")
  RestSuiviDAtelier corrige(@PathVariable UUID id, @PathVariable UUID evenementId, @RequestBody @Valid RestCorrection request) {
    return rendu(applicationService.corrige(request.toDomain(new SuiviDAtelierId(id), evenementId, AuteurConnecte.get())));
  }

  @PutMapping("/{id}/cloture")
  @Operation(
    summary = "Cloturer un element, ou deplacer sa cloture",
    description = """
    La cloture ne fige rien pour le gestionnaire : regularisation, annulation et correction restent possibles ensuite.
    Rappelee sur un element deja cloture, cette route deplace la date de cloture.
    """
  )
  @ApiResponse(responseCode = "404", description = "Suivi introuvable.")
  @ApiResponse(responseCode = "409", description = "Le journal porte un evenement posterieur a la date de cloture demandee.")
  RestSuiviDAtelier cloture(@PathVariable UUID id, @RequestBody @Valid RestCloture request) {
    return rendu(applicationService.cloture(request.toDomain(new SuiviDAtelierId(id), AuteurConnecte.get())));
  }

  @DeleteMapping("/{id}/cloture")
  @Operation(summary = "Rouvrir un element cloture", description = "Retire la cloture. L'element redevient pointable.")
  @ApiResponse(responseCode = "404", description = "Suivi introuvable.")
  RestSuiviDAtelier annuleLaCloture(@PathVariable UUID id) {
    return rendu(applicationService.annuleLaCloture(new SuiviDAtelierId(id)));
  }

  /**
   * Le suivi rendu avec ses ressources resolues : le journal ne stockant que des identifiants, l'affichage les relit.
   */
  private RestSuiviDAtelier rendu(SuiviDAtelier suivi) {
    return RestSuiviDAtelier.from(suivi, applicationService.annuairePour(suivi));
  }

  private static Optional<Periode> periode(Instant debut, Instant fin) {
    if (debut == null || fin == null) {
      return Optional.empty();
    }

    return Optional.of(new Periode(debut, fin));
  }

  private static Set<EtatDAtelier> etats(Set<EtatDAtelier> etats) {
    return etats == null ? Set.of() : etats;
  }
}
