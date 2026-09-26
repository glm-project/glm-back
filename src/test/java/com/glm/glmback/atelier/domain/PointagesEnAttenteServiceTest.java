package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static com.glm.glmback.shared.pagination.domain.PaginationFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class PointagesEnAttenteServiceTest {

  private static final UUID EVENEMENT = UUID.fromString("7a1b2c3d-4e5f-4061-8192-a3b4c5d6e7f8");
  private static final GesteDePresence PAUSE_DE_DUPONT = new GesteDePresence(
    OPERATEUR_ID_DUPONT,
    TypeDEvenementDePresence.PAUSE,
    Optional.of(LE_10_MAI_2026_A_12H)
  );
  private static final GesteDePresence DEPART_DE_DUPONT = new GesteDePresence(
    OPERATEUR_ID_DUPONT,
    TypeDEvenementDePresence.DEPART,
    Optional.empty()
  );
  private static final MotifDEcart MOTIF_DOUBLON = new MotifDEcart("Doublon d'un pointage deja saisi");

  private final AtomicReference<Instant> maintenant = new AtomicReference<>(LE_10_MAI_2026_A_13H);
  private final PointagesEnAttenteEnMemoire pointages = new PointagesEnAttenteEnMemoire();
  private final PointagesEnAttenteService service = new PointagesEnAttenteService(pointages, maintenant::get);

  @Test
  void shouldRendreLEnregistrementSansRienMettreEnAttente() {
    Recueil<String> recueil = service.recueille(new GesteRecu(EVENEMENT, PAUSE_DE_DUPONT, AUTEUR_DUPONT), () -> "journee");

    assertThat(recueil).isEqualTo(new Recueil.Enregistre<>("journee"));
    assertThat(pointages.tous()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("refusDeRattachement")
  void shouldMettreEnAttenteUnGesteQuOnNeSaitRattacher(RuntimeException refus, MotifDeMiseEnAttente motif) {
    Recueil<String> recueil = service.recueille(new GesteRecu(EVENEMENT, PAUSE_DE_DUPONT, AUTEUR_DUPONT), refuse(refus));

    PointageEnAttente enAttente = ((Recueil.MisEnAttente<String>) recueil).pointage();
    assertThat(pointages.tous()).containsExactly(enAttente);
    assertThat(enAttente.evenementDuPupitre()).isEqualTo(EVENEMENT);
    assertThat(enAttente.geste()).isEqualTo(PAUSE_DE_DUPONT);
    assertThat(enAttente.motif()).isEqualTo(motif);
    assertThat(enAttente.auteur()).isEqualTo(AUTEUR_DUPONT);
    assertThat(enAttente.dateDeReception()).isEqualTo(LE_10_MAI_2026_A_13H);
    assertThat(enAttente.traitement()).isEmpty();
  }

  private static Stream<Arguments> refusDeRattachement() {
    SuiviDAtelierId suivi = SuiviDAtelierId.newId();

    return Stream.of(
      Arguments.of(new OperateurDAtelierIntrouvableException(OPERATEUR_ID_DUPONT), MotifDeMiseEnAttente.OPERATEUR_INCONNU),
      Arguments.of(new PosteDAtelierIntrouvableException(POSTE_ID_FRAISEUSE_1), MotifDeMiseEnAttente.POSTE_INCONNU),
      Arguments.of(new SuiviDAtelierIntrouvableException(suivi), MotifDeMiseEnAttente.ELEMENT_INCONNU),
      Arguments.of(
        new TransitionDePresenceInterditeException(pauseDeDupontA(LE_10_MAI_2026_A_12H), EtatDePresence.EN_PAUSE),
        MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE
      ),
      Arguments.of(
        new TransitionDAtelierInterditeException(finSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_12H), EtatDActivite.ABSENTE),
        MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE
      ),
      Arguments.of(new AucuneJourneeDeTravailEnCoursException(OPERATEUR_ID_DUPONT), MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE),
      Arguments.of(new IdentifiantDEvenementReutiliseException(EVENEMENT), MotifDeMiseEnAttente.IDENTIFIANT_REUTILISE)
    );
  }

  @ParameterizedTest
  @MethodSource("refusQuiRemontent")
  void shouldLaisserRemonterLesAutresRefus(RuntimeException refus) {
    GesteRecu recu = new GesteRecu(EVENEMENT, PAUSE_DE_DUPONT, AUTEUR_DUPONT);

    assertThatThrownBy(() -> service.recueille(recu, refuse(refus))).isSameAs(refus);
    assertThat(pointages.tous()).isEmpty();
  }

  private static Stream<RuntimeException> refusQuiRemontent() {
    return Stream.of(
      new SuiviDAtelierClotureException(SuiviDAtelierId.newId()),
      new SaisieConcurrenteException(SuiviDAtelierId.newId()),
      new IllegalStateException("defaut technique")
    );
  }

  /**
   * Un pupitre qui rejoue le meme contenu sous un identifiant deja pris ne cree pas un second pointage en attente.
   */
  @Test
  void shouldNePasDoublerUnIdentifiantReutiliseRejoue() {
    GesteRecu recu = new GesteRecu(EVENEMENT, PAUSE_DE_DUPONT, AUTEUR_DUPONT);
    Recueil<String> premier = service.recueille(recu, refuse(new IdentifiantDEvenementReutiliseException(EVENEMENT)));
    maintenant.set(LE_10_MAI_2026_A_17H);

    Recueil<String> rejeu = service.recueille(recu, refuse(new IdentifiantDEvenementReutiliseException(EVENEMENT)));

    assertThat(rejeu).isEqualTo(premier);
    assertThat(pointages.tous()).hasSize(1);
  }

  @Test
  void shouldMettreEnAttenteUnAutreContenuSousLeMemeIdentifiantReutilise() {
    service.recueille(
      new GesteRecu(EVENEMENT, PAUSE_DE_DUPONT, AUTEUR_DUPONT),
      refuse(new IdentifiantDEvenementReutiliseException(EVENEMENT))
    );

    service.recueille(
      new GesteRecu(EVENEMENT, DEPART_DE_DUPONT, AUTEUR_DUPONT),
      refuse(new IdentifiantDEvenementReutiliseException(EVENEMENT))
    );

    assertThat(pointages.tous()).extracting(PointageEnAttente::geste).containsExactlyInAnyOrder(PAUSE_DE_DUPONT, DEPART_DE_DUPONT);
  }

  @Test
  void shouldMettreEnAttenteDeNouveauUnIdentifiantReutiliseDejaTraite() {
    PointageEnAttente premier = enAttente(
      service.recueille(
        new GesteRecu(EVENEMENT, PAUSE_DE_DUPONT, AUTEUR_DUPONT),
        refuse(new IdentifiantDEvenementReutiliseException(EVENEMENT))
      )
    );
    service.ecarte(premier.id(), AUTEUR_LEROY, MOTIF_DOUBLON);

    service.recueille(
      new GesteRecu(EVENEMENT, PAUSE_DE_DUPONT, AUTEUR_DUPONT),
      refuse(new IdentifiantDEvenementReutiliseException(EVENEMENT))
    );

    assertThat(pointages.tous()).hasSize(2);
  }

  @Test
  void shouldListerLesPointagesNonTraitesLePlusRecentDAbord() {
    PointageEnAttente matin = recuA(LE_10_MAI_2026_A_8H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.OPERATEUR_INCONNU);
    PointageEnAttente soir = recuA(LE_10_MAI_2026_A_17H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE);
    PointageEnAttente ecarte = recuA(LE_10_MAI_2026_A_12H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.OPERATEUR_INCONNU);
    service.ecarte(ecarte.id(), AUTEUR_LEROY, MOTIF_DOUBLON);

    assertThat(service.list(Optional.empty(), Optional.empty(), firstPageOfTen()).content()).containsExactly(soir, matin);
  }

  @Test
  void shouldFiltrerParOperateurEtParMotif() {
    PointageEnAttente deMartin = recuA(LE_10_MAI_2026_A_8H, OPERATEUR_ID_MARTIN, MotifDeMiseEnAttente.POSTE_INCONNU);
    recuA(LE_10_MAI_2026_A_9H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.OPERATEUR_INCONNU);

    assertThat(service.list(Optional.of(OPERATEUR_ID_MARTIN), Optional.empty(), firstPageOfTen()).content()).containsExactly(deMartin);
    assertThat(service.list(Optional.empty(), Optional.of(MotifDeMiseEnAttente.POSTE_INCONNU), firstPageOfTen()).content()).containsExactly(
      deMartin
    );
  }

  @Test
  void shouldNeRetenirQueLesPointagesNonTraitesCorrespondants() {
    PointageEnAttente ouvert = recuA(LE_10_MAI_2026_A_8H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.OPERATEUR_INCONNU);
    CriteresDePointageEnAttente tous = new CriteresDePointageEnAttente(Optional.empty(), Optional.empty());

    assertThat(tous.matches(ouvert)).isTrue();
    assertThat(tous.matches(ouvert.traite(new Application(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15)))).isFalse();
    assertThat(
      new CriteresDePointageEnAttente(Optional.of(OPERATEUR_ID_DUPONT), Optional.of(MotifDeMiseEnAttente.OPERATEUR_INCONNU)).matches(ouvert)
    ).isTrue();
    assertThat(new CriteresDePointageEnAttente(Optional.of(OPERATEUR_ID_MARTIN), Optional.empty()).matches(ouvert)).isFalse();
    assertThat(
      new CriteresDePointageEnAttente(Optional.empty(), Optional.of(MotifDeMiseEnAttente.POSTE_INCONNU)).matches(ouvert)
    ).isFalse();
    assertThatThrownBy(() -> new CriteresDePointageEnAttente(null, Optional.empty())).isExactlyInstanceOf(
      com.glm.glmback.shared.error.domain.MissingMandatoryValueException.class
    );
    assertThatThrownBy(() -> new CriteresDePointageEnAttente(Optional.empty(), null)).isExactlyInstanceOf(
      com.glm.glmback.shared.error.domain.MissingMandatoryValueException.class
    );
  }

  @Test
  void shouldAppliquerUnPointageEnAttente() {
    PointageEnAttente recu = recuA(LE_10_MAI_2026_A_8H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE);
    AtomicReference<PointageEnAttente> applique = new AtomicReference<>();
    maintenant.set(LE_11_MAI_2026_A_9H15);

    PointageEnAttente traite = service.applique(recu.id(), AUTEUR_LEROY, applique::set);

    assertThat(applique.get()).isEqualTo(recu);
    assertThat(traite.traitement()).contains(new Application(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15));
    assertThat(pointages.get(recu.id())).contains(traite);
  }

  /**
   * Une application refusee — l'operateur est toujours inconnu — laisse le pointage dans la liste.
   */
  @Test
  void shouldLaisserEnAttenteUnPointageDontLApplicationEstRefusee() {
    PointageEnAttente recu = recuA(LE_10_MAI_2026_A_8H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.OPERATEUR_INCONNU);
    OperateurDAtelierIntrouvableException refus = new OperateurDAtelierIntrouvableException(OPERATEUR_ID_DUPONT);

    assertThatThrownBy(() ->
      service.applique(recu.id(), AUTEUR_LEROY, pointage -> {
        throw refus;
      })
    ).isSameAs(refus);
    assertThat(pointages.get(recu.id())).contains(recu);
  }

  @Test
  void shouldNeJamaisAppliquerUnPointageDejaTraite() {
    PointageEnAttente recu = recuA(LE_10_MAI_2026_A_8H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.GESTE_HORS_SEQUENCE);
    service.ecarte(recu.id(), AUTEUR_LEROY, MOTIF_DOUBLON);
    AtomicReference<PointageEnAttente> applique = new AtomicReference<>();

    assertThatThrownBy(() -> service.applique(recu.id(), AUTEUR_LEROY, applique::set)).isExactlyInstanceOf(
      PointageEnAttenteDejaTraiteException.class
    );
    assertThat(applique.get()).isNull();
  }

  @Test
  void shouldEcarterUnPointageEnAttente() {
    PointageEnAttente recu = recuA(LE_10_MAI_2026_A_8H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.OPERATEUR_INCONNU);
    maintenant.set(LE_11_MAI_2026_A_9H15);

    PointageEnAttente ecarte = service.ecarte(recu.id(), AUTEUR_LEROY, MOTIF_DOUBLON);

    assertThat(ecarte.traitement()).contains(new Ecart(AUTEUR_LEROY, LE_11_MAI_2026_A_9H15, MOTIF_DOUBLON));
    assertThat(pointages.get(recu.id())).contains(ecarte);
  }

  @Test
  void shouldNePasEcarterDeuxFois() {
    PointageEnAttente recu = recuA(LE_10_MAI_2026_A_8H, OPERATEUR_ID_DUPONT, MotifDeMiseEnAttente.OPERATEUR_INCONNU);
    service.applique(recu.id(), AUTEUR_LEROY, pointage -> {});

    assertThatThrownBy(() -> service.ecarte(recu.id(), AUTEUR_LEROY, MOTIF_DOUBLON)).isExactlyInstanceOf(
      PointageEnAttenteDejaTraiteException.class
    );
  }

  @Test
  void shouldNeTraiterAucunPointageInconnu() {
    PointageEnAttenteId inconnu = PointageEnAttenteId.newId();

    assertThatThrownBy(() -> service.applique(inconnu, AUTEUR_LEROY, pointage -> {}))
      .isExactlyInstanceOf(PointageEnAttenteIntrouvableException.class)
      .hasMessageContaining(inconnu.uuid().toString());
    assertThatThrownBy(() -> service.ecarte(inconnu, AUTEUR_LEROY, MOTIF_DOUBLON)).isExactlyInstanceOf(
      PointageEnAttenteIntrouvableException.class
    );
  }

  private PointageEnAttente recuA(Instant reception, OperateurId operateur, MotifDeMiseEnAttente motif) {
    return pointages.create(
      PointageEnAttente.builder()
        .id(PointageEnAttenteId.newId())
        .evenementDuPupitre(UUID.randomUUID())
        .geste(new GesteDePresence(operateur, TypeDEvenementDePresence.PAUSE, Optional.empty()))
        .motif(motif)
        .auteur(AUTEUR_DUPONT)
        .dateDeReception(reception)
        .traitement(Optional.empty())
    );
  }

  private static PointageEnAttente enAttente(Recueil<String> recueil) {
    return ((Recueil.MisEnAttente<String>) recueil).pointage();
  }

  private static Supplier<String> refuse(RuntimeException refus) {
    return () -> {
      throw refus;
    };
  }
}
