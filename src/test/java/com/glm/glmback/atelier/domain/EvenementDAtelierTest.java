package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.shared.error.domain.MissingMandatoryValueException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class EvenementDAtelierTest {

  private static final EvenementDAtelierId ID = EvenementDAtelierId.newId();
  private static final ActiviteId ACTIVITE = ActiviteId.ouvertePar(ID);
  private static final ActiviteId ACTIVITE_VISEE = new ActiviteId(UUID.fromString("00000000-0000-0000-0000-000000000007"));
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
  void shouldBuildEvenement() {
    EvenementDAtelier evenement = EvenementDAtelier.builder()
      .id(ID)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ACTIVITE))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(SUR_FRAISEUSE_1)
      .nature(EN_FRAISAGE)
      .coutHoraire(COUT_HORAIRE)
      .tauxHoraire(TAUX_HORAIRE)
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .horodatage(HORODATAGE);

    assertThat(evenement.id()).isEqualTo(ID);
    assertThat(evenement.type()).isEqualTo(TypeDEvenementDAtelier.DEBUT);
    assertThat(evenement.intention()).isEqualTo(IntentionDePointage.OUVERTURE);
    assertThat(evenement.activite()).contains(ACTIVITE);
    assertThat(evenement.activiteVisee()).isEmpty();
    assertThat(evenement.operateur()).isEqualTo(OPERATEUR_ID_DUPONT);
    assertThat(evenement.poste()).contains(POSTE_ID_FRAISEUSE_1);
    assertThat(evenement.nature()).contains(NATURE_FRAISAGE);
    assertThat(evenement.coutHoraire()).contains(COUT_HORAIRE_FRAISEUSE_1);
    assertThat(evenement.tauxHoraire()).contains(TAUX_HORAIRE_DUPONT);
    assertThat(evenement.auteur()).isEqualTo(AUTEUR_DUPONT);
    assertThat(evenement.origine()).isEqualTo(OrigineDuPointage.POINTAGE);
    assertThat(evenement.horodatage()).isEqualTo(HORODATAGE);
  }

  @Test
  void shouldBuildEvenementSansPosteNiNatureNiMontant() {
    EvenementDAtelier evenement = EvenementDAtelier.builder()
      .id(ID)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ACTIVITE))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .coutHoraire(Optional.empty())
      .tauxHoraire(Optional.empty())
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
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
  void shouldBeUneRegularisationWhenRegulariseParLeGestionnaire() {
    EvenementDAtelier evenement = debutSurFraiseuse1RegulariseParLeroyA(LE_10_MAI_2026_A_8H);

    assertThat(evenement.estUneRegularisation()).isTrue();
  }

  /**
   * Un pupitre reste hors ligne rejoue son pointage apres coup, avec l'heure du geste : les deux dates s'ecartent,
   * mais ce n'est pas un acte du gestionnaire. L'ecart ne fait donc pas la regularisation, l'origine seule la dit.
   */
  @Test
  void shouldNotBeUneRegularisationWhenPointageEnregistreApresLeFait() {
    EvenementDAtelier evenement = evenementNeDe(OrigineDuPointage.POINTAGE, new Horodatage(LE_10_MAI_2026_A_8H, LE_10_MAI_2026_A_9H));

    assertThat(evenement.estUneRegularisation()).isFalse();
  }

  /**
   * Le gestionnaire qui regularise un fait a l'heure meme ou il le saisit fait bien une regularisation : c'est son acte
   * qui compte, pas l'ecart entre les deux dates.
   */
  @Test
  void shouldBeUneRegularisationWhenRegulariseAuMomentDuFait() {
    EvenementDAtelier evenement = evenementNeDe(OrigineDuPointage.REGULARISATION, Horodatage.saisiA(LE_10_MAI_2026_A_8H));

    assertThat(evenement.estUneRegularisation()).isTrue();
  }

  @Test
  void shouldBuildUneTransitionQuiOuvreUneActiviteEtEnViseUneAutre() {
    EvenementDAtelier transition = geste(
      TypeDEvenementDAtelier.NON_CONFORMITE,
      IntentionDePointage.TRANSITION,
      Optional.of(ACTIVITE),
      Optional.of(ACTIVITE_VISEE)
    );

    assertThat(transition.intention()).isEqualTo(IntentionDePointage.TRANSITION);
    assertThat(transition.activite()).contains(ACTIVITE);
    assertThat(transition.activiteVisee()).contains(ACTIVITE_VISEE);
  }

  @Test
  void shouldBuildUneFinQuiViseUneActiviteSansEnOuvrir() {
    EvenementDAtelier fin = geste(TypeDEvenementDAtelier.FIN, IntentionDePointage.FIN, Optional.empty(), Optional.of(ACTIVITE_VISEE));

    assertThat(fin.activite()).isEmpty();
    assertThat(fin.activiteVisee()).contains(ACTIVITE_VISEE);
  }

  @ParameterizedTest
  @MethodSource("gestesIncoherents")
  void shouldNotBuildUnGesteIncoherent(
    TypeDEvenementDAtelier type,
    IntentionDePointage intention,
    Optional<ActiviteId> activite,
    Optional<ActiviteId> activiteVisee
  ) {
    assertThatThrownBy(() -> geste(type, intention, activite, activiteVisee))
      .isExactlyInstanceOf(IntentionDePointageIncoherenteException.class)
      .hasMessageContaining(type.name())
      .hasMessageContaining(intention.name());
  }

  private static Stream<Arguments> gestesIncoherents() {
    Optional<ActiviteId> aucune = Optional.empty();
    Optional<ActiviteId> ouverte = Optional.of(ACTIVITE);
    Optional<ActiviteId> visee = Optional.of(ACTIVITE_VISEE);

    return Stream.of(
      Arguments.of(TypeDEvenementDAtelier.FIN, IntentionDePointage.OUVERTURE, ouverte, aucune),
      Arguments.of(TypeDEvenementDAtelier.FIN, IntentionDePointage.TRANSITION, ouverte, visee),
      Arguments.of(TypeDEvenementDAtelier.DEBUT, IntentionDePointage.FIN, aucune, visee),
      Arguments.of(TypeDEvenementDAtelier.DEBUT, IntentionDePointage.OUVERTURE, aucune, aucune),
      Arguments.of(TypeDEvenementDAtelier.DEBUT, IntentionDePointage.OUVERTURE, ouverte, visee),
      Arguments.of(TypeDEvenementDAtelier.NON_CONFORMITE, IntentionDePointage.TRANSITION, ouverte, aucune),
      Arguments.of(TypeDEvenementDAtelier.FIN, IntentionDePointage.FIN, aucune, aucune),
      Arguments.of(TypeDEvenementDAtelier.FIN, IntentionDePointage.FIN, ouverte, visee)
    );
  }

  private static Stream<Arguments> composantsManquants() {
    return Stream.of(
      construction(() -> evenement(null, TypeDEvenementDAtelier.DEBUT, OPERATEUR_ID_DUPONT, SUR_FRAISEUSE_1, EN_FRAISAGE), "id"),
      construction(() -> evenement(ID, null, OPERATEUR_ID_DUPONT, SUR_FRAISEUSE_1, EN_FRAISAGE), "type"),
      construction(() -> geste(null, Optional.of(ACTIVITE), Optional.empty()), "intention"),
      construction(() -> geste(IntentionDePointage.OUVERTURE, null, Optional.empty()), "activite"),
      construction(() -> geste(IntentionDePointage.OUVERTURE, Optional.of(ACTIVITE), null), "activite visee"),
      construction(() -> evenement(ID, TypeDEvenementDAtelier.DEBUT, null, SUR_FRAISEUSE_1, EN_FRAISAGE), "operateur"),
      construction(() -> evenement(ID, TypeDEvenementDAtelier.DEBUT, OPERATEUR_ID_DUPONT, null, EN_FRAISAGE), "poste de travail"),
      construction(() -> evenement(ID, TypeDEvenementDAtelier.DEBUT, OPERATEUR_ID_DUPONT, SUR_FRAISEUSE_1, null), "nature de l'operation"),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            IntentionDePointage.OUVERTURE,
            Optional.of(ACTIVITE),
            Optional.empty(),
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            null,
            TAUX_HORAIRE,
            AUTEUR_DUPONT,
            OrigineDuPointage.POINTAGE,
            HORODATAGE
          ),
        "cout horaire"
      ),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            IntentionDePointage.OUVERTURE,
            Optional.of(ACTIVITE),
            Optional.empty(),
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            COUT_HORAIRE,
            null,
            AUTEUR_DUPONT,
            OrigineDuPointage.POINTAGE,
            HORODATAGE
          ),
        "taux horaire"
      ),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            IntentionDePointage.OUVERTURE,
            Optional.of(ACTIVITE),
            Optional.empty(),
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            COUT_HORAIRE,
            TAUX_HORAIRE,
            null,
            OrigineDuPointage.POINTAGE,
            HORODATAGE
          ),
        "auteur"
      ),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            IntentionDePointage.OUVERTURE,
            Optional.of(ACTIVITE),
            Optional.empty(),
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            COUT_HORAIRE,
            TAUX_HORAIRE,
            AUTEUR_DUPONT,
            null,
            HORODATAGE
          ),
        "origine"
      ),
      construction(
        () ->
          new EvenementDAtelier(
            ID,
            TypeDEvenementDAtelier.DEBUT,
            IntentionDePointage.OUVERTURE,
            Optional.of(ACTIVITE),
            Optional.empty(),
            OPERATEUR_ID_DUPONT,
            SUR_FRAISEUSE_1,
            EN_FRAISAGE,
            COUT_HORAIRE,
            TAUX_HORAIRE,
            AUTEUR_DUPONT,
            OrigineDuPointage.POINTAGE,
            null
          ),
        "horodatage"
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
    new EvenementDAtelier(
      id,
      type,
      IntentionDePointage.OUVERTURE,
      Optional.of(ACTIVITE),
      Optional.empty(),
      operateur,
      poste,
      nature,
      COUT_HORAIRE,
      TAUX_HORAIRE,
      AUTEUR_DUPONT,
      OrigineDuPointage.POINTAGE,
      HORODATAGE
    );
  }

  private static void geste(IntentionDePointage intention, Optional<ActiviteId> activite, Optional<ActiviteId> activiteVisee) {
    geste(TypeDEvenementDAtelier.DEBUT, intention, activite, activiteVisee);
  }

  private static EvenementDAtelier geste(
    TypeDEvenementDAtelier type,
    IntentionDePointage intention,
    Optional<ActiviteId> activite,
    Optional<ActiviteId> activiteVisee
  ) {
    return new EvenementDAtelier(
      ID,
      type,
      intention,
      activite,
      activiteVisee,
      OPERATEUR_ID_DUPONT,
      SUR_FRAISEUSE_1,
      EN_FRAISAGE,
      COUT_HORAIRE,
      TAUX_HORAIRE,
      AUTEUR_DUPONT,
      OrigineDuPointage.POINTAGE,
      HORODATAGE
    );
  }

  private static EvenementDAtelier evenementNeDe(OrigineDuPointage origine, Horodatage horodatage) {
    return EvenementDAtelier.builder()
      .id(ID)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ACTIVITE))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(SUR_FRAISEUSE_1)
      .nature(EN_FRAISAGE)
      .coutHoraire(COUT_HORAIRE)
      .tauxHoraire(TAUX_HORAIRE)
      .auteur(AUTEUR_DUPONT)
      .origine(origine)
      .horodatage(horodatage);
  }
}
