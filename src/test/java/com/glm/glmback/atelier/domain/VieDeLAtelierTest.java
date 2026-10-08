package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Une journee d'atelier jouee de bout en bout, a travers les services et sans autre double que les repositories en
 * memoire.
 *
 * <p>
 * La ou les tests voisins verifient chacun une mecanique isolee, celui-ci enonce le fonctionnement demande par le
 * client : un operateur pointe ses ordres de fabrication, mene deux machines de front, et sa pause de midi arrete puis
 * relance tout ce qui est en cours. Il tient lieu de scenario metier tant que le contexte n'a ni adapter primaire ni
 * feature Gherkin.
 * </p>
 *
 * <p>
 * Le recit : Dupont demarre l'OF 42 sur la fraiseuse 1 a 8 h, l'OF 43 sur la fraiseuse 2 a 9 h, les arrete tous deux a
 * midi pour sa pause et les redemarre a 13 h — une fin et un debut par ordre, que le pupitre pointe pour lui —, termine
 * l'OF 43 a 16 h, puis rentre chez lui a 17 h <em>sans arreter l'OF 42</em>. Le lendemain, Leroy regularise la fin
 * oubliee.
 * </p>
 */
@UnitTest
class VieDeLAtelierTest {

  private static final Instant LE_11_MAI_2026_A_2H = Instant.parse("2026-05-11T02:00:00Z");

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
  private final TempsDAtelierService temps = new TempsDAtelierService(suivis);

  private SuiviDAtelierId premierOrdre;
  private SuiviDAtelierId secondOrdre;
  private PointageAEnregistrer premierApresMidi;

  @BeforeEach
  void laJourneeDuDixMai() {
    ilEst(LE_10_MAI_2026_A_7H);
    premierOrdre = engage(ELEMENT_OF_2026_000042);
    secondOrdre = engage(ELEMENT_OF_2026_000043);

    ilEst(LE_10_MAI_2026_A_8H);
    PointageAEnregistrer premierMatin = debut(premierOrdre, POSTE_ID_FRAISEUSE_1);
    atelier.pointe(premierMatin);

    ilEst(LE_10_MAI_2026_A_9H);
    PointageAEnregistrer secondMatin = debut(secondOrdre, POSTE_ID_FRAISEUSE_2);
    atelier.pointe(secondMatin);

    ilEst(LE_10_MAI_2026_A_12H);
    atelier.pointe(fin(premierMatin));
    atelier.pointe(fin(secondMatin));

    ilEst(LE_10_MAI_2026_A_13H);
    premierApresMidi = debut(premierOrdre, POSTE_ID_FRAISEUSE_1);
    atelier.pointe(premierApresMidi);
    PointageAEnregistrer secondApresMidi = debut(secondOrdre, POSTE_ID_FRAISEUSE_2);
    atelier.pointe(secondApresMidi);

    ilEst(LE_10_MAI_2026_A_16H);
    atelier.pointe(fin(secondApresMidi));

    ilEst(LE_11_MAI_2026_A_9H15);
  }

  /**
   * Le test qui porte le modele : la pause de midi scinde l'OF 42 par sa fin et son debut, puis, apres sa relance a
   * 13 h, l'OF 42 n'a plus recu le moindre pointage. Rien ne le borne a 17 h : il se termine automatiquement a son
   * echeance, 13 heures apres son debut, avec une anomalie.
   */
  @Test
  void shouldScinderLePremierOrdreASaPauseEtLeTerminerAutomatiquementASonEcheance() {
    assertThat(temps.tempsEffectif(premierOrdre, maintenant.get()))
      .extracting(IntervalleDActivite::poste, IntervalleDActivite::debut, IntervalleDActivite::fin, IntervalleDActivite::finAutomatique)
      .containsExactly(
        tuple(Optional.of(POSTE_ID_FRAISEUSE_1), LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H), false),
        tuple(Optional.of(POSTE_ID_FRAISEUSE_1), LE_10_MAI_2026_A_13H, Optional.of(LE_11_MAI_2026_A_2H), true)
      );
  }

  /**
   * Deux machines menees de front sur deux ordres distincts : le second s'arrete a sa propre fin, et son poste le
   * distingue du premier.
   */
  @Test
  void shouldArreterLeSecondOrdreASaPropreFin() {
    assertThat(temps.tempsEffectif(secondOrdre, maintenant.get()))
      .extracting(IntervalleDActivite::poste, IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(Optional.of(POSTE_ID_FRAISEUSE_2), LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(Optional.of(POSTE_ID_FRAISEUSE_2), LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H))
      );
  }

  /**
   * « Il a oublie de pointer… il faut pas que ce soit lui » : le lendemain, le gestionnaire regularise la fin oubliee a
   * 17 h. La fin reelle remplace la fin automatique, et l'anomalie disparait au recalcul.
   */
  @Test
  void shouldRemplacerLaFinAutomatiqueParLaFinRegulariseeLeLendemain() {
    atelier.regularise(
      RegularisationAEnregistrer.builder()
        .suivi(premierOrdre)
        .type(TypeDEvenementDAtelier.FIN)
        .intention(IntentionDePointage.FIN)
        .activiteVisee(Optional.of(ActiviteId.ouvertePar(premierApresMidi.evenement())))
        .operateur(OPERATEUR_ID_DUPONT)
        .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(LE_10_MAI_2026_A_17H)
    );

    assertThat(temps.tempsEffectif(premierOrdre, maintenant.get()))
      .last()
      .satisfies(intervalle -> {
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
        assertThat(intervalle.finAutomatique()).isFalse();
      });
  }

  /**
   * Le cout horaire du poste et le taux horaire de l'operateur sont figes a la saisie, sur le meme patron que la
   * nature : la fraiseuse 1 est valorisee et Dupont aussi, mais la fraiseuse 2 ne l'est pas — l'evenement du second
   * ordre porte donc le taux de Dupont sans aucun cout de poste.
   */
  @Test
  void shouldEstampillerLeCoutEtLeTauxHoraireALaSaisie() {
    assertThat(atelier.get(premierOrdre).journal().evenements()).allSatisfy(evenement -> {
      assertThat(evenement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1);
      assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT);
    });
    assertThat(atelier.get(secondOrdre).journal().evenements()).allSatisfy(evenement -> {
      assertThat(evenement.coutHoraire()).isEmpty();
      assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT);
    });
  }

  /**
   * « Pause / arret / reprise sont le meme mecanisme » : la pause de midi se lit dans le journal de chaque ordre, par la
   * fin et le debut que le pupitre y a pointes, et nulle part ailleurs.
   */
  @Test
  void shouldPorterLaPauseDeMidiDansLeJournalDeChaqueOrdre() {
    assertThat(atelier.get(premierOrdre).journal().evenements())
      .extracting(EvenementDAtelier::type)
      .containsExactly(TypeDEvenementDAtelier.DEBUT, TypeDEvenementDAtelier.FIN, TypeDEvenementDAtelier.DEBUT);
    assertThat(atelier.get(secondOrdre).journal().evenements())
      .extracting(EvenementDAtelier::type)
      .containsExactly(TypeDEvenementDAtelier.DEBUT, TypeDEvenementDAtelier.FIN, TypeDEvenementDAtelier.DEBUT, TypeDEvenementDAtelier.FIN);
  }

  private void ilEst(Instant instant) {
    maintenant.set(instant);
  }

  private SuiviDAtelierId engage(ElementEngageId element) {
    return atelier.engage(new EngagementAEnregistrer(element, AUTEUR_LEROY)).id();
  }

  private static PointageAEnregistrer debut(SuiviDAtelierId suivi, PosteDeTravailId poste) {
    return PointageAEnregistrer.builder()
      .suivi(suivi)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(poste))
      .auteur(AUTEUR_DUPONT);
  }

  /**
   * La fin de l'activite qu'a ouverte ce debut, sur le meme ordre et le meme poste, telle que le pupitre la pointe.
   */
  private static PointageAEnregistrer fin(PointageAEnregistrer debut) {
    return PointageAEnregistrer.builder()
      .suivi(debut.suivi())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activiteVisee(Optional.of(ActiviteId.ouvertePar(debut.evenement())))
      .operateur(debut.operateur())
      .poste(debut.poste())
      .auteur(debut.auteur());
  }

  private static final class ElementsEngageablesFiges implements ElementsEngageables {

    private static final Map<ElementEngageId, ElementEngage> ELEMENTS = Map.of(
      ELEMENT_OF_2026_000042,
      elementEngageOf2026000042(),
      ELEMENT_OF_2026_000043,
      elementEngageOf2026000043()
    );

    @Override
    public Optional<ElementEngage> get(ElementEngageId id) {
      return Optional.ofNullable(ELEMENTS.get(id));
    }
  }
}
