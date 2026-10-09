package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.activityduration.domain.MaximumActivityDuration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Ce que le service fait d'un pointage une fois le suivi decide : l'ecrire au journal, ou l'ecrire a l'audit. La regle
 * elle-meme est celle de {@link SuiviDAtelierReceptionTest}.
 */
@UnitTest
class SuivisDAtelierReceptionTest {

  private static final Instant LE_10_MAI_2026_A_21H = Instant.parse("2026-05-10T21:00:00Z");
  private static final Instant LE_10_MAI_2026_A_23H = Instant.parse("2026-05-10T23:00:00Z");
  private static final Instant LE_11_MAI_2026_A_1H = Instant.parse("2026-05-11T01:00:00Z");

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_7H);
  private final AtomicReference<MaximumActivityDuration> dureeMax = new AtomicReference<>(DUREE_MAXIMALE_TREIZE_HEURES);
  private final AtomicInteger lecturesDeLaDuree = new AtomicInteger();
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
    .dureeMax(() -> {
      lecturesDeLaDuree.incrementAndGet();

      return dureeMax.get();
    })
    .clock(maintenant::get);

  /**
   * Le serveur ecrit au journal ce que le pupitre ne dit pas : un debut ouvre une activite, une fin ferme celle qui est en
   * cours sur la cle, sans que le pointage la designe.
   */
  @Test
  void shouldTraduireUnDebutEnOuvertureEtUneFinEnFinDeLActiviteEnCours() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = pointage(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite fin = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.FIN), LE_10_MAI_2026_A_12H);

    assertThat(fin.issue()).isEqualTo(IssueDePointage.ACCEPTE);
    assertThat(fin.suivi().journal().evenements()).satisfiesExactly(
      ouvrant -> {
        assertThat(ouvrant.activite()).contains(ActiviteId.ouvertePar(debut.evenement()));
        assertThat(ouvrant.activiteVisee()).isEmpty();
      },
      terminant -> {
        assertThat(terminant.type()).isEqualTo(TypeDEvenementDAtelier.FIN);
        assertThat(terminant.activite()).isEmpty();
        assertThat(terminant.activiteVisee()).isEmpty();
      }
    );
    assertThat(pointagesIgnores.lignes()).isEmpty();
  }

  /**
   * Un debut recopie sur l'evenement qui ouvre l'activite la duree maximale en vigueur a cet instant : fixee a huit
   * heures par le gestionnaire, l'activite echoit huit heures plus tard.
   */
  @Test
  void shouldCopierSurLOuvertureLaDureeMaximaleEnVigueur() {
    SuiviDAtelier engage = engage();
    dureeMax.set(DUREE_MAXIMALE_HUIT_HEURES);

    PointageDAtelierTraite debut = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);

    assertThat(debut.suivi().journal().evenements())
      .singleElement()
      .extracting(EvenementDAtelier::dureeMax)
      .isEqualTo(Optional.of(DUREE_MAXIMALE_HUIT_HEURES));
    assertThat(debut.suivi().activites()).singleElement().extracting(Activite::echeance).isEqualTo(new Echeance(LE_10_MAI_2026_A_16H));
  }

  /**
   * Une non conformite ouvre elle aussi une activite : elle porte, comme un debut, la duree en vigueur.
   */
  @Test
  void shouldCopierLaDureeMaximaleSurUneNonConformite() {
    SuiviDAtelier engage = engage();
    dureeMax.set(DUREE_MAXIMALE_HUIT_HEURES);

    PointageDAtelierTraite nonConformite = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.NON_CONFORMITE), LE_10_MAI_2026_A_8H);

    assertThat(nonConformite.suivi().journal().evenements())
      .singleElement()
      .extracting(EvenementDAtelier::dureeMax)
      .isEqualTo(Optional.of(DUREE_MAXIMALE_HUIT_HEURES));
  }

  /**
   * Pas de retroactivite : une activite garde la duree en vigueur a son debut, meme si le gestionnaire la change ensuite.
   * Ouverte sous treize heures, elle accepte encore une fin a 17 h quand la duree est passee a huit ; la suivante, ouverte
   * apres le changement, prend huit heures.
   */
  @Test
  void shouldGarderLaDureeDeSonDebutQuandLeGestionnaireLaChangeEnsuite() {
    SuiviDAtelier engage = engage();
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);
    dureeMax.set(DUREE_MAXIMALE_HUIT_HEURES);

    PointageDAtelierTraite fin = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.FIN), LE_10_MAI_2026_A_17H);
    PointageDAtelierTraite suivant = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_17H);

    assertThat(fin.issue()).isEqualTo(IssueDePointage.ACCEPTE);
    assertThat(suivant.suivi().activites())
      .extracting(Activite::echeance)
      .containsExactly(new Echeance(LE_10_MAI_2026_A_21H), new Echeance(LE_11_MAI_2026_A_1H));
  }

  /**
   * Une fin ne porte aucune duree et n'en lit aucune, pas plus qu'un pointage ignore : la duree se lit quand le debut ou
   * la non conformite est accepte, et seulement alors.
   */
  @Test
  void shouldNeLireLaDureeQuePourUneOuvertureAcceptee() {
    SuiviDAtelier engage = engage();
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);
    assertThat(lecturesDeLaDuree).hasValue(1);

    PointageDAtelierTraite ignore = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_9H);
    PointageDAtelierTraite fin = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.FIN), LE_10_MAI_2026_A_12H);

    assertThat(ignore.issue()).isEqualTo(IssueDePointage.IGNORE);
    assertThat(fin.suivi().journal().evenements().getLast().dureeMax()).isEmpty();
    assertThat(lecturesDeLaDuree).hasValue(1);
  }

  /**
   * Un pointage ignore ne change rien au journal : il est ecrit a l'audit avec son horodatage, sa raison et le dernier
   * pointage accepte compare. Le suivi est verrouille comme pour une ecriture, pour que deux pointages simultanes se jugent
   * l'un apres l'autre.
   */
  @Test
  void shouldEcrireUnPointageIgnoreDansLAuditSansToucherAuJournal() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = pointage(engage.id(), TypeDEvenementDAtelier.DEBUT);
    SuiviDAtelier pointe = pointeA(debut, LE_10_MAI_2026_A_8H).suivi();
    PointageAEnregistrer doubleAppui = pointage(engage.id(), TypeDEvenementDAtelier.DEBUT);

    PointageDAtelierTraite ignore = pointeA(doubleAppui, LE_10_MAI_2026_A_9H);

    assertThat(ignore.issue()).isEqualTo(IssueDePointage.IGNORE);
    assertThat(ignore.suivi()).isEqualTo(pointe);
    assertThat(suivis.get(engage.id())).contains(pointe);
    assertThat(pointagesIgnores.lignes())
      .singleElement()
      .satisfies(ligne -> {
        assertThat(ligne.pointage()).isEqualTo(doubleAppui);
        assertThat(ligne.horodatage()).isEqualTo(Horodatage.saisiA(LE_10_MAI_2026_A_9H));
        assertThat(ligne.verdict()).isEqualTo(
          new VerdictDeReception.Ignore(RaisonDePointageIgnore.DEJA_EN_COURS, Optional.of(debut.evenement()))
        );
      });
    assertThat(suivis.verrouilles()).contains(engage.id());
  }

  @Test
  void shouldRefuserDeNouveauUnPointageDejaIgnoreSansNouvelleLigne() {
    SuiviDAtelier engage = engage();
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);
    PointageAEnregistrer doubleAppui = pointage(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(doubleAppui, LE_10_MAI_2026_A_9H);

    PointageDAtelierTraite renvoi = pointeA(doubleAppui, LE_10_MAI_2026_A_12H);

    assertThat(renvoi.issue()).isEqualTo(IssueDePointage.IGNORE);
    assertThat(pointagesIgnores.lignes()).hasSize(1);
  }

  /**
   * Les controles existants precedent la regle : un operateur inconnu, un poste inconnu, un operateur non habilite ou un
   * OF cloture sont refuses sans rien ecrire a l'audit, meme pour un pointage que la regle aurait ignore.
   */
  @Test
  void shouldRefuserAvantLaRegleSansRienAuditer() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer finSansActivite = pointage(engage.id(), TypeDEvenementDAtelier.FIN, new OperateurId(java.util.UUID.randomUUID()));

    assertThatThrownBy(() -> atelier.pointe(finSansActivite)).isExactlyInstanceOf(OperateurDAtelierIntrouvableException.class);
    assertThat(pointagesIgnores.lignes()).isEmpty();

    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY));
    assertThatThrownBy(() -> atelier.pointe(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT))).isExactlyInstanceOf(
      SuiviDAtelierClotureException.class
    );
    assertThatThrownBy(() -> atelier.pointe(pointage(engage.id(), TypeDEvenementDAtelier.NON_CONFORMITE))).isExactlyInstanceOf(
      SuiviDAtelierClotureException.class
    );
    assertThat(pointagesIgnores.lignes()).isEmpty();
  }

  /**
   * La cloture a ferme l'activite : une fin posterieure n'a plus rien a terminer et part en audit, elle n'est plus
   * absorbee.
   */
  @Test
  void shouldIgnorerUneFinPosterieureALaCloture() {
    SuiviDAtelier engage = engage();
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_12H)));

    PointageDAtelierTraite fin = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.FIN), LE_10_MAI_2026_A_16H);

    assertThat(fin.issue()).isEqualTo(IssueDePointage.IGNORE);
    assertThat(pointagesIgnores.lignes())
      .singleElement()
      .satisfies(ligne -> assertThat(ligne.verdict().raison()).isEqualTo(RaisonDePointageIgnore.AUCUNE_ACTIVITE));
  }

  /**
   * Une fin survenue avant la cloture, mais recue apres elle, est enregistree et terminee a son heure metier ; la
   * cloture reste acquise.
   */
  @Test
  void shouldEnregistrerUneFinAnterieureALaClotureRecueApresElle() {
    SuiviDAtelier engage = engage();
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_12H)));

    PointageDAtelierTraite fin = atelier.pointe(finRejoueeA(engage.id(), LE_10_MAI_2026_A_9H));

    assertThat(fin.issue()).isEqualTo(IssueDePointage.ACCEPTE);
    assertThat(fin.suivi().estCloture()).isTrue();
    assertThat(fin.suivi().activites()).singleElement().extracting(Activite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_9H));
  }

  /**
   * FIN 17 h recue apres la fin automatique de 21 h : l'echeance se juge sur l'heure du geste, la fin termine l'activite a
   * 17 h.
   */
  @Test
  void shouldTerminerASonHeureUneFinRejoueeApresLEcheance() {
    SuiviDAtelier engage = engage();
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    PointageDAtelierTraite fin = atelier.pointe(finRejoueeA(engage.id(), LE_10_MAI_2026_A_17H));

    assertThat(fin.issue()).isEqualTo(IssueDePointage.ACCEPTE);
    assertThat(fin.suivi().activites())
      .singleElement()
      .satisfies(activite -> {
        IntervalleDActivite intervalle = activite.a(LE_11_MAI_2026_A_9H15);
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
        assertThat(intervalle.finAutomatique()).isFalse();
      });
  }

  /**
   * Une fin pointee a 23 h apres l'echeance de 21 h n'est plus conservee sans effet : elle part en audit, et l'activite
   * garde sa fin automatique.
   */
  @Test
  void shouldIgnorerUneFinPointeeApresLEcheanceDeSaCible() {
    SuiviDAtelier engage = engage();
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite fin = pointeA(pointage(engage.id(), TypeDEvenementDAtelier.FIN), LE_10_MAI_2026_A_23H);

    assertThat(fin.issue()).isEqualTo(IssueDePointage.IGNORE);
    assertThat(fin.suivi().journal().evenements()).hasSize(1);
    assertThat(fin.suivi().activites()).singleElement().extracting(Activite::fin).isEqualTo(Optional.empty());
    assertThat(pointagesIgnores.lignes())
      .singleElement()
      .satisfies(ligne -> assertThat(ligne.verdict().raison()).isEqualTo(RaisonDePointageIgnore.APRES_ECHEANCE));
  }

  /**
   * Sur cette suite de gestes — un double debut, un double arret, une fin rejouee avant le dernier accepte, une fin a
   * l'heure du debut de l'activite qu'elle fermerait —, le journal ne garde que les gestes acceptes : la regle ecarte
   * chaque geste qui contredirait le precedent. La suite ne prouve que ces cas, pas une propriete generale.
   */
  @Test
  void shouldNeGarderQueLesActivitesDesGestesAcceptes() {
    SuiviDAtelier engage = engage();
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_8H);
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_9H);
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.FIN), LE_10_MAI_2026_A_12H);
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.FIN), LE_10_MAI_2026_A_12H);
    pointeA(finRejoueeA(engage.id(), LE_10_MAI_2026_A_9H), LE_10_MAI_2026_A_13H);
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.NON_CONFORMITE), LE_10_MAI_2026_A_13H);
    pointeA(pointage(engage.id(), TypeDEvenementDAtelier.FIN), LE_10_MAI_2026_A_13H);

    assertThat(suivis.get(engage.id()).orElseThrow().activites())
      .extracting(Activite::categorie, Activite::fin)
      .containsExactly(
        tuple(CategorieDActivite.TRAVAIL, Optional.of(LE_10_MAI_2026_A_12H)),
        tuple(CategorieDActivite.NON_CONFORMITE, Optional.empty())
      );
    assertThat(pointagesIgnores.lignes()).hasSize(4);
  }

  private SuiviDAtelier engage() {
    return atelier.engage(new EngagementAEnregistrer(ELEMENT_OF_2026_000042, AUTEUR_LEROY));
  }

  private PointageDAtelierTraite pointeA(PointageAEnregistrer pointage, Instant instant) {
    maintenant.set(instant);

    return atelier.pointe(pointage);
  }

  private static PointageAEnregistrer pointage(SuiviDAtelierId suivi, TypeDEvenementDAtelier type) {
    return pointage(suivi, type, OPERATEUR_ID_DUPONT);
  }

  private static PointageAEnregistrer pointage(SuiviDAtelierId suivi, TypeDEvenementDAtelier type, OperateurId operateur) {
    return PointageAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .operateur(operateur)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT);
  }

  private static PointageAEnregistrer finRejoueeA(SuiviDAtelierId suivi, Instant dateDeSurvenue) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(suivi)
      .type(TypeDEvenementDAtelier.FIN)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT)
      .dateDeSurvenue(Optional.of(dateDeSurvenue))
      .evenement(EvenementDAtelierId.newId());
  }

  private static final class ElementsEngageablesFiges implements ElementsEngageables {

    @Override
    public Optional<ElementEngage> get(ElementEngageId id) {
      return id.equals(ELEMENT_OF_2026_000042) ? Optional.of(elementEngageOf2026000042()) : Optional.empty();
    }
  }
}
