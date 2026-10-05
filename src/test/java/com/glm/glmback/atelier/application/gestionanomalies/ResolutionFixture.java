package com.glm.glmback.atelier.application.gestionanomalies;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;

import com.glm.glmback.atelier.domain.AnnulationAEnregistrer;
import com.glm.glmback.atelier.domain.CorrectionAEnregistrer;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.IntentionDePointage;
import com.glm.glmback.atelier.domain.RegularisationAEnregistrer;
import com.glm.glmback.atelier.domain.RevisionDuSuivi;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import com.glm.glmback.atelier.domain.gestionanomalies.ActeDeResolution;
import com.glm.glmback.atelier.domain.gestionanomalies.AdresseDossierAnomalie;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ResolutionFixture {

  public static final String TENANT_IMPECCMOLD = "impeccmold";
  public static final String SUJET_LEROY = "user-leroy-uuid";
  public static final String EMETTEUR_GLM = "https://identity.glm.example/realms/glm";
  public static final IdentiteDuGestionnaire GESTIONNAIRE_LEROY = new IdentiteDuGestionnaire(AUTEUR_LEROY, SUJET_LEROY, EMETTEUR_GLM);
  public static final ContexteDeResolution CONTEXTE_LEROY_IMPECCMOLD = new ContexteDeResolution(TENANT_IMPECCMOLD, GESTIONNAIRE_LEROY);
  public static final IdentiteDuGestionnaire GESTIONNAIRE_LEROY_RENOMME = new IdentiteDuGestionnaire(
    AUTEUR_MARTIN,
    SUJET_LEROY,
    EMETTEUR_GLM
  );
  public static final ContexteDeResolution CONTEXTE_LEROY_RENOMME_IMPECCMOLD = new ContexteDeResolution(
    TENANT_IMPECCMOLD,
    GESTIONNAIRE_LEROY_RENOMME
  );
  public static final ContexteDeResolution CONTEXTE_MARTIN_IMPECCMOLD = new ContexteDeResolution(
    TENANT_IMPECCMOLD,
    new IdentiteDuGestionnaire(AUTEUR_LEROY, "user-martin-uuid", EMETTEUR_GLM)
  );
  public static final ContexteDeResolution CONTEXTE_LEROY_AUTRE_EMETTEUR = new ContexteDeResolution(
    TENANT_IMPECCMOLD,
    new IdentiteDuGestionnaire(AUTEUR_LEROY, SUJET_LEROY, "https://identity.other.example/realms/glm")
  );
  public static final ContexteDeResolution CONTEXTE_LEROY_KATILYS = new ContexteDeResolution("katilys", GESTIONNAIRE_LEROY);

  private ResolutionFixture() {}

  public static RecuDActe recuDAnnulation(SuiviDAtelier suivi) {
    var pointage = suivi.journal().evenements().getFirst().id();
    var proposition = PropositionAConfirmer.builder()
      .commande(UUID.randomUUID())
      .adresse(new AdresseDossierAnomalie(suivi.id(), pointage))
      .revision(suivi.revision())
      .acte(new ActeDeResolution.Annulation(new AnnulationAEnregistrer(suivi.id(), pointage, AUTEUR_LEROY, MOTIF_ERREUR_DE_SAISIE)))
      .evenement(Optional.empty())
      .empreinteConsequences("consequences-annulation");
    return RecuDActe.builder()
      .proposition(proposition)
      .contexte(CONTEXTE_LEROY_IMPECCMOLD)
      .revisionEnregistree(new RevisionDuSuivi(1))
      .enregistreLe(LE_10_MAI_2026_A_17H)
      .activitesConcernees(Set.of())
      .evenementsTouches(List.of(pointage));
  }

  public static SuiviDAtelier suiviAvecTransitionDeMemeCategorie() {
    var debut = debutSansPosteParDupontA(LE_10_MAI_2026_A_8H);
    return suiviDAtelierEngage().enregistre(debut).enregistre(passageEnTravailDe(debut).a(LE_10_MAI_2026_A_12H));
  }

  public static PropositionAConfirmer propositionDAnnulationDeTransition(SuiviDAtelier suivi) {
    var pointage = suivi.journal().evenements().getLast().id();
    return PropositionAConfirmer.builder()
      .commande(UUID.randomUUID())
      .adresse(new AdresseDossierAnomalie(suivi.id(), pointage))
      .revision(suivi.revision())
      .acte(new ActeDeResolution.Annulation(new AnnulationAEnregistrer(suivi.id(), pointage, AUTEUR_LEROY, MOTIF_ERREUR_DE_SAISIE)))
      .evenement(Optional.empty())
      .empreinteConsequences("consequences-annulation");
  }

  public static PropositionAConfirmer propositionDeRegularisationDeFin(SuiviDAtelier suivi) {
    var debut = suivi.journal().evenements().getFirst();
    var instant = "2026-05-10T14:00:00.123456789+02:00";
    var commande = RegularisationAEnregistrer.builder()
      .suivi(suivi.id())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activiteVisee(debut.activite())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.empty())
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(Instant.parse(instant));
    return PropositionAConfirmer.builder()
      .commande(UUID.randomUUID())
      .adresse(new AdresseDossierAnomalie(suivi.id(), suivi.journal().evenements().getLast().id()))
      .revision(suivi.revision())
      .acte(new ActeDeResolution.Regularisation(commande, instant))
      .evenement(Optional.of(EvenementDAtelierId.newId()))
      .empreinteConsequences("consequences-annulation");
  }

  public static PropositionAConfirmer propositionDeCorrectionDeTransition(SuiviDAtelier suivi) {
    var proposition = propositionDeRegularisationDeFin(suivi);
    var regularisation = (ActeDeResolution.Regularisation) proposition.acte();
    return PropositionAConfirmer.builder()
      .commande(proposition.commande())
      .adresse(proposition.adresse())
      .revision(proposition.revision())
      .acte(
        new ActeDeResolution.Correction(
          new CorrectionAEnregistrer(proposition.adresse().pointage(), MOTIF_ERREUR_DE_SAISIE, regularisation.commande()),
          regularisation.instant()
        )
      )
      .evenement(proposition.evenement())
      .empreinteConsequences(proposition.empreinteConsequences());
  }
}
