package com.glm.glmback.atelier.domain.correctiondesactivites;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import com.glm.glmback.atelier.domain.ActiviteId;
import com.glm.glmback.atelier.domain.CategorieDActivite;
import com.glm.glmback.atelier.domain.Echeance;
import com.glm.glmback.atelier.domain.ElementEngage;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.RessourcesDAtelierEnMemoire;
import com.glm.glmback.atelier.domain.SuiviDAtelierId;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * La lecture de la journee : les minuits viennent du fuseau, les durees sont coupees a chacun, et les choix d'un ajout
 * sont les suivis presents et les postes habilites.
 */
@UnitTest
class JourneesACorrigerServiceTest {

  private static final LocalDate LE_5_OCTOBRE = LocalDate.parse("2026-10-05");
  private static final Instant EVALUATION = Instant.parse("2026-10-06T12:00:00Z");
  private static final Instant LE_5_A_6H = Instant.parse("2026-10-05T04:00:00Z");
  private static final Instant LE_5_A_8H = Instant.parse("2026-10-05T06:00:00Z");
  private static final Instant LE_5_A_10H = Instant.parse("2026-10-05T08:00:00Z");
  private static final Instant LE_5_A_22H = Instant.parse("2026-10-05T20:00:00Z");
  private static final Instant LE_6_A_3H30 = Instant.parse("2026-10-06T01:30:00Z");

  private final RessourcesDAtelierEnMemoire ressources = RessourcesDAtelierEnMemoire.deLAtelier();
  private final List<ActiviteProjetee> projetees = new ArrayList<>();
  private final List<SuiviPresent> presents = new ArrayList<>();
  private final List<FicheDElement> fiches = new ArrayList<>();
  private final List<Object[]> lectures = new ArrayList<>();
  private final JourneesACorrigerService journees = JourneesACorrigerService.builder()
    .operateurs(ressources.operateurs())
    .postes(ressources.postes())
    .habilitations(ressources.habilitations())
    .activites((operateur, debut, fin) -> {
      lectures.add(new Object[] { operateur, debut, fin });
      return List.copyOf(projetees);
    })
    .suivisPresents((debut, fin) -> List.copyOf(presents))
    .fiches(ids ->
      fiches
        .stream()
        .filter(fiche -> ids.contains(fiche.id()))
        .toList()
    )
    .fuseau(() -> ZoneId.of("Europe/Paris"));

  @Test
  void shouldCalculerLesBornesDuJourDansLeFuseauDeLEntreprise() {
    JourneeACorriger journee = journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION);

    assertThat(journee.bornes()).isEqualTo(
      new BornesDuJour(Instant.parse("2026-10-04T22:00:00Z"), Instant.parse("2026-10-05T22:00:00Z"), Instant.parse("2026-10-06T22:00:00Z"))
    );
    assertThat(lectures)
      .singleElement()
      .satisfies(lecture -> {
        assertThat(lecture[0]).isEqualTo(OPERATEUR_ID_DUPONT);
        assertThat(lecture[1]).isEqualTo(journee.bornes().debut());
        assertThat(lecture[2]).isEqualTo(journee.bornes().fin());
      });
    assertThat(journee.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(journee.jour()).isEqualTo(LE_5_OCTOBRE);
    assertThat(journee.evaluation()).isEqualTo(EVALUATION);
    assertThat(journee.activites()).isEmpty();
    assertThat(journee.dureeOperationnelle()).isEqualTo(Duration.ZERO);
  }

  @Test
  void shouldRefuserUnOperateurInconnu() {
    OperateurId inconnu = new OperateurId(UUID.randomUUID());

    assertThatThrownBy(() -> journees.lit(inconnu, LE_5_OCTOBRE, EVALUATION)).isExactlyInstanceOf(
      OperateurDAtelierIntrouvableException.class
    );
  }

  @Test
  void shouldRendreUneActiviteTermineeSurLeJourAvecSonPosteEtSaDuree() {
    projetees.add(activite(LE_5_A_8H, Optional.of(LE_5_A_10H), POSTE_ID_FRAISEUSE_1));

    ActiviteDeLaJournee activite = journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION).activites().getFirst();

    assertThat(activite.etat()).isEqualTo(EtatDActiviteDeLaJournee.TERMINEE);
    assertThat(activite.debut()).isEqualTo(LE_5_A_8H);
    assertThat(activite.fin()).contains(LE_5_A_10H);
    assertThat(activite.poste()).contains(POSTE_CONNU_FRAISEUSE_1);
    assertThat(activite.element()).isEqualTo(elementEngageOf2026000042());
    assertThat(activite.categorie()).isEqualTo(CategorieDActivite.TRAVAIL);
    assertThat(activite.dureeSurLeJour()).contains(Duration.ofHours(2));
    assertThat(activite.autresJours()).isEmpty();
  }

  /**
   * Une activite a cheval est coupee aux minuits de l'entreprise : le jour lu garde sa portion, l'autre jour est annonce
   * avec la sienne.
   */
  @Test
  void shouldCouperUneActiviteACheval() {
    projetees.add(activite(LE_5_A_22H, Optional.of(LE_6_A_3H30), POSTE_ID_FRAISEUSE_1));

    JourneeACorriger journee = journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION);

    ActiviteDeLaJournee activite = journee.activites().getFirst();
    assertThat(activite.dureeSurLeJour()).contains(Duration.ofHours(2));
    assertThat(activite.autresJours()).containsExactly(new PortionDeJour(LocalDate.parse("2026-10-06"), Duration.ofMinutes(210)));
    assertThat(journee.dureeOperationnelle()).isEqualTo(Duration.ofHours(2));
  }

  @Test
  void shouldLireLaPortionDuLendemainCommeLeJourLu() {
    projetees.add(activite(LE_5_A_22H, Optional.of(LE_6_A_3H30), POSTE_ID_FRAISEUSE_1));

    ActiviteDeLaJournee activite = journees.lit(OPERATEUR_ID_DUPONT, LocalDate.parse("2026-10-06"), EVALUATION).activites().getFirst();

    assertThat(activite.dureeSurLeJour()).contains(Duration.ofMinutes(210));
    assertThat(activite.autresJours()).containsExactly(new PortionDeJour(LE_5_OCTOBRE, Duration.ofHours(2)));
  }

  /**
   * Une fin automatique compte jusqu'a son echeance, treize heures apres son debut, et signale son etat. Une activite en
   * cours n'a ni fin, ni duree, ni autre jour, et ne compte pas dans la duree operationnelle.
   */
  @Test
  void shouldCompterUneFinAutomatiqueEtNePasCompterUneActiviteEnCours() {
    projetees.add(activite(LE_5_A_6H, Optional.empty(), POSTE_ID_FRAISEUSE_1));
    projetees.add(activite(Instant.parse("2026-10-06T10:00:00Z"), Optional.empty(), POSTE_ID_FRAISEUSE_2));

    JourneeACorriger journee = journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION);

    ActiviteDeLaJournee automatique = journee.activites().get(0);
    assertThat(automatique.etat()).isEqualTo(EtatDActiviteDeLaJournee.TERMINEE_AUTOMATIQUEMENT);
    assertThat(automatique.fin()).contains(LE_5_A_6H.plus(Duration.ofHours(13)));
    assertThat(automatique.dureeSurLeJour()).contains(Duration.ofHours(13));
    ActiviteDeLaJournee enCours = journee.activites().get(1);
    assertThat(enCours.etat()).isEqualTo(EtatDActiviteDeLaJournee.EN_COURS);
    assertThat(enCours.fin()).isEmpty();
    assertThat(enCours.dureeSurLeJour()).isEmpty();
    assertThat(enCours.autresJours()).isEmpty();
    assertThat(journee.dureeOperationnelle()).isEqualTo(Duration.ofHours(13));
  }

  /**
   * La duree operationnelle est une somme simple : deux activites menees de front comptent chacune, sans union des
   * intervalles.
   */
  @Test
  void shouldAdditionnerLesPortionsSansUnionDesIntervalles() {
    projetees.add(activite(LE_5_A_8H, Optional.of(LE_5_A_10H), POSTE_ID_FRAISEUSE_1));
    projetees.add(activite(LE_5_A_8H, Optional.of(LE_5_A_10H), POSTE_ID_FRAISEUSE_2));

    assertThat(journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION).dureeOperationnelle()).isEqualTo(Duration.ofHours(4));
  }

  @Test
  void shouldRendreUneActiviteSansPosteSansPoste() {
    projetees.add(activiteSansPoste(LE_5_A_8H, Optional.of(LE_5_A_10H)));

    assertThat(journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION).activites().getFirst().poste()).isEmpty();
  }

  /**
   * Une activite que la projection rendrait sans toucher le jour lu n'y compte rien.
   */
  @Test
  void shouldCompterZeroPourUneActiviteQuiNeToucheAucunPortionDuJour() {
    projetees.add(
      activite(Instant.parse("2026-10-07T08:00:00Z"), Optional.of(Instant.parse("2026-10-07T10:00:00Z")), POSTE_ID_FRAISEUSE_1)
    );

    ActiviteDeLaJournee activite = journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION).activites().getFirst();

    assertThat(activite.dureeSurLeJour()).contains(Duration.ZERO);
    assertThat(activite.autresJours()).hasSize(1);
  }

  @Test
  void shouldProposerLesSuivisPresentsEtLesPostesHabilitesParLibelle() {
    presents.add(
      SuiviPresent.builder()
        .id(SuiviDAtelierId.newId())
        .element(elementEngageOf2026000042())
        .engageLe(LE_5_A_6H)
        .clotureLe(Optional.empty())
    );
    presents.add(
      SuiviPresent.builder()
        .id(SuiviDAtelierId.newId())
        .element(elementEngageOf2026000043())
        .engageLe(LE_5_A_6H)
        .clotureLe(Optional.of(LE_5_A_10H))
    );

    ChoixDAjout choix = journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION).choix();

    assertThat(choix.suivis()).isEqualTo(presents);
    assertThat(choix.postes()).containsExactly(POSTE_CONNU_FRAISEUSE_1, POSTE_CONNU_FRAISEUSE_2);
    assertThat(journees.lit(OPERATEUR_ID_MARTIN, LE_5_OCTOBRE, EVALUATION).choix().postes()).isEmpty();
  }

  @Test
  void shouldRelireLaFicheDesElementsDeLaJournee() {
    FicheDElement fiche = new FicheDElement(
      ELEMENT_OF_2026_000042,
      Optional.of(new ReferenceDElement("1015")),
      Optional.of(new DescriptionDElement("Carter de pompe"))
    );
    fiches.add(fiche);
    fiches.add(new FicheDElement(new ElementEngageId(UUID.randomUUID()), Optional.empty(), Optional.empty()));
    projetees.add(activite(LE_5_A_8H, Optional.of(LE_5_A_10H), POSTE_ID_FRAISEUSE_1));

    JourneeACorriger journee = journees.lit(OPERATEUR_ID_DUPONT, LE_5_OCTOBRE, EVALUATION);

    assertThat(journee.fiche(ELEMENT_OF_2026_000042)).contains(fiche);
    assertThat(journee.fiche(ELEMENT_OF_2026_000043)).isEmpty();
    assertThat(journee.fiches()).containsExactly(fiche);
  }

  private static ActiviteProjetee activite(Instant debut, Optional<Instant> fin, PosteDeTravailId poste) {
    return projetee(debut, fin, Optional.of(poste));
  }

  private static ActiviteProjetee activiteSansPoste(Instant debut, Optional<Instant> fin) {
    return projetee(debut, fin, Optional.empty());
  }

  private static ActiviteProjetee projetee(Instant debut, Optional<Instant> fin, Optional<PosteDeTravailId> poste) {
    ElementEngage element = elementEngageOf2026000042();

    return ActiviteProjetee.builder()
      .id(new ActiviteId(UUID.randomUUID()))
      .suivi(SuiviDAtelierId.newId())
      .element(element)
      .poste(poste)
      .categorie(CategorieDActivite.TRAVAIL)
      .debut(debut)
      .echeance(new Echeance(debut.plus(Duration.ofHours(13))))
      .fin(fin);
  }
}
