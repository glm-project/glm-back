package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.IntegrationTest;
import com.glm.glmback.shared.multitenancy.infrastructure.primary.WithTenant;
import com.glm.glmback.shared.pagination.domain.Page;
import com.glm.glmback.shared.pagination.domain.Pageable;
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

  @Test
  @WithTenant("impeccmold")
  void shouldRendreLesMemesSuivisQueLeDoubleEnMemoire() {
    Instant lundi = Instant.parse("2042-01-05T07:00:00Z");
    Instant mardi = Instant.parse("2042-01-06T07:00:00Z");
    Instant mercredi = Instant.parse("2042-01-07T07:00:00Z");
    List<SuiviDAtelier> jeu = List.of(
      suiviEngageA(lundi),
      suiviEngageA(mardi).enregistre(debutA(mardi.plusSeconds(3600))),
      suiviEngageA(mercredi).cloture(new Cloture(AUTEUR_LEROY, Horodatage.saisiA(mercredi.plusSeconds(36000))))
    );

    SuivisDAtelierEnMemoire enMemoire = new SuivisDAtelierEnMemoire();
    jeu.forEach(suivi -> {
      enMemoire.create(suivi);
      inTransaction(() -> suivisPersistes.create(suivi));
    });

    Periode semaine = new Periode(lundi, mercredi);
    for (Set<EtatDAtelier> etats : List.of(
      Set.<EtatDAtelier>of(),
      Set.of(EtatDAtelier.EN_COURS),
      Set.of(EtatDAtelier.CLOTURE, EtatDAtelier.EN_ATTENTE)
    )) {
      SuiviDAtelierCriteria criteres = new SuiviDAtelierCriteria(Optional.of(semaine), etats);
      Page<SuiviDAtelier> attendue = enMemoire.list(criteres, PREMIERE_PAGE);
      Page<SuiviDAtelier> obtenue = inTransaction(() -> suivisPersistes.list(criteres, PREMIERE_PAGE));

      assertThat(obtenue.content()).describedAs("etats %s", etats).containsExactlyElementsOf(attendue.content());
      assertThat(obtenue.totalElementsCount()).isEqualTo(attendue.totalElementsCount());
    }
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

  private static EvenementDAtelier debutA(Instant date) {
    return EvenementDAtelier.builder()
      .id(EvenementDAtelierId.newId())
      .type(TypeDEvenementDAtelier.DEBUT)
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .auteur(AUTEUR_DUPONT)
      .horodatage(Horodatage.saisiA(date));
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
