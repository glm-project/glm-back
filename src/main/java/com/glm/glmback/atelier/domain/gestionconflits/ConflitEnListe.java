package com.glm.glmback.atelier.domain.gestionconflits;

import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.shared.error.domain.Assert;

/** Une sequence en conflit, lue dans les projections sans reconstituer son journal. */
public record ConflitEnListe(
  AdresseDossierConflit adresse,
  RevisionDuSuivi revision,
  ElementEngage element,
  CleDActivite cle,
  RepereDeSequence repere
) {
  public ConflitEnListe {
    Assert.notNull("adresse", adresse);
    Assert.notNull("revision", revision);
    Assert.notNull("element", element);
    Assert.notNull("cle", cle);
    Assert.notNull("repere", repere);
  }

  public static AdresseBuilder builder() {
    return adresse -> revision -> element -> cle -> repere -> new ConflitEnListe(adresse, revision, element, cle, repere);
  }

  public interface AdresseBuilder {
    RevisionBuilder adresse(AdresseDossierConflit adresse);
  }

  public interface RevisionBuilder {
    ElementBuilder revision(RevisionDuSuivi revision);
  }

  public interface ElementBuilder {
    CleBuilder element(ElementEngage element);
  }

  public interface CleBuilder {
    RepereBuilder cle(CleDActivite cle);
  }

  public interface RepereBuilder {
    ConflitEnListe repere(RepereDeSequence repere);
  }
}
