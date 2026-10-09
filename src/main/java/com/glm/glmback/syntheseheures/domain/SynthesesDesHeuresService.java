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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Assemble les durees hebdomadaires depuis les activites interpretees et le journal brut d'atelier. */
public final class SynthesesDesHeuresService {

  private static final Duration TOLERANCE_FUTURE = Duration.ofMinutes(2);

  /** L'heure metier, la fin avant l'ouverture a heure egale, puis l'identite. */
  private static final Comparator<PointageDElement> PAR_ORDRE_DU_JOURNAL = Comparator.comparing(PointageDElement::dateDeSurvenue)
    .thenComparingInt(pointage -> pointage.type() == TypeDEvenementDAtelier.FIN ? 0 : 1)
    .thenComparing(PointageDElement::id);

  private final OperateursConnus operateurs;
  private final FuseauHoraireDeLEntreprise fuseau;
  private final ActivitesDeLOperateur activites;
  private final JournalDeLOperateur journal;
  private final ElementsDeFabrication elements;
  private final PostesDeTravail postes;
  private final Clock clock;

  private SynthesesDesHeuresService(
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    ActivitesDeLOperateur activites,
    JournalDeLOperateur journal,
    ElementsDeFabrication elements,
    PostesDeTravail postes,
    Clock clock
  ) {
    this.operateurs = operateurs;
    this.fuseau = fuseau;
    this.activites = activites;
    this.journal = journal;
    this.elements = elements;
    this.postes = postes;
    this.clock = clock;
  }

  public static SynthesesDesHeuresServiceOperateursBuilder builder() {
    return operateurs ->
      fuseau ->
        activites ->
          journal ->
            elements -> postes -> clock -> new SynthesesDesHeuresService(operateurs, fuseau, activites, journal, elements, postes, clock);
  }

  public SyntheseDesHeures synthese(OperateurId operateur, SemaineCalendaire semaine) {
    return synthese(operateur, semaine, Optional.empty());
  }

  public SyntheseDesHeures synthese(OperateurId operateur, SemaineCalendaire semaine, Optional<Instant> evaluationDemandee) {
    OperateurConnu connu = operateurs.get(operateur).orElseThrow(() -> new OperateurInconnuException(operateur));
    DecoupageCalendaire decoupage = new DecoupageCalendaire(semaine, fuseau.zone());
    Instant maintenant = clock.now();
    Instant evaluation = evaluationDemandee.orElse(maintenant);
    if (evaluation.isAfter(maintenant.plus(TOLERANCE_FUTURE))) {
      throw new EvaluationFutureException(evaluation);
    }
    List<ActiviteDElement> travail = activites.recouvrant(operateur, decoupage.debut(), decoupage.finExclusive());
    List<IntervalleDUnJour> intervalles = travail
      .stream()
      .map(ActiviteDElement::activite)
      .map(activite -> activite.a(evaluation))
      .flatMap(intervalle -> decoupage.intervalles(intervalle, evaluation).stream())
      .toList();
    List<JournalDElement> journaux = journal.dans(operateur, decoupage.debut(), decoupage.finExclusive());
    List<PointageDElement> pointagesDElement = journaux
      .stream()
      .flatMap(entree -> entree.pointages().stream())
      .filter(pointage -> decoupage.jours().contains(jourDe(pointage)))
      .sorted(PAR_ORDRE_DU_JOURNAL)
      .toList();

    return SyntheseDesHeures.builder()
      .operateur(connu)
      .semaine(semaine)
      .evaluation(evaluation)
      .jours(jours(decoupage, intervalles, pointagesDElement))
      .elements(
        elementsDeLaSemaine(
          Stream.concat(travail.stream().map(ActiviteDElement::element), journaux.stream().map(JournalDElement::element)).toList(),
          intervalles,
          pointagesDElement
        )
      );
  }

  private List<JourDeSynthese> jours(
    DecoupageCalendaire decoupage,
    List<IntervalleDUnJour> intervalles,
    List<PointageDElement> pointagesDElement
  ) {
    Map<LocalDate, List<PointageDElement>> pointagesParJour = pointagesDElement
      .stream()
      .<PointageDElement>map(Function.identity())
      .collect(Collectors.groupingBy(this::jourDe));
    return decoupage
      .jours()
      .stream()
      .map(jour ->
        JourDeSynthese.builder().jour(jour).pointages(pointagesParJour.getOrDefault(jour, List.of())).dureeOperationnelle(somme(intervalles
              .stream()
              .filter(intervalle -> intervalle.jour().equals(jour))
              .map(IntervalleDUnJour::intervalle)
              .toList(), intervalle -> true))
      )
      .toList();
  }

  /**
   * Les elements touches dans la semaine, par premiere apparition puis par nom, avec leur fiche relue au referentiel.
   */
  private List<ElementDeLaSynthese> elementsDeLaSemaine(
    List<ElementEngage> elementsEngages,
    List<IntervalleDUnJour> intervalles,
    List<PointageDElement> pointagesDElement
  ) {
    Map<ElementId, ElementEngage> engages = elementsEngages
      .stream()
      .collect(Collectors.toMap(ElementEngage::id, Function.identity(), (premier, suivant) -> premier));
    Map<ElementId, Instant> apparitions = premieresApparitions(intervalles, pointagesDElement);
    Map<ElementId, FicheDElement> fiches = elements
      .parIds(apparitions.keySet())
      .stream()
      .collect(Collectors.toMap(FicheDElement::id, Function.identity()));
    Map<PosteDeTravailId, PosteConnu> connus = postesConnus(intervalles, pointagesDElement);

    return apparitions
      .keySet()
      .stream()
      .map(engages::get)
      .sorted(
        Comparator.<ElementEngage, Instant>comparing(engage -> apparitions.get(engage.id())).thenComparing(engage -> engage.nom().value())
      )
      .map(engage ->
        elementDeLaSynthese(
          engage,
          Optional.ofNullable(fiches.get(engage.id())),
          travailDe(engage.id(), intervalles),
          postesDeLElement(usagesDe(engage.id(), intervalles, pointagesDElement), connus)
        )
      )
      .toList();
  }

  private static Map<ElementId, Instant> premieresApparitions(
    List<IntervalleDUnJour> intervalles,
    List<PointageDElement> pointagesDElement
  ) {
    Stream<Map.Entry<ElementId, Instant>> parIntervalle = intervalles
      .stream()
      .map(IntervalleDUnJour::intervalle)
      .map(intervalle -> Map.entry(intervalle.activite().element(), intervalle.plage().debut()));
    Stream<Map.Entry<ElementId, Instant>> parPointage = pointagesDElement
      .stream()
      .map(pointage -> Map.entry(pointage.element(), pointage.dateDeSurvenue()));

    return Stream.concat(parIntervalle, parPointage).collect(
      Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (une, autre) -> une.isBefore(autre) ? une : autre, LinkedHashMap::new)
    );
  }

  private Map<PosteDeTravailId, PosteConnu> postesConnus(List<IntervalleDUnJour> intervalles, List<PointageDElement> pointagesDElement) {
    Set<PosteDeTravailId> pointes = Stream.concat(
      intervalles.stream().flatMap(intervalle -> intervalle.intervalle().activite().poste().stream()),
      pointagesDElement.stream().flatMap(pointage -> pointage.poste().stream())
    ).collect(Collectors.toSet());

    return postes.parIds(pointes).stream().collect(Collectors.toMap(PosteConnu::id, Function.identity()));
  }

  private static List<IntervalleDActivite> travailDe(ElementId element, List<IntervalleDUnJour> intervalles) {
    return intervalles
      .stream()
      .map(IntervalleDUnJour::intervalle)
      .filter(intervalle -> intervalle.activite().element().equals(element))
      .sorted(Comparator.comparing(intervalle -> intervalle.plage().debut()))
      .toList();
  }

  private static ElementDeLaSynthese elementDeLaSynthese(
    ElementEngage engage,
    Optional<FicheDElement> fiche,
    List<IntervalleDActivite> travail,
    List<PosteDeLElement> postes
  ) {
    return ElementDeLaSynthese.builder()
      .element(engage)
      .reference(fiche.flatMap(FicheDElement::reference))
      .description(fiche.flatMap(FicheDElement::description))
      .duree(somme(travail, intervalle -> true))
      .dureeNonConformite(somme(travail, intervalle -> intervalle.activite().categorie() == CategorieDActivite.NON_CONFORMITE))
      .postes(postes);
  }

  /**
   * Chaque poste et nature que l'element a vus dans la semaine, par son travail ou par un pointage qui n'en a laisse
   * aucun : tout poste nomme au journal doit trouver son libelle ici.
   */
  private static List<UsageDePoste> usagesDe(
    ElementId element,
    List<IntervalleDUnJour> intervalles,
    List<PointageDElement> pointagesDElement
  ) {
    Stream<UsageDePoste> parTravail = intervalles
      .stream()
      .map(IntervalleDUnJour::intervalle)
      .filter(intervalle -> intervalle.activite().element().equals(element))
      .map(intervalle -> new UsageDePoste(intervalle.plage().debut(), intervalle.activite().poste(), intervalle.activite().nature()));
    Stream<UsageDePoste> parPointage = pointagesDElement
      .stream()
      .filter(pointage -> pointage.element().equals(element))
      .map(pointage -> new UsageDePoste(pointage.dateDeSurvenue(), pointage.poste(), pointage.nature()));

    return Stream.concat(parTravail, parPointage).sorted(Comparator.comparing(UsageDePoste::date)).toList();
  }

  /**
   * Un couple par poste et nature distincts, dans l'ordre de premiere apparition. Ce qui est pointe sans poste n'en
   * donne aucun.
   */
  private static List<PosteDeLElement> postesDeLElement(List<UsageDePoste> usages, Map<PosteDeTravailId, PosteConnu> connus) {
    return usages
      .stream()
      .flatMap(usage ->
        usage
          .poste()
          .map(connus::get)
          .map(poste -> new PosteDeLElement(poste, usage.nature()))
          .stream()
      )
      .distinct()
      .toList();
  }

  private record UsageDePoste(Instant date, Optional<PosteDeTravailId> poste, Optional<NatureDOperation> nature) {}

  private static DureeTotale somme(List<IntervalleDActivite> travail, Predicate<IntervalleDActivite> retenu) {
    return DureeTotale.de(
      travail
        .stream()
        .filter(retenu)
        .map(IntervalleDActivite::plage)
        .filter(plage -> !plage.estOuverte())
        .map(SynthesesDesHeuresService::duree)
        .reduce(Duration.ZERO, Duration::plus)
    );
  }

  private static Duration duree(Plage plage) {
    return Duration.between(plage.debut(), plage.fin().orElseThrow());
  }

  private LocalDate jourDe(PointageDElement pointage) {
    return LocalDate.ofInstant(pointage.dateDeSurvenue(), fuseau.zone());
  }

  public interface SynthesesDesHeuresServiceOperateursBuilder {
    SynthesesDesHeuresServiceFuseauBuilder operateurs(OperateursConnus operateurs);
  }

  public interface SynthesesDesHeuresServiceFuseauBuilder {
    SynthesesDesHeuresServiceActivitesBuilder fuseau(FuseauHoraireDeLEntreprise fuseau);
  }

  public interface SynthesesDesHeuresServiceActivitesBuilder {
    SynthesesDesHeuresServiceJournalBuilder activites(ActivitesDeLOperateur activites);
  }

  public interface SynthesesDesHeuresServiceJournalBuilder {
    SynthesesDesHeuresServiceElementsBuilder journal(JournalDeLOperateur journal);
  }

  public interface SynthesesDesHeuresServiceElementsBuilder {
    SynthesesDesHeuresServicePostesBuilder elements(ElementsDeFabrication elements);
  }

  public interface SynthesesDesHeuresServicePostesBuilder {
    SynthesesDesHeuresServiceClockBuilder postes(PostesDeTravail postes);
  }

  public interface SynthesesDesHeuresServiceClockBuilder {
    SynthesesDesHeuresService clock(Clock clock);
  }
}
