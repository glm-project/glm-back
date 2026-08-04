package com.glm.glmback.elementdefabrication.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

public record OrdreDeFabrication(OrdreDeFabricationId id, Fiche fiche) implements ElementDeFabrication {
  public OrdreDeFabrication {
    Assert.notNull("id", id);
    Assert.notNull("fiche", fiche);
  }

  public static OrdreDeFabricationIdBuilder builder() {
    return id ->
      titre ->
        description ->
          dateDeCreation ->
            dateDeModification ->
              new OrdreDeFabrication(
                id,
                Fiche.builder().titre(titre).description(description).dateDeCreation(dateDeCreation).dateDeModification(dateDeModification)
              );
  }

  public interface OrdreDeFabricationIdBuilder {
    OrdreDeFabricationTitreBuilder id(OrdreDeFabricationId id);
  }

  public interface OrdreDeFabricationTitreBuilder {
    OrdreDeFabricationDescriptionBuilder titre(Titre titre);
  }

  public interface OrdreDeFabricationDescriptionBuilder {
    OrdreDeFabricationDateDeCreationBuilder description(Description description);
  }

  public interface OrdreDeFabricationDateDeCreationBuilder {
    OrdreDeFabricationDateDeModificationBuilder dateDeCreation(Instant dateDeCreation);
  }

  public interface OrdreDeFabricationDateDeModificationBuilder {
    OrdreDeFabrication dateDeModification(Instant dateDeModification);
  }
}
