package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.PointagesEnAttenteApplicationService;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.MotifDeMiseEnAttente;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PointageEnAttente;
import com.glm.glmback.atelier.domain.PointageEnAttenteId;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atelier/pointages-en-attente")
@Tag(
  name = "Atelier - pointages en attente",
  description = """
  Les gestes du pupitre qu'on ne sait rattacher a rien : operateur, poste ou element inconnu, geste rejoue dans le
  desordre, identifiant reutilise. Le pupitre a recu un succes (202) ; le geste est conserve tel quel, hors de tous
  les calculs, jusqu'a ce que le gestionnaire l'applique ou l'ecarte. L'operateur n'en est pas informe.
  """
)
class PointageEnAttenteResource {

  private final PointagesEnAttenteApplicationService applicationService;

  PointageEnAttenteResource(PointagesEnAttenteApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(
    summary = "Lister les pointages en attente",
    description = "Les pointages non traites, le plus recent d'abord par reception. Filtres facultatifs sur l'operateur et le motif."
  )
  @ApiResponse(responseCode = "200", description = "La page demandee, triee par reception descendante.")
  @ApiResponse(responseCode = "400", description = "Motif de mise en attente inconnu.")
  RestPage<RestPointageEnAttente> list(
    @RequestParam(required = false) UUID operateur,
    @RequestParam(required = false) MotifDeMiseEnAttente motif,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    Page<PointageEnAttente> resultat = applicationService.list(
      Optional.ofNullable(operateur).map(OperateurId::new),
      Optional.ofNullable(motif),
      new Pageable(page, size)
    );
    AnnuaireDAtelier annuaire = applicationService.annuairePour(resultat.content());

    return RestPage.from(resultat, pointage -> RestPointageEnAttente.from(pointage, annuaire));
  }

  @PostMapping("/{id}/application")
  @Operation(
    summary = "Appliquer un pointage en attente",
    description = """
    Enregistre le geste tel quel, a sa date, comme une regularisation du gestionnaire : corrigeable ensuite comme toute
    autre. Une arrivee ouvre sa journee ; un autre geste de presence s'inscrit dans la journee qui contient sa date, a
    defaut dans la journee en cours. Refusee avec explication, l'application laisse le pointage en attente.
    """
  )
  @ApiResponse(responseCode = "200", description = "Le pointage applique.")
  @ApiResponse(
    responseCode = "404",
    description = "Pointage en attente inconnu, ou operateur, poste, element ou journee toujours introuvable."
  )
  @ApiResponse(
    responseCode = "409",
    description = "Pointage deja traite, ou regularisation refusee (transition, chevauchement, habilitation, cloture)."
  )
  RestPointageEnAttente applique(@PathVariable UUID id) {
    return rendu(applicationService.applique(new PointageEnAttenteId(id), AuteurConnecte.get()));
  }

  @PostMapping("/{id}/ecart")
  @Operation(
    summary = "Ecarter un pointage en attente",
    description = "Le geste sort de la liste sans rien ecrire au journal. Le motif est obligatoire."
  )
  @ApiResponse(responseCode = "200", description = "Le pointage ecarte.")
  @ApiResponse(responseCode = "400", description = "Motif absent ou vide.")
  @ApiResponse(responseCode = "404", description = "Pointage en attente inconnu.")
  @ApiResponse(responseCode = "409", description = "Pointage deja traite.")
  RestPointageEnAttente ecarte(@PathVariable UUID id, @RequestBody @Valid RestEcart request) {
    return rendu(applicationService.ecarte(new PointageEnAttenteId(id), AuteurConnecte.get(), request.toDomain()));
  }

  private RestPointageEnAttente rendu(PointageEnAttente pointage) {
    return RestPointageEnAttente.from(pointage, applicationService.annuairePour(List.of(pointage)));
  }
}
