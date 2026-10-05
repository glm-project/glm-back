package com.glm.glmback.atelier.application.gestionanomalies;

import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.gestionanomalies.ActeDeResolution;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import com.glm.glmback.atelier.domain.gestionanomalies.PropositionInvalideException;
import com.glm.glmback.shared.error.domain.Assert;
import java.util.Optional;
import java.util.UUID;

public record PropositionAConfirmer(
  UUID commande,
  AdresseDossierAnomalie adresse,
  RevisionDuSuivi revision,
  ActeDeResolution acte,
  Optional<EvenementDAtelierId> evenement,
  String empreinteConsequences
) {
  public PropositionAConfirmer {
    Assert.notNull("commande", commande);
    Assert.notNull("adresse", adresse);
    Assert.notNull("revision", revision);
    Assert.notNull("acte", acte);
    Assert.notNull("evenement", evenement);
    Assert.notBlank("empreinte des consequences", empreinteConsequences);
    var suivi = switch (acte) {
      case ActeDeResolution.Annulation annulation -> annulation.commande().suivi();
      case ActeDeResolution.Correction correction -> correction.commande().remplacement().suivi();
      case ActeDeResolution.Regularisation regularisation -> regularisation.commande().suivi();
    };
    if (
      !adresse.suivi().equals(suivi)
      || (acte instanceof ActeDeResolution.Annulation) == evenement.isPresent()
      || evenement.map(id -> id.uuid().equals(commande)).orElse(false)
    ) {
      throw new PropositionInvalideException();
    }
  }

  private PropositionAConfirmer(Builder builder) {
    this(builder.commande, builder.adresse, builder.revision, builder.acte, builder.evenement, builder.empreinte);
  }

  public boolean memeDemandeQue(PropositionAConfirmer reprise) {
    return (
      commande.equals(reprise.commande())
      && adresse.equals(reprise.adresse())
      && revision.equals(reprise.revision())
      && acte.memeDemandeQue(reprise.acte())
      && evenement.equals(reprise.evenement())
      && empreinteConsequences.equals(reprise.empreinteConsequences())
    );
  }

  public static CommandeBuilder builder() {
    return new Builder();
  }

  private static final class Builder
    implements CommandeBuilder, AdresseBuilder, RevisionBuilder, ActeBuilder, EvenementBuilder, EmpreinteBuilder
  {

    private UUID commande;
    private AdresseDossierAnomalie adresse;
    private RevisionDuSuivi revision;
    private ActeDeResolution acte;
    private Optional<EvenementDAtelierId> evenement;
    private String empreinte;

    @Override
    public AdresseBuilder commande(UUID value) {
      commande = value;
      return this;
    }

    @Override
    public RevisionBuilder adresse(AdresseDossierAnomalie value) {
      adresse = value;
      return this;
    }

    @Override
    public ActeBuilder revision(RevisionDuSuivi value) {
      revision = value;
      return this;
    }

    @Override
    public EvenementBuilder acte(ActeDeResolution value) {
      acte = value;
      return this;
    }

    @Override
    public EmpreinteBuilder evenement(Optional<EvenementDAtelierId> value) {
      evenement = value;
      return this;
    }

    @Override
    public PropositionAConfirmer empreinteConsequences(String value) {
      empreinte = value;
      return new PropositionAConfirmer(this);
    }
  }

  public interface CommandeBuilder {
    AdresseBuilder commande(UUID commande);
  }

  public interface AdresseBuilder {
    RevisionBuilder adresse(AdresseDossierAnomalie adresse);
  }

  public interface RevisionBuilder {
    ActeBuilder revision(RevisionDuSuivi revision);
  }

  public interface ActeBuilder {
    EvenementBuilder acte(ActeDeResolution acte);
  }

  public interface EvenementBuilder {
    EmpreinteBuilder evenement(Optional<EvenementDAtelierId> evenement);
  }

  public interface EmpreinteBuilder {
    PropositionAConfirmer empreinteConsequences(String empreinte);
  }
}
