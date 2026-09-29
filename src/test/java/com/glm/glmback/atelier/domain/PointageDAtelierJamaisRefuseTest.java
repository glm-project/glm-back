package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Strategie « bornes de fin de journee », lot 8a : arreter un OF n'est jamais refuse a l'operateur. Une fin sur une
 * activite qu'il a deja arretee, ou sur un OF cloture entre-temps, ne change rien : elle est absorbee. Demarrer sur un
 * OF cloture reste la seule exception, refusee avec un message.
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
   * Le double appui sur « arreter » : la seconde fin, qui vise la meme activite, est absorbee.
   */
  @Test
  void shouldAbsorberUneSecondeFin() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    SuiviDAtelier arrete = pointeA(finDe(debut), LE_10_MAI_2026_A_12H).suivi();

    PointageDAtelierTraite seconde = pointeA(finDe(debut), LE_10_MAI_2026_A_12H.plusSeconds(2));

    assertThat(seconde.absorbe()).isTrue();
    assertThat(seconde.suivi()).isEqualTo(arrete);
    assertThat(arrete.journal().evenements()).hasSize(2);
  }

  @Test
  void shouldEnregistrerUneFinDUneActiviteEnCours() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);

    PointageDAtelierTraite fin = pointeA(finDe(debut), LE_10_MAI_2026_A_12H);

    assertThat(fin.absorbe()).isFalse();
    assertThat(fin.suivi().etat()).isEqualTo(EtatDAtelier.INTERROMPU);
  }

  /**
   * La cloture a deja arrete l'activite : arreter apres coup ne change rien.
   */
  @Test
  void shouldAbsorberUneFinSurUnOfCloture() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    SuiviDAtelier cloture = atelier.cloture(new ClotureAEnregistrer(engage.id(), AUTEUR_LEROY, Optional.of(LE_10_MAI_2026_A_12H)));

    PointageDAtelierTraite fin = pointeA(finDe(debut), LE_10_MAI_2026_A_16H);

    assertThat(fin.absorbe()).isTrue();
    assertThat(fin.suivi()).isEqualTo(cloture);
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
   * Une fin rejouee dans le desordre, datee avant le debut qu'elle vise, n'est pas redondante : elle reste refusee.
   */
  @Test
  void shouldToujoursRefuserUneFinRejoueeDansLeDesordre() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_12H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    PointageAEnregistrer finAnterieure = finRejoueeA(debut, LE_10_MAI_2026_A_9H);

    assertThatThrownBy(() -> atelier.pointe(finAnterieure)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
  }

  /**
   * Une fin rejouee avant la fin deja pointee n'est pas un double appui : datee avant le dernier fait de son activite,
   * elle reste refusee.
   */
  @Test
  void shouldToujoursRefuserUneFinRejoueeAvantLaFinDejaPointee() {
    SuiviDAtelier engage = engage();
    PointageAEnregistrer debut = ouverture(engage.id(), TypeDEvenementDAtelier.DEBUT);
    pointeA(debut, LE_10_MAI_2026_A_8H);
    pointeA(finDe(debut), LE_10_MAI_2026_A_12H);
    maintenant.set(LE_10_MAI_2026_A_13H);
    PointageAEnregistrer finAnterieure = finRejoueeA(debut, LE_10_MAI_2026_A_9H);

    assertThatThrownBy(() -> atelier.pointe(finAnterieure)).isExactlyInstanceOf(TransitionDAtelierInterditeException.class);
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
}
