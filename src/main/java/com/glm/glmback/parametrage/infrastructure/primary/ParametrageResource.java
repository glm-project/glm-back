package com.glm.glmback.parametrage.infrastructure.primary;

import com.glm.glmback.parametrage.application.ParametrageApplicationService;
import com.glm.glmback.parametrage.domain.FormatDImage;
import com.glm.glmback.parametrage.domain.Logo;
import com.glm.glmback.parametrage.domain.VersionDuLogo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/parametrage")
@Tag(
  name = "Parametrage",
  description = """
  Les reglages que l'entreprise fixe elle-meme, un seul jeu pour toute l'entreprise.

  Le gestionnaire les modifie ; l'operateur (role USER) les consulte.
  """
)
class ParametrageResource {

  private static final Duration UN_AN = Duration.ofDays(365);
  private static final Map<FormatDImage, MediaType> TYPES = Map.of(
    FormatDImage.PNG,
    MediaType.IMAGE_PNG,
    FormatDImage.JPEG,
    MediaType.IMAGE_JPEG
  );

  private final ParametrageApplicationService applicationService;

  ParametrageResource(ParametrageApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  @Operation(summary = "Lire le parametrage de l'entreprise", description = "Un reglage jamais fixe vaut sa valeur par defaut.")
  @ApiResponse(responseCode = "200", description = "Le parametrage de l'entreprise.")
  RestParametrage lisLeParametrage() {
    return RestParametrage.from(applicationService.get());
  }

  @PutMapping("/duree-max-d-activite")
  @Operation(
    summary = "Fixer la duree max d'une activite",
    description = """
    Le temps au bout duquel une activite que rien n'a terminee se termine automatiquement, d'une heure a
    vingt-quatre heures.
    """
  )
  @ApiResponse(responseCode = "200", description = "Le parametrage, avec la duree fixee.")
  @ApiResponse(responseCode = "400", description = "Duree absente, illisible, ou hors des bornes.")
  RestParametrage fixeLaDureeMaxDActivite(@RequestBody @Valid RestDureeMaxDActivite request) {
    return RestParametrage.from(applicationService.fixeLaDureeMaxDActivite(request.toDomain()));
  }

  @PutMapping(path = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
    summary = "Deposer le logo de l'entreprise",
    description = """
    Le fichier, dans la partie logo, remplace le logo courant. Il s'affiche tel quel en en-tete de la supervision, du
    pupitre et des PDF : une image PNG ou JPEG de 50 x 50 pixels exactement, de 20 Ko au plus. Le format se juge sur
    le contenu, jamais sur le nom du fichier ni sur le type annonce.
    """
  )
  @ApiResponse(responseCode = "200", description = "Le logo est depose ; sa version entre dans l'adresse de l'image.")
  @ApiResponse(responseCode = "400", description = "Fichier absent, illisible, trop lourd, d'un autre format ou d'autres dimensions.")
  RestLogo deposeLeLogo(@RequestPart("logo") MultipartFile logo) throws IOException {
    return RestLogo.from(applicationService.deposeLeLogo(logo.getBytes()).version());
  }

  @GetMapping(path = "/logo/{version}", produces = { MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE })
  @Operation(
    summary = "Lire l'image du logo",
    description = """
    L'image du logo courant, a l'adresse de sa version, lue dans le parametrage. La reponse se garde en cache sans
    limite (Cache-Control: private, max-age d'un an, immutable) : un nouveau logo change d'adresse, l'ancienne ne
    rend plus rien.
    """
  )
  @ApiResponse(responseCode = "200", description = "L'image, en PNG ou en JPEG.")
  @ApiResponse(responseCode = "404", description = "Cette version n'est pas celle du logo courant, ou l'entreprise n'a pas de logo.")
  ResponseEntity<byte[]> lisLeLogo(@PathVariable @Pattern(regexp = "^[0-9a-f]{16}$") String version) {
    Logo logo = applicationService.logo(new VersionDuLogo(version));

    return ResponseEntity.ok()
      .contentType(TYPES.get(logo.format()))
      .cacheControl(CacheControl.maxAge(UN_AN).cachePrivate().immutable())
      .body(logo.contenu());
  }

  @DeleteMapping("/logo")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
    summary = "Retirer le logo de l'entreprise",
    description = "Les en-tetes reviennent au logo de GLM. Sans effet si l'entreprise n'a pas de logo."
  )
  @ApiResponse(responseCode = "204", description = "L'entreprise n'a plus de logo.")
  void retireLeLogo() {
    applicationService.retireLeLogo();
  }
}
