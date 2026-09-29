package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Confronte l'adapter de persistance au double en memoire sur les memes donnees.
 *
 * <p>
 * {@link SuiviDAtelierCriteria#matches} et {@link JourneeDeTravailCriteria#matches} vivent dans le domaine pour que le
 * double et l'adapter ne puissent pas diverger. L'adapter ne les appelle plus : l'etat et le debut n'etant pas
 * stockes mais projetes, il traduit les memes regles en SQL. La garantie que donnait le code partage est donc
 * retablie ici, par l'execution.
 * </p>
 *
 * <p>
 * Les dates sont toutes distinctes a dessein : le departage sur l'identifiant n'est pas comparable entre les deux
 * mondes, {@code UUID.compareTo} comparant des entiers signes la ou PostgreSQL compare des octets non signes. Les deux
 * ordres restent totaux, donc la pagination reste correcte de part et d'autre — ils ne sont simplement pas le meme
 * ordre a horodatage identique, ce que le domaine se garde deja de produire en faisant avancer l'horloge.
 * </p>
 */
@IntegrationTest
class PariteDesRepositoriesDAtelierIT {

  private static final Pageable PREMIERE_PAGE = new Pageable(0, 10);

  @Autowired
  private SuiviDAtelierRepository suivisPersistes;

  @Autowired
  private JourneeDeTravailRepository journeesPersistees;

  @Autowired
  private TransactionTemplate transactions;

  /**
   * L'etat se juge a l'instant d'evaluation des criteres : juste avant l'echeance de l'activite de mardi, puis a
   * l'echeance pile, ou elle cesse d'etre en cours sans aucune ecriture. Le jeu couvre chaque etat, y compris un suivi
   * en attente dont le seul pointage est annule, une activite terminee au-dela de son echeance par une regularisation,
   * et une sequence en conflit dont les activites a resoudre ne sont pas en cours.
   */
  @Test
  @WithTenant("impeccmold")
  void shouldRendreLesMemesSuivisQueLeDoubleEnMemoire() {
    Instant lundi = Instant.parse("2042-01-05T07:00:00Z");
    Instant mardi = Instant.parse("2042-01-06T07:00:00Z");
    Instant mercredi = Instant.parse("2042-01-07T07:00:00Z");
    Instant echeanceDeMardi = mardi.plusSeconds(3600).plus(Duration.ofHours(13));
    EvenementDAtelier annule = debutA(lundi.plusSeconds(7200));
    EvenementDAtelier termine = debutA(lundi.plusSeconds(3600 * 3));
    EvenementDAtelier prolonge = debutA(lundi.plusSeconds(3600 * 4));
    EvenementDAtelier remplacee = debutA(mardi.plusSeconds(3600 * 2));
    List<SuiviDAtelier> jeu = List.of(
      suiviEngageA(lundi),
      suiviEngageA(lundi.plusSeconds(3600))
        .enregistre(annule)
        .annule(annule.id(), new Annulation(AUTEUR_LEROY, mercredi, MOTIF_ERREUR_DE_SAISIE)),
      suiviEngageA(lundi.plusSeconds(3600 * 2)).enregistre(termine).enregistre(finDe(termine).a(lundi.plusSeconds(3600 * 5))),
      suiviEngageA(lundi.plusSeconds(3600 * 3)).enregistre(prolonge).enregistre(finRegulariseeA(prolonge, mercredi)),
      suiviEngageA(mardi).enregistre(debutA(mardi.plusSeconds(3600))),
      suiviEngageA(mardi.plusSeconds(3600))
        .enregistre(remplacee)
        .enregistre(debutA(mardi.plusSeconds(3600 * 3)))
        .enregistre(finDe(remplacee).a(mardi.plusSeconds(3600 * 4))),
      suiviEngageA(mercredi).cloture(new Cloture(AUTEUR_LEROY, Horodatage.saisiA(mercredi.plusSeconds(36000))))
    );

    SuivisDAtelierEnMemoire enMemoire = new SuivisDAtelierEnMemoire();
    jeu.forEach(suivi -> {
      enMemoire.create(suivi);
      inTransaction(() -> suivisPersistes.create(suivi));
    });

    Periode semaine = new Periode(lundi, mercredi);
    for (Instant evaluation : List.of(echeanceDeMardi.minusSeconds(1), echeanceDeMardi)) {
      for (Set<EtatDAtelier> etats : List.of(
        Set.<EtatDAtelier>of(),
        Set.of(EtatDAtelier.EN_COURS),
        Set.of(EtatDAtelier.INTERROMPU),
        Set.of(EtatDAtelier.CLOTURE, EtatDAtelier.EN_ATTENTE),
        Set.of(EtatDAtelier.EN_COURS, EtatDAtelier.INTERROMPU)
      )) {
        SuiviDAtelierCriteria criteres = new SuiviDAtelierCriteria(Optional.of(semaine), etats, evaluation);
        Page<SuiviDAtelier> attendue = enMemoire.list(criteres, PREMIERE_PAGE);
        Page<SuiviDAtelier> obtenue = inTransaction(() -> suivisPersistes.list(criteres, PREMIERE_PAGE));

        assertThat(obtenue.content()).describedAs("etats %s a %s", etats, evaluation).containsExactlyElementsOf(attendue.content());
        assertThat(obtenue.totalElementsCount()).isEqualTo(attendue.totalElementsCount());
      }
    }
    assertThat(
      enMemoire
        .list(new SuiviDAtelierCriteria(Optional.of(semaine), Set.of(EtatDAtelier.EN_COURS), echeanceDeMardi), PREMIERE_PAGE)
        .content()
    ).isEmpty();
    assertThat(
      enMemoire
        .list(
          new SuiviDAtelierCriteria(Optional.of(semaine), Set.of(EtatDAtelier.EN_COURS), echeanceDeMardi.minusSeconds(1)),
          PREMIERE_PAGE
        )
        .content()
    ).hasSize(1);
  }

  @Test
  @WithTenant("impeccmold")
  void shouldRendreLesMemesJourneesQueLeDoubleEnMemoire() {
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    Instant lundi = Instant.parse("2042-02-05T07:00:00Z");
    Instant mardi = Instant.parse("2042-02-06T07:00:00Z");
    List<JourneeDeTravail> jeu = List.of(journeeOuverteA(operateur, lundi), journeeCompleteA(operateur, mardi));

    JourneesDeTravailEnMemoire enMemoire = new JourneesDeTravailEnMemoire();
    jeu.forEach(journee -> {
      enMemoire.create(journee);
      inTransaction(() -> journeesPersistees.create(journee));
    });

    for (Optional<Periode> periode : List.of(Optional.<Periode>empty(), Optional.of(new Periode(mardi, mardi)))) {
      JourneeDeTravailCriteria criteres = new JourneeDeTravailCriteria(periode, Optional.of(operateur));
      Page<JourneeDeTravail> attendue = enMemoire.list(criteres, PREMIERE_PAGE);
      Page<JourneeDeTravail> obtenue = inTransaction(() -> journeesPersistees.list(criteres, PREMIERE_PAGE));

      assertThat(obtenue.content()).describedAs("periode %s", periode).containsExactlyElementsOf(attendue.content());
      assertThat(obtenue.totalElementsCount()).isEqualTo(attendue.totalElementsCount());
    }
  }

  @Test
  @WithTenant("impeccmold")
  void shouldTrouverLaMemeJourneeContenantUnInstantQueLeDoubleEnMemoire() {
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    Instant arrivee = Instant.parse("2042-03-05T07:00:00Z");
    JourneeDeTravail complete = journeeCompleteA(operateur, arrivee);

    JourneesDeTravailEnMemoire enMemoire = new JourneesDeTravailEnMemoire();
    enMemoire.create(complete);
    inTransaction(() -> journeesPersistees.create(complete));

    for (Instant instant : List.of(arrivee.minusSeconds(1), arrivee, arrivee.plusSeconds(18000), arrivee.plusSeconds(36001))) {
      assertThat(inTransaction(() -> journeesPersistees.journeeContenant(operateur, instant)))
        .describedAs("instant %s", instant)
        .isEqualTo(enMemoire.journeeContenant(operateur, instant));
    }
  }

  private static SuiviDAtelier suiviEngageA(Instant date) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), NOM_OF_2026_000042, TypeDElementEngage.ORDRE_DE_FABRICATION))
      .engagement(new Engagement(AUTEUR_LEROY, date))
      .journal(JournalDAtelier.vide());
  }

  private static EvenementDAtelier finRegulariseeA(EvenementDAtelier ouvrant, Instant date) {
    return EvenementDAtelier.builder()
      .id(EvenementDAtelierId.newId())
      .type(TypeDEvenementDAtelier.FIN)
      .intention(IntentionDePointage.FIN)
      .activite(Optional.empty())
      .activiteVisee(ouvrant.activite())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_FRAISEUSE_1))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DUPONT))
      .auteur(AUTEUR_LEROY)
      .origine(OrigineDuPointage.REGULARISATION)
      .horodatage(Horodatage.saisiA(date));
  }

  private static EvenementDAtelier debutA(Instant date) {
    EvenementDAtelierId id = EvenementDAtelierId.newId();

    return EvenementDAtelier.builder()
      .id(id)
      .type(TypeDEvenementDAtelier.DEBUT)
      .intention(IntentionDePointage.OUVERTURE)
      .activite(Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_FRAISEUSE_1))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DUPONT))
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .horodatage(Horodatage.saisiA(date));
  }

  /**
   * Les anomalies se jugent a l'instant de lecture, sur des colonnes projetees : l'adapter doit retenir exactement ce
   * que {@link CriteresDAnomalie#matches} retient, bornes du seuil comprises.
   */
  @Test
  @WithTenant("impeccmold")
  void shouldRendreLesMemesAnomaliesQueLeDoubleEnMemoire() {
    OperateurId operateur = new OperateurId(UUID.randomUUID());
    Instant lundi = Instant.parse("2042-04-07T07:00:00Z");
    Instant samedi = Instant.parse("2042-04-12T07:00:00Z");
    Instant maintenant = samedi.plus(Duration.ofHours(13));
    List<JourneeDeTravail> jeu = List.of(
      journeeOuverteA(operateur, lundi),
      journeeFermeeApres(operateur, Instant.parse("2042-04-08T07:00:00Z"), Duration.ofHours(10)),
      journeeFermeeApres(operateur, Instant.parse("2042-04-09T07:00:00Z"), Duration.ofHours(16)),
      journeeFermeeApres(operateur, Instant.parse("2042-04-10T07:00:00Z"), Duration.ofHours(13)),
      journeeFermeeApres(operateur, Instant.parse("2042-04-11T07:00:00Z"), Duration.ofHours(13).plusSeconds(1)),
      journeeFermeeApres(operateur, Instant.parse("2042-04-11T21:00:00Z"), Duration.ofHours(13).plusMillis(500)),
      journeeOuverteA(operateur, samedi)
    );

    JourneesDeTravailEnMemoire enMemoire = new JourneesDeTravailEnMemoire();
    jeu.forEach(journee -> {
      enMemoire.create(journee);
      inTransaction(() -> journeesPersistees.create(journee));
    });

    for (Optional<TypeDAnomalie> type : List.of(
      Optional.<TypeDAnomalie>empty(),
      Optional.of(TypeDAnomalie.JOURNEE_SANS_DEPART),
      Optional.of(TypeDAnomalie.AMPLITUDE_EXCESSIVE)
    )) {
      CriteresDAnomalie criteres = new CriteresDAnomalie(
        maintenant,
        new AmplitudeMaximale(Duration.ofHours(13)),
        Optional.of(operateur),
        type
      );
      Page<JourneeDeTravail> attendue = enMemoire.enAnomalie(criteres, PREMIERE_PAGE);
      Page<JourneeDeTravail> obtenue = inTransaction(() -> journeesPersistees.enAnomalie(criteres, PREMIERE_PAGE));

      assertThat(obtenue.content()).describedAs("type %s", type).containsExactlyElementsOf(attendue.content());
      assertThat(obtenue.totalElementsCount()).isEqualTo(attendue.totalElementsCount());
    }
    assertThat(
      enMemoire
        .enAnomalie(
          new CriteresDAnomalie(maintenant, new AmplitudeMaximale(Duration.ofHours(13)), Optional.of(operateur), Optional.empty()),
          PREMIERE_PAGE
        )
        .totalElementsCount()
    ).isEqualTo(4);
  }

  private static JourneeDeTravail journeeFermeeApres(OperateurId operateur, Instant arrivee, Duration amplitude) {
    return journeeOuverteA(operateur, arrivee).enregistre(presence(TypeDEvenementDePresence.DEPART, arrivee.plus(amplitude)));
  }

  private static JourneeDeTravail journeeOuverteA(OperateurId operateur, Instant arrivee) {
    return JourneeDeTravail.ouverte(JourneeDeTravailId.newId(), operateur).enregistre(presence(TypeDEvenementDePresence.ARRIVEE, arrivee));
  }

  private static JourneeDeTravail journeeCompleteA(OperateurId operateur, Instant arrivee) {
    return journeeOuverteA(operateur, arrivee).enregistre(presence(TypeDEvenementDePresence.DEPART, arrivee.plusSeconds(36000)));
  }

  private static EvenementDePresence presence(TypeDEvenementDePresence type, Instant date) {
    return EvenementDePresence.builder()
      .id(EvenementDePresenceId.newId())
      .type(type)
      .auteur(AUTEUR_DUPONT)
      .horodatage(Horodatage.saisiA(date));
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
