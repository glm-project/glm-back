package com.glm.glmback.coutderevient.infrastructure.primary;

import com.glm.glmback.coutderevient.domain.AnnuaireDuCout;
import com.glm.glmback.coutderevient.domain.AnomalieDuPointage;
import com.glm.glmback.coutderevient.domain.CategorieDActivite;
import com.glm.glmback.coutderevient.domain.CoutHoraire;
import com.glm.glmback.coutderevient.domain.PointageAResoudre;
import com.glm.glmback.coutderevient.domain.PointageDeCout;
import com.glm.glmback.coutderevient.domain.PointageValorise;
import com.glm.glmback.coutderevient.domain.TauxHoraire;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(
  name = "RestPointageDuCout",
  description = """
  Un pointage d'une ligne, avec tout ce qui permet de refaire son calcul.

  Termine, il porte sa duree, sa machine (cout horaire x duree, jamais divisee) et sa main d'oeuvre, somme de ses
  parts. A resoudre, il n'a ni fin, ni duree, ni montant : seulement sa fin au plus tard et les faits contradictoires
  de sa sequence. Les montants sont deja au centime, et ceux des pointages s'additionnent exactement au cout de la
  ligne quand il est complet.
  """
)
record RestPointageDeCout(
  @Schema(description = "Anomalies du pointage, vide s'il n'en a aucune.", requiredMode = Schema.RequiredMode.REQUIRED)
  List<AnomalieDuPointage> anomalies,
  @Schema(description = "Operateur du pointage.", requiredMode = Schema.RequiredMode.REQUIRED) RestOperateurCite operateur,
  @Schema(description = "Poste du pointage, absent sans poste.") RestPosteCite poste,
  @Schema(description = "Travail ou reprise de non conformite.", requiredMode = Schema.RequiredMode.REQUIRED) CategorieDActivite categorie,
  @Schema(description = "Debut du pointage.", requiredMode = Schema.RequiredMode.REQUIRED) Instant debut,
  @Schema(description = "Fin du pointage, absente s'il est a resoudre.") Instant fin,
  @Schema(description = "Fin au plus tard d'un pointage a resoudre.") Instant finAuPlusTard,
  @Schema(description = "Duree du pointage, incomplete s'il est a resoudre.", requiredMode = Schema.RequiredMode.REQUIRED)
  RestDureeDuCout duree,
  @Schema(description = "Cout horaire du poste fige a la saisie, absent s'il n'est pas valorise.", example = "48.00")
  BigDecimal coutHoraire,
  @Schema(description = "Taux horaire de l'operateur fige a la saisie, absent s'il n'est pas valorise.", example = "35.00")
  BigDecimal tauxHoraire,
  @Schema(description = "Cout du pointage.", requiredMode = Schema.RequiredMode.REQUIRED) RestCout cout,
  @Schema(
    description = "Parts du pointage termine, une par fenetre de partage ; vide s'il est a resoudre.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestPartDuPointage> parts,
  @Schema(
    description = "Faits contradictoires d'un pointage a resoudre, dans leur ordre ; vide sinon.",
    requiredMode = Schema.RequiredMode.REQUIRED
  )
  List<RestPointageEnConflit> contradictoires
) {
  static RestPointageDeCout from(PointageDeCout pointage, AnnuaireDuCout annuaire) {
    return switch (pointage) {
      case PointageValorise valorise -> from(
        pointage,
        annuaire,
        null,
        valorise
          .parts()
          .stream()
          .map(part -> RestPartDuPointage.from(part, annuaire))
          .toList(),
        List.of()
      );
      case PointageAResoudre aResoudre -> from(
        pointage,
        annuaire,
        aResoudre.finAuPlusTard().orElse(null),
        List.of(),
        aResoudre.contradictoires().stream().map(RestPointageEnConflit::from).toList()
      );
    };
  }

  private static RestPointageDeCout from(
    PointageDeCout pointage,
    AnnuaireDuCout annuaire,
    Instant finAuPlusTard,
    List<RestPartDuPointage> parts,
    List<RestPointageEnConflit> contradictoires
  ) {
    return new RestPointageDeCout(
      pointage.anomalies(),
      RestOperateurCite.from(pointage.activite().operateur(), annuaire),
      pointage
        .activite()
        .poste()
        .map(poste -> RestPosteCite.from(poste, annuaire))
        .orElse(null),
      pointage.activite().categorie(),
      pointage.debut(),
      pointage.fin().orElse(null),
      finAuPlusTard,
      RestDureeDuCout.from(pointage.duree()),
      pointage.activite().coutHoraire().map(CoutHoraire::value).orElse(null),
      pointage.activite().tauxHoraire().map(TauxHoraire::value).orElse(null),
      RestCout.from(pointage.cout()),
      parts,
      contradictoires
    );
  }
}
