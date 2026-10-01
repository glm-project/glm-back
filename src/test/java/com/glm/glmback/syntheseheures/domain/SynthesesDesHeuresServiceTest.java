package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class SynthesesDesHeuresServiceTest {

  private static final OperateursConnus REFERENTIEL = id ->
    Optional.of(OPERATEUR_CONNU_DUPONT).filter(operateur -> operateur.id().equals(id));
  private static final ElementsDeFabrication REFERENTIEL_DES_ELEMENTS = ids ->
    List.of(FICHE_DU_CARTER)
      .stream()
      .filter(fiche -> ids.contains(fiche.id()))
      .toList();
  private static final PostesDeTravail REFERENTIEL_DES_POSTES = ids ->
    List.of(POSTE_CONNU_DMU_50, POSTE_CONNU_TOUR_14)
      .stream()
      .filter(poste -> ids.contains(poste.id()))
      .toList();

  @Test
  void shouldNotLireLaSyntheseDUnOperateurInconnu() {
    assertThatThrownBy(() -> service(List.of(), List.of(), LE_MARDI_12_MAI_2026_A_10H).synthese(OPERATEUR_ID_MARTIN, SEMAINE_20_DE_2026))
      .isExactlyInstanceOf(OperateurInconnuException.class)
      .hasMessageContaining(OPERATEUR_ID_MARTIN.uuid().toString());
  }

  @Test
  void shouldPorterLIdentiteEtLesSeptJoursVides() {
    SyntheseDesHeures synthese = synthese(List.of(), List.of());
    assertThat(synthese.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(synthese.semaine()).isEqualTo(SEMAINE_20_DE_2026);
    assertThat(synthese.jours())
      .extracting(JourDeSynthese::jour)
      .containsExactly(
        LocalDate.of(2026, 5, 11),
        LocalDate.of(2026, 5, 12),
        LocalDate.of(2026, 5, 13),
        LocalDate.of(2026, 5, 14),
        LocalDate.of(2026, 5, 15),
        LocalDate.of(2026, 5, 16),
        LocalDate.of(2026, 5, 17)
      );
    assertThat(synthese.jours()).allSatisfy(jour -> {
      assertThat(jour.pointages()).isEmpty();
      assertThat(jour.dureeOperationnelle()).isEqualTo(DureeTotale.de(Duration.ZERO));
    });
    assertThat(synthese.elements()).isEmpty();
  }

  @Test
  void shouldDemanderActivitesEtJournalSurLaSemaineLocaleEtLireLHorlogeUneSeuleFois() {
    AtomicReference<List<Instant>> bornesActivites = new AtomicReference<>();
    AtomicReference<List<Instant>> bornesJournal = new AtomicReference<>();
    AtomicInteger lectures = new AtomicInteger();
    SynthesesDesHeuresService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(() -> ZONE_PARIS)
      .activites((operateur, debut, fin) -> {
        assertThat(operateur).isEqualTo(OPERATEUR_ID_DUPONT);
        bornesActivites.set(List.of(debut, fin));
        return List.of();
      })
      .journal((operateur, debut, fin) -> {
        bornesJournal.set(List.of(debut, fin));
        return List.of();
      })
      .conflits(operateur -> List.of())
      .elements(REFERENTIEL_DES_ELEMENTS)
      .postes(REFERENTIEL_DES_POSTES)
      .clock(() -> {
        lectures.incrementAndGet();
        return LE_MARDI_12_MAI_2026_A_10H;
      })
      .synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
    assertThat(bornesActivites.get()).containsExactly(Instant.parse("2026-05-10T22:00:00Z"), Instant.parse("2026-05-17T22:00:00Z"));
    assertThat(bornesJournal.get()).isEqualTo(bornesActivites.get());
    assertThat(lectures).hasValue(1);
  }

  @Test
  void shouldCompterAvecLInstantServeurRenduSansReechantillonnerAEcheance() {
    AtomicInteger lectures = new AtomicInteger();
    SynthesesDesHeuresService service = SynthesesDesHeuresService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(() -> ZONE_PARIS)
      .activites((operateur, debut, fin) -> List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, travailOuvertA(LE_LUNDI_11_MAI_2026_A_8H))))
      .journal((operateur, debut, fin) -> List.of())
      .conflits(operateur -> List.of())
      .elements(REFERENTIEL_DES_ELEMENTS)
      .postes(REFERENTIEL_DES_POSTES)
      .clock(() -> lectures.getAndIncrement() == 0 ? Instant.parse("2026-05-11T18:59:59Z") : Instant.parse("2026-05-11T19:00:00Z"));

    SyntheseDesHeures synthese = service.synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(lectures).hasValue(1);
    assertThat(synthese.evaluation()).isEqualTo(Instant.parse("2026-05-11T18:59:59Z"));
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ZERO));
    assertThat(synthese.elements()).singleElement().extracting(ElementDeLaSynthese::duree).isEqualTo(DureeTotale.de(Duration.ZERO));
  }

  @Test
  void shouldGarderLElementEnCoursALInstantChoisiAvecUneSeuleLectureServeur() {
    AtomicInteger lectures = new AtomicInteger();
    SynthesesDesHeuresService service = SynthesesDesHeuresService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(() -> ZONE_PARIS)
      .activites((operateur, debut, fin) ->
        List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, travailOuvertA(Instant.parse("2026-05-10T20:00:00Z"))))
      )
      .journal((operateur, debut, fin) -> List.of())
      .conflits(operateur -> List.of())
      .elements(REFERENTIEL_DES_ELEMENTS)
      .postes(REFERENTIEL_DES_POSTES)
      .clock(() -> lectures.getAndIncrement() == 0 ? Instant.parse("2026-05-11T09:00:05Z") : LE_MARDI_12_MAI_2026_A_10H);

    SyntheseDesHeures synthese = service.synthese(
      OPERATEUR_ID_DUPONT,
      SEMAINE_20_DE_2026,
      Optional.of(Instant.parse("2026-05-10T23:00:00Z"))
    );

    assertThat(lectures).hasValue(1);
    assertThat(synthese.evaluation()).isEqualTo(Instant.parse("2026-05-10T23:00:00Z"));
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ZERO));
    assertThat(synthese.elements())
      .singleElement()
      .satisfies(element -> {
        assertThat(element.element().id()).isEqualTo(ELEMENT_ID_CARTER);
        assertThat(element.duree()).isEqualTo(DureeTotale.de(Duration.ZERO));
      });
  }

  @Test
  void shouldCompterEntierUnIntervalleTermineSansArrivee() {
    SyntheseDesHeures synthese = synthese(List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, travailDuCarterDe8HA10H())), List.of());
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureeOperationnelle()).isEqualTo(DureeTotale.de(Duration.ofHours(2)));
    assertThat(synthese.elements()).singleElement().extracting(ElementDeLaSynthese::duree).isEqualTo(DureeTotale.de(Duration.ofHours(2)));
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ofHours(2)));
  }

  @Test
  void shouldCompterTreizeHeuresAUneActiviteEchue() {
    SyntheseDesHeures synthese = service(
      List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, travailOuvertA(LE_LUNDI_11_MAI_2026_A_8H))),
      List.of(),
      Instant.parse("2026-05-11T19:00:00Z")
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ofHours(13)));
  }

  @Test
  void shouldGarderUneActiviteEnCoursSansCompterDeDureeAvantEcheance() {
    SyntheseDesHeures synthese = service(
      List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, travailOuvertA(LE_LUNDI_11_MAI_2026_A_8H))),
      List.of(),
      Instant.parse("2026-05-11T18:59:00Z")
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ZERO));
    assertThat(synthese.elements()).singleElement().extracting(ElementDeLaSynthese::duree).isEqualTo(DureeTotale.de(Duration.ZERO));
  }

  @Test
  void shouldConserverLElementEnCoursCommenceDimancheSansPointageDansLaSemaine() {
    SyntheseDesHeures synthese = service(
      List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, travailOuvertA(Instant.parse("2026-05-10T20:00:00Z")))),
      List.of(),
      Instant.parse("2026-05-10T23:00:00Z")
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ZERO));
    assertThat(synthese.elements())
      .singleElement()
      .extracting(element -> element.element().id())
      .isEqualTo(ELEMENT_ID_CARTER);
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages()).isEmpty();
  }

  @Test
  void shouldRepartirUnPosteDeNuitDe20HA8HSurDeuxSemaines() {
    ActiviteInterpretee nuit = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(LE_DIMANCHE_10_MAI_2026_A_20H, Optional.of(LE_LUNDI_11_MAI_2026_A_8H)))
      .echeance(LE_LUNDI_11_MAI_2026_A_8H.plusSeconds(3600))
      .finAuPlusTard(Optional.empty());
    SynthesesDesHeuresService service = service(
      List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, nuit)),
      List.of(),
      LE_MARDI_12_MAI_2026_A_10H
    );
    assertThat(service.synthese(OPERATEUR_ID_DUPONT, SEMAINE_19_DE_2026).dureeOperationnelleTotale()).isEqualTo(
      DureeTotale.de(Duration.ofHours(4))
    );
    assertThat(service.synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026).dureeOperationnelleTotale()).isEqualTo(
      DureeTotale.de(Duration.ofHours(8))
    );
  }

  @Test
  void shouldCompterUnJourTraverseSansPointagePropre() {
    ActiviteInterpretee longue = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(LE_DIMANCHE_10_MAI_2026_A_20H, Optional.of(LE_MERCREDI_13_MAI_2026_A_8H)))
      .echeance(LE_LUNDI_11_MAI_2026_A_8H.plusSeconds(3600))
      .finAuPlusTard(Optional.empty());
    SyntheseDesHeures synthese = synthese(List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, longue)), List.of());
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).dureeOperationnelle()).isEqualTo(DureeTotale.de(Duration.ofHours(24)));
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).pointages()).isEmpty();
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ofHours(56)));
  }

  @Test
  void shouldCumulerParElementDeuxElementsDe8HA9HSimultanes() {
    Plage heure = new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.of(LE_LUNDI_11_MAI_2026_A_9H));
    ActiviteInterpretee carter = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(heure)
      .echeance(LE_LUNDI_11_MAI_2026_A_23H)
      .finAuPlusTard(Optional.empty());
    ActiviteInterpretee bride = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DE_LA_BRIDE)
      .activite(activiteDeTravailDeLaBrideSurLaDmu50())
      .plage(heure)
      .echeance(LE_LUNDI_11_MAI_2026_A_23H)
      .finAuPlusTard(Optional.empty());
    SyntheseDesHeures synthese = synthese(
      List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, carter), new ActiviteDElement(ELEMENT_ENGAGE_BRIDE, bride)),
      List.of()
    );
    assertThat(synthese.elements())
      .extracting(ElementDeLaSynthese::duree)
      .containsExactly(DureeTotale.de(Duration.ofHours(1)), DureeTotale.de(Duration.ofHours(1)));
    assertThat(synthese.elements())
      .extracting(element -> element.element().nom())
      .containsExactly(NOM_OF_2026_000007, NOM_PRD_2026_000015);
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ofHours(2)));
  }

  @Test
  void shouldCompterLaNCUneSeuleFoisDansLeTotal() {
    ActiviteInterpretee nc = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DE_LA_BRIDE)
      .activite(activiteDeNonConformiteDuCarterSurLaDmu50())
      .plage(new Plage(LE_LUNDI_11_MAI_2026_A_10H, Optional.of(LE_LUNDI_11_MAI_2026_A_11H)))
      .echeance(LE_LUNDI_11_MAI_2026_A_23H)
      .finAuPlusTard(Optional.empty());
    SyntheseDesHeures synthese = synthese(
      List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, travailDuCarterDe8HA10H()), new ActiviteDElement(ELEMENT_ENGAGE_CARTER, nc)),
      List.of()
    );
    assertThat(synthese.elements())
      .singleElement()
      .satisfies(element -> {
        assertThat(element.duree()).isEqualTo(DureeTotale.de(Duration.ofHours(3)));
        assertThat(element.dureeNonConformite()).isEqualTo(DureeTotale.de(Duration.ofHours(1)));
      });
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(DureeTotale.de(Duration.ofHours(3)));
  }

  @Test
  void shouldRelireLaFicheEtLesPostes() {
    SyntheseDesHeures synthese = synthese(List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, travailDuCarterDe8HA10H())), List.of());
    assertThat(synthese.elements())
      .singleElement()
      .satisfies(element -> {
        assertThat(element.reference()).contains(REFERENCE_1015);
        assertThat(element.description()).contains(DESCRIPTION_CARTER_DE_POMPE);
        assertThat(element.postes()).containsExactly(new PosteDeLElement(POSTE_CONNU_DMU_50, Optional.of(NATURE_FRAISAGE)));
      });
  }

  @Test
  void shouldGarderLeJournalEtLElementSansActiviteInterpretable() {
    PointageDElement fin = new PointageDElement(
      POINTAGE_ID_1,
      IntentionDePointage.OUVERTURE,
      Optional.empty(),
      TypeDEvenementDAtelier.FIN,
      ELEMENT_ID_BRIDE,
      Optional.of(POSTE_ID_TOUR),
      Optional.of(NATURE_TOURNAGE),
      LE_LUNDI_11_MAI_2026_A_8H
    );
    SyntheseDesHeures synthese = synthese(List.of(), List.of(new JournalDElement(ELEMENT_ENGAGE_BRIDE, List.of(fin))));
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages()).containsExactly(fin);
    assertThat(synthese.elements())
      .singleElement()
      .satisfies(element -> {
        assertThat(element.duree()).isEqualTo(DureeTotale.de(Duration.ZERO));
        assertThat(element.reference()).isEmpty();
        assertThat(element.description()).isEmpty();
        assertThat(element.postes()).containsExactly(new PosteDeLElement(POSTE_CONNU_TOUR_14, Optional.of(NATURE_TOURNAGE)));
      });
  }

  @Test
  void shouldTrierLesPointagesParIntentionPuisIdentiteAHeureEgale() {
    PointageDElement ouverture = PointageDElement.builder()
      .id(POINTAGE_ID_1)
      .intention(IntentionDePointage.OUVERTURE)
      .cible(Optional.empty())
      .type(TypeDEvenementDAtelier.DEBUT)
      .element(ELEMENT_ID_CARTER)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H);
    PointageDElement transition = PointageDElement.builder()
      .id(POINTAGE_ID_3)
      .intention(IntentionDePointage.TRANSITION)
      .cible(Optional.empty())
      .type(TypeDEvenementDAtelier.NON_CONFORMITE)
      .element(ELEMENT_ID_CARTER)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H);
    PointageDElement fin = PointageDElement.builder()
      .id(POINTAGE_ID_2)
      .intention(IntentionDePointage.FIN)
      .cible(Optional.empty())
      .type(TypeDEvenementDAtelier.FIN)
      .element(ELEMENT_ID_BRIDE)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H);
    SyntheseDesHeures synthese = synthese(
      List.of(),
      List.of(
        new JournalDElement(ELEMENT_ENGAGE_CARTER, List.of(ouverture, transition)),
        new JournalDElement(ELEMENT_ENGAGE_BRIDE, List.of(fin))
      )
    );
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages()).containsExactly(fin, transition, ouverture);
  }

  @Test
  void shouldDepartagerParIdentiteDeuxGestesDeMemeIntentionSansDependreDeLElement() {
    PointageDElement premier = PointageDElement.builder()
      .id(POINTAGE_ID_1)
      .intention(IntentionDePointage.OUVERTURE)
      .cible(Optional.empty())
      .type(TypeDEvenementDAtelier.DEBUT)
      .element(ELEMENT_ID_BRIDE)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H);
    PointageDElement second = PointageDElement.builder()
      .id(POINTAGE_ID_2)
      .intention(IntentionDePointage.OUVERTURE)
      .cible(Optional.empty())
      .type(TypeDEvenementDAtelier.DEBUT)
      .element(ELEMENT_ID_CARTER)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_8H);
    SyntheseDesHeures synthese = synthese(
      List.of(),
      List.of(new JournalDElement(ELEMENT_ENGAGE_CARTER, List.of(second)), new JournalDElement(ELEMENT_ENGAGE_BRIDE, List.of(premier)))
    );
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages()).containsExactly(premier, second);
  }

  private static ActiviteInterpretee travailOuvertA(Instant debut) {
    return ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(debut, Optional.empty()))
      .echeance(debut.plusSeconds(13 * 3600))
      .finAuPlusTard(Optional.empty());
  }

  private static SyntheseDesHeures synthese(List<ActiviteDElement> activites, List<JournalDElement> journal) {
    return service(activites, journal, LE_MARDI_12_MAI_2026_A_10H).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
  }

  private static SynthesesDesHeuresService service(List<ActiviteDElement> activites, List<JournalDElement> journal, Instant evaluation) {
    return SynthesesDesHeuresService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(() -> ZONE_PARIS)
      .activites((operateur, debut, fin) -> activites)
      .journal((operateur, debut, fin) -> journal)
      .conflits(operateur -> List.of())
      .elements(REFERENTIEL_DES_ELEMENTS)
      .postes(REFERENTIEL_DES_POSTES)
      .clock(() -> evaluation);
  }

  private static JourDeSynthese jourDe(SyntheseDesHeures synthese, LocalDate date) {
    return synthese
      .jours()
      .stream()
      .filter(jour -> jour.jour().equals(date))
      .findFirst()
      .orElseThrow();
  }
}
