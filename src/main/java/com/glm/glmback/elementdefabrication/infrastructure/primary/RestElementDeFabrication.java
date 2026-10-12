package com.glm.glmback.elementdefabrication.infrastructure.primary;

import com.glm.glmback.elementdefabrication.domain.CycleDeVie;
import com.glm.glmback.elementdefabrication.domain.Description;
import com.glm.glmback.elementdefabrication.domain.ElementDansSonCycle;
import com.glm.glmback.elementdefabrication.domain.ElementDeFabrication;
import com.glm.glmback.elementdefabrication.domain.EtatEnAtelier;
import com.glm.glmback.elementdefabrication.domain.PassageClos;
import com.glm.glmback.elementdefabrication.domain.PassageEnAtelier;
import com.glm.glmback.elementdefabrication.domain.PassageOuvert;
import com.glm.glmback.elementdefabrication.domain.Reference;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

record RestElementDeFabrication(
  String categorie,
  UUID id,
  String nom,
  String reference,
  String description,
  Instant dateDeCreation,
  Instant dateDeModification,
  @Schema(
    description = "Ou en est l'element, lu sur son suivi d'atelier. Renseigne par la liste seulement : EN_PREPARATION sans suivi, EN_ATELIER suivi ouvert, CLOTURE suivi cloture."
  )
  CycleDeVie cycle,
  @Schema(description = "Le suivi d'atelier de l'element, absent en preparation. C'est lui que visent cloture et reouverture.") UUID suivi,
  @Schema(description = "L'etat du suivi ouvert a l'instant de la lecture, absent hors de l'atelier.") EtatEnAtelier etatDAtelier,
  @Schema(description = "Mise a l'atelier, absente en preparation.") Instant misALAtelierLe,
  @Schema(description = "Cloture du suivi, presente pour un element cloture seulement.") Instant clotureLe,
  @Schema(description = "Auteur de la cloture, present pour un element cloture seulement.") String clotureePar
) {
  static RestElementDeFabrication from(ElementDeFabrication element) {
    return avec(element, null, Optional.empty());
  }

  static RestElementDeFabrication from(ElementDansSonCycle lu) {
    return avec(lu.element(), lu.cycle(), lu.passage());
  }

  private static RestElementDeFabrication avec(ElementDeFabrication element, CycleDeVie cycle, Optional<PassageEnAtelier> passage) {
    return new RestElementDeFabrication(
      element.categorie().value(),
      element.id().uuid(),
      element.nom().value(),
      element.reference().map(Reference::value).orElse(null),
      element.description().map(Description::value).orElse(null),
      element.dateDeCreation(),
      element.dateDeModification(),
      cycle,
      passage.map(lu -> lu.suivi().uuid()).orElse(null),
      passage
        .filter(PassageOuvert.class::isInstance)
        .map(lu -> ((PassageOuvert) lu).etat())
        .orElse(null),
      passage.map(PassageEnAtelier::misALAtelierLe).orElse(null),
      clos(passage).map(PassageClos::clotureLe).orElse(null),
      clos(passage)
        .map(lu -> lu.cloturePar().value())
        .orElse(null)
    );
  }

  private static Optional<PassageClos> clos(Optional<PassageEnAtelier> passage) {
    return passage.filter(PassageClos.class::isInstance).map(PassageClos.class::cast);
  }
}
