package com.glm.glmback.atelier.infrastructure.secondary.gestionconflits;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.glm.glmback.atelier.application.gestionconflits.EmpreintesDesConsequences;
import com.glm.glmback.atelier.domain.Annulation;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.IntervalleDActivite;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

final class Sha256EmpreintesDesConsequences implements EmpreintesDesConsequences {

  private static final JsonMapper JSON = JsonMapper.builder()
    .addMixIn(Annulation.class, MetadonneesDAnnulation.class)
    .addMixIn(EvenementDAtelier.class, MetadonneesDuFait.class)
    .addMixIn(Horodatage.class, MetadonneesDHorodatage.class)
    .addMixIn(SuiviDAtelier.class, RevisionTechnique.class)
    .build();
  private final CryptographieDesReferences cryptographie;

  Sha256EmpreintesDesConsequences(CryptographieDesReferences cryptographie) {
    this.cryptographie = cryptographie;
  }

  @Override
  public String calcule(SuiviDAtelier apres, Instant evaluation) {
    var consequences = new ConsequencesMetier(
      apres,
      apres
        .activites()
        .stream()
        .map(activite -> activite.a(evaluation))
        .toList()
    );
    try {
      return HexFormat.of().formatHex(cryptographie.empreinte(JSON.writeValueAsString(consequences).getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Impossible de calculer les consequences de l'acte", e);
    }
  }

  @JsonIgnoreProperties({ "auteur", "date" })
  private interface MetadonneesDAnnulation {}

  @JsonIgnoreProperties({ "auteur" })
  private interface MetadonneesDuFait {}

  @JsonIgnoreProperties({ "dateDEnregistrement" })
  private interface MetadonneesDHorodatage {}

  @JsonIgnoreProperties({ "revision" })
  private interface RevisionTechnique {}

  private record ConsequencesMetier(SuiviDAtelier suivi, List<IntervalleDActivite> activites) {}
}
