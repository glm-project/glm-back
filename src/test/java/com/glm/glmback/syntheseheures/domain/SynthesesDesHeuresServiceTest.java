package com.glm.glmback.syntheseheures.domain;

import static com.glm.glmback.syntheseheures.domain.SyntheseHeuresFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

@UnitTest
class SynthesesDesHeuresServiceTest {

  private static final OperateursConnus REFERENTIEL = id ->
    Optional.of(OPERATEUR_CONNU_DUPONT).filter(operateur -> operateur.id().equals(id));
  private static final FuseauHoraireDeLEntreprise A_PARIS = () -> ZONE_PARIS;
  private static final PointagesDAtelier AUCUN_POINTAGE = (operateur, periode) -> Optional.empty();
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
    SynthesesDesHeuresService service = service(PresencesEnMemoire.sansJournee(), AUCUN_POINTAGE, LE_MARDI_12_MAI_2026_A_10H);

    assertThatThrownBy(() -> service.synthese(OPERATEUR_ID_MARTIN, SEMAINE_20_DE_2026))
      .isExactlyInstanceOf(OperateurInconnuException.class)
      .hasMessageContaining(OPERATEUR_ID_MARTIN.uuid().toString());
  }

  @Test
  void shouldPorterLIdentiteRelueEtLaSemaineDemandee() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.sansJournee());

    assertThat(synthese.operateur()).isEqualTo(OPERATEUR_CONNU_DUPONT);
    assertThat(synthese.semaine()).isEqualTo(SEMAINE_20_DE_2026);
  }

  /**
   * Sept jours toujours, meme vides : un trou dans la liste obligerait le lecteur a deviner s'il manque une journee
   * ou si l'operateur n'etait pas la.
   */
  @Test
  void shouldRendreLesSeptJoursDeLaSemaineSansPresence() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.sansJournee());

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
      assertThat(jour.duree()).isZero();
    });
  }

  @Test
  void shouldDemanderLesJourneesRecouvrantLaSemaineDansLaZoneDeLEntreprise() {
    PresencesEnMemoire presences = PresencesEnMemoire.sansJournee();

    syntheseDeDupont(presences);

    assertThat(presences.debutDemande()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
    assertThat(presences.finExclusiveDemandee()).isEqualTo(Instant.parse("2026-05-17T22:00:00Z"));
  }

  @Test
  void shouldIgnorerUneJourneeHorsDeLaSemaine() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuDimanchePrecedentDe8HA17H())));

    assertThat(synthese.jours()).allSatisfy(jour -> {
      assertThat(jour.pointages()).isEmpty();
      assertThat(jour.duree()).isZero();
    });
  }

  @Test
  void shouldPorterLesPointagesDuJourTriesParHeure() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages())
      .extracting(PointageDuJour::dateDeSurvenue)
      .containsExactly(LE_LUNDI_11_MAI_2026_A_8H, LE_LUNDI_11_MAI_2026_A_17H);
  }

  /**
   * La pause de midi n'est pas un pointage de presence : la duree du jour court de l'arrivee au depart.
   */
  @Test
  void shouldSommerLesFenetresFermeesPourLaDureeDuJour() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isEqualTo(Duration.ofHours(9));
  }

  /**
   * Une equipe de nuit compte sur deux jours : la duree se repartit exactement comme les pointages, de part et
   * d'autre de minuit.
   */
  @Test
  void shouldRepartirLaDureeDUneJourneeAChevalSurMinuitSurSesDeuxJours() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundi22HAuMardi2H())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isEqualTo(Duration.ofHours(2));
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).duree()).isEqualTo(Duration.ofHours(2));
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages())
      .extracting(PointageDuJour::dateDeSurvenue)
      .containsExactly(LE_LUNDI_11_MAI_2026_A_22H);
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).pointages())
      .extracting(PointageDuJour::dateDeSurvenue)
      .containsExactly(LE_MARDI_12_MAI_2026_A_2H);
  }

  /**
   * Sans depart pointe, aucune horloge dans ce contexte ne peut dire combien de temps s'est ecoule : la duree du
   * jour reste nulle, meme si l'arrivee est bien visible dans le releve.
   */
  @Test
  void shouldNotCompterDeDureePourUneFenetreEncoreOuverte() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuMardiOuverteA8H())));

    assertThat(jourDe(synthese, MARDI_12_MAI_2026).duree()).isZero();
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).pointages())
      .extracting(PointageDuJour::dateDeSurvenue)
      .containsExactly(LE_MARDI_12_MAI_2026_A_8H);
  }

  /**
   * Un pointage fautif ne bloque jamais la generation du releve : il est ignore silencieusement, absent du jour
   * qui le portait, sans que la duree n'en tienne compte.
   */
  @Test
  void shouldIgnorerLePointageFautifSansLeCompterDansLaDuree() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuMercrediAvecDepartSansArrivee())));

    JourDeSynthese mercredi = jourDe(synthese, MERCREDI_13_MAI_2026);
    assertThat(mercredi.pointages()).isEmpty();
    assertThat(mercredi.duree()).isZero();
  }

  @Test
  void shouldSommerLaDureeDesSeptJoursPourLaDureeTotale() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H(), journeeDuMardiOuverteA8H())));

    assertThat(synthese.dureeTotale()).isEqualTo(Duration.ofHours(9));
  }

  /**
   * E2 : lundi sans depart, lu mardi. La journee est abandonnee et fermee a sa fin presumee, la fin de l'OF 43 a
   * 16:00. Sans depart, elle n'a qu'une fenetre : ses 9 h sont presumees, aucune n'est pointee.
   */
  @Test
  void shouldSeparerLesHeuresPointeesDesHeuresPresumees() {
    AtomicReference<Plage> recherche = new AtomicReference<>();
    PointagesDAtelier finDeLOf43 = (operateur, periode) -> {
      recherche.set(periode);
      return Optional.of(LE_LUNDI_11_MAI_2026_A_16H);
    };

    SyntheseDesHeures synthese = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe7HSansDepart())),
      finDeLOf43,
      LE_MARDI_12_MAI_2026_A_10H
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    JourDeSynthese lundi = jourDe(synthese, LUNDI_11_MAI_2026);
    assertThat(lundi.duree()).isZero();
    assertThat(lundi.dureePresumee()).isEqualTo(Duration.ofHours(9));
    assertThat(synthese.dureeTotale()).isZero();
    assertThat(synthese.dureePresumeeTotale()).isEqualTo(Duration.ofHours(9));
    assertThat(recherche.get()).isEqualTo(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_20H)));
  }

  /**
   * E3 : le poste de nuit oublie ne laisse que son arrivee et un debut d'OF cinq minutes plus tard.
   */
  @Test
  void shouldPresumerCinqMinutesAUnPosteDeNuitOublie() {
    JourneeDeTravail nuit = new JourneeDeTravail(List.of(arriveeA(LE_LUNDI_11_MAI_2026_A_20H)));

    SyntheseDesHeures synthese = service(
      PresencesEnMemoire.avec(List.of(nuit)),
      (operateur, periode) -> Optional.of(LE_LUNDI_11_MAI_2026_A_20H05),
      LE_MARDI_12_MAI_2026_A_20H
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isZero();
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureePresumee()).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void shouldNeRienPresumerDUneJourneeEncoreSousLeSeuil() {
    SyntheseDesHeures synthese = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe7HSansDepart())),
      (operateur, periode) -> {
        throw new AssertionError("aucun pointage ne doit etre cherche pour une journee en cours");
      },
      LE_LUNDI_11_MAI_2026_A_20H
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isZero();
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureePresumee()).isZero();
  }

  @Test
  void shouldNeRienPresumerDUneJourneeFermee() {
    SyntheseDesHeures synthese = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      (operateur, periode) -> {
        throw new AssertionError("aucun pointage ne doit etre cherche pour une journee fermee");
      },
      LE_MARDI_12_MAI_2026_A_20H
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(synthese.dureeTotale()).isEqualTo(Duration.ofHours(9));
    assertThat(synthese.dureePresumeeTotale()).isZero();
  }

  /**
   * Issue #59 : une journee fermee de plus de 24 h n'est comptee que jusqu'a sa fin presumee, la fin de l'OF 43 a
   * 16:00 : 9 h presumees lundi, rien mardi ni mercredi.
   */
  @Test
  void shouldBornerASaFinPresumeeUneJourneeFermeeDePlusDe24H() {
    AtomicReference<Plage> recherche = new AtomicReference<>();
    PointagesDAtelier finDeLOf43 = (operateur, periode) -> {
      recherche.set(periode);
      return Optional.of(LE_LUNDI_11_MAI_2026_A_16H);
    };

    SyntheseDesHeures synthese = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundi7HAuMercredi12H())),
      finDeLOf43,
      LE_MERCREDI_13_MAI_2026_A_12H
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isZero();
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureePresumee()).isEqualTo(Duration.ofHours(9));
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).duree()).isZero();
    assertThat(jourDe(synthese, MERCREDI_13_MAI_2026).duree()).isZero();
    assertThat(synthese.dureeTotale()).isZero();
    assertThat(synthese.dureePresumeeTotale()).isEqualTo(Duration.ofHours(9));
    assertThat(recherche.get()).isEqualTo(new Plage(LE_LUNDI_11_MAI_2026_A_7H, Optional.of(LE_LUNDI_11_MAI_2026_A_20H)));
  }

  /**
   * E7 : le poste de nuit du dimanche au lundi se partage entre deux semaines de releve, a minuit a Paris.
   */
  @Test
  void shouldDecouperUnPosteDeNuitSurDeuxSemaines() {
    PresencesEnMemoire presences = PresencesEnMemoire.avec(List.of(journeeDuDimanche20HAuLundi8H()));

    SyntheseDesHeures semaine19 = service(presences, AUCUN_POINTAGE, LE_MARDI_12_MAI_2026_A_10H).synthese(
      OPERATEUR_ID_DUPONT,
      SEMAINE_19_DE_2026
    );
    SyntheseDesHeures semaine20 = service(presences, AUCUN_POINTAGE, LE_MARDI_12_MAI_2026_A_10H).synthese(
      OPERATEUR_ID_DUPONT,
      SEMAINE_20_DE_2026
    );

    assertThat(jourDe(semaine19, DIMANCHE_10_MAI_2026).duree()).isEqualTo(Duration.ofHours(4));
    assertThat(semaine19.dureeTotale()).isEqualTo(Duration.ofHours(4));
    assertThat(jourDe(semaine20, LUNDI_11_MAI_2026).duree()).isEqualTo(Duration.ofHours(8));
    assertThat(semaine20.dureeTotale()).isEqualTo(Duration.ofHours(8));
  }

  @Test
  void shouldDemanderLeTravailDepuisLeLundiQuandAucuneJourneeNeLePrecede() {
    TravailEnMemoire travail = TravailEnMemoire.sansSuivi();

    syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())), travail);

    assertThat(travail.depuisDemande()).isEqualTo(Instant.parse("2026-05-10T22:00:00Z"));
    assertThat(travail.finExclusiveDemandee()).isEqualTo(Instant.parse("2026-05-17T22:00:00Z"));
  }

  @Test
  void shouldDemanderLeTravailDepuisLaPremiereArriveeQuandElleEstAnterieureAuLundi() {
    TravailEnMemoire travail = TravailEnMemoire.sansSuivi();

    syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H(), journeeDuDimanche20HAuLundi8H())), travail);

    assertThat(travail.depuisDemande()).isEqualTo(LE_DIMANCHE_10_MAI_2026_A_20H);
  }

  @Test
  void shouldNeRendreNiTempsOperationnelNiElementSansTravail() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())));

    assertThat(synthese.elements()).isEmpty();
    assertThat(synthese.dureeOperationnelleTotale()).isZero();
  }

  /**
   * Une fin a midi coupe le travail, un debut le relance : la coupure ne compte pas, et le depart arrete ce que
   * personne n'a arrete.
   */
  @Test
  void shouldCompterLeTravailDuJourSansLaCoupure() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(
          suiviDuCarter(
            debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H),
            finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H),
            debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_13H)
          )
        )
      )
    );

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureeOperationnelle()).isEqualTo(Duration.ofHours(8));
    assertThat(synthese.dureeOperationnelleTotale()).isEqualTo(Duration.ofHours(8));
  }

  @Test
  void shouldCompterLaNonConformiteDansLaDureeDeLElement() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(
          suiviDuCarter(
            debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H),
            nonConformiteSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_10H),
            finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_11H)
          )
        )
      )
    );

    assertThat(synthese.elements())
      .singleElement()
      .satisfies(element -> {
        assertThat(element.duree()).isEqualTo(Duration.ofHours(3));
        assertThat(element.dureeNonConformite()).isEqualTo(Duration.ofHours(1));
        assertThat(element.dureePresumee()).isZero();
      });
  }

  /**
   * D2 : une heure passee sur deux elements compte sur chacun, et la semaine en est la somme.
   */
  @Test
  void shouldCumulerParElementDeuxElementsTravaillesEnMemeTemps() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H)), suiviDeLaBride(debutAuTourA(LE_LUNDI_11_MAI_2026_A_8H)))
      )
    );

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureeOperationnelle()).isEqualTo(Duration.ofHours(18));
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isEqualTo(Duration.ofHours(9));
    assertThat(synthese.elements()).extracting(ElementDeLaSynthese::duree).containsExactly(Duration.ofHours(9), Duration.ofHours(9));
  }

  @Test
  void shouldRendreLaSommeDesElementsEgaleAuTotalDeLaSemaine() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H(), journeeDuLundi22HAuMardi2H())),
      TravailEnMemoire.avec(
        List.of(
          suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H), finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H)),
          suiviDeLaBride(debutAuTourA(LE_LUNDI_11_MAI_2026_A_10H), debutAuTourA(LE_LUNDI_11_MAI_2026_A_23H))
        )
      )
    );

    assertThat(synthese.elements().stream().map(ElementDeLaSynthese::duree).reduce(Duration.ZERO, Duration::plus)).isEqualTo(
      synthese.dureeOperationnelleTotale()
    );
  }

  @Test
  void shouldRepartirSurDeuxJoursLeTravailDUnPosteDeNuit() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundi22HAuMardi2H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_23H), finSurLaDmu50A(LE_MARDI_12_MAI_2026_A_1H))))
    );

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureeOperationnelle()).isEqualTo(Duration.ofHours(1));
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).dureeOperationnelle()).isEqualTo(Duration.ofHours(1));
  }

  @Test
  void shouldPresumerLeTravailDUneJourneeAbandonnee() {
    SyntheseDesHeures synthese = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe7HSansDepart())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H)))),
      (operateur, periode) -> Optional.of(LE_LUNDI_11_MAI_2026_A_16H),
      LE_MARDI_12_MAI_2026_A_10H
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureeOperationnelle()).isZero();
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureeOperationnellePresumee()).isEqualTo(Duration.ofHours(8));
    assertThat(synthese.dureeOperationnellePresumeeTotale()).isEqualTo(Duration.ofHours(8));
    assertThat(synthese.elements())
      .singleElement()
      .satisfies(element -> {
        assertThat(element.duree()).isZero();
        assertThat(element.dureePresumee()).isEqualTo(Duration.ofHours(8));
      });
  }

  @Test
  void shouldNeRienCompterDUnTravailEnCours() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuMardiOuverteA8H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_MARDI_12_MAI_2026_A_9H))))
    );

    assertThat(jourDe(synthese, MARDI_12_MAI_2026).dureeOperationnelle()).isZero();
    assertThat(synthese.elements()).singleElement().extracting(ElementDeLaSynthese::duree).isEqualTo(Duration.ZERO);
  }

  /**
   * Un debut hors de toute journee ne compte pas, mais il reste au journal brut, et son element reste rendu.
   */
  @Test
  void shouldGarderAuJournalUnPointageHorsDeTouteJourneeSansLeCompter() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_18H))))
    );

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureeOperationnelle()).isZero();
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages())
      .last()
      .isEqualTo(pointageDuCarter(TypeDEvenementDAtelier.DEBUT, LE_LUNDI_11_MAI_2026_A_18H));
    assertThat(synthese.elements())
      .extracting(element -> element.element().id())
      .containsExactly(ELEMENT_ID_CARTER);
  }

  /**
   * A instant egal : l'arrivee, puis les pointages d'element, puis le depart.
   */
  @Test
  void shouldOrdonnerLeJournalDuJourAInstantEgal() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H), finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_17H))))
    );

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages()).containsExactly(
      arriveeA(LE_LUNDI_11_MAI_2026_A_8H),
      pointageDuCarter(TypeDEvenementDAtelier.DEBUT, LE_LUNDI_11_MAI_2026_A_8H),
      pointageDuCarter(TypeDEvenementDAtelier.FIN, LE_LUNDI_11_MAI_2026_A_17H),
      departA(LE_LUNDI_11_MAI_2026_A_17H)
    );
  }

  /**
   * Deux elements arretes au meme instant, a la pause : l'element departage, quel que soit l'ordre des suivis.
   */
  @Test
  void shouldDepartagerParLElementDeuxPointagesSimultanes() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(suiviDeLaBride(debutAuTourA(LE_LUNDI_11_MAI_2026_A_10H)), suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_10H)))
      )
    );

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages())
      .filteredOn(PointageDElement.class::isInstance)
      .extracting(pointage -> ((PointageDElement) pointage).element())
      .containsExactly(ELEMENT_ID_CARTER, ELEMENT_ID_BRIDE);
  }

  @Test
  void shouldIgnorerAuJournalUnPointageHorsDeLaSemaine() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(finSurLaDmu50A(LE_DIMANCHE_10_MAI_2026_A_20H))))
    );

    assertThat(synthese.jours()).allSatisfy(jour -> assertThat(jour.pointages()).noneMatch(PointageDElement.class::isInstance));
    assertThat(synthese.elements()).isEmpty();
  }

  /**
   * Les elements suivent leur premiere apparition, premier travail ou premier pointage, puis leur nom.
   */
  @Test
  void shouldOrdonnerLesElementsParPremiereApparitionPuisParNom() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(
          suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_9H)),
          suiviDeLaBride(finAuTourA(LE_LUNDI_11_MAI_2026_A_8H), debutAuTourA(LE_LUNDI_11_MAI_2026_A_10H))
        )
      )
    );

    assertThat(synthese.elements())
      .extracting(element -> element.element().id())
      .containsExactly(ELEMENT_ID_BRIDE, ELEMENT_ID_CARTER);
  }

  @Test
  void shouldDepartagerParLeNomDeuxElementsApparusEnsemble() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_9H)), suiviDeLaBride(debutAuTourA(LE_LUNDI_11_MAI_2026_A_9H)))
      )
    );

    assertThat(synthese.elements())
      .extracting(element -> element.element().nom())
      .containsExactly(NOM_OF_2026_000007, NOM_PRD_2026_000015);
  }

  @Test
  void shouldRendreUnSeulElementPourUnReengagementApresCloture() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(
          new SuiviDuTravail(
            ELEMENT_ENGAGE_CARTER,
            new JournalDAtelier(List.of(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H))),
            Optional.of(LE_LUNDI_11_MAI_2026_A_10H)
          ),
          suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_11H), finSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_12H))
        )
      )
    );

    assertThat(synthese.elements()).singleElement().extracting(ElementDeLaSynthese::duree).isEqualTo(Duration.ofHours(3));
  }

  @Test
  void shouldRelireLaFicheDeLElementAuReferentiel() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H)), suiviDeLaBride(debutAuTourA(LE_LUNDI_11_MAI_2026_A_9H)))
      )
    );

    assertThat(synthese.elements().getFirst().element()).isEqualTo(ELEMENT_ENGAGE_CARTER);
    assertThat(synthese.elements().getFirst().reference()).contains(REFERENCE_1015);
    assertThat(synthese.elements().getFirst().description()).contains(DESCRIPTION_CARTER_DE_POMPE);
    assertThat(synthese.elements().getLast().reference()).isEmpty();
    assertThat(synthese.elements().getLast().description()).isEmpty();
  }

  /**
   * Un couple par poste et nature distincts, dans l'ordre de premiere apparition ; le travail sans poste n'en donne
   * aucun, ni un poste que le referentiel ne connait pas.
   */
  @Test
  void shouldRendreLesPostesDeLElementDansLOrdreDePremiereApparition() {
    PointageDAtelier surUnPosteInconnu = PointageDAtelier.builder()
      .type(TypeDEvenementDAtelier.DEBUT)
      .poste(Optional.of(new PosteDeTravailId(java.util.UUID.fromString("99999999-9999-9999-9999-999999999999"))))
      .nature(Optional.of(NATURE_FRAISAGE))
      .dateDeSurvenue(LE_LUNDI_11_MAI_2026_A_12H);

    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(
        List.of(
          suiviDuCarter(
            debutAuTourA(LE_LUNDI_11_MAI_2026_A_8H),
            debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_9H),
            debutAuTourA(LE_LUNDI_11_MAI_2026_A_10H),
            debutSansPosteA(LE_LUNDI_11_MAI_2026_A_11H),
            surUnPosteInconnu
          )
        )
      )
    );

    assertThat(synthese.elements().getFirst().postes()).containsExactly(
      new PosteDeLElement(POSTE_CONNU_TOUR_14, Optional.of(NATURE_TOURNAGE)),
      new PosteDeLElement(POSTE_CONNU_DMU_50, Optional.of(NATURE_FRAISAGE))
    );
  }

  /**
   * Un pointage qui ne laisse aucun travail, hors de toute journee, nomme pourtant son poste au journal : le releve
   * doit en porter le libelle.
   */
  @Test
  void shouldRendreLePosteDUnPointageQuiNeLaisseAucunTravail() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17H())),
      TravailEnMemoire.avec(List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_9H), debutAuTourA(LE_LUNDI_11_MAI_2026_A_18H))))
    );

    assertThat(synthese.elements().getFirst().postes()).containsExactly(
      new PosteDeLElement(POSTE_CONNU_DMU_50, Optional.of(NATURE_FRAISAGE)),
      new PosteDeLElement(POSTE_CONNU_TOUR_14, Optional.of(NATURE_TOURNAGE))
    );
  }

  @Test
  void shouldRendreLaSommeDesDureesPresumeesDesElementsEgaleAuTotalPresumeDeLaSemaine() {
    SyntheseDesHeures synthese = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe7HSansDepart())),
      TravailEnMemoire.avec(
        List.of(suiviDuCarter(debutSurLaDmu50A(LE_LUNDI_11_MAI_2026_A_8H)), suiviDeLaBride(debutAuTourA(LE_LUNDI_11_MAI_2026_A_10H)))
      ),
      (operateur, periode) -> Optional.of(LE_LUNDI_11_MAI_2026_A_16H),
      LE_MARDI_12_MAI_2026_A_10H
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(synthese.elements().stream().map(ElementDeLaSynthese::dureePresumee).reduce(Duration.ZERO, Duration::plus))
      .isEqualTo(synthese.dureeOperationnellePresumeeTotale())
      .isEqualTo(Duration.ofHours(14));
  }

  private static SuiviDuTravail suiviDuCarter(PointageDAtelier... pointages) {
    return new SuiviDuTravail(ELEMENT_ENGAGE_CARTER, new JournalDAtelier(List.of(pointages)), Optional.empty());
  }

  private static SuiviDuTravail suiviDeLaBride(PointageDAtelier... pointages) {
    return new SuiviDuTravail(ELEMENT_ENGAGE_BRIDE, new JournalDAtelier(List.of(pointages)), Optional.empty());
  }

  private static PointageDElement pointageDuCarter(TypeDEvenementDAtelier type, Instant date) {
    return new PointageDElement(type, ELEMENT_ID_CARTER, Optional.of(POSTE_ID_DMU_50), Optional.of(NATURE_FRAISAGE), date);
  }

  private static SyntheseDesHeures syntheseDeDupont(PresencesEnMemoire presences) {
    return syntheseDeDupont(presences, TravailEnMemoire.sansSuivi());
  }

  private static SyntheseDesHeures syntheseDeDupont(PresencesEnMemoire presences, TravailEnMemoire travail) {
    return service(presences, travail, AUCUN_POINTAGE, LE_MARDI_12_MAI_2026_A_10H).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
  }

  private static SynthesesDesHeuresService service(PresencesEnMemoire presences, PointagesDAtelier pointages, Instant maintenant) {
    return service(presences, TravailEnMemoire.sansSuivi(), pointages, maintenant);
  }

  private static SynthesesDesHeuresService service(
    PresencesEnMemoire presences,
    TravailEnMemoire travail,
    PointagesDAtelier pointages,
    Instant maintenant
  ) {
    return SynthesesDesHeuresService.builder()
      .presences(presences)
      .operateurs(REFERENTIEL)
      .fuseau(A_PARIS)
      .seuil(() -> AMPLITUDE_MAXIMALE_13H)
      .pointages(pointages)
      .travail(travail)
      .elements(REFERENTIEL_DES_ELEMENTS)
      .postes(REFERENTIEL_DES_POSTES)
      .clock(() -> maintenant);
  }

  private static JourDeSynthese jourDe(SyntheseDesHeures synthese, LocalDate jour) {
    return synthese
      .jours()
      .stream()
      .filter(jourDeSynthese -> jourDeSynthese.jour().equals(jour))
      .findFirst()
      .orElseThrow();
  }
}
