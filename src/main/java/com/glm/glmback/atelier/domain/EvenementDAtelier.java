package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un fait du journal d'atelier : tel operateur a fait telle action sur tel poste de travail, a telle heure.
 *
 * <p>
 * L'operateur et l'auteur sont deux choses differentes : un gestionnaire qui rattrape un oubli saisit un evenement dont
 * l'operateur est celui dont le temps est affecte, et dont l'auteur est lui-meme.
 * </p>
 *
 * <p>
 * La nature de l'operation est recopiee du profil de l'operateur au moment de la saisie. Elle ne porte aucun invariant
 * et ne participe pas a l'identite de l'activite : elle n'est qu'un axe d'agregation, fige pour que la synthese d'un
 * element ne change pas le jour ou un profil change.
 * </p>
 */
public record EvenementDAtelier(
  EvenementDAtelierId id,
  TypeDEvenementDAtelier type,
  Operateur operateur,
  Optional<PosteDeTravail> poste,
  Optional<NatureDOperation> nature,
  Auteur auteur,
  Horodatage horodatage,
  Optional<Annulation> annulation
) {
  public EvenementDAtelier {
    Assert.notNull("id", id);
    Assert.notNull("type", type);
    Assert.notNull("operateur", operateur);
    Assert.notNull("poste de travail", poste);
    Assert.notNull("nature de l'operation", nature);
    Assert.notNull("auteur", auteur);
    Assert.notNull("horodatage", horodatage);
    Assert.notNull("annulation", annulation);
  }

  private EvenementDAtelier(EvenementDAtelierBuilder builder) {
    this(builder.id, builder.type, builder.operateur, builder.poste, builder.nature, builder.auteur, builder.horodatage, Optional.empty());
  }

  static EvenementDAtelierIdBuilder builder() {
    return new EvenementDAtelierBuilder();
  }

  public EvenementDAtelier annule(Annulation annulation) {
    if (estAnnule()) {
      throw new EvenementDejaAnnuleException(id);
    }

    return new EvenementDAtelier(id, type, operateur, poste, nature, auteur, horodatage, Optional.of(annulation));
  }

  public boolean estAnnule() {
    return annulation.isPresent();
  }

  /**
   * Vrai si le fait a ete enregistre apres coup, que ce soit par un gestionnaire ou par l'operateur lui-meme via une
   * option de pointage en retard : c'est l'ecart entre les deux dates qui le dit, jamais l'identite de l'auteur.
   */
  public boolean estUneRegularisation() {
    return horodatage.estDifferee();
  }

  public boolean estSaisiParUnTiers() {
    return !auteur.value().equals(operateur.value());
  }

  public CleDActivite cle() {
    return new CleDActivite(operateur, poste);
  }

  public Instant dateDeSurvenue() {
    return horodatage.dateDeSurvenue();
  }

  public Instant dateDEnregistrement() {
    return horodatage.dateDEnregistrement();
  }

  private static final class EvenementDAtelierBuilder
    implements
      EvenementDAtelierIdBuilder,
      EvenementDAtelierTypeBuilder,
      EvenementDAtelierOperateurBuilder,
      EvenementDAtelierPosteBuilder,
      EvenementDAtelierNatureBuilder,
      EvenementDAtelierAuteurBuilder,
      EvenementDAtelierHorodatageBuilder
  {

    private EvenementDAtelierId id;
    private TypeDEvenementDAtelier type;
    private Operateur operateur;
    private Optional<PosteDeTravail> poste;
    private Optional<NatureDOperation> nature;
    private Auteur auteur;
    private Horodatage horodatage;

    @Override
    public EvenementDAtelierTypeBuilder id(EvenementDAtelierId id) {
      this.id = id;

      return this;
    }

    @Override
    public EvenementDAtelierOperateurBuilder type(TypeDEvenementDAtelier type) {
      this.type = type;

      return this;
    }

    @Override
    public EvenementDAtelierPosteBuilder operateur(Operateur operateur) {
      this.operateur = operateur;

      return this;
    }

    @Override
    public EvenementDAtelierNatureBuilder poste(Optional<PosteDeTravail> poste) {
      this.poste = poste;

      return this;
    }

    @Override
    public EvenementDAtelierAuteurBuilder nature(Optional<NatureDOperation> nature) {
      this.nature = nature;

      return this;
    }

    @Override
    public EvenementDAtelierHorodatageBuilder auteur(Auteur auteur) {
      this.auteur = auteur;

      return this;
    }

    @Override
    public EvenementDAtelier horodatage(Horodatage horodatage) {
      this.horodatage = horodatage;

      return new EvenementDAtelier(this);
    }
  }

  interface EvenementDAtelierIdBuilder {
    EvenementDAtelierTypeBuilder id(EvenementDAtelierId id);
  }

  interface EvenementDAtelierTypeBuilder {
    EvenementDAtelierOperateurBuilder type(TypeDEvenementDAtelier type);
  }

  interface EvenementDAtelierOperateurBuilder {
    EvenementDAtelierPosteBuilder operateur(Operateur operateur);
  }

  interface EvenementDAtelierPosteBuilder {
    EvenementDAtelierNatureBuilder poste(Optional<PosteDeTravail> poste);
  }

  interface EvenementDAtelierNatureBuilder {
    EvenementDAtelierAuteurBuilder nature(Optional<NatureDOperation> nature);
  }

  interface EvenementDAtelierAuteurBuilder {
    EvenementDAtelierHorodatageBuilder auteur(Auteur auteur);
  }

  interface EvenementDAtelierHorodatageBuilder {
    EvenementDAtelier horodatage(Horodatage horodatage);
  }
}
