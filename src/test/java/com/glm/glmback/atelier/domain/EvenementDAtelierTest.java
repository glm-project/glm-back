package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class EvenementDAtelierTest {

  private static final EvenementDAtelierId ID = EvenementDAtelierId.newId();
  private static final Horodatage HORODATAGE = Horodatage.saisiA(LE_10_MAI_2026_A_8H);
  private static final Optional<PosteDeTravailId> SUR_FRAISEUSE_1 = Optional.of(POSTE_ID_FRAISEUSE_1);
  private static final Optional<NatureDOperation> EN_FRAISAGE = Optional.of(NATURE_FRAISAGE);
  private static final Optional<CoutHoraire> COUT_HORAIRE = Optional.of(COUT_HORAIRE_FRAISEUSE_1);
  private static final Optional<TauxHoraire> TAUX_HORAIRE = Optional.of(TAUX_HORAIRE_DUPONT);

  @ParameterizedTest
  @MethodSource("composantsManquants")
  void shouldNotBuildWithoutMandatoryComposant(Runnable construction, String champ) {
    assertThatThrownBy(construction::run).isExactlyInstanceOf(MissingMandatoryValueException.class).hasMessageContaining(champ);
  }

  @Test
  void shouldBuildEvenementNonAnnule() {
    EvenementDAtelier evenement = EvenementDAtelier.builder()
      .id(ID)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(SUR_FRAISEUSE_1)
      .nature(EN_FRAISAGE)
      .coutHoraire(COUT_HORAIRE)
      .tauxHoraire(TAUX_HORAIRE)
      .auteur(AUTEUR_DUPONT)
      .horodatage(HORODATAGE);

    assertThat(evenement.id()).isEqualTo(ID);
    assertThat(evenement.type()).isEqualTo(TypeDEvenementDAtelier.DEBUT);
    assertThat(evenement.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(evenement.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(evenement.nature()).contains(NATURE_FRAISAGE);
    assertThat(evenement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1);
    assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT);
    assertThat(evenement.auteur()).isEqualTo(AUTEUR_DUPONT);
    assertThat(evenement.horodatage()).isEqualTo(HORODATAGE);
    assertThat(evenement.annulation()).isEmpty();
    assertThat(evenement.estAnnule()).isFalse();
  }

  @Test
  void shouldBuildEvenementSansPosteNiNatureNiMontant() {
    EvenementDAtelier evenement = EvenementDAtelier.builder()
      .id(ID)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .coutHoraire(Optional.empty())
      .tauxHoraire(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .horodatage(HORODATAGE);

    assertThat(evenement.poste()).isEmpty();
    assertThat(evenement.nature()).isEmpty();
    assertThat(evenement.coutHoraire()).isEmpty();
    assertThat(evenement.tauxHoraire()).isEmpty();
    assertThat(evenement.cle()).isEqualTo(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.empty()));
  }

  @Test
  void shouldReadDatesFromHorodatage() {
    EvenementDAtelier evenement = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_8H);

    assertThat(evenement.dateDeSurvenue()).isEqualTo(LE_10_MAI_2026_A_8H);
    assertThat(evenement.dateDEnregistrement()).isEqualTo(LE_11_MAI_2026_A_9H15);
  }

  @Test
  void shouldReadCleDActivite() {
    assertThat(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H).cle()).isEqualTo(cleDeFraiseuse1DeDupont());
  }

  @Test
  void shouldNotBeUneRegularisationWhenSaisiAuMomentDuFait() {
    EvenementDAtelier evenement = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

    assertThat(evenement.estUneRegularisation()).isFalse();
  }

  @Test
  void shouldBeUneRegularisationWhenEnregistreApresLeFait() {
    EvenementDAtelier evenement = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_8H);

    assertThat(evenement.estUneRegularisation()).isTrue();
  }

  /**
   * Le pointage en retard que le client envisage : l'operateur saisit lui-meme, mais apres coup. C'est bien une
   * regularisation, et c'est l'ecart des deux dates qui le dit — jamais l'identite de l'auteur.
   */
  @Test
  void shouldBeUneRegularisationSaisieParLOperateurLuiMeme() {
    EvenementDAtelier evenement = EvenementDAtelier.builder()
      .id(ID)
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(SUR_FRAISEUSE_1)
      .nature(EN_FRAISAGE)
      .coutHoraire(COUT_HORAIRE)
      .tauxHoraire(TAUX_HORAIRE)
      .auteur(AUTEUR_DUPONT)
      .horodatage(new Horodatage(LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_9H));

    assertThat(evenement.estUneRegularisation()).isTrue();
  }

  @Test
  void shouldMarquerLEvenementCommeAnnule() {
    EvenementDAtelier annule = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H).annule(annulationParLeroy());

    assertThat(annule.estAnnule()).isTrue();
    assertThat(annule.annulation()).contains(annulationParLeroy());
  }

  /**
   * L'annulation ne doit pas faire perdre les montants figes a la saisie : ils doivent rester lisibles sur
   * l'evenement annule, exactement comme la nature.
   */
  @Test
  void shouldConserverLeCoutEtLeTauxHoraireALAnnulation() {
    EvenementDAtelier annule = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H).annule(annulationParLeroy());

    assertThat(annule.coutHoraire()).isEqualTo(COUT_HORAIRE);
    assertThat(annule.tauxHoraire()).isEqualTo(TAUX_HORAIRE);
  }

  @Test
  void shouldNotAnnulerUnEvenementDejaAnnule() {
    EvenementDAtelier annule = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H).annule(annulationParLeroy());
    Annulation seconde = annulationParLeroy();

    assertThatThrownBy(() -> annule.annule(seconde))
      .isExactlyInstanceOf(EvenementDejaAnnuleException.class)
      .hasMessageContaining("annule");
  }

  private static Stream<Arguments> composantsManquants() {
    return Stream.of(
      construction(() -> evenement(null, TypeDEvenementDAtelier.DEBUT, OPERATEUR_ID_DUPONT, SUR_FRAISEUSE_1, EN_FRAISAGE), "id"),
      construction(() -> evenement(ID, null, OPERATEUR_ID_DUPONT, SUR_FRAISEUSE_1, EN_FRAISAGE), "type"),
      construction(() -> evenement(ID, TypeDEvenementDAtelier.DEBUT, null, SUR_FRAISEUSE_1, EN_FRAISAGE), "operateur"),
      construction(() -> evenement(ID, TypeDEvenementDAtelier.DEBUT, OPERATEUR_ID_DUPONT, null, EN_FRAISAGE), "poste de travail"),
      construction(() -> evenement(ID, TypeDEvenementDAtelier.DEBUT, OPERATEUR_ID_DUPONT, SUR_FRAISEUSE_1, null), "nature de l'operation"),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            null,
            TAUX_HORAIRE,
            AUTEUR_DUPONT,
            HORODATAGE,
            Optional.empty()
          ),
        "cout horaire"
      ),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            COUT_HORAIRE,
            null,
            AUTEUR_DUPONT,
            HORODATAGE,
            Optional.empty()
          ),
        "taux horaire"
      ),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            COUT_HORAIRE,
            TAUX_HORAIRE,
            null,
            HORODATAGE,
            Optional.empty()
          ),
        "auteur"
      ),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            COUT_HORAIRE,
            TAUX_HORAIRE,
            AUTEUR_DUPONT,
            null,
            Optional.empty()
          ),
        "horodatage"
      ),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            COUT_HORAIRE,
            TAUX_HORAIRE,
            AUTEUR_DUPONT,
            HORODATAGE,
            null
          ),
        "annulation"
      )
    );
  }

  private static Arguments construction(Runnable construction, String champ) {
    return Arguments.of(construction, champ);
  }

  private static void evenement(
    EvenementDAtelierId id,
    TypeDEvenementDAtelier type,
    OperateurId operateur,
    Optional<PosteDeTravailId> poste,
    Optional<NatureDOperation> nature
  ) {
    new EvenementDAtelier(id, type, operateur, poste, nature, COUT_HORAIRE, TAUX_HORAIRE, AUTEUR_DUPONT, HORODATAGE, Optional.empty());
  }
}
