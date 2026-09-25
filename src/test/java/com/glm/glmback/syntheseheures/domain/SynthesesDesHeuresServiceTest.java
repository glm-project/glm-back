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
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).pointages())
      .extracting(EvenementDePresence::dateDeSurvenue)
      .containsExactly(LE_LUNDI_11_MAI_2026_A_8H, LE_LUNDI_11_MAI_2026_A_12H, LE_LUNDI_11_MAI_2026_A_13H, LE_LUNDI_11_MAI_2026_A_17H);
  }

  @Test
  void shouldSommerLesFenetresFermeesDeLaPauseDeMidiPourLaDureeDuJour() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi())));

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isEqualTo(Duration.ofHours(8));
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
      .extracting(EvenementDePresence::dateDeSurvenue)
      .containsExactly(LE_LUNDI_11_MAI_2026_A_22H);
    assertThat(jourDe(synthese, MARDI_12_MAI_2026).pointages())
      .extracting(EvenementDePresence::dateDeSurvenue)
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
      .extracting(EvenementDePresence::dateDeSurvenue)
      .containsExactly(LE_MARDI_12_MAI_2026_A_8H);
  }

  /**
   * Un pointage fautif ne bloque jamais la generation du releve : il est ignore silencieusement, absent du jour
   * qui le portait, sans que la duree n'en tienne compte.
   */
  @Test
  void shouldIgnorerLePointageFautifSansLeCompterDansLaDuree() {
    SyntheseDesHeures synthese = syntheseDeDupont(PresencesEnMemoire.avec(List.of(journeeDuMercrediAvecPauseSansArrivee())));

    JourDeSynthese mercredi = jourDe(synthese, MERCREDI_13_MAI_2026);
    assertThat(mercredi.pointages()).isEmpty();
    assertThat(mercredi.duree()).isZero();
  }

  @Test
  void shouldSommerLaDureeDesSeptJoursPourLaDureeTotale() {
    SyntheseDesHeures synthese = syntheseDeDupont(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi(), journeeDuMardiOuverteA8H()))
    );

    assertThat(synthese.dureeTotale()).isEqualTo(Duration.ofHours(8));
  }

  /**
   * E2 : lundi sans depart, lu mardi. La journee est abandonnee et fermee a sa fin presumee, la fin de l'OF 43 a
   * 16:00 : 5 h pointees, 3 h presumees.
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
    assertThat(lundi.duree()).isEqualTo(Duration.ofHours(5));
    assertThat(lundi.dureePresumee()).isEqualTo(Duration.ofHours(3));
    assertThat(synthese.dureeTotale()).isEqualTo(Duration.ofHours(5));
    assertThat(synthese.dureePresumeeTotale()).isEqualTo(Duration.ofHours(3));
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

    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).duree()).isEqualTo(Duration.ofHours(5));
    assertThat(jourDe(synthese, LUNDI_11_MAI_2026).dureePresumee()).isZero();
  }

  @Test
  void shouldNeRienPresumerDUneJourneeFermee() {
    SyntheseDesHeures synthese = service(
      PresencesEnMemoire.avec(List.of(journeeDuLundiDe8HA17HAvecPauseDeMidi())),
      (operateur, periode) -> {
        throw new AssertionError("aucun pointage ne doit etre cherche pour une journee fermee");
      },
      LE_MARDI_12_MAI_2026_A_20H
    ).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);

    assertThat(synthese.dureeTotale()).isEqualTo(Duration.ofHours(8));
    assertThat(synthese.dureePresumeeTotale()).isZero();
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

  private static SyntheseDesHeures syntheseDeDupont(PresencesEnMemoire presences) {
    return service(presences, AUCUN_POINTAGE, LE_MARDI_12_MAI_2026_A_10H).synthese(OPERATEUR_ID_DUPONT, SEMAINE_20_DE_2026);
  }

  private static SynthesesDesHeuresService service(PresencesEnMemoire presences, PointagesDAtelier pointages, Instant maintenant) {
    return SynthesesDesHeuresService.builder()
      .presences(presences)
      .operateurs(REFERENTIEL)
      .fuseau(A_PARIS)
      .seuil(() -> AMPLITUDE_MAXIMALE_13H)
      .pointages(pointages)
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
