package com.glm.glmback.atelier.domain.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.AUTEUR_LEROY;

import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Engagement;
import com.glm.glmback.atelier.domain.JournalDAtelier;
import com.glm.glmback.atelier.domain.Nom;
import com.glm.glmback.atelier.domain.NomDElement;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.Prenom;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.TypeDElementEngage;
import java.time.Instant;
import java.util.UUID;

public final class FinsAutomatiquesFixture {

  public static final Instant LE_10_MAI_2026_A_21H_UTC = Instant.parse("2026-05-10T21:00:00Z");

  private FinsAutomatiquesFixture() {}

  public static ElementEngage elementDeFinAutomatiqueNomme(String nom) {
    return new ElementEngage(new ElementEngageId(UUID.randomUUID()), new NomDElement(nom), TypeDElementEngage.ORDRE_DE_FABRICATION);
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
}
