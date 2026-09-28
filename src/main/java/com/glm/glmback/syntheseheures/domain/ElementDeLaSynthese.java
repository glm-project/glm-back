package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Un element sur lequel l'operateur a travaille ou pointe dans la semaine, et le temps qu'il y a passe.
 *
 * <p>
 * Nom et type viennent du suivi, copies a l'engagement ; reference et description sont relues au referentiel, et
 * absentes si l'element a ete supprime. Les durees se cumulent par element : une heure passee sur deux elements compte
 * sur chacun.
 * </p>
 */
public record ElementDeLaSynthese(
  ElementEngage element,
  Optional<ReferenceDElement> reference,
  Optional<DescriptionDElement> description,
  Duration duree,
  Duration dureeNonConformite,
  Duration dureePresumee,
  List<PosteDeLElement> postes
) {
  public ElementDeLaSynthese {
    Assert.notNull("element", element);
    Assert.notNull("reference", reference);
    Assert.notNull("description", description);
    Assert.notNull("duree", duree);
    Assert.notNull("duree de non conformite", dureeNonConformite);
    Assert.notNull("duree presumee", dureePresumee);
    Assert.field("postes", postes).notNull().noNullElement();
  }

  private ElementDeLaSynthese(ElementDeLaSyntheseBuilder builder) {
    this(
      builder.element,
      builder.reference,
      builder.description,
      builder.duree,
      builder.dureeNonConformite,
      builder.dureePresumee,
      builder.postes
    );
  }

  static ElementDeLaSyntheseElementBuilder builder() {
    return new ElementDeLaSyntheseBuilder();
  }

  private static final class ElementDeLaSyntheseBuilder
    implements
      ElementDeLaSyntheseElementBuilder,
      ElementDeLaSyntheseReferenceBuilder,
      ElementDeLaSyntheseDescriptionBuilder,
      ElementDeLaSyntheseDureeBuilder,
      ElementDeLaSyntheseDureeNonConformiteBuilder,
      ElementDeLaSyntheseDureePresumeeBuilder,
      ElementDeLaSynthesePostesBuilder
  {

    private ElementEngage element;
    private Optional<ReferenceDElement> reference;
    private Optional<DescriptionDElement> description;
    private Duration duree;
    private Duration dureeNonConformite;
    private Duration dureePresumee;
    private List<PosteDeLElement> postes;

    @Override
    public ElementDeLaSyntheseReferenceBuilder element(ElementEngage element) {
      this.element = element;

      return this;
    }

    @Override
    public ElementDeLaSyntheseDescriptionBuilder reference(Optional<ReferenceDElement> reference) {
      this.reference = reference;

      return this;
    }

    @Override
    public ElementDeLaSyntheseDureeBuilder description(Optional<DescriptionDElement> description) {
      this.description = description;

      return this;
    }

    @Override
    public ElementDeLaSyntheseDureeNonConformiteBuilder duree(Duration duree) {
      this.duree = duree;

      return this;
    }

    @Override
    public ElementDeLaSyntheseDureePresumeeBuilder dureeNonConformite(Duration dureeNonConformite) {
      this.dureeNonConformite = dureeNonConformite;

      return this;
    }

    @Override
    public ElementDeLaSynthesePostesBuilder dureePresumee(Duration dureePresumee) {
      this.dureePresumee = dureePresumee;

      return this;
    }

    @Override
    public ElementDeLaSynthese postes(List<PosteDeLElement> postes) {
      this.postes = postes;

      return new ElementDeLaSynthese(this);
    }
  }

  interface ElementDeLaSyntheseElementBuilder {
    ElementDeLaSyntheseReferenceBuilder element(ElementEngage element);
  }

  interface ElementDeLaSyntheseReferenceBuilder {
    ElementDeLaSyntheseDescriptionBuilder reference(Optional<ReferenceDElement> reference);
  }

  interface ElementDeLaSyntheseDescriptionBuilder {
    ElementDeLaSyntheseDureeBuilder description(Optional<DescriptionDElement> description);
  }

  interface ElementDeLaSyntheseDureeBuilder {
    ElementDeLaSyntheseDureeNonConformiteBuilder duree(Duration duree);
  }

  interface ElementDeLaSyntheseDureeNonConformiteBuilder {
    ElementDeLaSyntheseDureePresumeeBuilder dureeNonConformite(Duration dureeNonConformite);
  }

  interface ElementDeLaSyntheseDureePresumeeBuilder {
    ElementDeLaSynthesePostesBuilder dureePresumee(Duration dureePresumee);
  }

  interface ElementDeLaSynthesePostesBuilder {
    ElementDeLaSynthese postes(List<PosteDeLElement> postes);
  }
}
