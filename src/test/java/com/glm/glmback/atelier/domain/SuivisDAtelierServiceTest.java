package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class SuivisDAtelierServiceTest {

  private static final ElementEngageId ELEMENT_INCONNU = new ElementEngageId(UUID.randomUUID());
  private static final Instant LE_10_MAI_2026_A_14H = Instant.parse("2026-05-10T14:00:00Z");
  private static final Instant LE_10_MAI_2026_A_22H = Instant.parse("2026-05-10T22:00:00Z");

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final AtomicReference<MaximumActivityDuration> dureeMaximale = new AtomicReference<>(DUREE_MAXIMALE_TREIZE_HEURES);
  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();
  private final PointagesIgnoresEnMemoire pointagesIgnores = new PointagesIgnoresEnMemoire();
  private final RessourcesDAtelierEnMemoire ressources = RessourcesDAtelierEnMemoire.deLAtelier();
  private final SuivisDAtelierService atelier = SuivisDAtelierService.builder()
    .repository(suivis)
    .elements(new ElementsEngageablesFiges())
    .operateurs(ressources.operateurs())
    .postes(ressources.postes())
    .habilitations(ressources.habilitations())
    .pointagesIgnores(pointagesIgnores)
    .dureeMaximaleDActivite(dureeMaximale::get)
    .clock(maintenant::get);

  @Test
  void shouldEngagerUnElementDansLAtelier() {
    SuiviDAtelier suivi = engage();

    assertThat(suivi.element()).isEqualTo(elementEngageOf2026000042());
    assertThat(suivi.engagement()).isEqualTo(engagementParLeroy());
    assertThat(suivi.etat(LE_10_MAI_2026_A_7H)).isEqualTo(EtatDAtelier.EN_ATTENTE);
  }

  @Test
  void shouldRefuserUneRegularisationFutureSansModifierLeSuivi() {
    // GIVEN
    var suivi = suiviAvecUnTravailOublie();
    var commande = regularisationDeLaFin(suivi, LE_11_MAI_2026_A_9H15.plusSeconds(1));
    // WHEN THEN
    assertThatThrownBy(() -> atelier.regularise(commande)).isExactlyInstanceOf(DateDeSurvenueFutureException.class);
    assertThat(suivis.get(suivi.id())).contains(suivi);
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

    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id())).suivi();

    assertThat(pointe.journal().evenements())
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

  /**
   * Le pupitre reste hors ligne rejoue son geste apres coup, a l'heure ou il a eu lieu : la route des pointages en fait
   * toujours un pointage, jamais une regularisation, quel que soit l'ecart entre les deux dates.
   */
  @Test
  void shouldConserverCommeUnPointageUnGesteRejoueHorsLigne() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_9H);

    SuiviDAtelier pointe = atelier
      .pointe(
        PointageAEnregistrer.pupitreBuilder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
          .auteur(AUTEUR_DUPONT)
          .dateDeSurvenue(Optional.of(LE_10_MAI_2026_A_8H))
          .evenement(EvenementDAtelierId.newId())
      )
      .suivi();

    assertThat(pointe.journal().evenements())
      .singleElement()
      .satisfies(evenement -> {
        assertThat(evenement.origine()).isEqualTo(OrigineDuPointage.POINTAGE);
        assertThat(evenement.estUneRegularisation()).isFalse();
      });
  }

  /**
   * Le pointage qui ouvre une activite lui donne son identite : la sienne. C'est elle que vise la fin que le
   * gestionnaire regularise.
   */
  @Test
  void shouldDonnerAUneOuvertureLIdentiteDeSonActivite() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = debutSurFraiseuse1(engage.id());

    SuiviDAtelier pointe = atelier.pointe(debut).suivi();

    assertThat(pointe.journal().evenements())
      .singleElement()
      .satisfies(evenement -> {
        assertThat(evenement.type()).isEqualTo(TypeDEvenementDAtelier.DEBUT);
        assertThat(evenement.activite()).contains(ActiviteId.ouvertePar(debut.evenement()));
        assertThat(evenement.activiteVisee()).isEmpty();
      });
  }

  @Test
  void shouldRefuserUneRegularisationQuiViseUneActiviteIntrouvable() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_11_MAI_2026_A_9H15);
    RegularisationAEnregistrer fin = regularisationDe(engage.id(), new ActiviteId(UUID.randomUUID()), LE_10_MAI_2026_A_12H);

    assertThatThrownBy(() -> atelier.regularise(fin)).isExactlyInstanceOf(ActiviteViseeIntrouvableException.class);
  }

  @Test
  void shouldPointerSansPosteDeTravail() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier
      .pointe(
        PointageAEnregistrer.builder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.empty())
          .auteur(AUTEUR_DUPONT)
      )
      .suivi();

    assertThat(pointe.journal().evenements())
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

    SuiviDAtelier pointe = atelier
      .pointe(
        PointageAEnregistrer.builder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_MARTIN)
          .poste(Optional.empty())
          .auteur(AUTEUR_MARTIN)
      )
      .suivi();

    assertThat(pointe.journal().evenements())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.nature()).isEmpty());
  }

  /**
   * La nature vient du poste, jamais de la personne : le meme operateur, sur deux postes, produit deux natures.
   */
  @Test
  void shouldReprendreLaNatureDuPosteEtNonDeLOperateur() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier
      .pointe(
        PointageAEnregistrer.builder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.of(POSTE_ID_FRAISEUSE_2))
          .auteur(AUTEUR_DUPONT)
      )
      .suivi();

    assertThat(pointe.journal().evenements())
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

    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id())).suivi();

    assertThat(pointe.journal().evenements())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1));
  }

  @Test
  void shouldPointerSansCoutHoraireFauteDePoste() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier
      .pointe(
        PointageAEnregistrer.builder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.empty())
          .auteur(AUTEUR_DUPONT)
      )
      .suivi();

    assertThat(pointe.journal().evenements())
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

    SuiviDAtelier pointe = atelier
      .pointe(
        PointageAEnregistrer.builder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.of(POSTE_ID_FRAISEUSE_2))
          .auteur(AUTEUR_DUPONT)
      )
      .suivi();

    assertThat(pointe.journal().evenements())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.coutHoraire()).isEmpty());
  }

  @Test
  void shouldEstampillerLeTauxHoraireDeLOperateur() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier.pointe(debutSurFraiseuse1(engage.id())).suivi();

    assertThat(pointe.journal().evenements())
      .singleElement()
      .satisfies(evenement -> assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT));
  }

  @Test
  void shouldPointerSansTauxHoraireQuandLOperateurNEnAPas() {
    SuiviDAtelier engage = engage();

    SuiviDAtelier pointe = atelier
      .pointe(
        PointageAEnregistrer.builder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_MARTIN)
          .poste(Optional.empty())
          .auteur(AUTEUR_MARTIN)
      )
      .suivi();

    assertThat(pointe.journal().evenements())
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
   * le back-office contournerait la regle que le pupitre applique. Ici l'habilitation de Martin a disparu depuis son
   * debut de 8 h sur la fraiseuse 1.
   */
  @Test
  void shouldNotRegulariserSurUnPosteNonHabilite() {
    SuiviDAtelier engage = engage();
    SuiviDAtelier suivi = suivis.update(engage.enregistre(debutSurFraiseuse1ParMartinA(LE_10_MAI_2026_A_8H)));
    maintenant.set(LE_11_MAI_2026_A_9H15);
    RegularisationAEnregistrer commande = regularisationDeLaFin(suivi, LE_10_MAI_2026_A_17H);

    assertThatThrownBy(() -> atelier.regularise(commande)).isExactlyInstanceOf(OperateurNonHabiliteException.class);
  }

  @Test
  void shouldMenerDeuxPostesDeFrontSurLeMemeElement() {
    SuiviDAtelier engage = engage();
    atelier.pointe(debutSurFraiseuse1(engage.id()));

    SuiviDAtelier pointe = atelier
      .pointe(
        PointageAEnregistrer.builder()
          .suivi(engage.id())
          .type(TypeDEvenementDAtelier.DEBUT)
          .operateur(OPERATEUR_ID_DUPONT)
          .poste(Optional.of(POSTE_ID_FRAISEUSE_2))
          .auteur(AUTEUR_DUPONT)
      )
      .suivi();

    assertThat(pointe.activitesEnCours(LE_10_MAI_2026_A_7H))
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

  /**
   * L'activite est echue quand l'est la duree que son debut a portee, pas celle du reglage du jour : ouverte sous huit
   * heures, elle se regularise des 16 h, meme si le gestionnaire est revenu a treize heures ; avant 16 h, elle n'est pas
   * echue.
   */
  @Test
  void shouldRegulariserUneActiviteEchueSelonLaDureeQueSonDebutAPortee() {
    SuiviDAtelier engage = engage();
    dureeMaximale.set(DUREE_MAXIMALE_HUIT_HEURES);
    maintenant.set(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = atelier.pointe(debutSurFraiseuse1(engage.id())).suivi();
    dureeMaximale.set(DUREE_MAXIMALE_TREIZE_HEURES);
    RegularisationAEnregistrer commande = regularisationDeLaFin(suivi, LE_10_MAI_2026_A_16H);

    maintenant.set(LE_10_MAI_2026_A_16H.minusSeconds(1));
    assertThatThrownBy(() -> atelier.regularise(commande)).isExactlyInstanceOf(ActiviteNonEchueException.class);
    maintenant.set(LE_10_MAI_2026_A_16H);
    RegularisationTraitee regularise = atelier.regularise(commande);

    assertThat(regularise.rejeu()).isFalse();
    assertThat(regularise.suivi().journal().evenements())
      .last()
      .satisfies(fin -> {
        assertThat(fin.estUneRegularisation()).isTrue();
        assertThat(fin.dureeMax()).isEmpty();
      });
  }

  /**
   * L'operateur et le poste se deduisent de l'activite : la fin est du meme couple, saisie par le gestionnaire.
   */
  @Test
  void shouldDeduireDeLActiviteLOperateurLePosteEtLeTypeDeLaRegularisation() {
    SuiviDAtelier suivi = suiviAvecUnTravailOublie();
    RegularisationAEnregistrer commande = regularisationDeLaFin(suivi, LE_10_MAI_2026_A_17H);

    RegularisationTraitee regularise = atelier.regularise(commande);

    assertThat(regularise.rejeu()).isFalse();
    assertThat(regularise.suivi().journal().evenements())
      .last()
      .satisfies(evenement -> {
        assertThat(evenement.id()).isEqualTo(commande.evenement());
        assertThat(evenement.type()).isEqualTo(TypeDEvenementDAtelier.FIN);
        assertThat(evenement.activite()).isEmpty();
        assertThat(evenement.activiteVisee()).contains(commande.activite());
        assertThat(evenement.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
        assertThat(evenement.poste()).contains(POSTE_ID_FRAISEUSE_1);
        assertThat(evenement.auteur()).isEqualTo(AUTEUR_LEROY);
        assertThat(evenement.estUneRegularisation()).isTrue();
        assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H);
        assertThat(evenement.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
      });
  }

  /**
   * C'est l'acte du gestionnaire qui fait la regularisation, pas l'ecart entre les deux dates : un fait regularise a
   * l'heure meme de sa saisie en reste une.
   */
  @Test
  void shouldConserverLOrigineDUneRegularisationSaisieALHeureDuFait() {
    SuiviDAtelier suivi = suiviAvecUnTravailOublie();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    RegularisationTraitee regularise = atelier.regularise(regularisationDeLaFin(suivi, LE_11_MAI_2026_A_9H15));

    assertThat(regularise.suivi().journal().evenements())
      .last()
      .satisfies(evenement -> {
        assertThat(evenement.origine()).isEqualTo(OrigineDuPointage.REGULARISATION);
        assertThat(evenement.dateDeSurvenue()).isEqualTo(evenement.dateDEnregistrement());
      });
  }

  /**
   * La regularisation ecrit le meme evenement que le pointage : la nature, le cout et le taux horaires y sont donc figes
   * de la meme facon, sans quoi le back-office contournerait la capture que le pupitre applique.
   */
  @Test
  void shouldEstampillerLaNatureLeCoutEtLeTauxHoraireALaRegularisation() {
    SuiviDAtelier suivi = suiviAvecUnTravailOublie();

    RegularisationTraitee regularise = atelier.regularise(regularisationDeLaFin(suivi, LE_10_MAI_2026_A_17H));

    assertThat(regularise.suivi().journal().evenements())
      .last()
      .satisfies(evenement -> {
        assertThat(evenement.nature()).contains(NATURE_FRAISAGE);
        assertThat(evenement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1);
        assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT);
      });
  }

  /**
   * Le renvoi de la meme saisie, de meme identifiant, repond comme un succes et n'ecrit rien : l'identifiant deja au
   * journal est verifie avant toute regle, sans quoi l'activite desormais regularisee ferait refuser le renvoi.
   */
  @Test
  void shouldRepondreCommeUnSuccesAuRenvoiDeLaMemeRegularisationSansRienEcrire() {
    SuiviDAtelier suivi = suiviAvecUnTravailOublie();
    RegularisationAEnregistrer commande = regularisationDeLaFin(suivi, LE_10_MAI_2026_A_17H);
    RegularisationTraitee premiere = atelier.regularise(commande);

    RegularisationTraitee renvoi = atelier.regularise(commande);

    assertThat(premiere.rejeu()).isFalse();
    assertThat(renvoi.rejeu()).isTrue();
    assertThat(renvoi.suivi()).isEqualTo(premiere.suivi());
    assertThat(suivis.get(suivi.id())).contains(premiere.suivi());
  }

  /**
   * L'idempotence se juge sur toute la table des evenements, pas sur le seul journal du suivi : l'identifiant d'un geste
   * d'un autre suivi est un renvoi, qui ne heurte pas la cle primaire.
   */
  @Test
  void shouldRepondreCommeUnSuccesAuRenvoiDUnIdentifiantDejaAuJournalDUnAutreSuivi() {
    SuiviDAtelier suivi = suiviAvecUnTravailOublie();
    EvenementDAtelier gesteDUnAutreSuivi = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H);
    suivis.create(suiviDAtelierEngage().enregistre(gesteDUnAutreSuivi));
    RegularisationAEnregistrer commande = RegularisationAEnregistrer.builder()
      .suivi(suivi.id())
      .evenement(gesteDUnAutreSuivi.id())
      .activite(suivi.journal().evenements().getFirst().activite().orElseThrow())
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(LE_10_MAI_2026_A_17H);

    RegularisationTraitee renvoi = atelier.regularise(commande);

    assertThat(renvoi.rejeu()).isTrue();
    assertThat(renvoi.suivi()).isEqualTo(suivi);
    assertThat(suivis.get(suivi.id())).contains(suivi);
  }

  /**
   * La cloture ferme le pointage aux operateurs, elle ne fige rien pour le gestionnaire : il regularise jusqu'a elle.
   */
  @Test
  void shouldRegulariserUnSuiviDejaCloture() {
    SuiviDAtelier suivi = suivis.update(suiviAvecUnTravailOublie().cloture(clotureParLeroyA(LE_10_MAI_2026_A_22H)));

    RegularisationTraitee regularise = atelier.regularise(regularisationDeLaFin(suivi, LE_10_MAI_2026_A_22H));

    assertThat(regularise.suivi().journal().evenements()).hasSize(2);
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

    assertThat(
      atelier.list(Optional.empty(), Set.of(EtatDAtelier.EN_ATTENTE), LE_10_MAI_2026_A_7H, firstPageOfTen()).content()
    ).containsExactly(engage);
  }

  private SuiviDAtelier engage() {
    return atelier.engage(new EngagementAEnregistrer(ELEMENT_OF_2026_000042, AUTEUR_LEROY));
  }

  private PointageAEnregistrer pointeA(PointageAEnregistrer pointage, Instant instant) {
    maintenant.set(instant);
    atelier.pointe(pointage);

    return pointage;
  }

  /**
   * Un suivi dont le travail de Dupont, ouvert a 8 h sur la fraiseuse 1, n'a jamais ete termine : lu le lendemain a 9 h 15,
   * il a atteint son echeance de 21 h.
   */
  private SuiviDAtelier suiviAvecUnTravailOublie() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    SuiviDAtelier suivi = atelier.pointe(debutSurFraiseuse1(engage.id())).suivi();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    return suivi;
  }

  private static RegularisationAEnregistrer regularisationDeLaFin(SuiviDAtelier suivi, Instant date) {
    return regularisationDe(suivi.id(), suivi.journal().evenements().getFirst().activite().orElseThrow(), date);
  }

  private static RegularisationAEnregistrer regularisationDe(SuiviDAtelierId suivi, ActiviteId activite, Instant date) {
    return RegularisationAEnregistrer.builder()
      .suivi(suivi)
      .evenement(EvenementDAtelierId.newId())
      .activite(activite)
      .auteur(AUTEUR_LEROY)
      .dateDeSurvenue(date);
  }

  private static PointageAEnregistrer debutSurFraiseuse1(SuiviDAtelierId suivi) {
    return PointageAEnregistrer.builder()
      .suivi(suivi)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT);
  }

  private static final class ElementsEngageablesFiges implements ElementsEngageables {

    @Override
    public Optional<ElementEngage> get(ElementEngageId id) {
      return id.equals(ELEMENT_OF_2026_000042) ? Optional.of(elementEngageOf2026000042()) : Optional.empty();
    }
  }
}
