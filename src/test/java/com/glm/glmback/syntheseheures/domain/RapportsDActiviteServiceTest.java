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

/**
 * Le rapport de mai 2026, lu le mercredi 13 mai a midi : les heures restent en clair, ce sont elles qui portent les
 * chevauchements et les minuits.
 */
@UnitTest
class RapportsDActiviteServiceTest {

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
  void shouldNotLireLeRapportDUnOperateurInconnu() {
    assertThatThrownBy(() -> service(List.of()).rapport(OPERATEUR_ID_MARTIN, MAI_2026))
      .isExactlyInstanceOf(OperateurInconnuException.class)
      .hasMessageContaining(OPERATEUR_ID_MARTIN.uuid().toString());
  }

  @Test
  void shouldRefuserUnePeriodeQuiNAPasCommence() {
    assertThatThrownBy(() -> service(List.of()).rapport(OPERATEUR_ID_DUPONT, new MoisCalendaire(2026, 6)))
      .isExactlyInstanceOf(PeriodeFutureException.class)
      .hasMessageContaining("2026-06-01")
      .hasMessageContaining("2026-06-30");
  }

  @Test
  void shouldLireLaPeriodeEnCoursCommencee() {
    assertThat(service(List.of()).rapport(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026).jours()).hasSize(7);
  }

  @Test
  void shouldDemanderLesActivitesSurLeMoisLocalEtLireLHorlogeUneSeuleFois() {
    AtomicReference<List<Instant>> bornes = new AtomicReference<>();
    AtomicInteger lectures = new AtomicInteger();

    RapportDActivite rapport = RapportsDActiviteService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(() -> ZONE_PARIS)
      .activites((operateur, debut, fin) -> {
        assertThat(operateur).isEqualTo(OPERATEUR_ID_DUPONT);
        bornes.set(List.of(debut, fin));
        return List.of();
      })
      .elements(REFERENTIEL_DES_ELEMENTS)
      .postes(REFERENTIEL_DES_POSTES)
      .clock(() -> {
        lectures.incrementAndGet();
        return LE_MERCREDI_13_MAI_2026_A_12H;
      })
      .rapport(OPERATEUR_ID_DUPONT, MAI_2026);

    assertThat(bornes.get()).containsExactly(Instant.parse("2026-04-30T22:00:00Z"), Instant.parse("2026-05-31T22:00:00Z"));
    assertThat(lectures).hasValue(1);
    assertThat(rapport.evaluation()).isEqualTo(LE_MERCREDI_13_MAI_2026_A_12H);
  }

  @Test
  void shouldPorterLOperateurLaPeriodeEtTousLesJoursDuMoisVides() {
    RapportDActivite rapport = rapport(List.of());

    assertThat(rapport.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(rapport.periode()).isEqualTo(MAI_2026);
    assertThat(rapport.fuseau()).isEqualTo(ZONE_PARIS);
    assertThat(rapport.jours())
      .hasSize(31)
      .allSatisfy(jour -> assertThat(jour.lignes()).isEmpty());
    assertThat(rapport.jours().getFirst().jour()).isEqualTo(LocalDate.of(2026, 5, 1));
    assertThat(rapport.tempsPointe()).isEqualTo(heures(0));
    assertThat(rapport.joursAvecPointage()).isZero();
    assertThat(rapport.elements()).isEmpty();
    assertThat(rapport.natures()).isEmpty();
  }

  /** Une heure sur deux elements compte sur chacun : 8h-10h et 9h-11h font quatre heures dans la journee. */
  @Test
  void shouldCompterSurChaqueElementUneHeurePasseeSurDeux() {
    RapportDActivite rapport = rapport(
      List.of(
        new ActiviteDElement(ELEMENT_ENGAGE_CARTER, terminee(ACTIVITE_ID_DU_CARTER, activiteDeTravailDuCarterSurLaDmu50(), 11, 8, 11, 10)),
        new ActiviteDElement(ELEMENT_ENGAGE_BRIDE, terminee(ACTIVITE_ID_DE_LA_BRIDE, activiteDeTravailDeLaBrideSurLaDmu50(), 11, 9, 11, 11))
      )
    );

    JourDuRapport lundi = jourDe(rapport, LUNDI_11_MAI_2026);
    assertThat(lundi.duree()).isEqualTo(heures(4));
    assertThat(lundi.simultanee()).isTrue();
    assertThat(lundi.lignes()).extracting(LigneDuJournal::activite).containsExactly(ACTIVITE_ID_DU_CARTER, ACTIVITE_ID_DE_LA_BRIDE);
    assertThat(lundi.lignes()).allMatch(LigneDuJournal::simultanee);
    assertThat(rapport.tempsPointe()).isEqualTo(heures(4));
    assertThat(rapport.elements())
      .extracting(element -> element.element().nom(), ElementDeLaSynthese::duree)
      .containsExactly(tuple(NOM_OF_2026_000007, heures(2)), tuple(NOM_PRD_2026_000015, heures(2)));
    assertThat(rapport.natures()).containsExactly(new NatureDuRapport(Optional.of(NATURE_FRAISAGE), heures(4)));
    assertThat(rapport.journal()).hasSize(2);
    assertThat(rapport.joursAvecPointage()).isEqualTo(1);
  }

  /** Deux pointages qui se suivent ne sont pas simultanes : la fin de l'un est le debut de l'autre. */
  @Test
  void shouldNePasSignalerDeuxPointagesQuiSeSuivent() {
    RapportDActivite rapport = rapport(
      List.of(
        new ActiviteDElement(ELEMENT_ENGAGE_CARTER, terminee(ACTIVITE_ID_DU_CARTER, activiteDeTravailDuCarterSurLaDmu50(), 11, 8, 11, 10)),
        new ActiviteDElement(ELEMENT_ENGAGE_CARTER, terminee(ACTIVITE_ID_DU_TOUR, activiteDeTravailDuCarterAuTour(), 11, 10, 11, 13))
      )
    );

    JourDuRapport lundi = jourDe(rapport, LUNDI_11_MAI_2026);
    assertThat(lundi.simultanee()).isFalse();
    assertThat(lundi.lignes())
      .extracting(ligne -> ligne.poste().orElseThrow(), LigneDuJournal::nature, LigneDuJournal::duree)
      .containsExactly(
        tuple(POSTE_CONNU_DMU_50, Optional.of(NATURE_FRAISAGE), heures(2)),
        tuple(POSTE_CONNU_TOUR_14, Optional.of(NATURE_TOURNAGE), heures(3))
      );
    assertThat(rapport.natures()).containsExactly(
      new NatureDuRapport(Optional.of(NATURE_TOURNAGE), heures(3)),
      new NatureDuRapport(Optional.of(NATURE_FRAISAGE), heures(2))
    );
    ElementDeLaSynthese carter = rapport.elements().getFirst();
    assertThat(carter.reference()).contains(REFERENCE_1015);
    assertThat(carter.description()).contains(DESCRIPTION_CARTER_DE_POMPE);
    assertThat(carter.postes()).containsExactly(
      new PosteDeLElement(POSTE_CONNU_DMU_50, Optional.of(NATURE_FRAISAGE)),
      new PosteDeLElement(POSTE_CONNU_TOUR_14, Optional.of(NATURE_TOURNAGE))
    );
  }

  @Test
  void shouldCouperAMinuitUnPointageDeNuit() {
    RapportDActivite rapport = rapport(
      List.of(
        new ActiviteDElement(ELEMENT_ENGAGE_CARTER, terminee(ACTIVITE_ID_DU_CARTER, activiteDeTravailDuCarterSurLaDmu50(), 11, 22, 12, 2))
      )
    );

    assertThat(jourDe(rapport, LUNDI_11_MAI_2026).duree()).isEqualTo(heures(2));
    assertThat(jourDe(rapport, MARDI_12_MAI_2026).duree()).isEqualTo(heures(2));
    assertThat(rapport.journal()).hasSize(2).extracting(LigneDuJournal::activite).containsOnly(ACTIVITE_ID_DU_CARTER);
    assertThat(rapport.elements().getFirst().duree()).isEqualTo(heures(4));
  }

  @Test
  void shouldCompterJusquASonEcheanceUnPointageEnFinAutomatique() {
    ActiviteInterpretee oubliee = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_CARTER)
      .activite(activiteDeTravailDuCarterSurLaDmu50())
      .plage(new Plage(LE_LUNDI_11_MAI_2026_A_8H, Optional.empty()))
      .echeance(LE_LUNDI_11_MAI_2026_A_18H);

    RapportDActivite rapport = rapport(List.of(new ActiviteDElement(ELEMENT_ENGAGE_CARTER, oubliee)));

    LigneDuJournal ligne = rapport.journal().getFirst();
    assertThat(ligne.etat()).isEqualTo(EtatDActivite.TERMINEE_AUTOMATIQUEMENT);
    assertThat(ligne.plage().fin()).contains(LE_LUNDI_11_MAI_2026_A_18H);
    assertThat(ligne.duree()).isEqualTo(heures(10));
    assertThat(jourDe(rapport, LUNDI_11_MAI_2026).finAutomatique()).isTrue();
    assertThat(jourDe(rapport, MARDI_12_MAI_2026).finAutomatique()).isFalse();
    assertThat(rapport.finsAutomatiques()).isEqualTo(1);
  }

  /** En cours le mercredi a midi : le pointage figure au journal, sans fin ni duree, et ne recouvre rien. */
  @Test
  void shouldMontrerSansDureeUnPointageEnCours() {
    ActiviteInterpretee enCours = ActiviteInterpretee.builder()
      .id(ACTIVITE_ID_DU_TOUR)
      .activite(activiteDeTravailDuCarterAuTour())
      .plage(new Plage(LE_MERCREDI_13_MAI_2026_A_8H, Optional.empty()))
      .echeance(LE_MERCREDI_13_MAI_2026_A_8H.plus(Duration.ofHours(13)));

    RapportDActivite rapport = rapport(
      List.of(
        new ActiviteDElement(ELEMENT_ENGAGE_CARTER, enCours),
        new ActiviteDElement(ELEMENT_ENGAGE_BRIDE, terminee(ACTIVITE_ID_DE_LA_BRIDE, activiteDeTravailDeLaBrideSurLaDmu50(), 13, 9, 13, 11))
      )
    );

    JourDuRapport mercredi = jourDe(rapport, LocalDate.of(2026, 5, 13));
    assertThat(mercredi.lignes())
      .extracting(LigneDuJournal::etat, LigneDuJournal::duree, LigneDuJournal::simultanee)
      .containsExactly(tuple(EtatDActivite.EN_COURS, heures(0), false), tuple(EtatDActivite.TERMINEE, heures(2), false));
    assertThat(mercredi.lignes().getFirst().plage().fin()).isEmpty();
    assertThat(rapport.tempsPointe()).isEqualTo(heures(2));
    assertThat(rapport.joursAvecPointage()).isEqualTo(1);
    assertThat(rapport.finsAutomatiques()).isZero();
  }

  @Test
  void shouldPresenterAPartLaNonConformiteCompriseDansLeTotal() {
    RapportDActivite rapport = rapport(
      List.of(
        new ActiviteDElement(ELEMENT_ENGAGE_CARTER, terminee(ACTIVITE_ID_DU_CARTER, activiteDeTravailDuCarterSurLaDmu50(), 11, 8, 11, 10)),
        new ActiviteDElement(
          ELEMENT_ENGAGE_CARTER,
          terminee(ACTIVITE_ID_DU_TOUR, activiteDeNonConformiteDuCarterSurLaDmu50(), 11, 10, 11, 11)
        )
      )
    );

    assertThat(jourDe(rapport, LUNDI_11_MAI_2026).dureeNonConformite()).isEqualTo(heures(1));
    assertThat(rapport.dureeNonConformite()).isEqualTo(heures(1));
    assertThat(rapport.tempsPointe()).isEqualTo(heures(3));
    assertThat(rapport.elements().getFirst().dureeNonConformite()).isEqualTo(heures(1));
    assertThat(rapport.journal())
      .extracting(LigneDuJournal::categorie)
      .containsExactly(CategorieDActivite.TRAVAIL, CategorieDActivite.NON_CONFORMITE);
  }

  /** Pointe sans poste : ni poste ni nature, la nature absente vient apres les autres, l'element sans fiche reste. */
  @Test
  void shouldGarderUnPointageSansPosteNiNatureNiFiche() {
    Activite sansPoste = Activite.builder()
      .element(ELEMENT_ID_BRIDE)
      .poste(Optional.empty())
      .nature(Optional.empty())
      .categorie(CategorieDActivite.TRAVAIL);

    RapportDActivite rapport = rapport(
      List.of(
        new ActiviteDElement(ELEMENT_ENGAGE_BRIDE, terminee(ACTIVITE_ID_DE_LA_BRIDE, sansPoste, 11, 8, 11, 10)),
        new ActiviteDElement(ELEMENT_ENGAGE_CARTER, terminee(ACTIVITE_ID_DU_CARTER, activiteDeTravailDuCarterSurLaDmu50(), 12, 8, 12, 10))
      )
    );

    assertThat(rapport.natures()).containsExactly(
      new NatureDuRapport(Optional.of(NATURE_FRAISAGE), heures(2)),
      new NatureDuRapport(Optional.empty(), heures(2))
    );
    ElementDeLaSynthese bride = rapport.elements().getFirst();
    assertThat(bride.element()).isEqualTo(ELEMENT_ENGAGE_BRIDE);
    assertThat(bride.reference()).isEmpty();
    assertThat(bride.postes()).isEmpty();
    assertThat(rapport.journal().getFirst().poste()).isEmpty();
  }

  /** Le temps decroissant ordonne les elements, avant leur nom. */
  @Test
  void shouldOrdonnerLesElementsParTempsDecroissant() {
    RapportDActivite rapport = rapport(
      List.of(
        new ActiviteDElement(ELEMENT_ENGAGE_BRIDE, terminee(ACTIVITE_ID_DE_LA_BRIDE, activiteDeTravailDeLaBrideSurLaDmu50(), 11, 8, 11, 9)),
        new ActiviteDElement(ELEMENT_ENGAGE_CARTER, terminee(ACTIVITE_ID_DU_CARTER, activiteDeTravailDuCarterSurLaDmu50(), 12, 8, 12, 11))
      )
    );

    assertThat(rapport.elements()).extracting(ElementDeLaSynthese::element).containsExactly(ELEMENT_ENGAGE_CARTER, ELEMENT_ENGAGE_BRIDE);
  }

  private static ActiviteInterpretee terminee(
    ActiviteId id,
    Activite activite,
    int jourDeDebut,
    int heureDeDebut,
    int jourDeFin,
    int heureDeFin
  ) {
    return ActiviteInterpretee.builder()
      .id(id)
      .activite(activite)
      .plage(new Plage(aParis(jourDeDebut, heureDeDebut), Optional.of(aParis(jourDeFin, heureDeFin))))
      .echeance(aParis(jourDeFin, heureDeFin).plus(Duration.ofHours(1)));
  }

  private static Instant aParis(int jourDeMai, int heure) {
    return LocalDate.of(2026, 5, jourDeMai).atTime(heure, 0).atZone(ZONE_PARIS).toInstant();
  }

  private static DureeTotale heures(int heures) {
    return DureeTotale.de(Duration.ofHours(heures));
  }

  private static RapportDActivite rapport(List<ActiviteDElement> activites) {
    return service(activites).rapport(OPERATEUR_ID_DUPONT, MAI_2026);
  }

  private static RapportsDActiviteService service(List<ActiviteDElement> activites) {
    return RapportsDActiviteService.builder()
      .operateurs(REFERENTIEL)
      .fuseau(() -> ZONE_PARIS)
      .activites((operateur, debut, fin) -> activites)
      .elements(REFERENTIEL_DES_ELEMENTS)
      .postes(REFERENTIEL_DES_POSTES)
      .clock(() -> LE_MERCREDI_13_MAI_2026_A_12H);
  }

  private static JourDuRapport jourDe(RapportDActivite rapport, LocalDate date) {
    return rapport
      .jours()
      .stream()
      .filter(jour -> jour.jour().equals(date))
      .findFirst()
      .orElseThrow();
  }
}
