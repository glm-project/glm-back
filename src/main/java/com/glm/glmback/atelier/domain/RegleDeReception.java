package com.glm.glmback.atelier.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * La regle qui juge un pointage a son arrivee, sur la cle (operateur, poste) d'un suivi.
 *
 * <p>
 * Elle verifie dans l'ordre : le pointage n'est pas plus ancien que le dernier accepte de la cle ; l'echeance de la
 * derniere activite, jugee sur l'heure du geste ; puis le tableau. Rien n'est en cours quand la cle n'a jamais ete
 * ouverte, ou que sa derniere activite est terminee — par une fin, par la cloture, ou par son echeance, atteinte
 * lorsque l'heure du geste est superieure ou egale au debut plus la duree maximale. Une fin ferme l'activite en cours ;
 * un debut ou une non conformite en ouvre une, sauf si une activite est en cours.
 * </p>
 *
 * <p>
 * La cloture ne termine l'activite que pour un geste qui lui est posterieur : une fin survenue avant la cloture, mais
 * recue apres elle, termine toujours l'activite a son heure.
 * </p>
 */
final class RegleDeReception {

  private RegleDeReception() {}

  static VerdictDeReception juge(
    JournalDAtelier journal,
    Optional<Instant> cloture,
    CleDActivite cle,
    TypeDEvenementDAtelier type,
    Instant survenue
  ) {
    List<EvenementDAtelier> faits = journal
      .evenements()
      .stream()
      .filter(fait -> fait.cle().equals(cle))
      .toList();
    Optional<EvenementDAtelierId> dernierAccepte = faits
      .stream()
      .reduce((premier, second) -> second)
      .map(EvenementDAtelier::id);
    if (faits.stream().anyMatch(fait -> survenue.isBefore(fait.dateDeSurvenue()))) {
      return new VerdictDeReception.Ignore(RaisonDePointageIgnore.ANTERIEUR, dernierAccepte);
    }

    Optional<Activite> sansFin = SequenceDActivites.activites(faits, cloture.filter(date -> date.isBefore(survenue)))
      .stream()
      .reduce((premiere, seconde) -> seconde)
      .filter(derniere -> derniere.fin().isEmpty());
    Optional<Activite> enCours = sansFin.filter(derniere -> !derniere.echeance().estAtteinteA(survenue));

    if (type == TypeDEvenementDAtelier.FIN) {
      if (enCours.isPresent()) {
        return new VerdictDeReception.Accepte(enCours.map(Activite::id));
      }

      return new VerdictDeReception.Ignore(
        sansFin.isPresent() ? RaisonDePointageIgnore.APRES_ECHEANCE : RaisonDePointageIgnore.AUCUNE_ACTIVITE,
        dernierAccepte
      );
    }

    if (enCours.isPresent()) {
      return new VerdictDeReception.Ignore(RaisonDePointageIgnore.DEJA_EN_COURS, dernierAccepte);
    }

    return new VerdictDeReception.Accepte(Optional.empty());
  }
}
