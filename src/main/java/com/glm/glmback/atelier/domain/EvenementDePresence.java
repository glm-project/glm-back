package com.glm.glmback.atelier.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.Optional;

/**
 * Un fait du journal de presence : l'operateur est arrive, s'est mis en pause, a repris ou est parti.
 *
 * <p>
 * L'operateur n'est pas un composant : la journee de travail le porte deja. L'auteur, lui, en est un, parce qu'un
 * gestionnaire peut saisir l'arrivee que l'operateur a oublie de pointer.
 * </p>
 */
public record EvenementDePresence(
  EvenementDePresenceId id,
  TypeDEvenementDePresence type,
  Auteur auteur,
  Horodatage horodatage,
  Optional<Annulation> annulation
) {
  public EvenementDePresence {
    Assert.notNull("id", id);
    Assert.notNull("type", type);
    Assert.notNull("auteur", auteur);
    Assert.notNull("horodatage", horodatage);
    Assert.notNull("annulation", annulation);
  }

  static EvenementDePresenceIdBuilder builder() {
    return id -> type -> auteur -> horodatage -> new EvenementDePresence(id, type, auteur, horodatage, Optional.empty());
  }

  public EvenementDePresence annule(Annulation annulation) {
    if (estAnnule()) {
      throw new EvenementDePresenceDejaAnnuleException(id);
    }

    return new EvenementDePresence(id, type, auteur, horodatage, Optional.of(annulation));
  }

  public boolean estAnnule() {
    return annulation.isPresent();
  }

  public Instant dateDeSurvenue() {
    return horodatage.dateDeSurvenue();
  }

  public Instant dateDEnregistrement() {
    return horodatage.dateDEnregistrement();
  }

  interface EvenementDePresenceIdBuilder {
    EvenementDePresenceTypeBuilder id(EvenementDePresenceId id);
  }

  interface EvenementDePresenceTypeBuilder {
    EvenementDePresenceAuteurBuilder type(TypeDEvenementDePresence type);
  }

  interface EvenementDePresenceAuteurBuilder {
    EvenementDePresenceHorodatageBuilder auteur(Auteur auteur);
  }

  interface EvenementDePresenceHorodatageBuilder {
    EvenementDePresence horodatage(Horodatage horodatage);
  }
}
