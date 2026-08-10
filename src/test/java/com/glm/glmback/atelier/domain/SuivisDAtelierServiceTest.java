package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class SuivisDAtelierServiceTest {

  private static final ElementEngageId ELEMENT_INCONNU = new ElementEngageId(UUID.randomUUID());

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();
  private final SuivisDAtelierService atelier = SuivisDAtelierService.builder()
    .repository(suivis)
    .elements(new ElementsEngageablesFiges())
    .fonctions(new FonctionsFigees())
    .clock(maintenant::get);

  @Test
  void shouldEngagerUnElementDansLAtelier() {
    SuiviDAtelier suivi = engage();

    assertThat(suivi.element()).isEqualTo(elementEngageOf2026000042());
    assertThat(suivi.engagement()).isEqualTo(engagementParLeroy());
    assertThat(suivi.etat()).isEqualTo(EtatDAtelier.EN_ATTENTE);
  }

  @Test
  void shouldNotEngagerUnElementInconnu() {
    EngagementAEnregistrer commande = new EngagementAEnregistrer(ELEMENT_INCONNU, AUTEUR_LEROY);

    assertThatThrownBy(() -> atelier.engage(commande)).isExactlyInstanceOf(ElementEngageableIntrouvableException.class);
  }

  @Test
  void shouldNotEngagerDeuxFoisLeMemeElement() {
    engage();
    EngagementAEnregistrer commande = new EngagementAEnregistrer(ELEMENT_OF_2026_000042, AUTEUR_LEROY);

    assertThatThrownBy(() -> atelier.engage(commande)).isExactlyInstanceOf(ElementDejaEngageException.class);
  }

  @Test
  void shouldDaterUnPointageSurLHorlogeEtEstampillerLaFonction() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_9H);

    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id()));

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> {
        assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_9H);
        assertThat(evenement.dateDEnregistrement()).isEqualTo(LE_10_MAI_2026_A_9H);
        assertThat(evenement.auteur()).isEqualTo(AUTEUR_DUPONT);
        assertThat(evenement.poste()).contains(POSTE_FRAISEUSE_1);
        assertThat(evenement.nature()).contains(NATURE_FRAISAGE);
        assertThat(evenement.estUneRegularisation()).isFalse();
      });
  }

  @Test
  void shouldPointerSansPosteDeTravail() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(new PointageAEnregistrer(engage.id(), TypeDEvenementDAtelier.DEBUT, OPERATEUR_DUPONT));

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.poste()).isEmpty());
  }

  /**
   * Aucune fonction au profil : rien n'est estampille, et surtout rien n'est refuse. Le client ne demande aucun
   * blocage, et toutes les entreprises clientes n'ont pas de specialites a distinguer.
   */
  @Test
  void shouldPointerSansFonctionAuProfil() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_MARTIN)
        .poste(Optional.of(POSTE_FRAISEUSE_1))
    );

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.nature()).isEmpty());
  }

  @Test
  void shouldMenerDeuxPostesDeFrontSurLeMemeElement() {
    SuiviDAtelier engage = engage();
    atelier.pointe(debutSurFraiseuse1(engage.id()));

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_DUPONT)
        .poste(Optional.of(POSTE_FRAISEUSE_2))
    );

    assertThat(pointe.activitesEnCours())
      .extracting(ActiviteEnCours::poste)
      .containsExactlyInAnyOrder(Optional.of(POSTE_FRAISEUSE_1), Optional.of(POSTE_FRAISEUSE_2));
  }

  @Test
  void shouldNotPointerSurUnSuiviCloture() {
    SuiviDAtelier engage = engage();
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY));
    PointageAEnregistrer commande = debutSurFraiseuse1(engage.id());

    assertThatThrownBy(() -> atelier.pointe(commande)).isExactlyInstanceOf(SuiviDAtelierClotureException.class);
  }

  @Test
  void shouldNotPointerSurUnSuiviInconnu() {
    PointageAEnregistrer commande = debutSurFraiseuse1(SuiviDAtelierId.newId());

    assertThatThrownBy(() -> atelier.pointe(commande)).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  @Test
  void shouldDaterUneRegularisationSurLaValeurFournie() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    SuiviDAtelier regularise = atelier.regularise(regularisationDeDebutA(engage.id(), LE_10_MAI_2026_A_8H));

    assertThat(regularise.journal().actifs())
      .singleElement()
      .satisfies(evenement -> {
        assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_8H);
        assertThat(evenement.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
        assertThat(evenement.operateur()).isEqualTo(OPERATEUR_DUPONT);
        assertThat(evenement.auteur()).isEqualTo(AUTEUR_LEROY);
        assertThat(evenement.estUneRegularisation()).isTrue();
        assertThat(evenement.estSaisiParUnTiers()).isTrue();
      });
  }

  @Test
  void shouldAnnulerUneSaisieEnTrop() {
    SuiviDAtelier engage = engage();
    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id()));
    EvenementDAtelierId debut = pointe.journal().actifs().getFirst().id();

    SuiviDAtelier annule = atelier.annule(
      AnnulationAEnregistrer.builder().suivi(engage.id()).evenement(debut).auteur(AUTEUR_LEROY).motif(MOTIF_ERREUR_DE_SAISIE)
    );

    assertThat(annule.journal().actifs()).isEmpty();
    assertThat(annule.journal().evenement(debut))
      .get()
      .satisfies(evenement -> assertThat(evenement.annulation()).map(Annulation::auteur).contains(AUTEUR_LEROY));
  }

  @Test
  void shouldCorrigerUneSaisieFausseEnUnSeulActe() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_9H);
    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id()));
    EvenementDAtelierId debutFautif = pointe.journal().actifs().getFirst().id();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    SuiviDAtelier corrige = atelier.corrige(
      new CorrectionAEnregistrer(debutFautif, MOTIF_ERREUR_DE_SAISIE, regularisationDeDebutA(engage.id(), LE_10_MAI_2026_A_8H))
    );

    assertThat(corrige.activites())
      .singleElement()
      .satisfies(intervalle -> assertThat(intervalle.debut()).isEqualTo(LE_10_MAI_2026_A_8H));
  }

  @Test
  void shouldCloturerALHeureCouranteParDefaut() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_17H);

    SuiviDAtelier cloture = atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY));

    assertThat(cloture.estCloture()).isTrue();
    assertThat(cloture.cloture())
      .get()
      .satisfies(fin -> assertThat(fin.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H));
  }

  @Test
  void shouldCloturerAUneHeurePassee() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    SuiviDAtelier cloture = atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_17H)));

    assertThat(cloture.cloture())
      .get()
      .satisfies(fin -> assertThat(fin.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H));
  }

  /**
   * La cloture ferme le pointage aux operateurs, elle ne fige rien pour le gestionnaire.
   */
  @Test
  void shouldRegulariserUnSuiviDejaCloture() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_17H);
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY));

    SuiviDAtelier regularise = atelier.regularise(regularisationDeDebutA(engage.id(), LE_10_MAI_2026_A_8H));

    assertThat(regularise.journal().actifs()).hasSize(1);
  }

  @Test
  void shouldRouvrirUnSuiviCloture() {
    SuiviDAtelier engage = engage();
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY));

    SuiviDAtelier rouvert = atelier.annuleLaCloture(engage.id());

    assertThat(rouvert.estCloture()).isFalse();
  }

  @Test
  void shouldNotGetSuiviInconnu() {
    SuiviDAtelierId inconnu = SuiviDAtelierId.newId();

    assertThatThrownBy(() -> atelier.get(inconnu)).isExactlyInstanceOf(SuiviDAtelierIntrouvableException.class);
  }

  @Test
  void shouldListerLesSuivisActifsSansPeriode() {
    SuiviDAtelier engage = engage();

    assertThat(atelier.list(Optional.empty(), Set.of(EtatDAtelier.EN_ATTENTE), firstPageOfTen()).content()).containsExactly(engage);
  }

  private SuiviDAtelier engage() {
    return atelier.engage(new EngagementAEnregistrer(ELEMENT_OF_2026_000042, AUTEUR_LEROY));
  }

  private static PointageAEnregistrer debutSurFraiseuse1(SuiviDAtelierId suivi) {
    return PointageAEnregistrer.builder()
      .suivi(suivi)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_DUPONT)
      .poste(Optional.of(POSTE_FRAISEUSE_1));
  }

  private static RegularisationAEnregistrer regularisationDeDebutA(SuiviDAtelierId suivi, Instant date) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_DUPONT)
      .poste(Optional.of(POSTE_FRAISEUSE_1))
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(date);
  }

  private static final class ElementsEngageablesFiges implements ElementsEngageables {

    @Override
    public Optional<ElementEngage> get(ElementEngageId id) {
      return id.equals(ELEMENT_OF_2026_000042) ? Optional.of(elementEngageOf2026000042()) : Optional.empty();
    }
  }

  private static final class FonctionsFigees implements FonctionsDesOperateurs {

    @Override
    public Optional<NatureDOperation> fonction(Operateur operateur) {
      return operateur.equals(OPERATEUR_DUPONT) ? Optional.of(NATURE_FRAISAGE) : Optional.empty();
    }
  }
}
