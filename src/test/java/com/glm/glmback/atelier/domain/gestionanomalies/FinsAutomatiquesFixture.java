package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.AUTEUR_DUPONT;
import static com.glm.glmback.atelier.domain.AtelierFixture.AUTEUR_LEROY;
import static com.glm.glmback.atelier.domain.AtelierFixture.CATEGORIE_OF;
import static com.glm.glmback.atelier.domain.AtelierFixture.NOM_OF_2026_000042;
import static com.glm.glmback.atelier.domain.AtelierFixture.OPERATEUR_ID_DUPONT;

import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.Horodatage;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.Nom;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OrigineDuPointage;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.Prenom;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class FinsAutomatiquesFixture {

  public static final Instant LE_10_MAI_2026_A_21H_UTC = Instant.parse("2026-05-10T21:00:00Z");

  private FinsAutomatiquesFixture() {}

  public static ElementEngage elementDeFinAutomatiqueNomme(String nom) {
    return new ElementEngage(new ElementEngageId(UUID.randomUUID()), new NomDElement(nom), CATEGORIE_OF);
  }

  /** Un suivi engage avant toute date de test : ses pointages peuvent etre dates librement apres le 1er janvier 2025. */
  public static SuiviDAtelier suiviEngageLe1erJanvier2025Pour(ElementEngage element) {
    return suiviIdentifieEngageLe1erJanvier2025(SuiviDAtelierId.newId(), element);
  }

  public static SuiviDAtelier suiviIdentifieEngageLe1erJanvier2025(SuiviDAtelierId id, ElementEngage element) {
    return SuiviDAtelier.builder()
      .id(id)
      .element(element)
      .engagement(new Engagement(AUTEUR_LEROY, Instant.parse("2025-01-01T00:00:00Z")))
      .journal(JournalDAtelier.vide());
  }

  public static OperateurConnu operateurAnnaFinAutomatiquePourcent() {
    return OperateurConnu.builder()
      .id(new OperateurId(UUID.randomUUID()))
      .nom(new Nom("Fin_%Auto2044"))
      .prenom(new Prenom("Anna"))
      .tauxHoraire(null);
  }

  public static OperateurConnu operateurZoeAutre2044() {
    return OperateurConnu.builder()
      .id(new OperateurId(UUID.randomUUID()))
      .nom(new Nom("Autre2044"))
      .prenom(new Prenom("Zoe"))
      .tauxHoraire(null);
  }

  public static OperateurConnu operateurBerthe2043() {
    return OperateurConnu.builder()
      .id(new OperateurId(UUID.randomUUID()))
      .nom(new Nom("Parite_%2043"))
      .prenom(new Prenom("Berthe"))
      .tauxHoraire(null);
  }

  public static OperateurConnu operateurCharles2043() {
    return OperateurConnu.builder()
      .id(new OperateurId(UUID.randomUUID()))
      .nom(new Nom("Autre_Parite2043"))
      .prenom(new Prenom("Charles"))
      .tauxHoraire(null);
  }

  /** La ligne de la premiere activite du suivi, telle que la liste la lit a son echeance. */
  public static FinAutomatiqueEnListe ligneDeLaFinAutomatiqueDe(SuiviDAtelier suivi) {
    var activite = suivi.activites().getFirst();
    return FinAutomatiqueEnListe.builder()
      .adresse(new AdresseDossierAnomalie(suivi.id(), activite.ouvrant().id()))
      .revision(suivi.revision())
      .element(suivi.element())
      .cle(activite.cle())
      .activite(activite.id())
      .debut(activite.debut())
      .echeance(activite.echeance().value());
  }

  public static SuiviDAtelier suiviOF2026000042EngageA(Instant debut) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), NOM_OF_2026_000042, CATEGORIE_OF))
      .engagement(new Engagement(AUTEUR_LEROY, debut.minusSeconds(3600)))
      .journal(JournalDAtelier.vide());
  }

  public static OperateurConnu operateurJeanMartinPourcent() {
    return OperateurConnu.builder()
      .id(new OperateurId(UUID.randomUUID()))
      .nom(new Nom("Martin_%"))
      .prenom(new Prenom("Jean"))
      .tauxHoraire(null);
  }

  public static ElementEngage elementFiltrePourcentA() {
    return new ElementEngage(new ElementEngageId(UUID.randomUUID()), new NomDElement("FILTRE_2043_%A"), CATEGORIE_OF);
  }

  public static SuiviDAtelier suiviPourFiltre2043(ElementEngage element) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(element)
      .engagement(new Engagement(AUTEUR_LEROY, Instant.parse("2043-01-09T07:00:00Z")))
      .journal(JournalDAtelier.vide());
  }

  public static EvenementDAtelier debutDu9Janvier2043A8hPar(OperateurId operateur) {
    EvenementDAtelierId id = EvenementDAtelierId.newId();
    return EvenementDAtelier.builder()
      .id(id)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(Optional.empty())
      .operateur(operateur)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .coutHoraire(Optional.empty())
      .tauxHoraire(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .horodatage(Horodatage.saisiA(Instant.parse("2043-01-09T08:00:00Z")));
  }

  public static ElementEngage elementLitteralePourcentSoulignementAntislash2043() {
    return new ElementEngage(new ElementEngageId(UUID.randomUUID()), new NomDElement("LITTERALE_%\\2043"), CATEGORIE_OF);
  }

  public static ElementEngage elementLitteraleXX2043() {
    return new ElementEngage(new ElementEngageId(UUID.randomUUID()), new NomDElement("LITTERALEXX2043"), CATEGORIE_OF);
  }

  public static EvenementDAtelier.EvenementDAtelierHorodatageBuilder debutIdentifieSurUnPosteDeMemeUuid(EvenementDAtelierId id) {
    return EvenementDAtelier.builder()
      .id(id)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(new PosteDeTravailId(id.uuid())))
      .nature(Optional.empty())
      .coutHoraire(Optional.empty())
      .tauxHoraire(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE);
  }
}
