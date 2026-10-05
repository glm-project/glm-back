package com.glm.glmback.atelier.domain.gestionanomalies;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;

/**
 * Une activite terminee a son echeance faute de fin reelle, lue dans les projections sans reconstituer son journal.
 *
 * <p>
 * L'adresse du dossier est celle de l'ouvrant actif ; {@code activite} est l'identite d'origine de l'activite, que
 * visent les actes. La ligne donne le debut et l'echeance : aucune duree n'est calculee ici.
 * </p>
 */
public record FinAutomatiqueEnListe(
  AdresseDossierAnomalie adresse,
  RevisionDuSuivi revision,
  ElementEngage element,
  CleDActivite cle,
  ActiviteId activite,
  Instant debut,
  Instant echeance
) {
  public FinAutomatiqueEnListe {
    Assert.notNull("adresse", adresse);
    Assert.notNull("revision", revision);
    Assert.notNull("element", element);
    Assert.notNull("cle", cle);
    Assert.notNull("activite", activite);
    Assert.notNull("debut", debut);
    Assert.field("echeance", echeance).afterOrAt(debut);
  }

  public static AdresseBuilder builder() {
    return adresse ->
      revision ->
        element ->
          cle -> activite -> debut -> echeance -> new FinAutomatiqueEnListe(adresse, revision, element, cle, activite, debut, echeance);
  }

  public interface AdresseBuilder {
    RevisionBuilder adresse(AdresseDossierAnomalie adresse);
  }

  public interface RevisionBuilder {
    ElementBuilder revision(RevisionDuSuivi revision);
  }

  public interface ElementBuilder {
    CleBuilder element(ElementEngage element);
  }

  public interface CleBuilder {
    ActiviteBuilder cle(CleDActivite cle);
  }

  public interface ActiviteBuilder {
    DebutBuilder activite(ActiviteId activite);
  }

  public interface DebutBuilder {
    EcheanceBuilder debut(Instant debut);
  }

  public interface EcheanceBuilder {
    FinAutomatiqueEnListe echeance(Instant echeance);
  }
}
