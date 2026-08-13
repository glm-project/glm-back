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
 * Une journee d'atelier jouee de bout en bout, a travers les deux services et sans autre double que les repositories
 * en memoire.
 *
 * <p>
 * La ou les tests voisins verifient chacun une mecanique isolee, celui-ci enonce le fonctionnement demande par le
 * client : un operateur pointe sa presence d'un cote, ses ordres de fabrication de l'autre, mene deux machines de
 * front, et un seul bouton de pause suffit a scinder tout ce qui est en cours. Il tient lieu de scenario metier tant
 * que le contexte n'a ni adapter primaire ni feature Gherkin.
 * </p>
 *
 * <p>
 * Le recit : Dupont arrive a 7 h, demarre l'OF 42 sur la fraiseuse 1 a 8 h, l'OF 43 sur la fraiseuse 2 a 9 h, part en
 * pause a midi, reprend a 13 h, termine l'OF 43 a 16 h, puis rentre chez lui a 17 h <em>sans rien pointer</em> — ni
 * son depart, ni la fin de l'OF 42. Le lendemain, Leroy regularise le depart oublie.
 * </p>
 */
@UnitTest
class VieDeLAtelierTest {

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final SuivisDAtelierEnMemoire suivis = new SuivisDAtelierEnMemoire();
  private final JourneesDeTravailEnMemoire journees = new JourneesDeTravailEnMemoire();
  private final RessourcesDAtelierEnMemoire ressources = RessourcesDAtelierEnMemoire.deLAtelier();
  private final SuivisDAtelierService atelier = SuivisDAtelierService.builder()
    .repository(suivis)
    .elements(new ElementsEngageablesFiges())
    .operateurs(ressources.operateurs())
    .postes(ressources.postes())
    .habilitations(ressources.habilitations())
    .clock(maintenant::get);
  private final JourneesDeTravailService presence = new JourneesDeTravailService(journees, ressources.operateurs(), maintenant::get);
  private final TempsDAtelierService temps = new TempsDAtelierService(suivis, journees);

  private SuiviDAtelierId premierOrdre;
  private SuiviDAtelierId secondOrdre;
  private JourneeDeTravailId journeeDeDupont;

  @BeforeEach
  void laJourneeDuDixMai() {
    ilEst(LE_10_MAI_2026_A_7H);
    premierOrdre = engage(ELEMENT_OF_2026_000042);
    secondOrdre = engage(ELEMENT_OF_2026_000043);
    journeeDeDupont = presence.arrive(new ArriveeAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT)).id();

    ilEst(LE_10_MAI_2026_A_8H);
    atelier.pointe(pointage(premierOrdre, TypeDEvenementDAtelier.DEBUT, POSTE_ID_FRAISEUSE_1));

    ilEst(LE_10_MAI_2026_A_9H);
    atelier.pointe(pointage(secondOrdre, TypeDEvenementDAtelier.DEBUT, POSTE_ID_FRAISEUSE_2));

    ilEst(LE_10_MAI_2026_A_12H);
    presence.pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.PAUSE));

    ilEst(LE_10_MAI_2026_A_13H);
    presence.pointe(new PointageDePresenceAEnregistrer(OPERATEUR_ID_DUPONT, AUTEUR_DUPONT, TypeDEvenementDePresence.REPRISE));

    ilEst(LE_10_MAI_2026_A_16H);
    atelier.pointe(pointage(secondOrdre, TypeDEvenementDAtelier.FIN, POSTE_ID_FRAISEUSE_2));

    ilEst(LE_11_MAI_2026_A_9H15);
    presence.regularise(
      RegularisationDePresenceAEnregistrer.builder()
        .journee(journeeDeDupont)
        .type(TypeDEvenementDePresence.DEPART)
        .auteur(AUTEUR_LEROY)
        .dateDeSurvenue(LE_10_MAI_2026_A_17H)
    );
  }

  /**
   * « Les heures de presence, c'est les heures ou il arrive a la societe, il pointe et il part » : l'amplitude court
   * de l'arrivee au depart, la pause n'en retirant que son propre creux.
   */
  @Test
  void shouldSuivreLaPresenceDeLArriveeAuDepart() {
    JourneeDeTravail journee = presence.get(journeeDeDupont);

    assertThat(journee.amplitude()).contains(new Periode(LE_10_MAI_2026_A_7H, LE_10_MAI_2026_A_17H));
    assertThat(journee.fenetres()).containsExactly(
      new FenetreDePresence(LE_10_MAI_2026_A_7H, Optional.of(LE_10_MAI_2026_A_12H)),
      new FenetreDePresence(LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
    );
  }

  /**
   * Le test qui porte le modele : apres son debut a 8 h, l'OF 42 n'a plus recu le moindre pointage. C'est la presence
   * seule qui le scinde a midi et le referme au depart regularise.
   */
  @Test
  void shouldScinderEtRefermerLePremierOrdreSansAucunPointageApresSonDebut() {
    assertThat(temps.tempsEffectif(premierOrdre))
      .extracting(IntervalleDActivite::poste, IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(Optional.of(POSTE_ID_FRAISEUSE_1), LE_10_MAI_2026_A_8H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(Optional.of(POSTE_ID_FRAISEUSE_1), LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_17H))
      );
  }

  /**
   * Deux machines menees de front sur deux ordres distincts : le second s'arrete a sa propre fin, sans attendre le
   * depart, et son poste le distingue du premier.
   */
  @Test
  void shouldArreterLeSecondOrdreASaPropreFinPlutotQuAuDepart() {
    assertThat(temps.tempsEffectif(secondOrdre))
      .extracting(IntervalleDActivite::poste, IntervalleDActivite::debut, IntervalleDActivite::fin)
      .containsExactly(
        tuple(Optional.of(POSTE_ID_FRAISEUSE_2), LE_10_MAI_2026_A_9H, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(Optional.of(POSTE_ID_FRAISEUSE_2), LE_10_MAI_2026_A_13H, Optional.of(LE_10_MAI_2026_A_16H))
      );
  }

  /**
   * « Ne mettez qu'un bouton pas trois » : un seul geste de pause, et aucune recopie dans les ordres en cours. Le
   * journal de chaque ordre ne contient que ce que l'operateur y a pointe.
   */
  @Test
  void shouldNePasRecopierLaPauseDansLeJournalDesOrdres() {
    assertThat(atelier.get(premierOrdre).journal().actifs())
      .extracting(EvenementDAtelier::type)
      .containsExactly(TypeDEvenementDAtelier.DEBUT);
    assertThat(atelier.get(secondOrdre).journal().actifs())
      .extracting(EvenementDAtelier::type)
      .containsExactly(TypeDEvenementDAtelier.DEBUT, TypeDEvenementDAtelier.FIN);
  }

  /**
   * « Il a oublie de pointer le matin… mais il faut compter son temps de presence aussi » — « il faut pas que ce soit
   * lui » : la regularisation garde la date de l'acte et celle de la saisie, sous le nom du gestionnaire.
   */
  @Test
  void shouldTracerLeDepartRegulariseLeLendemainParUnTiers() {
    EvenementDePresence depart = presence.get(journeeDeDupont).journal().evenements().getLast();

    assertThat(depart.type()).isEqualTo(TypeDEvenementDePresence.DEPART);
    assertThat(depart.auteur()).isEqualTo(AUTEUR_LEROY);
    assertThat(depart.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_17H);
    assertThat(depart.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
  }

  /**
   * Une seule regularisation de depart suffit a refermer tous les ordres restes ouverts : le correctif est porte par
   * la presence, jamais recopie ordre par ordre.
   */
  @Test
  void shouldRefermerTousLesOrdresRestesOuvertsSurLaSeuleRegularisationDeDepart() {
    assertThat(temps.tempsEffectif(premierOrdre)).noneMatch(IntervalleDActivite::estOuvert);
    assertThat(temps.tempsEffectif(secondOrdre)).noneMatch(IntervalleDActivite::estOuvert);
  }

  private void ilEst(Instant instant) {
    maintenant.set(instant);
  }

  private SuiviDAtelierId engage(ElementEngageId element) {
    return atelier.engage(new EngagementAEnregistrer(element, AUTEUR_LEROY)).id();
  }

  private static PointageAEnregistrer pointage(SuiviDAtelierId suivi, TypeDEvenementDAtelier type, PosteDeTravailId poste) {
    return PointageAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(poste))
      .auteur(AUTEUR_DUPONT);
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
