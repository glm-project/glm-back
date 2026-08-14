package com.glm.glmback.operateur.domain;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class OperateursFixture {

  public static final Nom NOM_DUPONT = new Nom("Dupont");
  public static final Nom NOM_MARTIN = new Nom("Martin");
  public static final Prenom PRENOM_JEAN = new Prenom("Jean");
  public static final Prenom PRENOM_SOPHIE = new Prenom("Sophie");

  public static final Matricule MATRICULE_049 = new Matricule("049");
  public static final Matricule MATRICULE_050 = new Matricule("050");

  public static final TauxHoraire TAUX_HORAIRE_22 = new TauxHoraire(new BigDecimal("22.00"));
  public static final TauxHoraire TAUX_HORAIRE_25 = new TauxHoraire(new BigDecimal("25.00"));

  public static final LibelleDePoste LIBELLE_TOUR_1 = new LibelleDePoste("Tour 1");
  public static final LibelleDePoste LIBELLE_POSTE_DE_SOUDURE = new LibelleDePoste("Poste de soudure");

  public static final NatureDeTravail NATURE_TOURNAGE = new NatureDeTravail("tournage");
  public static final NatureDeTravail NATURE_SOUDAGE = new NatureDeTravail("soudage");

  public static final PosteHabilitableId ID_TOUR_1 = new PosteHabilitableId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  public static final PosteHabilitableId ID_POSTE_DE_SOUDURE = new PosteHabilitableId(
    UUID.fromString("22222222-2222-2222-2222-222222222222")
  );

  public static final PosteHabilitable POSTE_HABILITABLE_TOUR_1 = new PosteHabilitable(ID_TOUR_1, LIBELLE_TOUR_1, NATURE_TOURNAGE);
  public static final PosteHabilitable POSTE_HABILITABLE_POSTE_DE_SOUDURE = new PosteHabilitable(
    ID_POSTE_DE_SOUDURE,
    LIBELLE_POSTE_DE_SOUDURE,
    NATURE_SOUDAGE
  );

  private OperateursFixture() {}

  public static Set<PosteHabilitableId> habilitationsDeSoudureEtDeTournage() {
    return Set.of(ID_POSTE_DE_SOUDURE, ID_TOUR_1);
  }

  public static Set<PosteHabilitableId> habilitationDeTournage() {
    return Set.of(ID_TOUR_1);
  }

  public static Operateur operateurDupont() {
    return operateurDupont(OperateurId.newId());
  }

  public static Operateur operateurDupont(OperateurId id) {
    return Operateur.builder()
      .id(id)
      .nom(NOM_DUPONT)
      .prenom(PRENOM_JEAN)
      .matricule(MATRICULE_049.value())
      .tauxHoraire(null)
      .postes(habilitationsDeSoudureEtDeTournage());
  }

  public static Operateur operateurMartinSansMatricule() {
    return Operateur.builder()
      .id(OperateurId.newId())
      .nom(NOM_MARTIN)
      .prenom(PRENOM_SOPHIE)
      .matricule(null)
      .tauxHoraire(null)
      .postes(Set.of());
  }

  public static OperateurACreer operateurACreerDupont() {
    return new OperateurACreer(NOM_DUPONT, PRENOM_JEAN, Optional.of(MATRICULE_049), Optional.empty(), habilitationsDeSoudureEtDeTournage());
  }

  public static OperateurACreer operateurACreerDupontAvecTauxHoraire() {
    return new OperateurACreer(
      NOM_DUPONT,
      PRENOM_JEAN,
      Optional.of(MATRICULE_049),
      Optional.of(TAUX_HORAIRE_22),
      habilitationsDeSoudureEtDeTournage()
    );
  }

  public static OperateurACreer operateurACreerMartin() {
    return new OperateurACreer(NOM_MARTIN, PRENOM_SOPHIE, Optional.of(MATRICULE_050), Optional.empty(), habilitationDeTournage());
  }

  public static OperateurACreer operateurACreerMartinSansMatricule() {
    return new OperateurACreer(NOM_MARTIN, PRENOM_SOPHIE, Optional.empty(), Optional.empty(), Set.of());
  }

  public static OperateurACreer operateurACreerDupontSansMatricule() {
    return new OperateurACreer(NOM_DUPONT, PRENOM_JEAN, Optional.empty(), Optional.empty(), Set.of());
  }

  public static OperateurAModifier operateurAModifierDupont(OperateurId id) {
    return new OperateurAModifier(id, NOM_DUPONT, PRENOM_JEAN, Optional.of(MATRICULE_049), Optional.empty(), habilitationDeTournage());
  }

  public static OperateurAModifier operateurAModifierDupontAvecTauxHoraire(OperateurId id) {
    return new OperateurAModifier(
      id,
      NOM_DUPONT,
      PRENOM_JEAN,
      Optional.of(MATRICULE_049),
      Optional.of(TAUX_HORAIRE_25),
      habilitationDeTournage()
    );
  }

  public static OperateurAModifier operateurAModifierMartin(OperateurId id) {
    return new OperateurAModifier(id, NOM_MARTIN, PRENOM_SOPHIE, Optional.of(MATRICULE_050), Optional.empty(), habilitationDeTournage());
  }

  public static ProfilDOperateur profilDeDupont() {
    return new ProfilDOperateur(operateurDupont(), java.util.List.of(POSTE_HABILITABLE_TOUR_1, POSTE_HABILITABLE_POSTE_DE_SOUDURE));
  }

  public static OperateurCriteria criteresDeTournage() {
    return new OperateurCriteria(Optional.of(ID_TOUR_1));
  }

  public static OperateurCriteria criteresSansFiltre() {
    return new OperateurCriteria(Optional.empty());
  }
}
