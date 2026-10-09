package com.glm.glmback.atelier.infrastructure.primary;

import com.glm.glmback.atelier.application.SuivisDAtelierApplicationService;
import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.EtatDAtelier;
import com.glm.glmback.atelier.domain.LectureDuSuivi;
import com.glm.glmback.atelier.domain.Periode;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import com.glm.glmback.shared.pagination.infrastructure.primary.RestPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
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
  travail. Le gestionnaire (role GESTIONNAIRE) engage les elements, les cloture et rattrape les saisies oubliees.

  Rien de ce qui se deduit n'est stocke : etat et activites en cours sont recalcules du journal a chaque lecture, a
  l'instant de cette lecture. Une activite que rien n'a terminee se termine automatiquement a son echeance, son debut
  plus 13 heures, sans qu'aucun evenement ne soit ecrit.
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
    L'etat se juge a l'instant de la lecture, celui de chaque ligne rendue : un element dont la seule activite a atteint
    son echeance n'est plus EN_COURS. La periode, quand elle est fournie, porte sur la date d'engagement.

    Chaque ligne conserve l'etat et les activites en cours, mais ne contient pas de journal.
    Le journal complet se consulte via GET /api/atelier/suivis/{id}.
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
    Page<LectureDuSuivi> resultat = applicationService.list(periode(debut, fin), etats(etats), new Pageable(page, size));
    AnnuaireDAtelier annuaire = applicationService.annuairePourSuivis(resultat.content().stream().map(LectureDuSuivi::suivi).toList());

    return RestPage.from(resultat, lecture -> RestSyntheseDeSuiviDAtelier.from(lecture, annuaire));
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
  @Operation(summary = "Consulter un element engage", description = "Le suivi complet, avec son journal.")
  @ApiResponse(responseCode = "404", description = "Suivi introuvable.")
  RestSuiviDAtelier get(@PathVariable UUID id) {
    return rendu(applicationService.get(new SuiviDAtelierId(id)));
  }

  @PostMapping("/{id}/pointages")
  @Operation(
    summary = "Pointer un debut, une non conformite ou une fin",
    description = """
    Le geste de l'operateur, date a l'instant present ou a l'heure de son geste (`dateDeSurvenue`, hors ligne). Il ne
    designe aucune activite : un debut ou une non conformite en ouvre une, une fin ferme celle qui est en cours sur la cle
    (operateur, element, poste).

    Le serveur juge chaque pointage a son arrivee. Apres les controles habituels (operateur, poste, habilitation, element
    cloture), un pointage plus ancien que le dernier pointage accepte de la cle est ignore (`ANTERIEUR`, a heure egale
    il passe), ainsi qu'une fin qui n'est pas posterieure au debut de l'activite qu'elle fermerait (`ANTERIEUR`) ; une activite dont l'echeance est atteinte a l'heure du geste (debut plus 13 heures, borne comprise) compte
    comme terminee ; puis : rien en cours accepte un debut ou une non conformite et ignore une fin (`APRES_ECHEANCE` si
    la derniere activite est echue sans fin, sinon `AUCUNE_ACTIVITE`) ; une activite en cours ignore un debut ou une non
    conformite (`DEJA_EN_COURS`) et accepte une fin. Un pointage ignore n'entre pas au journal : il laisse une ligne
    d'audit en base, et la reponse est le refus 409 `pointage-ignore`, qui ne s'affiche pas a l'operateur.

    Une pause se pointe par une fin pour chaque activite en cours, puis un debut ou une non conformite a la reprise.
    """
  )
  @ApiResponse(responseCode = "201", description = "Le pointage est accepte et entre au journal.")
  @ApiResponse(
    responseCode = "200",
    description = """
    Renvoi : l'identifiant du pointage figure deja dans la table des evenements, quel que soit le suivi qui le porte, et
    rien n'est ecrit. La reponse rend le suivi de la route.
    """
  )
  @ApiResponse(responseCode = "400", description = "Le corps est invalide, ou la date de survenue est future.")
  @ApiResponse(responseCode = "404", description = "Suivi, operateur ou poste de travail introuvable.")
  @ApiResponse(
    responseCode = "409",
    description = """
    Pointage ignore par la regle de reception (`pointage-ignore`, a ne pas afficher : le pupitre se recale sur le
    referentiel), y compris un renvoi d'un pointage deja ignore ; demarrer ou pointer une non conformite sur un element
    cloture (`suivi-d-atelier-cloture`, seul refus a afficher a l'operateur) ; operateur non habilite sur ce poste.
    """
  )
  ResponseEntity<RestSuiviDAtelier> pointe(@PathVariable UUID id, @RequestBody @Valid RestPointage request) {
    var resultat = applicationService.pointeDuPupitre(request.toDomain(new SuiviDAtelierId(id), AuteurConnecte.get()));
    return ResponseEntity.status(resultat.rejeu() ? HttpStatus.OK : HttpStatus.CREATED).body(rendu(resultat.agregat()));
  }

  @PostMapping("/{id}/regularisations")
  @Operation(
    summary = "Regulariser la fin d'une activite echue",
    description = """
    Le gestionnaire etablit la fin d'une activite que rien n'a terminee avant son echeance (une fin automatique). Le
    corps ne porte que l'identifiant de la saisie, que le client genere une fois par saisie, l'activite et l'heure du
    fait : l'operateur, le poste et le type se deduisent de l'activite. La regularisation ne passe pas par la regle de
    reception des pointages, et son heure peut depasser l'echeance de l'activite.

    L'heure ne depasse ni l'instant present, ni le debut suivant de la meme cle (operateur et poste), ni la cloture ; le
    dossier de la fin automatique donne ces deux dernieres bornes (`borneDeFin`).

    L'identifiant est verifie avant toute regle : un renvoi de la meme saisie repond 200 et n'ecrit rien.
    """
  )
  @ApiResponse(responseCode = "201", description = "La fin est regularisee et portee au journal.")
  @ApiResponse(
    responseCode = "200",
    description = "Renvoi : l'evenement de cet identifiant figure deja au journal, rien n'est ecrit de plus."
  )
  @ApiResponse(responseCode = "400", description = "Le corps est invalide, ou l'heure de la fin est future.")
  @ApiResponse(responseCode = "404", description = "Suivi ou activite introuvable.")
  @ApiResponse(
    responseCode = "409",
    description = """
    Activite non echue (ou deja terminee par un pointage) ou deja regularisee, fin avant le debut de l'activite ou apres sa
    borne, operateur non habilite sur ce poste, ou saisie concurrente (le dossier est a relire).
    """
  )
  ResponseEntity<RestSuiviDAtelier> regularise(@PathVariable UUID id, @RequestBody @Valid RestRegularisation request) {
    var resultat = applicationService.regularise(request.toDomain(new SuiviDAtelierId(id), AuteurConnecte.get()));
    return ResponseEntity.status(resultat.rejeu() ? HttpStatus.OK : HttpStatus.CREATED).body(rendu(resultat.agregat()));
  }

  @PutMapping("/{id}/cloture")
  @Operation(
    summary = "Cloturer un element, ou deplacer sa cloture",
    description = """
    La cloture ne fige rien pour le gestionnaire : la regularisation reste possible ensuite.
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
  private RestSuiviDAtelier rendu(LectureDuSuivi lecture) {
    return RestSuiviDAtelier.from(lecture, applicationService.annuairePour(lecture.suivi()));
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
