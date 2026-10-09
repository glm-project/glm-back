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
 * {@link SuiviDAtelierCriteria#matches} vit dans le domaine pour que le
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
  private TransactionTemplate transactions;

  /**
   * L'etat se juge a l'instant d'evaluation des criteres : juste avant l'echeance de l'activite de mardi, puis a
   * l'echeance pile, ou elle cesse d'etre en cours sans aucune ecriture. Le jeu couvre chaque etat, y compris un suivi
   * en attente sans pointage, et une activite terminee au-dela de son echeance par une regularisation.
   */
  @Test
  @WithTenant("impeccmold")
  void shouldRendreLesMemesSuivisQueLeDoubleEnMemoire() {
    Instant lundi = Instant.parse("2042-01-05T07:00:00Z");
    Instant mardi = Instant.parse("2042-01-06T07:00:00Z");
    Instant mercredi = Instant.parse("2042-01-07T07:00:00Z");
    Instant echeanceDeMardi = mardi.plusSeconds(3600).plus(Duration.ofHours(13));
    EvenementDAtelier termine = debutA(lundi.plusSeconds(3600 * 3));
    EvenementDAtelier prolonge = debutA(lundi.plusSeconds(3600 * 4));
    EvenementDAtelier terminee = debutA(mardi.plusSeconds(3600 * 2));
    List<SuiviDAtelier> jeu = List.of(
      suiviEngageA(lundi),
      suiviEngageA(lundi.plusSeconds(3600 * 2)).enregistre(termine).enregistre(finDe(termine).a(lundi.plusSeconds(3600 * 5))),
      suiviEngageA(lundi.plusSeconds(3600 * 3)).enregistre(prolonge).enregistre(finRegulariseeA(prolonge, mercredi)),
      suiviEngageA(mardi).enregistre(debutA(mardi.plusSeconds(3600))),
      suiviEngageA(mardi.plusSeconds(3600)).enregistre(terminee).enregistre(finDe(terminee).a(mardi.plusSeconds(3600 * 4))),
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

  private static SuiviDAtelier suiviEngageA(Instant date) {
    return SuiviDAtelier.builder()
      .id(SuiviDAtelierId.newId())
      .element(new ElementEngage(new ElementEngageId(UUID.randomUUID()), NOM_OF_2026_000042, CATEGORIE_OF))
      .engagement(new Engagement(AUTEUR_LEROY, date))
      .journal(JournalDAtelier.vide());
  }

  private static EvenementDAtelier finRegulariseeA(EvenementDAtelier ouvrant, Instant date) {
    return EvenementDAtelier.builder()
      .id(EvenementDAtelierId.newId())
      .type(TypeDEvenementDAtelier.FIN)
      .activite(Optional.empty())
      .activiteVisee(ouvrant.activite())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_FRAISEUSE_1))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DUPONT))
      .dureeMax(Optional.empty())
      .auteur(AUTEUR_LEROY)
      .origine(OrigineDuPointage.REGULARISATION)
      .horodatage(Horodatage.saisiA(date));
  }

  private static EvenementDAtelier debutA(Instant date) {
    EvenementDAtelierId id = EvenementDAtelierId.newId();

    return EvenementDAtelier.builder()
      .id(id)
      .type(TypeDEvenementDAtelier.DEBUT)
      .activite(Optional.of(ActiviteId.ouvertePar(id)))
      .activiteVisee(Optional.empty())
      .operateur(OPERATEUR_ID_DUPONT)
      .poste(Optional.of(POSTE_ID_FRAISEUSE_1))
      .nature(Optional.of(NATURE_FRAISAGE))
      .coutHoraire(Optional.of(COUT_HORAIRE_FRAISEUSE_1))
      .tauxHoraire(Optional.of(TAUX_HORAIRE_DUPONT))
      .dureeMax(Optional.of(DUREE_MAXIMALE_TREIZE_HEURES))
      .auteur(AUTEUR_DUPONT)
      .origine(OrigineDuPointage.POINTAGE)
      .horodatage(Horodatage.saisiA(date));
  }

  private <T> T inTransaction(Supplier<T> action) {
    return transactions.execute(status -> action.get());
  }
}
