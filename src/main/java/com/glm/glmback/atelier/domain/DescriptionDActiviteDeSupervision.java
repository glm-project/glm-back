package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

public record DescriptionDActiviteDeSupervision(
  ActiviteId id,
  OperateurId operateur,
  ElementDeSupervision element,
  Optional<PosteDeSupervision> poste,
  CategorieDActivite categorie,
  Instant debut,
  Echeance echeance
) {
  public DescriptionDActiviteDeSupervision {
    Assert.notNull("id", id);
    Assert.notNull("operateur", operateur);
    Assert.notNull("element", element);
    Assert.notNull("poste", poste);
    Assert.notNull("categorie", categorie);
    Assert.notNull("debut", debut);
    Assert.notNull("echeance", echeance);
  }

  private DescriptionDActiviteDeSupervision(Builder b) {
    this(b.id, b.operateur, b.element, b.poste, b.categorie, b.debut, b.echeance);
  }

  public static IdStep builder() {
    return new Builder();
  }

  private static final class Builder implements IdStep, OperateurStep, ElementStep, PosteStep, CategorieStep, DebutStep, EcheanceStep {

    private ActiviteId id;
    private OperateurId operateur;
    private ElementDeSupervision element;
    private Optional<PosteDeSupervision> poste;
    private CategorieDActivite categorie;
    private Instant debut;
    private Echeance echeance;

    public OperateurStep id(ActiviteId id) {
      this.id = id;
      return this;
    }

    public ElementStep operateur(OperateurId operateur) {
      this.operateur = operateur;
      return this;
    }

    public PosteStep element(ElementDeSupervision element) {
      this.element = element;
      return this;
    }

    public CategorieStep poste(Optional<PosteDeSupervision> poste) {
      this.poste = poste;
      return this;
    }

    public DebutStep categorie(CategorieDActivite categorie) {
      this.categorie = categorie;
      return this;
    }

    public EcheanceStep debut(Instant debut) {
      this.debut = debut;
      return this;
    }

    public DescriptionDActiviteDeSupervision echeance(Echeance echeance) {
      this.echeance = echeance;
      return new DescriptionDActiviteDeSupervision(this);
    }
  }

  public interface IdStep {
    OperateurStep id(ActiviteId id);
  }

  public interface OperateurStep {
    ElementStep operateur(OperateurId operateur);
  }

  public interface ElementStep {
    PosteStep element(ElementDeSupervision element);
  }

  public interface PosteStep {
    CategorieStep poste(Optional<PosteDeSupervision> poste);
  }

  public interface CategorieStep {
    DebutStep categorie(CategorieDActivite categorie);
  }

  public interface DebutStep {
    EcheanceStep debut(Instant debut);
  }

  public interface EcheanceStep {
    DescriptionDActiviteDeSupervision echeance(Echeance echeance);
  }
}
