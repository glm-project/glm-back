package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Une fin qui contredit le journal est conservee, et la sequence est en conflit ; une fin posterieure a la cloture
 * de l'OF est absorbee, la cloture ayant deja termine l'activite. Le demarrage sur un OF cloture et la cible
 * introuvable ou d'un autre poste restent refuses.
 */
@UnitTest
class PointageDAtelierJamaisRefuseTest {

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

  /**
   * Le double appui sur « arreter » : la seconde fin vise une activite deja terminee par la premiere. Elle n'est plus
   * absorbee : conservee, elle met la sequence en conflit.
   */
  @Test
  void shouldConserverEnConflitUneSecondeFin() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    pointeA(finDe(debut), LE_10_MAI_2026_A_12H);
    PointageAEnregistrer seconde = finDe(debut);

    PointageDAtelierTraite traite = pointeA(seconde, LE_10_MAI_2026_A_12H.plusSeconds(2));

    assertThat(traite.absorbe()).isFalse();
    assertThat(traite.suivi().journal().evenements()).hasSize(3);
    assertThat(traite.suivi().conflits())
      .singleElement()
      .satisfies(conflit -> assertThat(conflit.pointages()).contains(seconde.evenement()));
  }

  /**
   * FIN 23 h apres fin automatique 21 h : la fin pointee apres l'echeance de sa cible n'est pas un double appui. Elle
   * est enregistree, sans effet : l'activite garde sa borne automatique.
   */
  @Test
  void shouldConserverUneFinPointeeApresLEcheanceDeSaCible() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite fin = pointeA(finDe(debut), Instant.parse("2026-05-10T23:00:00Z"));

    assertThat(fin.absorbe()).isFalse();
    assertThat(fin.suivi().journal().evenements()).hasSize(2);
    assertThat(fin.suivi().activites()).singleElement().extracting(Activite::fin).isEqualTo(Optional.empty());
  }

  /**
   * FIN 17 h recue apres la fin automatique de 21 h : rejouee le lendemain avec l'heure de son geste, elle termine
   * l'activite a 17 h.
   */
  @Test
  void shouldTerminerASonHeureUneFinRejoueeApresLEcheance() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    PointageDAtelierTraite fin = atelier.pointe(finRejoueeA(debut, LE_10_MAI_2026_A_17H));

    assertThat(fin.absorbe()).isFalse();
    assertThat(intervalles(fin.suivi(), LE_11_MAI_2026_A_9H15))
      .singleElement()
      .satisfies(intervalle -> {
        assertThat(intervalle.fin()).contains(LE_10_MAI_2026_A_17H);
        assertThat(intervalle.finAutomatique()).isFalse();
      });
  }

  /**
   * Une seconde fin pointee apres une relance contredit le journal sans jamais terminer la relance : elle est
   * conservee, et la relance, qui chevauche la contradiction, n'est plus en cours.
   */
  @Test
  void shouldConserverEnConflitUneSecondeFinQuandUneRelanceEstEnCours() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    pointeA(finDe(debut), LE_10_MAI_2026_A_9H);
    pointeA(ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT), LE_10_MAI_2026_A_12H);

    PointageDAtelierTraite seconde = pointeA(finDe(debut), LE_10_MAI_2026_A_13H);

    assertThat(seconde.absorbe()).isFalse();
    assertThat(seconde.suivi().journal().evenements()).hasSize(4);
    assertThat(seconde.suivi().activitesEnCours(LE_10_MAI_2026_A_13H)).isEmpty();
    assertThat(seconde.suivi().conflits()).hasSize(1);
  }

  @Test
  void shouldEnregistrerUneFinDUneActiviteEnCours() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite fin = pointeA(finDe(debut), LE_10_MAI_2026_A_12H);

    assertThat(fin.absorbe()).isFalse();
    assertThat(fin.suivi().etat(LE_10_MAI_2026_A_12H)).isEqualTo(EtatDAtelier.INTERROMPU);
  }

  /**
   * La cloture a deja arrete l'activite : une fin pointee apres elle ne change rien.
   */
  @Test
  void shouldAbsorberUneFinPosterieureALaCloture() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    SuiviDAtelier cloture = atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_12H)));

    PointageDAtelierTraite fin = pointeA(finDe(debut), LE_10_MAI_2026_A_16H);

    assertThat(fin.absorbe()).isTrue();
    assertThat(fin.suivi()).isEqualTo(cloture);
  }

  /**
   * Une fin survenue avant la cloture, mais recue apres elle, est enregistree et terminee a son heure metier ; la
   * cloture reste acquise.
   */
  @Test
  void shouldEnregistrerUneFinAnterieureALaClotureRecueApresElle() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_12H)));

    PointageDAtelierTraite fin = atelier.pointe(finRejoueeA(debut, LE_10_MAI_2026_A_9H));

    assertThat(fin.absorbe()).isFalse();
    assertThat(fin.suivi().estCloture()).isTrue();
    assertThat(fin.suivi().activites()).singleElement().extracting(Activite::fin).isEqualTo(Optional.of(LE_10_MAI_2026_A_9H));
  }

  /**
   * Sur un OF cloture comme ailleurs, une fin qui vise une activite introuvable dans ce suivi est refusee avant d'etre
   * absorbee.
   */
  @Test
  void shouldRefuserSurUnOfClotureUneFinQuiViseUneActiviteIntrouvable() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_8H)));
    maintenant.set(LE_10_MAI_2026_A_9H);
    PointageAEnregistrer fin = finDe(ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT));

    assertThatThrownBy(() -> atelier.pointe(fin)).isExactlyInstanceOf(ActiviteViseeIntrouvableException.class);
  }

  @Test
  void shouldToujoursRefuserUnDebutSurUnOfCloture() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_8H)));
    maintenant.set(LE_10_MAI_2026_A_9H);
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);

    assertThatThrownBy(() -> atelier.pointe(debut)).isExactlyInstanceOf(SuiviDAtelierClotureException.class);
  }

  @Test
  void shouldToujoursRefuserUneNonConformiteSurUnOfCloture() {
    SuiviDAtelier engage = engage();
    maintenant.set(LE_10_MAI_2026_A_8H);
    atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_8H)));
    maintenant.set(LE_10_MAI_2026_A_9H);
    PointageAEnregistrer nonConformite = ouverture(engage.id(), TypeDEvenementDAtelier.NON_CONFORMITE);

    assertThatThrownBy(() -> atelier.pointe(nonConformite)).isExactlyInstanceOf(SuiviDAtelierClotureException.class);
  }

  /**
   * Une fin rejouee dans le desordre, datee avant le debut qu'elle vise, contredit le journal : elle est conservee, et
   * l'activite qu'elle vise est a resoudre.
   */
  @Test
  void shouldConserverEnConflitUneFinRejoueeDansLeDesordre() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_12H);
    maintenant.set(LE_10_MAI_2026_A_13H);

    PointageDAtelierTraite fin = atelier.pointe(finRejoueeA(debut, LE_10_MAI_2026_A_9H));

    assertThat(fin.absorbe()).isFalse();
    assertThat(fin.suivi().activites()).singleElement().extracting(Activite::aResoudre).isEqualTo(true);
  }

  /**
   * Une fin rejouee avant la fin deja pointee de la meme activite : les deux fins se contredisent, et la plus tardive
   * vise desormais une activite deja terminee. Les deux sont conservees, en conflit.
   */
  @Test
  void shouldConserverEnConflitUneFinRejoueeAvantLaFinDejaPointee() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    pointeA(finDe(debut), LE_10_MAI_2026_A_12H);
    maintenant.set(LE_10_MAI_2026_A_13H);

    PointageDAtelierTraite fin = atelier.pointe(finRejoueeA(debut, LE_10_MAI_2026_A_9H));

    assertThat(fin.suivi().journal().evenements()).hasSize(3);
    assertThat(fin.suivi().conflits()).hasSize(1);
  }

  private SuiviDAtelier engage() {
    return atelier.engage(new EngagementAEnregistrer(ELEMENT_OF_2026_000042, AUTEUR_LEROY));
  }

  private PointageDAtelierTraite pointeA(PointageAEnregistrer pointage, Instant instant) {
    maintenant.set(instant);

    return atelier.pointe(pointage);
  }

  private static PointageAEnregistrer ouverture(SuiviDAtelierId suivi, TypeDEvenementDAtelier type) {
    return PointageAEnregistrer.builder()
      .suivi(suivi)
      .type(type)
      .intention(IntentionDePointage.OUVERTURE)
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT);
  }

  private static PointageAEnregistrer finDe(PointageAEnregistrer ouvrant) {
    return PointageAEnregistrer.builder()
      .suivi(ouvrant.suivi())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activiteVisee(Optional.of(ActiviteId.ouvertePar(ouvrant.evenement())))
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .auteur(AUTEUR_DUPONT);
  }

  private static PointageAEnregistrer finRejoueeA(PointageAEnregistrer ouvrant, Instant dateDeSurvenue) {
    return PointageAEnregistrer.pupitreBuilder()
      .suivi(ouvrant.suivi())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activiteVisee(Optional.of(ActiviteId.ouvertePar(ouvrant.evenement())))
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

  private static List<IntervalleDActivite> intervalles(SuiviDAtelier suivi, Instant evaluation) {
    return suivi
      .activites()
      .stream()
      .map(activite -> activite.a(evaluation))
      .toList();
  }
}
