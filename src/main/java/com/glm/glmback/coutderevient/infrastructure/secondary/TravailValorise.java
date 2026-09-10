package com.glm.glmback.coutderevient.infrastructure.secondary;

import com.glm.glmback.coutderevient.domain.ElementId;
import com.glm.glmback.coutderevient.domain.EvenementDAtelier;
import com.glm.glmback.coutderevient.domain.JournalDAtelier;
import com.glm.glmback.coutderevient.domain.OccupationDesOperateurs;
import com.glm.glmback.coutderevient.domain.OperateurId;
import com.glm.glmback.coutderevient.domain.SuiviDuTravail;
import com.glm.glmback.coutderevient.domain.TravailDeLElement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

/**
 * Le journal d'atelier lu dans ses propres tables, sans jamais importer son code.
 *
 * <p>
 * Les deux lectures repondent a deux questions differentes sur la meme matiere : ce qui a ete fait
 * <em>sur cet element</em>, et ce que ses operateurs menaient <em>de front</em>. La seconde est ce qui donne le
 * diviseur, et elle ne peut pas se deduire de la premiere.
 * </p>
 *
 * <p>
 * Jamais une requete par suivi : les suivis d'abord, leurs journaux d'un seul coup ensuite. Les evenements annules
 * sont ecartes des le SQL — les rapporter pour les filtrer ferait porter au domaine une correction qui ne le regarde
 * pas.
 * </p>
 */
@Repository
class TravailValorise implements TravailDeLElement, OccupationDesOperateurs {

  private final SpringDataSuivisValorisesRepository suivis;
  private final SpringDataEvenementsDAtelierValorisesRepository evenements;

  TravailValorise(SpringDataSuivisValorisesRepository suivis, SpringDataEvenementsDAtelierValorisesRepository evenements) {
    this.suivis = suivis;
    this.evenements = evenements;
  }

  @Override
  public List<SuiviDuTravail> suivis(ElementId element) {
    List<SuiviValoriseEntity> passages = suivis.findByElementId(element.uuid());

    return journaux(passages, evenements.findBySuiviIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAscIdAsc(identifiants(passages)));
  }

  @Override
  public List<SuiviDuTravail> suivis(Set<OperateurId> operateurs, Instant avant) {
    Set<UUID> concernes = uuids(operateurs);
    Set<UUID> passages = evenements.suivisDesOperateurs(concernes, avant);

    return journaux(
      suivis.findByIdIn(passages),
      evenements.findBySuiviIdInAndOperateurIdInAndAnnulationDateIsNullOrderByDateDeSurvenueAscIdAsc(passages, concernes)
    );
  }

  /**
   * Restreindre un journal a certains operateurs ne casse aucune sequence : le repli se joue par cle d'activite, donc
   * par couple operateur et poste.
   */
  private static List<SuiviDuTravail> journaux(List<SuiviValoriseEntity> passages, List<EvenementDAtelierValoriseEntity> lus) {
    Map<UUID, List<EvenementDAtelier>> parSuivi = lus
      .stream()
      .collect(
        Collectors.groupingBy(
          EvenementDAtelierValoriseEntity::suiviId,
          Collectors.mapping(EvenementDAtelierValoriseEntity::toDomain, Collectors.<EvenementDAtelier>toList())
        )
      );

    return passages
      .stream()
      .map(passage -> new SuiviDuTravail(new JournalDAtelier(journal(parSuivi, passage.id())), passage.cloture()))
      .toList();
  }

  private static List<EvenementDAtelier> journal(Map<UUID, List<EvenementDAtelier>> parSuivi, UUID suivi) {
    return Optional.ofNullable(parSuivi.get(suivi)).orElseGet(List::of);
  }

  private static Set<UUID> identifiants(List<SuiviValoriseEntity> passages) {
    return passages.stream().map(SuiviValoriseEntity::id).collect(Collectors.toSet());
  }

  private static Set<UUID> uuids(Set<OperateurId> operateurs) {
    return operateurs.stream().map(OperateurId::uuid).collect(Collectors.toSet());
  }
}
