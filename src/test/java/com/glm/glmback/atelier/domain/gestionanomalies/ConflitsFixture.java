package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

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
import com.glm.glmback.atelier.domain.TypeDElementEngage;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class ConflitsFixture {

  private ConflitsFixture() {}

  public static SuiviDAtelier suiviOF2026000042EngageA(Instant debut) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), NOM_OF_2026_000042, TypeDElementEngage.ORDRE_DE_FABRICATION))
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

  public static OperateurConnu operateurPaulDurandConflits2043() {
    return OperateurConnu.builder()
      .id(new OperateurId(UUID.randomUUID()))
      .nom(new Nom("Durand_conflits_2043"))
      .prenom(new Prenom("Paul"))
      .tauxHoraire(null);
  }

  public static ElementEngage elementFiltrePourcentA() {
    return new ElementEngage(
      new ElementEngageId(UUID.randomUUID()),
      new NomDElement("FILTRE_2043_%A"),
      TypeDElementEngage.ORDRE_DE_FABRICATION
    );
  }

  public static ElementEngage elementFiltrePourcentB() {
    return new ElementEngage(
      new ElementEngageId(UUID.randomUUID()),
      new NomDElement("FILTRE_2043_%B"),
      TypeDElementEngage.ORDRE_DE_FABRICATION
    );
  }

  public static ElementEngage elementAutre2043() {
    return new ElementEngage(
      new ElementEngageId(UUID.randomUUID()),
      new NomDElement("AUTRE_2043"),
      TypeDElementEngage.ORDRE_DE_FABRICATION
    );
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
      .remplace(Optional.empty())
      .horodatage(Horodatage.saisiA(Instant.parse("2043-01-09T08:00:00Z")));
  }

  public static ElementEngage elementLitteralePourcentSoulignementAntislash2043() {
    return new ElementEngage(
      new ElementEngageId(UUID.randomUUID()),
      new NomDElement("LITTERALE_%\\2043"),
      TypeDElementEngage.ORDRE_DE_FABRICATION
    );
  }

  public static ElementEngage elementLitteraleXX2043() {
    return new ElementEngage(
      new ElementEngageId(UUID.randomUUID()),
      new NomDElement("LITTERALEXX2043"),
      TypeDElementEngage.ORDRE_DE_FABRICATION
    );
  }

  public static ConflitEnListe ligneDuPremierConflitDe(SuiviDAtelier suivi) {
    var sequence = suivi.conflits().getFirst();
    var premier = suivi
      .journal()
      .evenements()
      .stream()
      .filter(fait -> fait.id().equals(sequence.pointages().getFirst()))
      .findFirst()
      .orElseThrow();
    return ConflitEnListe.builder()
      .adresse(new AdresseDossierAnomalie(suivi.id(), premier.id()))
      .revision(suivi.revision())
      .element(suivi.element())
      .cle(sequence.cle())
      .repere(new RepereDeSequence(premier.dateDeSurvenue(), sequence.pointages().size()));
  }

  public static SuiviDAtelier suiviDu12Janvier2043Identifie(SuiviDAtelierId id) {
    return SuiviDAtelier.builder()
      .id(id)
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), new NomDElement("ORDRE_2043_01_12"), TypeDElementEngage.PRODUIT))
      .engagement(new Engagement(AUTEUR_LEROY, Instant.parse("2043-01-12T07:00:00Z")))
      .journal(JournalDAtelier.vide());
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
      .origine(OrigineDuPointage.POINTAGE)
      .remplace(Optional.empty());
  }
}
