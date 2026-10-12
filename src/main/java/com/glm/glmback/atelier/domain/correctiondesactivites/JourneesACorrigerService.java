package com.glm.glmback.atelier.domain.correctiondesactivites;

import com.glm.glmback.atelier.domain.AnnuaireDAtelier;
import com.glm.glmback.atelier.domain.ElementEngageId;
import com.glm.glmback.atelier.domain.Habilitations;
import com.glm.glmback.atelier.domain.OperateurConnu;
import com.glm.glmback.atelier.domain.OperateurDAtelierIntrouvableException;
import com.glm.glmback.atelier.domain.OperateurId;
import com.glm.glmback.atelier.domain.OperateursConnus;
import com.glm.glmback.atelier.domain.PosteConnu;
import com.glm.glmback.atelier.domain.PosteDeTravailId;
import com.glm.glmback.atelier.domain.PostesConnus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * La lecture de la journee d'un operateur pour le gestionnaire : les activites qui touchent le jour, coupees aux minuits
 * de l'entreprise, et les choix d'un ajout.
 *
 * <p>
 * Les minuits viennent du fuseau de l'entreprise, jamais du client. Les activites sont lues sur la projection de
 * l'atelier, donc apres les retraits et les changements de poste ; les ressources (operateur, postes, fiches des
 * elements) sont resolues en un aller par port. Un operateur inconnu est refuse.
 * </p>
 */
public final class JourneesACorrigerService {

  private final OperateursConnus operateurs;
  private final PostesConnus postes;
  private final Habilitations habilitations;
  private final ActivitesDeLOperateur activites;
  private final SuivisPresents suivisPresents;
  private final FichesDElement fiches;
  private final FuseauHoraireDeLEntreprise fuseau;

  private JourneesACorrigerService(
    OperateursConnus operateurs,
    PostesConnus postes,
    Habilitations habilitations,
    ActivitesDeLOperateur activites,
    SuivisPresents suivisPresents,
    FichesDElement fiches,
    FuseauHoraireDeLEntreprise fuseau
  ) {
    this.operateurs = operateurs;
    this.postes = postes;
    this.habilitations = habilitations;
    this.activites = activites;
    this.suivisPresents = suivisPresents;
    this.fiches = fiches;
    this.fuseau = fuseau;
  }

  public static JourneesACorrigerServiceOperateursBuilder builder() {
    return operateurs ->
      postes ->
        habilitations ->
          activites ->
            suivisPresents ->
              fiches ->
                fuseau -> new JourneesACorrigerService(operateurs, postes, habilitations, activites, suivisPresents, fiches, fuseau);
  }

  /**
   * La journee de l'operateur a l'instant d'evaluation donne, qui decide si une activite sans fin est en cours ou
   * terminee automatiquement.
   */
  public JourneeACorriger lit(OperateurId id, LocalDate jour, Instant evaluation) {
    OperateurConnu operateur = operateurs.get(id).orElseThrow(() -> new OperateurDAtelierIntrouvableException(id));
    DecoupageParJour decoupage = new DecoupageParJour(fuseau.zone());
    BornesDuJour bornes = new BornesDuJour(
      decoupage.debutDe(jour),
      decoupage.debutDe(jour.plusDays(1)),
      decoupage.debutDe(jour.plusDays(2))
    );
    List<ActiviteProjetee> touchees = activites.touchant(id, bornes.debut(), bornes.fin());
    List<SuiviPresent> presents = suivisPresents.entre(bornes.debut(), bornes.fin());
    List<PosteDeTravailId> habilites = habilitations.postesHabilites(id);
    AnnuaireDAtelier annuaire = annuaire(operateur, touchees, habilites);

    List<ActiviteDeLaJournee> activitesDeLaJournee = touchees
      .stream()
      .map(activite -> activiteDeLaJournee(activite, annuaire, decoupage, jour, evaluation))
      .toList();
    Duration dureeOperationnelle = activitesDeLaJournee
      .stream()
      .flatMap(activite -> activite.dureeSurLeJour().stream())
      .reduce(Duration.ZERO, Duration::plus);

    return JourneeACorriger.builder()
      .operateur(operateur)
      .jour(jour)
      .bornes(bornes)
      .dureeOperationnelle(dureeOperationnelle)
      .activites(activitesDeLaJournee)
      .choix(new ChoixDAjout(presents, postesHabilites(habilites, annuaire)))
      .fiches(fiches.parIds(elementsDe(touchees, presents)))
      .evaluation(evaluation);
  }

  private ActiviteDeLaJournee activiteDeLaJournee(
    ActiviteProjetee activite,
    AnnuaireDAtelier annuaire,
    DecoupageParJour decoupage,
    LocalDate jour,
    Instant evaluation
  ) {
    EtatDActiviteDeLaJournee etat = activite.etatA(evaluation);
    List<PortionDeJour> portions =
      etat == EtatDActiviteDeLaJournee.EN_COURS ? List.of() : decoupage.portions(activite.debut(), activite.finRetenue());

    return ActiviteDeLaJournee.builder()
      .id(activite.id())
      .suivi(activite.suivi())
      .element(activite.element())
      .poste(activite.poste().flatMap(annuaire::poste))
      .categorie(activite.categorie())
      .debut(activite.debut())
      .fin(etat == EtatDActiviteDeLaJournee.EN_COURS ? Optional.empty() : Optional.of(activite.finRetenue()))
      .etat(etat)
      .dureeSurLeJour(etat == EtatDActiviteDeLaJournee.EN_COURS ? Optional.empty() : Optional.of(dureeDu(jour, portions)))
      .autresJours(
        portions
          .stream()
          .filter(portion -> !portion.jour().equals(jour))
          .toList()
      );
  }

  /**
   * La portion comptee ce jour-la, nulle quand l'activite ne le touche pas.
   */
  private static Duration dureeDu(LocalDate jour, List<PortionDeJour> portions) {
    return portions
      .stream()
      .filter(portion -> portion.jour().equals(jour))
      .map(PortionDeJour::duree)
      .findFirst()
      .orElse(Duration.ZERO);
  }

  private AnnuaireDAtelier annuaire(OperateurConnu operateur, List<ActiviteProjetee> touchees, List<PosteDeTravailId> habilites) {
    Set<PosteDeTravailId> aResoudre = new HashSet<>(habilites);
    touchees.forEach(activite -> activite.poste().ifPresent(aResoudre::add));

    return AnnuaireDAtelier.de(List.of(operateur), postes.parIds(aResoudre));
  }

  private static List<PosteConnu> postesHabilites(List<PosteDeTravailId> habilites, AnnuaireDAtelier annuaire) {
    return habilites
      .stream()
      .flatMap(poste -> annuaire.poste(poste).stream())
      .sorted(Comparator.comparing((PosteConnu poste) -> poste.libelle().value()).thenComparing(poste -> poste.id().uuid()))
      .toList();
  }

  private static Set<ElementEngageId> elementsDe(List<ActiviteProjetee> touchees, List<SuiviPresent> presents) {
    return Stream.concat(
      touchees.stream().map(activite -> activite.element().id()),
      presents.stream().map(suivi -> suivi.element().id())
    ).collect(Collectors.toSet());
  }

  public interface JourneesACorrigerServiceOperateursBuilder {
    JourneesACorrigerServicePostesBuilder operateurs(OperateursConnus operateurs);
  }

  public interface JourneesACorrigerServicePostesBuilder {
    JourneesACorrigerServiceHabilitationsBuilder postes(PostesConnus postes);
  }

  public interface JourneesACorrigerServiceHabilitationsBuilder {
    JourneesACorrigerServiceActivitesBuilder habilitations(Habilitations habilitations);
  }

  public interface JourneesACorrigerServiceActivitesBuilder {
    JourneesACorrigerServiceSuivisPresentsBuilder activites(ActivitesDeLOperateur activites);
  }

  public interface JourneesACorrigerServiceSuivisPresentsBuilder {
    JourneesACorrigerServiceFichesBuilder suivisPresents(SuivisPresents suivisPresents);
  }

  public interface JourneesACorrigerServiceFichesBuilder {
    JourneesACorrigerServiceFuseauBuilder fiches(FichesDElement fiches);
  }

  public interface JourneesACorrigerServiceFuseauBuilder {
    JourneesACorrigerService fuseau(FuseauHoraireDeLEntreprise fuseau);
  }
}
