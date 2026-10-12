package com.glm.glmback.atelier.domain;

import static com.glm.glmback.atelier.domain.AtelierFixture.*;
import static org.assertj.core.api.Assertions.*;

import com.glm.glmback.UnitTest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Ce que les corrections du gestionnaire changent a la lecture du journal : un retrait ecarte l'activite visee, un
 * changement de poste la fait compter sur un autre poste. Aucun fait n'est modifie, et la lecture par cle d'origine
 * reste la seule interpretation des gestes.
 */
@UnitTest
class JournalDAtelierCorrectionsTest {

  private static final Instant LE_10_MAI_2026_A_10H = Instant.parse("2026-05-10T10:00:00Z");
  private static final Instant LE_10_MAI_2026_A_11H = Instant.parse("2026-05-10T11:00:00Z");
  private static final Instant LE_10_MAI_2026_A_14H = Instant.parse("2026-05-10T14:00:00Z");
  private static final Instant LE_10_MAI_2026_A_15H = Instant.parse("2026-05-10T15:00:00Z");
  private static final Instant LE_11_MAI_2026_A_10H = Instant.parse("2026-05-11T10:00:00Z");

  private final EvenementDAtelier travail = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);

  private static List<Activite> activites(EvenementDAtelier... faits) {
    return new JournalDAtelier(List.of(faits)).activites(Optional.empty());
  }

  @Test
  void shouldEcarterUneActiviteTerminee() {
    List<Activite> activites = activites(
      travail,
      finDe(travail).a(LE_10_MAI_2026_A_12H),
      retraitParLeroyDe(travail).a(LE_11_MAI_2026_A_9H)
    );

    assertThat(activites).isEmpty();
  }

  @Test
  void shouldEcarterUneFinAutomatique() {
    assertThat(activites(travail, retraitParLeroyDe(travail).a(LE_11_MAI_2026_A_9H))).isEmpty();
  }

  /**
   * Un retrait ne ferme rien et n'ouvre rien : les autres activites de la meme cle, meme restees en cours, sont lues
   * comme avant.
   */
  @Test
  void shouldLaisserLesAutresActivitesDeLaCle() {
    EvenementDAtelier suivante = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_14H);

    List<Activite> activites = activites(
      travail,
      finDe(travail).a(LE_10_MAI_2026_A_12H),
      suivante,
      retraitParLeroyDe(travail).a(LE_10_MAI_2026_A_15H)
    );

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.ouvrant()).isEqualTo(suivante);
        assertThat(activite.fin()).isEmpty();
      });
  }

  @Test
  void shouldFaireCompterUneActiviteSurLePosteDeSonChangement() {
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier changement = changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_9H);

    List<Activite> activites = activites(travail, fin, changement);

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.cle()).isEqualTo(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2)));
        assertThat(activite.porteur()).isEqualTo(changement);
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_12H);
        assertThat(activite.evenementDeFin()).contains(fin);
      });
  }

  /**
   * La fin d'une regularisation ecrite apres un changement de poste porte la cle du nouveau poste, mais elle vise
   * toujours l'activite : elle la ferme, quelle que soit la cle sur laquelle elle est ecrite.
   */
  @Test
  void shouldFermerParUneFinRegulariseeEcriteSurLeNouveauPoste() {
    EvenementDAtelier changement = changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_8H);
    EvenementDAtelier fin = finRegulariseeSurFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_9H);

    List<Activite> activites = activites(travail, changement, fin);

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).contains(LE_11_MAI_2026_A_9H);
        assertThat(activite.evenementDeFin()).contains(fin);
        assertThat(activite.cle().poste()).contains(POSTE_ID_FRAISEUSE_2);
      });
  }

  /**
   * Les gestes restent lus sur la cle ou ils ont ete pointes : apres le changement, un debut pointe sur l'ancien poste
   * ouvre une activite distincte, et la fin pointee ensuite ferme la sienne, pas celle qui a change de poste.
   */
  @Test
  void shouldLireLesGestesSurLaCleOuIlsOntEtePointes() {
    EvenementDAtelier fin = finDe(travail).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier suivante = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_14H);
    EvenementDAtelier finSuivante = finDe(suivante).a(LE_10_MAI_2026_A_15H);

    List<Activite> activites = activites(
      travail,
      fin,
      suivante,
      finSuivante,
      changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_9H)
    );

    assertThat(activites)
      .extracting(Activite::cle)
      .containsExactly(new CleDActivite(OPERATEUR_ID_DUPONT, Optional.of(POSTE_ID_FRAISEUSE_2)), cleDeFraiseuse1DeDupont());
    assertThat(activites).extracting(Activite::evenementDeFin).containsExactly(Optional.of(fin), Optional.of(finSuivante));
  }

  @Test
  void shouldRetenirLeDernierChangementDePoste() {
    EvenementDAtelier premier = changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_8H);
    EvenementDAtelier dernier = changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_10H);

    assertThat(activites(travail, finDe(travail).a(LE_10_MAI_2026_A_12H), dernier, premier))
      .singleElement()
      .extracting(Activite::porteur)
      .isEqualTo(dernier);
  }

  /**
   * Une fin regularisee qui vise une activite que le journal n'a jamais ouverte n'a aucun effet, comme avant.
   */
  @Test
  void shouldIgnorerUneFinRegulariseeSurUneActiviteInconnue() {
    EvenementDAtelier orpheline = finRegulariseeParLeroyDe(debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_7H)).a(LE_10_MAI_2026_A_9H);

    assertThat(activites(travail, orpheline))
      .singleElement()
      .satisfies(activite -> assertThat(activite.fin()).isEmpty());
  }

  @Test
  void shouldTerminerParLaClotureUneActiviteDontLePosteAChange() {
    Instant cloture = LE_10_MAI_2026_A_12H;

    List<Activite> activites = new JournalDAtelier(
      List.of(travail, changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_10_MAI_2026_A_9H))
    ).activites(Optional.of(cloture));

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).contains(cloture);
        assertThat(activite.evenementDeFin()).isEmpty();
      });
  }

  /**
   * Une activite ajoutee sur la cle qu'une autre activite a quittee ne prend pas la fin pointee de celle-ci : la fin reste
   * liee a l'activite qu'elle a fermee, quels que soient les gestes qui s'ouvrent ensuite sur l'ancienne cle.
   */
  @Test
  void shouldGarderLaFinPointeeDUneActiviteDeplaceeQuandOnAjouteSurSonAncienPoste() {
    EvenementDAtelier fin = finPointeeDe(travail).a(LE_10_MAI_2026_A_10H);
    EvenementDAtelier ajoutee = debutAjouteParLeroySurFraiseuse1ParDupontA(LE_10_MAI_2026_A_9H);
    EvenementDAtelier finAjoutee = finRegulariseeParLeroyDe(ajoutee).a(LE_10_MAI_2026_A_9H.plusSeconds(1800));

    List<Activite> activites = activites(
      travail,
      fin,
      changementDePosteVersFraiseuse2ParLeroyDe(travail).a(LE_11_MAI_2026_A_8H),
      ajoutee,
      finAjoutee
    );

    assertThat(activites).hasSize(2);
    assertThat(activites.get(0).ouvrant()).isEqualTo(travail);
    assertThat(activites.get(0).fin()).contains(LE_10_MAI_2026_A_10H);
    assertThat(activites.get(0).evenementDeFin()).contains(fin);
    assertThat(activites.get(0).cle().poste()).contains(POSTE_ID_FRAISEUSE_2);
    assertThat(activites.get(1).ouvrant()).isEqualTo(ajoutee);
    assertThat(activites.get(1).fin()).contains(LE_10_MAI_2026_A_9H.plusSeconds(1800));
  }

  /**
   * Un pointage recu apres un retrait, dans la plage de l'activite retiree, ouvre une activite que la fin pointee de
   * l'activite retiree ne ferme pas : cette fin a ete recue avant lui, elle reste liee a l'activite qu'elle a fermee.
   */
  @Test
  void shouldNePasFermerUnPointageRecuApresUnRetraitParLaFinDeLActiviteRetiree() {
    EvenementDAtelier retiree = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier recu = debutSurFraiseuse1ParDupontRecuLe(LE_11_MAI_2026_A_9H.plusSeconds(60)).a(LE_10_MAI_2026_A_10H);

    List<Activite> activites = activites(
      retiree,
      finPointeeDe(retiree).a(LE_10_MAI_2026_A_12H),
      retraitParLeroyDe(retiree).a(LE_11_MAI_2026_A_9H),
      recu
    );

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.ouvrant()).isEqualTo(recu);
        assertThat(activite.fin()).isEmpty();
      });
  }

  @Test
  void shouldNePasRefermerParLaClotureUneActiviteQueUneFinRegulariseeAvaitDejaTerminee() {
    Instant cloture = LE_10_MAI_2026_A_16H;
    EvenementDAtelier regularisee = finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_12H);

    List<Activite> activites = new JournalDAtelier(List.of(travail, regularisee)).activites(Optional.of(cloture));

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_12H);
        assertThat(activite.evenementDeFin()).contains(regularisee);
      });
  }

  /**
   * Le serveur lie la fin pointee a l'activite qu'il jugeait en cours : la lecture ne la devine plus. Ici l'activite de la
   * fraiseuse 1 a ete amenee sur la fraiseuse 2, ou une autre activite avait deja sa fin regularisee ; la fin pointee tardive
   * ferme celle qu'on a amenee, et laisse a l'autre sa fin.
   */
  @Test
  void shouldFermerLActiviteAmeneeSurLaCleEtLaisserSaFinARegularisee() {
    EvenementDAtelier deja = debutSurFraiseuse2ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier finRegularisee = finRegulariseeSurFraiseuse2ParLeroyDe(deja).a(LE_10_MAI_2026_A_9H);
    EvenementDAtelier amenee = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    EvenementDAtelier finTardive = finPointeeSurFraiseuse2De(amenee).a(LE_10_MAI_2026_A_11H);

    List<Activite> activites = activites(
      deja,
      finRegularisee,
      amenee,
      changementDePosteVersFraiseuse2ParLeroyDe(amenee).a(LE_11_MAI_2026_A_8H),
      finTardive
    );

    assertThat(activites).hasSize(2);
    assertThat(activites.get(0).ouvrant()).isEqualTo(deja);
    assertThat(activites.get(0).fin()).contains(LE_10_MAI_2026_A_9H);
    assertThat(activites.get(0).evenementDeFin()).contains(finRegularisee);
    assertThat(activites.get(1).ouvrant()).isEqualTo(amenee);
    assertThat(activites.get(1).fin()).contains(LE_10_MAI_2026_A_11H);
    assertThat(activites.get(1).evenementDeFin()).contains(finTardive);
  }

  @Test
  void shouldFermerLActiviteAmeneeSurLaCleSansAutreActiviteSurCetteCle() {
    EvenementDAtelier amenee = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_10H);
    EvenementDAtelier finTardive = finPointeeSurFraiseuse2De(amenee).a(LE_10_MAI_2026_A_11H);

    assertThat(activites(amenee, changementDePosteVersFraiseuse2ParLeroyDe(amenee).a(LE_11_MAI_2026_A_8H), finTardive))
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_11H);
        assertThat(activite.a(LE_11_MAI_2026_A_9H).finAutomatique()).isFalse();
      });
  }

  /**
   * La lecture ne depend plus de la date d'enregistrement : ici la fin de l'activite retiree est enregistree apres le debut
   * recu depuis, alors que le serveur l'avait jugee avant. Elle reste liee a son activite.
   */
  @Test
  void shouldLireUneFinLieeSansRegarderLOrdreDEnregistrement() {
    EvenementDAtelier retiree = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_8H);
    EvenementDAtelier recu = debutSurFraiseuse1ParDupontRecuLe(LE_11_MAI_2026_A_9H).a(LE_10_MAI_2026_A_10H);
    EvenementDAtelier finEnregistreeApres = finPointeeRecueLe11MaiDe(retiree).a(LE_10_MAI_2026_A_12H);

    List<Activite> activites = activites(
      retiree,
      recu,
      finEnregistreeApres,
      retraitParLeroyDe(retiree).a(LE_11_MAI_2026_A_9H.plusSeconds(1))
    );

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.ouvrant()).isEqualTo(recu);
        assertThat(activite.fin()).isEmpty();
      });
  }

  /**
   * Les fins pointees d'avant la liaison ne portent aucune cible : elles se lisent comme toujours, par la cle et l'ordre
   * des heures de geste, quel que soit l'ordre dans lequel le journal les a enregistrees.
   */
  @Test
  void shouldLireUneFinSansCibleParLaCleEtLOrdreDesGestes() {
    EvenementDAtelier ouverture = debutSurFraiseuse1ParDupontRecuLe(LE_11_MAI_2026_A_9H).a(LE_10_MAI_2026_A_8H);
    EvenementDAtelier fin = finDe(ouverture).a(LE_10_MAI_2026_A_12H);
    EvenementDAtelier suivante = debutSurFraiseuse1ParDupontA(LE_10_MAI_2026_A_14H);

    List<Activite> activites = activites(suivante, fin, ouverture);

    assertThat(activites).extracting(Activite::fin).containsExactly(Optional.of(LE_10_MAI_2026_A_12H), Optional.empty());
    assertThat(activites.getFirst().evenementDeFin()).contains(fin);
  }

  /**
   * Une fin ne remplace jamais une fin deja posee : une fin sans cible, lue par la cle, ne reprend pas l'activite dont le
   * gestionnaire a regularise la fin.
   */
  @Test
  void shouldNePasRemplacerLaFinRegulariseeParUneFinSansCible() {
    EvenementDAtelier regularisee = finRegulariseeParLeroyDe(travail).a(LE_10_MAI_2026_A_9H);

    List<Activite> activites = activites(travail, regularisee, finDe(travail).a(LE_10_MAI_2026_A_12H));

    assertThat(activites)
      .singleElement()
      .satisfies(activite -> {
        assertThat(activite.fin()).contains(LE_10_MAI_2026_A_9H);
        assertThat(activite.evenementDeFin()).contains(regularisee);
      });
  }
}
