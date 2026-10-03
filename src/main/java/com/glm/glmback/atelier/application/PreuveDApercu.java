package com.glm.glmback.atelier.application;

import com.glm.glmback.atelier.domain.ActeDeResolution;
import com.glm.glmback.atelier.domain.AdresseDossierConflit;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record PreuveDApercu(
  UUID commande,
  AdresseDossierConflit adresse,
  RevisionDuSuivi revision,
  ContexteDeResolution contexte,
  ActeDeResolution acte,
  Optional<EvenementDAtelierId> evenement,
  Instant evaluation,
  Instant expireLe,
  String empreinteConsequences
) {
  public PreuveDApercu {
    Assert.notNull("commande", commande);
    Assert.notNull("adresse", adresse);
    Assert.notNull("revision", revision);
    Assert.notNull("contexte", contexte);
    Assert.notNull("acte", acte);
    Assert.notNull("evenement", evenement);
    Assert.notNull("evaluation", evaluation);
    Assert.field("expiration", expireLe).afterOrAt(evaluation);
    Assert.notBlank("empreinte des consequences", empreinteConsequences);
  }

  private PreuveDApercu(Builder builder) {
    this(
      builder.commande,
      builder.adresse,
      builder.revision,
      builder.contexte,
      builder.acte,
      builder.evenement,
      builder.evaluation,
      builder.expireLe,
      builder.empreinte
    );
  }

  public static CommandeBuilder builder() {
    return new Builder();
  }

  private static final class Builder
    implements
      CommandeBuilder,
      AdresseBuilder,
      RevisionBuilder,
      ContexteBuilder,
      ActeBuilder,
      EvenementBuilder,
      EvaluationBuilder,
      ExpirationBuilder,
      EmpreinteBuilder
  {

    private UUID commande;
    private AdresseDossierConflit adresse;
    private RevisionDuSuivi revision;
    private ContexteDeResolution contexte;
    private ActeDeResolution acte;
    private Optional<EvenementDAtelierId> evenement;
    private Instant evaluation;
    private Instant expireLe;
    private String empreinte;

    @Override
    public AdresseBuilder commande(UUID value) {
      commande = value;
      return this;
    }

    @Override
    public RevisionBuilder adresse(AdresseDossierConflit value) {
      adresse = value;
      return this;
    }

    @Override
    public ContexteBuilder revision(RevisionDuSuivi value) {
      revision = value;
      return this;
    }

    @Override
    public ActeBuilder contexte(ContexteDeResolution value) {
      contexte = value;
      return this;
    }

    @Override
    public EvenementBuilder acte(ActeDeResolution value) {
      acte = value;
      return this;
    }

    @Override
    public EvaluationBuilder evenement(Optional<EvenementDAtelierId> value) {
      evenement = value;
      return this;
    }

    @Override
    public ExpirationBuilder evaluation(Instant value) {
      evaluation = value;
      return this;
    }

    @Override
    public EmpreinteBuilder expireLe(Instant value) {
      expireLe = value;
      return this;
    }

    @Override
    public PreuveDApercu empreinteConsequences(String value) {
      empreinte = value;
      return new PreuveDApercu(this);
    }
  }

  public interface CommandeBuilder {
    AdresseBuilder commande(UUID commande);
  }

  public interface AdresseBuilder {
    RevisionBuilder adresse(AdresseDossierConflit adresse);
  }

  public interface RevisionBuilder {
    ContexteBuilder revision(RevisionDuSuivi revision);
  }

  public interface ContexteBuilder {
    ActeBuilder contexte(ContexteDeResolution contexte);
  }

  public interface ActeBuilder {
    EvenementBuilder acte(ActeDeResolution acte);
  }

  public interface EvenementBuilder {
    EvaluationBuilder evenement(Optional<EvenementDAtelierId> evenement);
  }

  public interface EvaluationBuilder {
    ExpirationBuilder evaluation(Instant evaluation);
  }

  public interface ExpirationBuilder {
    EmpreinteBuilder expireLe(Instant expireLe);
  }

  public interface EmpreinteBuilder {
    PreuveDApercu empreinteConsequences(String empreinte);
  }
}
