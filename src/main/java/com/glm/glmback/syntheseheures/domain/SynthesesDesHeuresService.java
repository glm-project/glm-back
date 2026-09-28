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

/**
 * Assemble le releve hebdomadaire d'un operateur a partir des journaux de l'atelier.
 *
 * <p>
 * Rien n'est stocke, tout est recalcule : les journees qui recouvrent la semaine sont repliees en pointages et
 * fenetres de presence, puis chacun est ramene aux jours du calendrier qu'il traverse. Un pointage fautif n'empeche
 * jamais la lecture — voir {@link JourneeDeTravail}, dont le repli est tolerant.
 * </p>
 *
 * <p>
 * Le temps operationnel suit le meme chemin : les suivis de l'operateur sont replies en intervalles d'activite, reduits
 * aux fenetres de la journee ou chacun a commence, puis coupes aux memes minuits. Ses durees se cumulent par element.
 * </p>
 */
public final class SynthesesDesHeuresService {

  /**
   * A instant egal : l'arrivee, puis les pointages d'element, puis le depart. Le tri est stable : il garde l'ordre des
   * pointages d'element, deja ranges par {@link #PAR_ELEMENT}.
   */
  private static final Comparator<PointageDuJour> PAR_ORDRE_DU_JOURNAL = Comparator.comparing(
    PointageDuJour::dateDeSurvenue
  ).thenComparingInt(SynthesesDesHeuresService::rang);

  /**
   * A instant egal, deux elements se departagent par leur identifiant, pour que deux lectures rendent le meme ordre ;
   * sur un meme element, l'ordre du journal est garde.
   */
  private static final Comparator<PointageDElement> PAR_ELEMENT = Comparator.comparing(PointageDElement::dateDeSurvenue).thenComparing(
    pointage -> pointage.element().uuid()
  );

  private static final int RANG_ARRIVEE = 0;
  private static final int RANG_ELEMENT = 1;
  private static final int RANG_DEPART = 2;

  private final PresenceDeLOperateur presences;
  private final OperateursConnus operateurs;
  private final FuseauHoraireDeLEntreprise fuseau;
  private final SeuilDAmplitude seuil;
  private final PointagesDAtelier pointages;
  private final TravailDeLOperateur travail;
  private final ElementsDeFabrication elements;
  private final PostesDeTravail postes;
  private final Clock clock;

  private SynthesesDesHeuresService(
    PresenceDeLOperateur presences,
    OperateursConnus operateurs,
    FuseauHoraireDeLEntreprise fuseau,
    SeuilDAmplitude seuil,
    PointagesDAtelier pointages,
    TravailDeLOperateur travail,
    ElementsDeFabrication elements,
    PostesDeTravail postes,
    Clock clock
  ) {
    this.presences = presences;
    this.operateurs = operateurs;
    this.fuseau = fuseau;
    this.seuil = seuil;
    this.pointages = pointages;
    this.travail = travail;
    this.elements = elements;
    this.postes = postes;
    this.clock = clock;
  }

  public static SynthesesDesHeuresServicePresencesBuilder builder() {
    return presences ->
      operateurs ->
        fuseau ->
          seuil ->
            pointages ->
              travail ->
                elements ->
                  postes ->
                    clock ->
                      new SynthesesDesHeuresService(presences, operateurs, fuseau, seuil, pointages, travail, elements, postes, clock);
  }

  public SyntheseDesHeures synthese(OperateurId operateur, SemaineCalendaire semaine) {
    OperateurConnu connu = operateurs.get(operateur).orElseThrow(() -> new OperateurInconnuException(operateur));
    DecoupageCalendaire decoupage = new DecoupageCalendaire(semaine, fuseau.zone());
    Instant maintenant = clock.now();
    AmplitudeMaximale amplitude = seuil.amplitudeMaximale();
    List<JourneeDeTravail> journees = presences
      .journeesRecouvrant(operateur, decoupage.debut(), decoupage.finExclusive())
      .stream()
      .map(journee -> lue(operateur, journee, maintenant, amplitude))
      .toList();
    List<SuiviDuTravail> suivis = travail.suivis(operateur, depuis(journees, decoupage), decoupage.finExclusive());
    ReductionALaPresence reduction = new ReductionALaPresence(journees);
    List<IntervalleDUnJour> intervalles = suivis
      .stream()
      .flatMap(suivi -> suivi.intervalles().stream())
      .flatMap(intervalle -> reduction.reduit(intervalle).stream())
      .flatMap(intervalle -> decoupage.intervalles(intervalle).stream())
      .toList();
    List<PointageDElement> pointagesDElement = suivis
      .stream()
      .flatMap(suivi -> suivi.pointages().stream())
      .filter(pointage -> decoupage.jours().contains(jourDe(pointage)))
      .sorted(PAR_ELEMENT)
      .toList();

    return SyntheseDesHeures.builder()
      .operateur(connu)
      .semaine(semaine)
      .jours(jours(decoupage, journees, intervalles, pointagesDElement))
      .elements(elementsDeLaSemaine(suivis, intervalles, pointagesDElement));
  }

  /**
   * Les suivis se cherchent depuis la plus precoce des arrivees, ou le lundi s'il est anterieur : un poste de nuit
   * arrive le dimanche a pu demarrer son travail avant minuit.
   */
  private static Instant depuis(List<JourneeDeTravail> journees, DecoupageCalendaire decoupage) {
    return journees
      .stream()
      .flatMap(journee -> journee.arrivee().stream())
      .filter(arrivee -> arrivee.isBefore(decoupage.debut()))
      .min(Comparator.naturalOrder())
      .orElse(decoupage.debut());
  }

  private List<JourDeSynthese> jours(
    DecoupageCalendaire decoupage,
    List<JourneeDeTravail> journees,
    List<IntervalleDUnJour> intervalles,
    List<PointageDElement> pointagesDElement
  ) {
    Map<LocalDate, List<PointageDuJour>> pointagesParJour = pointagesParJour(journees, pointagesDElement, decoupage);
    List<PlageDUnJour> presence = journees
      .stream()
      .flatMap(journee -> journee.fenetres().stream())
      .flatMap(fenetre -> decoupage.plages(fenetre).stream())
      .toList();
    Map<LocalDate, Duration> dureeParJour = dureeParJour(presence.stream(), PlageDUnJour::jour, PlageDUnJour::plage, false);
    Map<LocalDate, Duration> dureePresumeeParJour = dureeParJour(presence.stream(), PlageDUnJour::jour, PlageDUnJour::plage, true);
    Map<LocalDate, Duration> operationnelleParJour = dureeParJour(
      intervalles.stream(),
      IntervalleDUnJour::jour,
      SynthesesDesHeuresService::plageDe,
      false
    );
    Map<LocalDate, Duration> operationnellePresumeeParJour = dureeParJour(
      intervalles.stream(),
      IntervalleDUnJour::jour,
      SynthesesDesHeuresService::plageDe,
      true
    );

    return decoupage
      .jours()
      .stream()
      .map(jour ->
        JourDeSynthese.builder()
          .jour(jour)
          .pointages(pointagesParJour.getOrDefault(jour, List.of()))
          .duree(dureeParJour.getOrDefault(jour, Duration.ZERO))
          .dureePresumee(dureePresumeeParJour.getOrDefault(jour, Duration.ZERO))
          .dureeOperationnelle(operationnelleParJour.getOrDefault(jour, Duration.ZERO))
          .dureeOperationnellePresumee(operationnellePresumeeParJour.getOrDefault(jour, Duration.ZERO))
      )
      .toList();
  }

  /**
   * Le journal brut de chaque jour : la presence, et tous les pointages d'element de l'operateur dates de ce jour, meme
   * hors de toute journee.
   */
  private Map<LocalDate, List<PointageDuJour>> pointagesParJour(
    List<JourneeDeTravail> journees,
    List<PointageDElement> pointagesDElement,
    DecoupageCalendaire decoupage
  ) {
    Stream<PointageDuJour> presence = journees
      .stream()
      .<PointageDuJour>flatMap(journee -> journee.pointages().stream())
      .filter(evenement -> decoupage.jours().contains(jourDe(evenement)));

    return Stream.concat(presence, pointagesDElement.stream()).sorted(PAR_ORDRE_DU_JOURNAL).collect(Collectors.groupingBy(this::jourDe));
  }

  /**
   * La duree pointee ou presumee de chaque jour : seules les plages closes comptent, une plage encore ouverte ne
   * contribuant rien tant qu'elle n'est ni fermee ni presumee.
   */
  private static <T> Map<LocalDate, Duration> dureeParJour(
    Stream<T> plages,
    Function<T, LocalDate> jour,
    Function<T, Plage> plage,
    boolean presumee
  ) {
    return plages
      .filter(comptee(plage, presumee))
      .collect(Collectors.groupingBy(jour, Collectors.reducing(Duration.ZERO, element -> duree(plage.apply(element)), Duration::plus)));
  }

  /**
   * Les elements touches dans la semaine, par premiere apparition puis par nom, avec leur fiche relue au referentiel.
   */
  private List<ElementDeLaSynthese> elementsDeLaSemaine(
    List<SuiviDuTravail> suivis,
    List<IntervalleDUnJour> intervalles,
    List<PointageDElement> pointagesDElement
  ) {
    Map<ElementId, ElementEngage> engages = suivis
      .stream()
      .map(SuiviDuTravail::element)
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
      .duree(somme(travail, intervalle -> true, false))
      .dureeNonConformite(somme(travail, intervalle -> intervalle.activite().categorie() == CategorieDActivite.NON_CONFORMITE, false))
      .dureePresumee(somme(travail, intervalle -> true, true))
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

  private static Duration somme(List<IntervalleDActivite> travail, Predicate<IntervalleDActivite> retenu, boolean presumee) {
    return travail
      .stream()
      .filter(retenu)
      .map(IntervalleDActivite::plage)
      .filter(comptee(Function.identity(), presumee))
      .map(SynthesesDesHeuresService::duree)
      .reduce(Duration.ZERO, Duration::plus);
  }

  private static <T> Predicate<T> comptee(Function<T, Plage> plage, boolean presumee) {
    return element -> !plage.apply(element).estOuverte() && plage.apply(element).presumee() == presumee;
  }

  private static Plage plageDe(IntervalleDUnJour intervalle) {
    return intervalle.intervalle().plage();
  }

  private static Duration duree(Plage plage) {
    return Duration.between(plage.debut(), plage.fin().orElseThrow());
  }

  private static int rang(PointageDuJour pointage) {
    return switch (pointage) {
      case EvenementDePresence evenement when evenement.type() == TypeDEvenementDePresence.ARRIVEE -> RANG_ARRIVEE;
      case EvenementDePresence evenement -> RANG_DEPART;
      case PointageDElement element -> RANG_ELEMENT;
    };
  }

  private LocalDate jourDe(PointageDuJour pointage) {
    return LocalDate.ofInstant(pointage.dateDeSurvenue(), fuseau.zone());
  }

  /**
   * La journee telle qu'on la lit maintenant : une journee abandonnee, ou fermee plus de 24 h apres son arrivee, est
   * fermee a sa fin presumee, et le dernier pointage d'OF de l'operateur n'est demande que pour elle.
   */
  private JourneeDeTravail lue(OperateurId operateur, JourneeDeTravail journee, Instant maintenant, AmplitudeMaximale amplitude) {
    if (!journee.estPresumeePour(maintenant, amplitude)) {
      return journee;
    }

    return journee.presumee(amplitude, pointages.dernierPointage(operateur, journee.fenetreDeRecherche(amplitude).orElseThrow()));
  }

  public interface SynthesesDesHeuresServicePresencesBuilder {
    SynthesesDesHeuresServiceOperateursBuilder presences(PresenceDeLOperateur presences);
  }

  public interface SynthesesDesHeuresServiceOperateursBuilder {
    SynthesesDesHeuresServiceFuseauBuilder operateurs(OperateursConnus operateurs);
  }

  public interface SynthesesDesHeuresServiceFuseauBuilder {
    SynthesesDesHeuresServiceSeuilBuilder fuseau(FuseauHoraireDeLEntreprise fuseau);
  }

  public interface SynthesesDesHeuresServiceSeuilBuilder {
    SynthesesDesHeuresServicePointagesBuilder seuil(SeuilDAmplitude seuil);
  }

  public interface SynthesesDesHeuresServicePointagesBuilder {
    SynthesesDesHeuresServiceTravailBuilder pointages(PointagesDAtelier pointages);
  }

  public interface SynthesesDesHeuresServiceTravailBuilder {
    SynthesesDesHeuresServiceElementsBuilder travail(TravailDeLOperateur travail);
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
