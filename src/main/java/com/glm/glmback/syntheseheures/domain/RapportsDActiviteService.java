package com.glm.glmback.syntheseheures.domain;

import com.glm.glmback.shared.time.domain.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Assemble le rapport d'activite d'un operateur sur une periode, depuis les activites interpretees par atelier.
 *
 * <p>
 * Le decoupage et les durees sont ceux de la synthese des heures. Seul le journal brut n'est pas lu : un geste sans
 * activite ne porte aucun temps, et le rapport ne parle que du temps pointe.
 * </p>
 */
public final class RapportsDActiviteService {

  private static final Comparator<IntervalleDUnJour> PAR_ORDRE_CHRONOLOGIQUE = Comparator.<IntervalleDUnJour, Instant>comparing(portion ->
    portion.intervalle().plage().debut()
  )
    .thenComparing(portion -> portion.intervalle().plage().fin().orElse(Instant.MAX))
    .thenComparing(portion -> portion.intervalle().lecture().id().uuid());

  private final OperateursConnus operateurs;
  private final FuseauHoraireDeLEntreprise fuseau;
  private final ActivitesDeLOperateur activites;
  private final ElementsDeFabrication elements;
  private final PostesDeTravail postes;
  private final Clock clock;

  private RapportsDActiviteService(
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    ActivitesDeLOperateur activites,
    ElementsDeFabrication elements,
    PostesDeTravail postes,
    Clock clock
  ) {
    this.operateurs = operateurs;
    this.fuseau = fuseau;
    this.activites = activites;
    this.elements = elements;
    this.postes = postes;
    this.clock = clock;
  }

  public static RapportsDActiviteServiceOperateursBuilder builder() {
    return operateurs ->
      fuseau ->
        activites -> elements -> postes -> clock -> new RapportsDActiviteService(operateurs, fuseau, activites, elements, postes, clock);
  }

  public RapportDActivite rapport(OperateurId operateur, PeriodeCalendaire periode) {
    OperateurConnu connu = operateurs.get(operateur).orElseThrow(() -> new OperateurInconnuException(operateur));
    DecoupageCalendaire decoupage = new DecoupageCalendaire(periode, fuseau.zone());
    Instant evaluation = clock.now();
    if (decoupage.debut().isAfter(evaluation)) {
      throw new PeriodeFutureException(periode);
    }

    List<ActiviteDElement> travail = activites.recouvrant(operateur, decoupage.debut(), decoupage.finExclusive());
    Map<ElementId, ElementEngage> engages = travail
      .stream()
      .map(ActiviteDElement::element)
      .collect(Collectors.toMap(ElementEngage::id, Function.identity(), (premier, suivant) -> premier));
    List<IntervalleDUnJour> portions = travail
      .stream()
      .map(activite -> activite.activite().a(evaluation))
      .flatMap(intervalle -> decoupage.intervalles(intervalle, evaluation).stream())
      .sorted(PAR_ORDRE_CHRONOLOGIQUE)
      .toList();
    Map<PosteDeTravailId, PosteConnu> connus = postesConnus(portions);

    List<JourDuRapport> jours = decoupage
      .jours()
      .stream()
      .map(jour -> new JourDuRapport(jour, lignesDu(jour, portions, engages, connus)))
      .toList();
    List<LigneDuJournal> journal = jours
      .stream()
      .flatMap(jour -> jour.lignes().stream())
      .toList();

    return RapportDActivite.builder()
      .operateur(connu)
      .periode(periode)
      .evaluation(evaluation)
      .fuseau(fuseau.zone())
      .jours(jours)
      .elements(elementsDu(journal))
      .natures(naturesDu(journal));
  }

  private Map<PosteDeTravailId, PosteConnu> postesConnus(List<IntervalleDUnJour> portions) {
    Set<PosteDeTravailId> pointes = portions
      .stream()
      .flatMap(portion -> portion.intervalle().activite().poste().stream())
      .collect(Collectors.toSet());

    return postes.parIds(pointes).stream().collect(Collectors.toMap(PosteConnu::id, Function.identity()));
  }

  private static List<LigneDuJournal> lignesDu(
    LocalDate jour,
    List<IntervalleDUnJour> portions,
    Map<ElementId, ElementEngage> engages,
    Map<PosteDeTravailId, PosteConnu> connus
  ) {
    List<IntervalleDActivite> duJour = portions
      .stream()
      .filter(portion -> portion.jour().equals(jour))
      .map(IntervalleDUnJour::intervalle)
      .toList();

    return duJour
      .stream()
      .map(intervalle ->
        LigneDuJournal.builder()
          .activite(intervalle.lecture().id())
          .plage(intervalle.plage())
          .element(engages.get(intervalle.activite().element()))
          .poste(intervalle.activite().poste().map(connus::get))
          .nature(intervalle.activite().nature())
          .categorie(intervalle.activite().categorie())
          .etat(intervalle.lecture().etat())
          .simultanee(duJour.stream().anyMatch(autre -> seRecouvrent(intervalle, autre)))
      )
      .toList();
  }

  /** Deux activites differentes, terminees toutes deux, dont les plages se chevauchent strictement. */
  private static boolean seRecouvrent(IntervalleDActivite une, IntervalleDActivite autre) {
    if (une.lecture().id().equals(autre.lecture().id()) || une.plage().estOuverte() || autre.plage().estOuverte()) {
      return false;
    }

    return (
      autre.plage().debut().isBefore(une.plage().fin().orElseThrow()) && une.plage().debut().isBefore(autre.plage().fin().orElseThrow())
    );
  }

  /** Les elements par temps decroissant, puis par nom ; leurs fiches sont relues au referentiel. */
  private List<ElementDeLaSynthese> elementsDu(List<LigneDuJournal> journal) {
    Map<ElementId, List<LigneDuJournal>> parElement = journal
      .stream()
      .collect(Collectors.groupingBy(ligne -> ligne.element().id(), LinkedHashMap::new, Collectors.toList()));
    Map<ElementId, FicheDElement> fiches = elements
      .parIds(parElement.keySet())
      .stream()
      .collect(Collectors.toMap(FicheDElement::id, Function.identity()));

    return parElement
      .values()
      .stream()
      .map(lignes -> elementDu(lignes, Optional.ofNullable(fiches.get(lignes.getFirst().element().id()))))
      .sorted(
        Comparator.<ElementDeLaSynthese, Duration>comparing(element -> element.duree().valeur())
          .reversed()
          .thenComparing(element -> element.element().nom().value())
      )
      .toList();
  }

  private static ElementDeLaSynthese elementDu(List<LigneDuJournal> lignes, Optional<FicheDElement> fiche) {
    return ElementDeLaSynthese.builder()
      .element(lignes.getFirst().element())
      .reference(fiche.flatMap(FicheDElement::reference))
      .description(fiche.flatMap(FicheDElement::description))
      .duree(somme(lignes))
      .dureeNonConformite(
        somme(
          lignes
            .stream()
            .filter(ligne -> ligne.categorie() == CategorieDActivite.NON_CONFORMITE)
            .toList()
        )
      )
      .postes(
        lignes
          .stream()
          .flatMap(ligne ->
            ligne
              .poste()
              .map(poste -> new PosteDeLElement(poste, ligne.nature()))
              .stream()
          )
          .distinct()
          .toList()
      );
  }

  /** Les natures par temps decroissant, puis par libelle ; ce qui est pointe sans nature vient en dernier. */
  private static List<NatureDuRapport> naturesDu(List<LigneDuJournal> journal) {
    return journal
      .stream()
      .collect(Collectors.groupingBy(LigneDuJournal::nature, LinkedHashMap::new, Collectors.toList()))
      .entrySet()
      .stream()
      .map(nature -> new NatureDuRapport(nature.getKey(), somme(nature.getValue())))
      .sorted(
        Comparator.<NatureDuRapport, Duration>comparing(nature -> nature.duree().valeur())
          .reversed()
          .thenComparing(
            nature -> nature.nature().map(NatureDOperation::value).orElse(null),
            Comparator.nullsLast(Comparator.naturalOrder())
          )
      )
      .toList();
  }

  private static DureeTotale somme(List<LigneDuJournal> lignes) {
    return DureeTotale.somme(lignes.stream().map(LigneDuJournal::duree).toList());
  }

  public interface RapportsDActiviteServiceOperateursBuilder {
    RapportsDActiviteServiceFuseauBuilder operateurs(OperateursConnus operateurs);
  }

  public interface RapportsDActiviteServiceFuseauBuilder {
    RapportsDActiviteServiceActivitesBuilder fuseau(FuseauHoraireDeLEntreprise fuseau);
  }

  public interface RapportsDActiviteServiceActivitesBuilder {
    RapportsDActiviteServiceElementsBuilder activites(ActivitesDeLOperateur activites);
  }

  public interface RapportsDActiviteServiceElementsBuilder {
    RapportsDActiviteServicePostesBuilder elements(ElementsDeFabrication elements);
  }

  public interface RapportsDActiviteServicePostesBuilder {
    RapportsDActiviteServiceClockBuilder postes(PostesDeTravail postes);
  }

  public interface RapportsDActiviteServiceClockBuilder {
    RapportsDActiviteService clock(Clock clock);
  }
}
