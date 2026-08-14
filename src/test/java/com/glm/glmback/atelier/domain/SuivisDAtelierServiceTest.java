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
  private final RessourcesDAtelierEnMemoire ressources = RessourcesDAtelierEnMemoire.deLAtelier();
  private final SuivisDAtelierService atelier = SuivisDAtelierService.builder()
    .repository(suivis)
    .elements(new ElementsEngageablesFiges())
    .operateurs(ressources.operateurs())
    .postes(ressources.postes())
    .habilitations(ressources.habilitations())
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
  void shouldDaterUnPointageSurLHorlogeEtEstampillerLaNatureDuPoste() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_9H);

    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id()));

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> {
        assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_9H);
        assertThat(evenement.dateDEnregistrement()).isEqualTo(LE_10_MAI_2026_A_9H);
        assertThat(evenement.auteur()).isEqualTo(AUTEUR_DUPONT);
        assertThat(evenement.poste()).contains(POSTE_ID_FRAISEUSE_1);
        assertThat(evenement.nature()).contains(NATURE_FRAISAGE);
        assertThat(evenement.estUneRegularisation()).isFalse();
      });
  }

  @Test
  void shouldPointerSansPosteDeTravail() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.empty())
        .auteur(AUTEUR_DUPONT)
    );

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.poste()).isEmpty());
  }

  /**
   * Sans poste, aucune nature : une entreprise sans parc machine retrouve son comportement nominal, et rien n'est
   * refuse puisque aucune habilitation n'a de sens.
   */
  @Test
  void shouldPointerSansNatureFauteDePoste() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_MARTIN)
        .poste(Optional.empty())
        .auteur(AUTEUR_MARTIN)
    );

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.nature()).isEmpty());
  }

  /**
   * La nature vient du poste, jamais de la personne : le meme operateur, sur deux postes, produit deux natures.
   */
  @Test
  void shouldReprendreLaNatureDuPosteEtNonDeLOperateur() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.of(POSTE_ID_FRAISEUSE_2))
        .auteur(AUTEUR_DUPONT)
    );

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.nature()).contains(NATURE_TOURNAGE));
  }

  /**
   * Le cout horaire du poste est copie a la saisie, sur le meme patron que la nature : fige pour qu'une
   * revalorisation ulterieure du poste ne reecrive pas l'histoire d'un pointage deja fait.
   */
  @Test
  void shouldEstampillerLeCoutHoraireDuPoste() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id()));

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1));
  }

  @Test
  void shouldPointerSansCoutHoraireFauteDePoste() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.empty())
        .auteur(AUTEUR_DUPONT)
    );

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.coutHoraire()).isEmpty());
  }

  /**
   * Fraiseuse 2 n'est pas valorisee : distinct du cas "pas de poste", sans quoi une regression qui confondrait les
   * deux resterait invisible.
   */
  @Test
  void shouldPointerSansCoutHoraireQuandLePosteNEnAPas() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.of(POSTE_ID_FRAISEUSE_2))
        .auteur(AUTEUR_DUPONT)
    );

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.coutHoraire()).isEmpty());
  }

  @Test
  void shouldEstampillerLeTauxHoraireDeLOperateur() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id()));

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT));
  }

  @Test
  void shouldPointerSansTauxHoraireQuandLOperateurNEnAPas() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_MARTIN)
        .poste(Optional.empty())
        .auteur(AUTEUR_MARTIN)
    );

    assertThat(pointe.journal().actifs())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.tauxHoraire()).isEmpty());
  }

  @Test
  void shouldNotPointerPourUnOperateurInconnu() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer commande = PointageAEnregistrer.builder()
      .suivi(engage.id())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(new OperateurId(UUID.randomUUID()))
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_LEROY);

    assertThatThrownBy(() -> atelier.pointe(commande)).isExactlyInstanceOf(OperateurDAtelierIntrouvableException.class);
  }

  @Test
  void shouldNotPointerSurUnPosteInconnu() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer commande = PointageAEnregistrer.builder()
      .suivi(engage.id())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(new PosteDeTravailId(UUID.randomUUID())))
      .auteur(AUTEUR_DUPONT);

    assertThatThrownBy(() -> atelier.pointe(commande)).isExactlyInstanceOf(PosteDAtelierIntrouvableException.class);
  }

  /**
   * L'habilitation est la seule regle dure de ce contexte : Martin n'est declare sur aucun poste, il ne peut pas y
   * pointer.
   */
  @Test
  void shouldNotPointerSurUnPosteNonHabilite() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer commande = PointageAEnregistrer.builder()
      .suivi(engage.id())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_MARTIN)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_MARTIN);

    assertThatThrownBy(() -> atelier.pointe(commande)).isExactlyInstanceOf(OperateurNonHabiliteException.class);
  }

  /**
   * La regularisation ecrit le meme journal que le pointage : elle passe donc par les memes verifications, sans quoi
   * le back-office contournerait la regle que le pupitre applique.
   */
  @Test
  void shouldNotRegulariserSurUnPosteNonHabilite() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_11_MAI_2026_A_9H15);
    RegularisationAEnregistrer commande = RegularisationAEnregistrer.builder()
      .suivi(engage.id())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_MARTIN)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(LE_10_MAI_2026_A_8H);

    assertThatThrownBy(() -> atelier.regularise(commande)).isExactlyInstanceOf(OperateurNonHabiliteException.class);
  }

  @Test
  void shouldMenerDeuxPostesDeFrontSurLeMemeElement() {
    SuiviDAtelier engage = engage();
    atelier.pointe(debutSurFraiseuse1(engage.id()));

    SuiviDAtelier pointe = atelier.pointe(
      PointageAEnregistrer.builder()
        .suivi(engage.id())
        .type(TypeDEvenementDAtelier.DEBUT)
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.of(POSTE_ID_FRAISEUSE_2))
        .auteur(AUTEUR_DUPONT)
    );

    assertThat(pointe.activitesEnCours())
      .extracting(ActiviteEnCours::poste)
      .containsExactlyInAnyOrder(Optional.of(POSTE_ID_FRAISEUSE_1), Optional.of(POSTE_ID_FRAISEUSE_2));
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
        assertThat(evenement.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
        assertThat(evenement.auteur()).isEqualTo(AUTEUR_LEROY);
        assertThat(evenement.estUneRegularisation()).isTrue();
      });
  }

  /**
   * La regularisation ecrit le meme evenement que le pointage : le cout et le taux horaires y sont donc figes de la
   * meme facon, sans quoi le back-office contournerait la capture que le pupitre applique.
   */
  @Test
  void shouldEstampillerLeCoutEtLeTauxHoraireALaRegularisation() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    SuiviDAtelier regularise = atelier.regularise(regularisationDeDebutA(engage.id(), LE_10_MAI_2026_A_8H));

    assertThat(regularise.journal().actifs())
      .singleElement()
      .satisfies(evenement -> {
        assertThat(evenement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1);
        assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT);
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
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT);
  }

  private static RegularisationAEnregistrer regularisationDeDebutA(SuiviDAtelierId suivi, Instant date) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(date);
  }

  private static final class ElementsEngageablesFiges implements ElementsEngageables {

    @Override
    public Optional<ElementEngage> get(ElementEngageId id) {
      return id.equals(ELEMENT_OF_2026_000042) ? Optional.of(elementEngageOf2026000042()) : Optional.empty();
    }
  }
}
