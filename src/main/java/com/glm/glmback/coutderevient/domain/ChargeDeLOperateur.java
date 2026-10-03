package com.glm.glmback.coutderevient.domain;

import com.glm.glmback.shared.error.domain.Assert;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Ce qu'un operateur menait de front, decoupe en sous-periodes ou le nombre de postes occupes ne change pas.
 *
 * <p>
 * Le diviseur compte des <strong>postes</strong>, jamais des elements ni des activites : le client enonce la regle
 * deux fois de suite, cout horaire de chaque machine active non divise, taux horaire de l'operateur divise par le
 * nombre de machines qu'il utilise. Un operateur sur trois elements avec une seule machine n'est donc pas divise.
 * </p>
 *
 * <p>
 * Un pointage sans poste compte pour un poste, comme la cle d'activite de l'atelier ou l'absence de poste est une
 * valeur : une entreprise sans parc machine retrouve un diviseur de un partout.
 * </p>
 *
 * <p>
 * Les sous-periodes se reunissent en fenetres de partage, ou la main d'oeuvre est arrondie puis repartie au centime.
 * </p>
 */
public record ChargeDeLOperateur(List<SousPeriode> sousPeriodes, List<FenetreDePartage> fenetres) {
  public ChargeDeLOperateur {
    Assert.field("sous periodes", sousPeriodes).notNull().noNullElement();
    Assert.field("fenetres", fenetres).notNull().noNullElement();
  }

  /**
   * La charge deduite de tout ce que l'operateur a fait, tous elements confondus.
   *
   * <p>
   * Toutes elements confondus, parce que c'est la question posee : un nouveau pointage sur un second element change
   * la part deja attribuee au premier. Le reparti traverse les agregats, il ne peut donc etre qu'une projection.
   * </p>
   */
  static ChargeDeLOperateur de(List<TrancheDActivite> tranches) {
    return de(tranches, List.of());
  }

  static ChargeDeLOperateur de(List<TrancheDActivite> tranches, List<ZoneIncertaine> zones) {
    List<Instant> bornes = bornes(tranches, zones);
    List<SousPeriode> sousPeriodes = new ArrayList<>();

    for (int rang = 0; rang + 1 < bornes.size(); rang++) {
      Periode candidate = new Periode(bornes.get(rang), bornes.get(rang + 1));
      sousPeriode(tranches, zones, candidate).ifPresent(sousPeriodes::add);
    }

    return new ChargeDeLOperateur(List.copyOf(sousPeriodes), fenetres(tranches, sousPeriodes));
  }

  /**
   * La tranche donnee, decoupee en autant de parts que de fenetres de partage qu'elle traverse.
   */
  public List<TrancheValorisable> decoupe(TrancheDActivite tranche) {
    return fenetres
      .stream()
      .flatMap(fenetre ->
        tranche
          .reduiteA(fenetre.periode())
          .map(part -> new TrancheValorisable(part, fenetre))
          .stream()
      )
      .toList();
  }

  /**
   * Les sous-periodes adjacentes ou l'operateur occupe le meme ensemble de postes, avec un diviseur connu, ne forment
   * qu'une fenetre : une heure qui n'est pas partagee n'est jamais coupee en morceaux arrondis.
   */
  private static List<FenetreDePartage> fenetres(List<TrancheDActivite> tranches, List<SousPeriode> sousPeriodes) {
    List<SousPeriode> etendues = new ArrayList<>();
    for (SousPeriode sousPeriode : sousPeriodes) {
      if (!etendues.isEmpty() && prolonge(tranches, etendues.getLast(), sousPeriode)) {
        SousPeriode precedente = etendues.removeLast();
        etendues.add(
          new SousPeriode(new Periode(precedente.periode().debut(), sousPeriode.periode().fin()), precedente.diviseur().orElseThrow())
        );
      } else {
        etendues.add(sousPeriode);
      }
    }
    return etendues
      .stream()
      .map(etendue ->
        new FenetreDePartage(
          etendue,
          etendue
            .diviseur()
            .map(diviseur -> RepartitionDeMainDOeuvre.de(tranches, etendue.periode(), diviseur))
            .orElse(RepartitionDeMainDOeuvre.AUCUNE),
          tranches
            .stream()
            .filter(tranche -> tranche.periode().intersection(etendue.periode()).isPresent())
            .toList()
        )
      )
      .toList();
  }

  private static boolean prolonge(List<TrancheDActivite> tranches, SousPeriode precedente, SousPeriode suivante) {
    return (
      precedente.periode().fin().equals(suivante.periode().debut())
      && precedente.diviseur().isPresent()
      && suivante.diviseur().isPresent()
      && postes(tranches, precedente.periode()).equals(postes(tranches, suivante.periode()))
    );
  }

  private static List<Instant> bornes(List<TrancheDActivite> tranches, List<ZoneIncertaine> zones) {
    TreeSet<Instant> bornes = new TreeSet<>();
    tranches.forEach(tranche -> {
      bornes.add(tranche.periode().debut());
      bornes.add(tranche.periode().fin());
    });

    zones.forEach(zone -> {
      bornes.add(zone.periode().debut());
      bornes.add(zone.periode().fin());
    });
    return List.copyOf(bornes);
  }

  /**
   * Rien n'est rendu la ou l'operateur ne travaillait pas : entre deux pointages, il n'y a aucun diviseur a poser.
   */
  private static Optional<SousPeriode> sousPeriode(List<TrancheDActivite> tranches, List<ZoneIncertaine> zones, Periode candidate) {
    Set<Optional<PosteDeTravailId>> postes = postes(tranches, candidate);
    if (postes.isEmpty()) {
      return Optional.empty();
    }
    Set<ActiviteInterpretee> responsables = zones
      .stream()
      .filter(zone -> zone.periode().intersection(candidate).isPresent())
      .map(ZoneIncertaine::activite)
      .filter(activite -> !postes.contains(activite.activite().poste()))
      .collect(Collectors.toSet());
    Optional<Diviseur> diviseur = responsables.isEmpty() ? Optional.of(new Diviseur(postes.size())) : Optional.empty();
    return Optional.of(new SousPeriode(candidate, diviseur, responsables));
  }

  private static Set<Optional<PosteDeTravailId>> postes(List<TrancheDActivite> tranches, Periode periode) {
    return tranches
      .stream()
      .filter(tranche -> tranche.periode().intersection(periode).isPresent())
      .map(tranche -> tranche.activite().poste())
      .collect(Collectors.toSet());
  }
}
