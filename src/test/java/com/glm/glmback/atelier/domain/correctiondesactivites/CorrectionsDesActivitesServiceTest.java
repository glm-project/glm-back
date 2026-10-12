package com.glm.glmback.atelier.domain.correctiondesactivites;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.Activite;
import com.glm.glmback.atelier.domain.ActiviteEnCoursException;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.ActiviteViseeIntrouvableException;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.ChevauchementException;
import com.glm.glmback.atelier.domain.CleDActivite;
import com.glm.glmback.atelier.domain.DateDeSurvenueFutureException;
import com.glm.glmback.atelier.domain.EvenementApresClotureException;
import com.glm.glmback.atelier.domain.EvenementAvantEngagementException;
import com.glm.glmback.atelier.domain.EvenementDAtelier;
import com.glm.glmback.atelier.domain.EvenementDAtelierId;
import com.glm.glmback.atelier.domain.FinAvantDebutException;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateurNonHabiliteException;
import com.glm.glmback.atelier.domain.OrigineDuPointage;
import com.glm.glmback.atelier.domain.PosteDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RessourcesDAtelierEnMemoire;
import com.glm.glmback.atelier.domain.SuiviDAtelier;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import com.glm.glmback.atelier.domain.SuiviDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.SuivisDAtelierEnMemoire;
import com.glm.glmback.atelier.domain.TypeDEvenementDAtelier;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Les trois corrections du gestionnaire, avec leurs refus dans l'ordre ou le contrat les rend.
 */
@UnitTest
class CorrectionsDesActivitesServiceTest {

  private static final Instant A_9H = Instant.parse("2026-05-10T09:00:00Z");
  private static final Instant A_10H = Instant.parse("2026-05-10T10:00:00Z");
  private static final Instant A_14H = Instant.parse("2026-05-10T14:00:00Z");
  private static final Instant A_15H = Instant.parse("2026-05-10T15:00:00Z");
  private static final Instant A_22H = Instant.parse("2026-05-10T22:00:00Z");
  private static final Instant LE_11_MAI_2026_A_2H = Instant.parse("2026-05-11T02:00:00Z");
  private static final CleDActivite FRAISEUSE_2 = new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2));

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_11_MAI_2026_A_8H);
  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();
  private final RessourcesDAtelierEnMemoire ressources = RessourcesDAtelierEnMemoire.deLAtelier();
  private final CorrectionsDesActivitesService corrections = CorrectionsDesActivitesService.builder()
    .repository(suivis)
    .operateurs(ressources.operateurs())
    .postes(ressources.postes())
    .habilitations(ressources.habilitations())
    .dureeMax(() -> DUREE_MAXIMALE_HUIT_HEURES)
    .clock(maintenant::get);

  private final EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
  private final ActiviteId activite = travail.activite().orElseThrow();
  private final SuiviDAtelier suivi = suivis.create(
    suiviDAtelierEngage().enregistre(travail).enregistre(finDe(travail).a(LE_10_MAI_2026_A_12H))
  );

  @Test
  void shouldAjouterUneActiviteParUneOuvertureEtUneFinDOrigineRegularisation() {
    AjoutDUneActivite commande = ajout(A_14H, A_15H);

    CorrectionTraitee traitee = corrections.ajoute(commande);

    assertThat(traitee.rejeu()).isFalse();
    List<EvenementDAtelier> journal = suivis.get(suivi.id()).orElseThrow().journal().evenements();
    assertThat(journal).hasSize(4);
    EvenementDAtelier ouverture = journal.get(2);
    EvenementDAtelier fin = journal.get(3);
    assertThat(ouverture.id()).isEqualTo(commande.evenement());
    assertThat(ouverture.type()).isEqualTo(TypeDEvenementDAtelier.DEBUT);
    assertThat(ouverture.activite()).contains(commande.activite());
    assertThat(ouverture.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(ouverture.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(ouverture.nature()).contains(NATURE_FRAISAGE);
    assertThat(ouverture.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1);
    assertThat(ouverture.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT);
    assertThat(ouverture.dureeMax()).contains(DUREE_MAXIMALE_HUIT_HEURES);
    assertThat(ouverture.auteur()).isEqualTo(AUTEUR_LEROY);
    assertThat(ouverture.origine()).isEqualTo(OrigineDuPointage.REGULARISATION);
    assertThat(ouverture.dateDeSurvenue()).isEqualTo(A_14H);
    assertThat(ouverture.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_8H);
    assertThat(fin.id()).isEqualTo(commande.evenementDeFin());
    assertThat(fin.type()).isEqualTo(TypeDEvenementDAtelier.FIN);
    assertThat(fin.activiteVisee()).contains(commande.activite());
    assertThat(fin.activite()).isEmpty();
    assertThat(fin.origine()).isEqualTo(OrigineDuPointage.REGULARISATION);
    assertThat(fin.dateDeSurvenue()).isEqualTo(A_15H);
    assertThat(fin.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_8H);
    assertThat(traitee.suivi().activites()).anySatisfy(ajoutee -> {
      assertThat(ajoutee.id()).isEqualTo(commande.activite());
      assertThat(ajoutee.fin()).contains(A_15H);
      assertThat(ajoutee.a(maintenant.get()).finAutomatique()).isFalse();
    });
  }

  @Test
  void shouldAjouterUneNonConformiteSansPoste() {
    AjoutDUneActivite commande = AjoutDUneActivite.builder()
      .suivi(suivi.id())
      .evenement(EvenementDAtelierId.newId())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.empty())
      .categorie(CategorieDActivite.NON_CONFORMITE)
      .plage(new PlageDAjout(A_14H, A_15H))
      .auteur(AUTEUR_LEROY);

    corrections.ajoute(commande);

    EvenementDAtelier ouverture = suivis.get(suivi.id()).orElseThrow().journal().evenements().get(2);
    assertThat(ouverture.type()).isEqualTo(TypeDEvenementDAtelier.NON_CONFORMITE);
    assertThat(ouverture.poste()).isEmpty();
    assertThat(ouverture.nature()).isEmpty();
    assertThat(ouverture.coutHoraire()).isEmpty();
  }

  @Test
  void shouldAjouterUneActiviteQuiFinitLeLendemain() {
    AjoutDUneActivite commande = ajout(A_22H, LE_11_MAI_2026_A_2H);

    SuiviDAtelier apres = corrections.ajoute(commande).suivi();

    assertThat(apres.activites()).anySatisfy(ajoutee -> {
      assertThat(ajoutee.debut()).isEqualTo(A_22H);
      assertThat(ajoutee.fin()).contains(LE_11_MAI_2026_A_2H);
    });
  }

  @Test
  void shouldDeriverLIdentifiantDeLaFinDeCeluiDeLaSaisie() {
    AjoutDUneActivite commande = ajout(A_14H, A_15H);

    assertThat(commande.evenementDeFin()).isEqualTo(commande.evenementDeFin()).isNotEqualTo(commande.evenement());
    assertThat(ajout(A_14H, A_15H).evenementDeFin()).isNotEqualTo(commande.evenementDeFin());
  }

  @Test
  void shouldRepondreUnRejeuPourLaMemeSaisieSansRienEcrire() {
    AjoutDUneActivite commande = ajout(A_14H, A_15H);
    corrections.ajoute(commande);
    SuiviDAtelier apresLaPremiere = suivis.get(suivi.id()).orElseThrow();

    CorrectionTraitee rejeu = corrections.ajoute(commande);

    assertThat(rejeu.rejeu()).isTrue();
    assertThat(suivis.get(suivi.id())).contains(apresLaPremiere);
  }

  @Test
  void shouldRefuserUnAjoutSurUnSuiviInconnu() {
    AjoutDUneActivite commande = ajoutSur(SuiviDAtelierId.newId(), OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_1), A_14H, A_15H);

    assertThatThrownBy(() -> corrections.ajoute(commande)).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  @Test
  void shouldRefuserUnAjoutPourUnOperateurOuUnPosteInconnus() {
    AjoutDUneActivite operateurInconnu = ajoutSur(
      suivi.id(),
      new OperateurId(UUID.randomUUID()),
      Optional.of(POSTE_ID_FRAISEUSE_1),
      A_14H,
      A_15H
    );
    AjoutDUneActivite posteInconnu = ajoutSur(
      suivi.id(),
      OPERATEUR_ID_DUPONT,
      Optional.of(new PosteDeTravailId(UUID.randomUUID())),
      A_14H,
      A_15H
    );

    assertThatThrownBy(() -> corrections.ajoute(operateurInconnu)).isExactlyInstanceOf(OperateurDAtelierIntrouvableException.class);
    assertThatThrownBy(() -> corrections.ajoute(posteInconnu)).isExactlyInstanceOf(PosteDAtelierIntrouvableException.class);
  }

  /**
   * Les refus de ressource passent avant les refus de plage : un operateur non habilite est refuse meme si la plage est
   * incoherente.
   */
  @Test
  void shouldRefuserUnOperateurNonHabiliteAvantUnePlageIncoherente() {
    AjoutDUneActivite commande = ajoutSur(suivi.id(), OPERATEUR_ID_MARTIN, Optional.of(POSTE_ID_FRAISEUSE_1), A_15H, A_14H);

    assertThatThrownBy(() -> corrections.ajoute(commande)).isExactlyInstanceOf(OperateurNonHabiliteException.class);
  }

  @Test
  void shouldRefuserUneFinQuiNeSuitPasLeDebut() {
    assertThatThrownBy(() -> corrections.ajoute(ajout(A_15H, A_14H))).isExactlyInstanceOf(FinAvantDebutException.class);
    assertThatThrownBy(() -> corrections.ajoute(ajout(A_15H, A_15H))).isExactlyInstanceOf(FinAvantDebutException.class);
  }

  /**
   * L'ordre des refus de plage suit le contrat : fin avant debut, puis date future.
   */
  @Test
  void shouldRefuserUneFinFutureApresUneFinAvantDebut() {
    Instant futur = maintenant.get().plusSeconds(1);

    assertThatThrownBy(() -> corrections.ajoute(ajout(futur.plusSeconds(1), futur))).isExactlyInstanceOf(FinAvantDebutException.class);
    assertThatThrownBy(() -> corrections.ajoute(ajout(A_14H, futur))).isExactlyInstanceOf(DateDeSurvenueFutureException.class);
    assertThatCode(() -> corrections.ajoute(ajout(A_14H, maintenant.get()))).doesNotThrowAnyException();
  }

  @Test
  void shouldRefuserUnDebutAnterieurALEngagement() {
    assertThatThrownBy(() -> corrections.ajoute(ajout(LE_10_MAI_2026_A_7H.minusSeconds(1), A_9H))).isExactlyInstanceOf(
      EvenementAvantEngagementException.class
    );
  }

  @Test
  void shouldRefuserUneFinPosterieureALaCloture() {
    suivis.update(suivis.get(suivi.id()).orElseThrow().cloture(clotureParLeroyA(A_15H)));

    assertThatThrownBy(() -> corrections.ajoute(ajout(A_14H, A_15H.plusSeconds(1)))).isExactlyInstanceOf(
      EvenementApresClotureException.class
    );
    assertThatCode(() -> corrections.ajoute(ajout(A_14H, A_15H))).doesNotThrowAnyException();
  }

  @Test
  void shouldRefuserUnAjoutQuiChevaucheUneActiviteDeLaCle() {
    assertThatThrownBy(() -> corrections.ajoute(ajout(A_10H, A_14H))).isExactlyInstanceOf(ChevauchementException.class);
  }

  @Test
  void shouldChangerLePosteEnRecopiantLaNatureEtLeCoutDuNouveauPoste() {
    EvenementDAtelier surFraiseuse2 = debutSurFraiseuse2ParDupontA(A_14H);
    suivis.update(suivis.get(suivi.id()).orElseThrow().enregistre(surFraiseuse2).enregistre(finDe(surFraiseuse2).a(A_15H)));
    ChangementDePosteAEnregistrer commande = changement(activiteDe(surFraiseuse2), POSTE_ID_FRAISEUSE_1);

    CorrectionTraitee traitee = corrections.changeLePoste(commande);

    assertThat(traitee.rejeu()).isFalse();
    EvenementDAtelier changement = traitee
      .suivi()
      .journal()
      .evenements()
      .stream()
      .filter(e -> e.id().equals(commande.evenement()))
      .findFirst()
      .orElseThrow();
    assertThat(changement.type()).isEqualTo(TypeDEvenementDAtelier.CHANGEMENT_DE_POSTE);
    assertThat(changement.activiteVisee()).contains(activiteDe(surFraiseuse2));
    assertThat(changement.activite()).isEmpty();
    assertThat(changement.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(changement.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(changement.nature()).contains(NATURE_FRAISAGE);
    assertThat(changement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1);
    assertThat(changement.tauxHoraire()).isEqualTo(surFraiseuse2.tauxHoraire());
    assertThat(changement.auteur()).isEqualTo(AUTEUR_LEROY);
    assertThat(changement.origine()).isEqualTo(OrigineDuPointage.REGULARISATION);
    assertThat(changement.dateDeSurvenue()).isEqualTo(LE_11_MAI_2026_A_8H);
    assertThat(changement.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_8H);
    assertThat(traitee.suivi().activites()).anySatisfy(changee -> {
      assertThat(changee.id()).isEqualTo(activiteDe(surFraiseuse2));
      assertThat(changee.cle().poste()).contains(POSTE_ID_FRAISEUSE_1);
    });
  }

  @Test
  void shouldChangerLePosteUneActiviteDontLaFinEstAutomatique() {
    EvenementDAtelier orpheline = debutSurFraiseuse1ParDupontA(A_14H);
    maintenant.set(LE_11_MAI_2026_A_20H);
    suivis.update(suivis.get(suivi.id()).orElseThrow().enregistre(orpheline));

    SuiviDAtelier apres = corrections.changeLePoste(changement(activiteDe(orpheline), POSTE_ID_FRAISEUSE_2)).suivi();

    assertThat(apres.activites()).anySatisfy(changee -> {
      assertThat(changee.id()).isEqualTo(activiteDe(orpheline));
      assertThat(changee.cle()).isEqualTo(FRAISEUSE_2);
      assertThat(changee.a(maintenant.get()).finAutomatique()).isTrue();
    });
  }

  @Test
  void shouldRepondreUnRejeuPourLeMemeChangementDePoste() {
    ChangementDePosteAEnregistrer commande = changement(activite, POSTE_ID_FRAISEUSE_2);
    corrections.changeLePoste(commande);
    SuiviDAtelier apres = suivis.get(suivi.id()).orElseThrow();

    assertThat(corrections.changeLePoste(commande).rejeu()).isTrue();
    assertThat(suivis.get(suivi.id())).contains(apres);
  }

  @Test
  void shouldRefuserLesCorrectionsDUneActiviteInconnueOuEnCours() {
    EvenementDAtelier enCours = debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_7H);
    suivis.update(suivis.get(suivi.id()).orElseThrow().enregistre(enCours));
    ActiviteId inconnue = new ActiviteId(UUID.randomUUID());

    assertThatThrownBy(() -> corrections.changeLePoste(changement(inconnue, POSTE_ID_FRAISEUSE_2))).isExactlyInstanceOf(
      ActiviteViseeIntrouvableException.class
    );
    assertThatThrownBy(() -> corrections.changeLePoste(changement(activiteDe(enCours), POSTE_ID_FRAISEUSE_2))).isExactlyInstanceOf(
      ActiviteEnCoursException.class
    );
    assertThatThrownBy(() -> corrections.retire(retrait(inconnue))).isExactlyInstanceOf(ActiviteViseeIntrouvableException.class);
    assertThatThrownBy(() -> corrections.retire(retrait(activiteDe(enCours)))).isExactlyInstanceOf(ActiviteEnCoursException.class);
  }

  @Test
  void shouldRefuserUneCorrectionSurUnSuiviInconnu() {
    SuiviDAtelierId inconnu = SuiviDAtelierId.newId();

    assertThatThrownBy(() ->
      corrections.changeLePoste(
        ChangementDePosteAEnregistrer.builder()
          .suivi(inconnu)
          .evenement(EvenementDAtelierId.newId())
          .activite(activite)
          .poste(POSTE_ID_FRAISEUSE_2)
          .auteur(AUTEUR_LEROY)
      )
    ).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
    assertThatThrownBy(() ->
      corrections.retire(
        RetraitAEnregistrer.builder().suivi(inconnu).evenement(EvenementDAtelierId.newId()).activite(activite).auteur(AUTEUR_LEROY)
      )
    ).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  /**
   * L'activite se verifie avant le poste : une activite en cours est refusee pour cela, meme vers un poste inconnu.
   */
  @Test
  void shouldRefuserUneActiviteEnCoursAvantUnPosteInconnu() {
    EvenementDAtelier enCours = debutSurFraiseuse1ParDupontA(LE_11_MAI_2026_A_7H);
    suivis.update(suivis.get(suivi.id()).orElseThrow().enregistre(enCours));

    assertThatThrownBy(() ->
      corrections.changeLePoste(changement(activiteDe(enCours), new PosteDeTravailId(UUID.randomUUID())))
    ).isExactlyInstanceOf(ActiviteEnCoursException.class);
  }

  @Test
  void shouldRefuserUnNouveauPosteInconnuOuNonHabilite() {
    EvenementDAtelier deMartin = debutSurFraiseuse1ParMartinA(A_14H);
    suivis.update(suivis.get(suivi.id()).orElseThrow().enregistre(deMartin).enregistre(finDe(deMartin).a(A_15H)));

    assertThatThrownBy(() -> corrections.changeLePoste(changement(activite, new PosteDeTravailId(UUID.randomUUID())))).isExactlyInstanceOf(
      PosteDAtelierIntrouvableException.class
    );
    assertThatThrownBy(() -> corrections.changeLePoste(changement(activiteDe(deMartin), POSTE_ID_FRAISEUSE_2))).isExactlyInstanceOf(
      OperateurNonHabiliteException.class
    );
  }

  @Test
  void shouldRefuserUnChangementVersLePosteActuel() {
    assertThatThrownBy(() -> corrections.changeLePoste(changement(activite, POSTE_ID_FRAISEUSE_1)))
      .isExactlyInstanceOf(MemePosteException.class)
      .hasMessageContaining(POSTE_ID_FRAISEUSE_1.uuid().toString());
  }

  @Test
  void shouldRefuserUnChangementQuiChevaucheLeNouveauPoste() {
    EvenementDAtelier surFraiseuse2 = debutSurFraiseuse2ParDupontA(A_10H);
    suivis.update(suivis.get(suivi.id()).orElseThrow().enregistre(surFraiseuse2).enregistre(finDe(surFraiseuse2).a(A_14H)));

    assertThatThrownBy(() -> corrections.changeLePoste(changement(activite, POSTE_ID_FRAISEUSE_2))).isExactlyInstanceOf(
      ChevauchementException.class
    );
  }

  @Test
  void shouldRefuserUnChangementOuUnRetraitPosterieursALaCloture() {
    suivis.update(suivis.get(suivi.id()).orElseThrow().cloture(clotureParLeroyA(A_22H)));

    assertThatThrownBy(() -> corrections.changeLePoste(changement(activite, POSTE_ID_FRAISEUSE_2))).isExactlyInstanceOf(
      EvenementApresClotureException.class
    );
    assertThatThrownBy(() -> corrections.retire(retrait(activite))).isExactlyInstanceOf(EvenementApresClotureException.class);
  }

  @Test
  void shouldRetirerUneActiviteParUnFaitDePlus() {
    RetraitAEnregistrer commande = retrait(activite);

    CorrectionTraitee traitee = corrections.retire(commande);

    assertThat(traitee.rejeu()).isFalse();
    assertThat(traitee.suivi().journal().evenements()).hasSize(3);
    EvenementDAtelier retrait = traitee.suivi().journal().evenements().get(2);
    assertThat(retrait.id()).isEqualTo(commande.evenement());
    assertThat(retrait.type()).isEqualTo(TypeDEvenementDAtelier.RETRAIT);
    assertThat(retrait.activiteVisee()).contains(activite);
    assertThat(retrait.activite()).isEmpty();
    assertThat(retrait.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(retrait.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(retrait.auteur()).isEqualTo(AUTEUR_LEROY);
    assertThat(retrait.origine()).isEqualTo(OrigineDuPointage.REGULARISATION);
    assertThat(retrait.dateDeSurvenue()).isEqualTo(LE_11_MAI_2026_A_8H);
    assertThat(traitee.suivi().activites()).isEmpty();
  }

  /**
   * On doit pouvoir retirer une activite pointee sur un poste dont l'operateur a perdu l'habilitation : le retrait ne la
   * verifie pas.
   */
  @Test
  void shouldRetirerSansVerifierLHabilitation() {
    EvenementDAtelier deMartin = debutSurFraiseuse1ParMartinA(A_14H);
    suivis.update(suivis.get(suivi.id()).orElseThrow().enregistre(deMartin).enregistre(finDe(deMartin).a(A_15H)));

    assertThat(corrections.retire(retrait(activiteDe(deMartin))).suivi().activites()).extracting(Activite::id).containsExactly(activite);
  }

  @Test
  void shouldRepondreUnRejeuPourLeMemeRetraitMemeSiLActiviteEstDejaRetiree() {
    RetraitAEnregistrer commande = retrait(activite);
    corrections.retire(commande);
    SuiviDAtelier apres = suivis.get(suivi.id()).orElseThrow();

    assertThat(corrections.retire(commande).rejeu()).isTrue();
    assertThat(suivis.get(suivi.id())).contains(apres);
    assertThatThrownBy(() -> corrections.retire(retrait(activite))).isExactlyInstanceOf(ActiviteViseeIntrouvableException.class);
  }

  private static ActiviteId activiteDe(EvenementDAtelier ouvrant) {
    return ouvrant.activite().orElseThrow();
  }

  private AjoutDUneActivite ajout(Instant debut, Instant fin) {
    return ajoutSur(suivi.id(), OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_1), debut, fin);
  }

  private static AjoutDUneActivite ajoutSur(
    SuiviDAtelierId suivi,
    OperateurId operateur,
    Optional<PosteDeTravailId> poste,
    Instant debut,
    Instant fin
  ) {
    return AjoutDUneActivite.builder()
      .suivi(suivi)
      .evenement(EvenementDAtelierId.newId())
      .operateur(operateur)
      .poste(poste)
      .categorie(CategorieDActivite.TRAVAIL)
      .plage(new PlageDAjout(debut, fin))
      .auteur(AUTEUR_LEROY);
  }

  private ChangementDePosteAEnregistrer changement(ActiviteId visee, PosteDeTravailId poste) {
    return ChangementDePosteAEnregistrer.builder()
      .suivi(suivi.id())
      .evenement(EvenementDAtelierId.newId())
      .activite(visee)
      .poste(poste)
      .auteur(AUTEUR_LEROY);
  }

  private RetraitAEnregistrer retrait(ActiviteId visee) {
    return RetraitAEnregistrer.builder().suivi(suivi.id()).evenement(EvenementDAtelierId.newId()).activite(visee).auteur(AUTEUR_LEROY);
  }
}
