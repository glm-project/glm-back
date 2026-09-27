#!/usr/bin/env python3
"""
Generateur du jeu de donnees de recette de l'entreprise impeccmold : un moulier de la Plastics Vallee (Oyonnax),
vingt operateurs, cinq semaines d'atelier a la reprise des conges d'ete (semaines ISO 35 a 39 de 2026).

Le script est deterministe (graine fixe) et produit deux fichiers a cote de lui :

- impeccmold.sql : le jeu de donnees, a charger dans le schema impeccmold ;
- ATTENDUS.md    : les valeurs que l'application doit rendre (cout de revient, releve d'heures, anomalies, etats).

Il rejoue les deux automates du domaine (presence et activite) sur chaque journal avant de l'ecrire, calcule les
colonnes de projection comme le font les adapters, et refuse de produire un jeu qui chevaucherait deux journees d'un
meme operateur ou daterait un evenement hors de son suivi. Les valeurs attendues sont recalculees ici selon les
regles de documentation/contexte-metier.md, independamment du code Java : c'est ce qui en fait un controle.

Usage : python3 generer.py
"""

from __future__ import annotations

import random
import uuid
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import date, datetime, time, timedelta, timezone
from decimal import ROUND_HALF_UP, Decimal
from pathlib import Path
from zoneinfo import ZoneInfo

PARIS = ZoneInfo("Europe/Paris")
UTC = timezone.utc
ICI = Path(__file__).resolve().parent

SEUIL = timedelta(minutes=780)  # parametrage.amplitude_maximale_minutes, valeur semee par le changelog
POINTEUR = "user.impeccmold"  # le jeton du pupitre
GESTIONNAIRE = "gestionnaire.impeccmold"

# Instant de reference des valeurs attendues : tout le jeu date d'avant, aucune valeur passee n'en depend. Seul le
# bloc « situation du jour », ecrit relativement a now(), en depend, et il est decrit a part.
MAINTENANT = datetime(2026, 9, 27, 12, 0, tzinfo=PARIS)

random.seed(20260824)


def heure(jour: date, hhmm: str) -> datetime:
    """Une heure locale de Paris, avec secondes et fraction facultatives (« 19:00:00.5 »)."""
    parties = hhmm.split(":")
    secondes = Decimal(parties[2]) if len(parties) > 2 else Decimal(0)
    entieres = int(secondes)
    micro = int((secondes - entieres) * 1_000_000)
    return datetime.combine(jour, time(int(parties[0]), int(parties[1]), entieres, micro), tzinfo=PARIS)


def d(chaine: str) -> date:
    return date.fromisoformat(chaine)


def sql_instant(instant: datetime) -> str:
    return "'" + instant.astimezone(UTC).strftime("%Y-%m-%d %H:%M:%S.%f") + "'"


def sql_texte(valeur) -> str:
    if valeur is None:
        return "null"
    return "'" + str(valeur).replace("'", "''") + "'"


def sql_nombre(valeur) -> str:
    return "null" if valeur is None else str(valeur)


# ---------------------------------------------------------------------------------------------------------------------
# Identifiants lisibles : le premier groupe dit la table, le dernier le rang.
# ---------------------------------------------------------------------------------------------------------------------

_rangs: dict[str, int] = defaultdict(int)


def identifiant(prefixe: str) -> uuid.UUID:
    _rangs[prefixe] += 1
    return uuid.UUID(f"{prefixe}-0000-4000-8000-{_rangs[prefixe]:012d}")


# ---------------------------------------------------------------------------------------------------------------------
# Referentiel : postes de travail
# ---------------------------------------------------------------------------------------------------------------------


@dataclass
class Poste:
    code: str
    libelle: str
    # Historique (date d'effet, nature, cout horaire) : la derniere ligne est la valeur courante du referentiel.
    historique: list[tuple[date, str, Decimal | None]]
    id: uuid.UUID = field(default_factory=lambda: identifiant("20000000"))

    def a(self, jour: date) -> tuple[str, Decimal | None]:
        courant = self.historique[0]
        for ligne in self.historique:
            if ligne[0] <= jour:
                courant = ligne
        return courant[1], courant[2]

    @property
    def nature(self) -> str:
        return self.historique[-1][1]

    @property
    def cout(self) -> Decimal | None:
        return self.historique[-1][2]


DEBUT_DES_TEMPS = d("2020-01-01")

POSTES = {
    p.code: p
    for p in [
        Poste("UGV", "Fraiseuse UGV DMG HSC 75", [(DEBUT_DES_TEMPS, "Fraisage", Decimal("95.00"))]),
        Poste("5AX", "Centre d'usinage 5 axes Hermle C42", [(DEBUT_DES_TEMPS, "Fraisage", Decimal("110.00"))]),
        # Requalifiee le 16/09 : elle ne sert plus qu'au percage. Les pointages anterieurs gardent « Fraisage ».
        Poste(
            "HUR",
            "Fraiseuse conventionnelle Huron",
            [(DEBUT_DES_TEMPS, "Fraisage", Decimal("42.00")), (d("2026-09-16"), "Perçage", Decimal("42.00"))],
        ),
        Poste("TCN", "Tour CN Mazak QT 250", [(DEBUT_DES_TEMPS, "Tournage", Decimal("65.00"))]),
        Poste("TCV", "Tour conventionnel Cazeneuve HB 575", [(DEBUT_DES_TEMPS, "Tournage", Decimal("38.00"))]),
        Poste("ENF", "Enfonçage Charmilles Form 30", [(DEBUT_DES_TEMPS, "Électroérosion enfonçage", Decimal("72.00"))]),
        # Cout horaire revalorise le 14/09 : 75 -> 80. Les pointages anterieurs gardent 75.
        Poste(
            "FIL",
            "Érosion fil AgieCharmilles CUT 200",
            [
                (DEBUT_DES_TEMPS, "Électroérosion fil", Decimal("75.00")),
                (d("2026-09-14"), "Électroérosion fil", Decimal("80.00")),
            ],
        ),
        Poste("REC", "Rectifieuse plane Jones & Shipman 540", [(DEBUT_DES_TEMPS, "Rectification", Decimal("55.00"))]),
        Poste("PER", "Perceuse à forets profonds", [(DEBUT_DES_TEMPS, "Perçage", Decimal("50.00"))]),
        Poste("PO1", "Établi polissage 1", [(DEBUT_DES_TEMPS, "Polissage", Decimal("18.00"))]),
        Poste("PO2", "Établi polissage 2", [(DEBUT_DES_TEMPS, "Polissage", Decimal("18.00"))]),
        # Sans cout horaire : l'etabli n'est pas valorise, le cout machine y vaut zero.
        Poste("AJU", "Établi ajustage montage", [(DEBUT_DES_TEMPS, "Ajustage", None)]),
        Poste("SOU", "Soudure laser Alpha", [(DEBUT_DES_TEMPS, "Soudure", Decimal("85.00"))]),
        Poste("MMT", "Tridimensionnelle Zeiss Contura", [(DEBUT_DES_TEMPS, "Contrôle", Decimal("60.00"))]),
        Poste("PRS", "Presse d'essai Arburg 220 t", [(DEBUT_DES_TEMPS, "Essai", Decimal("90.00"))]),
        Poste("CAO", "Poste CAO 1", [(DEBUT_DES_TEMPS, "Conception", Decimal("35.00"))]),
        # Jamais habilitee, jamais pointee : c'est le seul poste que le referentiel laisse supprimer.
        Poste("RCY", "Rectifieuse cylindrique Studer", [(DEBUT_DES_TEMPS, "Rectification", Decimal("58.00"))]),
    ]
}


# ---------------------------------------------------------------------------------------------------------------------
# Referentiel : operateurs
# ---------------------------------------------------------------------------------------------------------------------


@dataclass
class Operateur:
    code: str
    nom: str
    prenom: str
    matricule: str | None
    taux: list[tuple[date, Decimal | None]]  # historique, la derniere ligne est la valeur courante
    postes: list[str]  # habilitations courantes
    equipe: str  # JOUR, MATIN, APREM, NUIT, AUCUNE
    metier: str
    postes_passes: list[tuple[str, date]] = field(default_factory=list)  # habilitations retirees : (poste, jusqu'au)
    parallele: bool = False  # mene volontiers deux postes de front
    id: uuid.UUID = field(default_factory=lambda: identifiant("10000000"))

    def taux_a(self, jour: date) -> Decimal | None:
        courant = self.taux[0][1]
        for depuis, valeur in self.taux:
            if depuis <= jour:
                courant = valeur
        return courant

    def postes_a(self, jour: date) -> list[str]:
        return self.postes + [code for code, jusqu in self.postes_passes if jour <= jusqu]


def taux(valeur: str | None) -> list[tuple[date, Decimal | None]]:
    return [(DEBUT_DES_TEMPS, Decimal(valeur) if valeur else None)]


OPERATEURS = {
    o.code: o
    for o in [
        Operateur("DUPONT", "DUPONT", "Jean-Marc", "M0412", taux("28.50"), ["UGV", "5AX", "HUR"], "NUIT", "Fraiseur UGV, poste de nuit"),
        Operateur("MARTIN_S", "MARTIN", "Sébastien", "M0418", taux("26.00"), ["5AX", "HUR", "UGV"], "MATIN", "Fraiseur", parallele=True),
        Operateur("BERNARD", "BERNARD", "Nathalie", "M0425", taux("24.20"), ["PO1", "PO2"], "JOUR", "Polisseuse"),
        Operateur("PERRIN", "PERRIN", "Christophe", "M0431", taux("27.80"), ["ENF", "FIL", "REC", "PER"], "JOUR", "Érosionniste", parallele=True),
        Operateur("GIROD", "GIROD", "Pascal", "M0302", taux("31.00"), ["AJU", "PRS", "SOU", "MMT"], "JOUR", "Mouliste, chef d'atelier"),
        Operateur("BOUVIER", "BOUVIER", "Mickaël", "M0447", taux("25.40"), ["TCN", "TCV"], "JOUR", "Tourneur"),
        Operateur("JACQUEMOUD", "JACQUEMOUD", "Élodie", "M0452", taux("23.60"), ["MMT"], "JOUR", "Contrôleuse métrologie"),
        Operateur("ROLLET", "ROLLET", "Thierry", "M0355", taux("29.90"), ["SOU", "AJU"], "JOUR", "Soudeur laser"),
        Operateur("MOREL", "MOREL", "Damien", "M0461", taux("24.80"), ["5AX", "UGV"], "APREM", "Fraiseur, équipe d'après-midi", parallele=True),
        Operateur("CHAPUIS", "CHAPUIS", "Kévin", "M0470", taux("22.50"), ["REC", "PER"], "MATIN", "Rectifieur"),
        Operateur("VUILLERMOZ", "VUILLERMOZ", "Anthony", "M0388", taux("26.70"), ["AJU", "PRS"], "JOUR", "Mouliste ajusteur"),
        # Homonyme de Sebastien : l'identite est unique sur le couple nom, prenom.
        Operateur("MARTIN_S2", "MARTIN", "Sandrine", "M0483", taux("21.90"), ["PO1", "PO2"], "JOUR", "Polisseuse"),
        Operateur("FAVRE", "FAVRE", "Julien", "M0215", taux("32.40"), ["CAO"], "JOUR", "Dessinateur-projeteur"),
        Operateur("GUYON", "GUYON", "Laurent", "M0399", taux("27.10"), ["PRS", "AJU"], "JOUR", "Régleur essais"),
        # Augmente le 14/09 : 25.00 -> 26.50. Les pointages anterieurs gardent 25.00.
        Operateur(
            "MERMET", "MERMET", "Bruno", "M0490",
            [(DEBUT_DES_TEMPS, Decimal("25.00")), (d("2026-09-14"), Decimal("26.50"))],
            ["UGV", "5AX", "HUR"], "MATIN", "Fraiseur", parallele=True,
        ),
        # Des-habilite du tour conventionnel le 04/09 : ses pointages anterieurs sur ce tour restent.
        Operateur(
            "COLLET", "COLLET", "Yannick", "M0501", taux("23.10"), ["HUR"], "JOUR", "Fraiseur conventionnel",
            postes_passes=[("TCV", d("2026-09-04"))],
        ),
        # Apprenti sans taux horaire : sa main d'oeuvre ne coute rien, le rapport ne l'invente pas.
        Operateur("PONCET", "PONCET", "Mathis", "A0026", taux(None), ["AJU"], "JOUR", "Apprenti mouliste"),
        # Interimaire sans matricule : il pointe sans poste, sa ligne de cout n'a pas de nature.
        Operateur("DAVID", "DAVID", "Lucas", None, taux("20.80"), [], "JOUR", "Intérimaire manutention"),
        Operateur("REY", "REY", "Céline", "M0512", taux("24.00"), ["PO2", "AJU"], "JOUR", "Polisseuse ajusteuse"),
        # Embauche au 28/09 : habilite, jamais pointe, donc encore supprimable.
        Operateur("SERVE", "SERVE", "Olivier", "M0520", taux("26.00"), ["TCN", "REC"], "AUCUNE", "Tourneur, embauché le 28/09"),
    ]
}

assert len(OPERATEURS) == 20


# ---------------------------------------------------------------------------------------------------------------------
# Elements de fabrication, suivis d'atelier, journaux
# ---------------------------------------------------------------------------------------------------------------------


@dataclass
class Element:
    code: str
    type: str  # ORDRE_DE_FABRICATION, PRODUIT
    reference: str | None
    description: str | None
    creation: datetime
    scenario: str | None = None  # element dedie a un scenario : aucun pointage aleatoire ne le touche
    disponible: tuple[date, date] | None = None  # pour la vie courante : premier et dernier jour pointable
    nom: str = ""
    id: uuid.UUID = field(default_factory=lambda: identifiant("30000000"))


@dataclass
class Annulation:
    auteur: str
    date: datetime
    motif: str


@dataclass
class EvenementDAtelier:
    suivi: "Suivi"
    type: str
    operateur: Operateur
    poste: Poste | None
    survenue: datetime
    enregistrement: datetime
    auteur: str
    annulation: Annulation | None = None
    id: uuid.UUID = field(default_factory=lambda: identifiant("70000000"))

    @property
    def jour(self) -> date:
        return self.survenue.astimezone(PARIS).date()

    @property
    def nature(self) -> str | None:
        return self.poste.a(self.jour)[0] if self.poste else None

    @property
    def cout(self) -> Decimal | None:
        return self.poste.a(self.jour)[1] if self.poste else None

    @property
    def taux(self) -> Decimal | None:
        return self.operateur.taux_a(self.jour)

    @property
    def cle(self):
        return (self.operateur.code, self.poste.code if self.poste else None)


@dataclass
class Suivi:
    element: Element
    engagement: datetime
    cloture: tuple[datetime, datetime] | None = None  # survenue, enregistrement
    evenements: list[EvenementDAtelier] = field(default_factory=list)
    id: uuid.UUID = field(default_factory=lambda: identifiant("40000000"))

    def actifs(self) -> list[EvenementDAtelier]:
        return sorted((e for e in self.evenements if e.annulation is None), key=lambda e: (e.survenue, e.id))


@dataclass
class EvenementDePresence:
    type: str
    survenue: datetime
    enregistrement: datetime
    auteur: str
    annulation: Annulation | None = None
    id: uuid.UUID = field(default_factory=lambda: identifiant("60000000"))


ORDRE_PRESENCE = {"ARRIVEE": 0, "PAUSE": 1, "REPRISE": 2, "DEPART": 3}


@dataclass
class Journee:
    operateur: Operateur
    evenements: list[EvenementDePresence] = field(default_factory=list)
    scenario: str | None = None
    id: uuid.UUID = field(default_factory=lambda: identifiant("50000000"))

    def actifs(self) -> list[EvenementDePresence]:
        return sorted(
            (e for e in self.evenements if e.annulation is None), key=lambda e: (e.survenue, ORDRE_PRESENCE[e.type])
        )

    def etat(self) -> str:
        etat = "ABSENT"
        for evenement in self.actifs():
            etat = transition_de_presence(etat, evenement.type)
        return etat

    def debut(self) -> datetime | None:
        actifs = self.actifs()
        return actifs[0].survenue if actifs else None

    def dernier_fait(self) -> datetime | None:
        actifs = self.actifs()
        return actifs[-1].survenue if actifs else None

    def depart(self) -> datetime | None:
        actifs = self.actifs()
        return actifs[-1].survenue if actifs and actifs[-1].type == "DEPART" else None

    def fenetres(self) -> list[tuple[datetime, datetime | None]]:
        actifs = self.actifs()
        fenetres = []
        etat = "ABSENT"
        for rang, evenement in enumerate(actifs):
            etat = transition_de_presence(etat, evenement.type)
            if etat == "PRESENT":
                suivant = actifs[rang + 1].survenue if rang + 1 < len(actifs) else None
                fenetres.append((evenement.survenue, suivant))
        return fenetres


def transition_de_presence(etat: str, type_: str) -> str:
    permis = {
        ("ABSENT", "ARRIVEE"): "PRESENT",
        ("PRESENT", "PAUSE"): "EN_PAUSE",
        ("PRESENT", "DEPART"): "ABSENT",
        ("EN_PAUSE", "REPRISE"): "PRESENT",
        ("EN_PAUSE", "DEPART"): "ABSENT",
    }
    if (etat, type_) not in permis:
        raise ValueError(f"transition de presence interdite : {etat} -> {type_}")
    return permis[(etat, type_)]


def transition_d_activite(etat: str, type_: str) -> str:
    if type_ == "DEBUT":
        return "EN_COURS"
    if type_ == "NON_CONFORMITE":
        return "EN_NON_CONFORMITE"
    if etat == "ABSENTE":
        raise ValueError("transition d'atelier interdite : ABSENTE -> FIN")
    return "ABSENTE"


ELEMENTS: dict[str, Element] = {}
SUIVIS: list[Suivi] = []
JOURNEES: list[Journee] = []


def element(code, type_, reference, description, creation, scenario=None, disponible=None) -> Element:
    nouveau = Element(code, type_, reference, description, creation, scenario, disponible)
    ELEMENTS[code] = nouveau
    return nouveau


def engage(code: str, quand: datetime) -> Suivi:
    suivi = Suivi(ELEMENTS[code], quand)
    SUIVIS.append(suivi)
    return suivi


def suivi_de(code: str) -> Suivi:
    """Le dernier passage de l'element en atelier."""
    return [s for s in SUIVIS if s.element.code == code][-1]


def pointe(suivi: Suivi, type_: str, operateur: str, poste: str | None, quand: datetime, enregistre=None, auteur=POINTEUR):
    evenement = EvenementDAtelier(
        suivi, type_, OPERATEURS[operateur], POSTES[poste] if poste else None, quand, enregistre or quand, auteur
    )
    suivi.evenements.append(evenement)
    return evenement


def journee(operateur: str, gestes: list[tuple[str, datetime]], scenario=None, enregistre=None) -> Journee:
    nouvelle = Journee(OPERATEURS[operateur], scenario=scenario)
    for type_, quand in gestes:
        nouvelle.evenements.append(EvenementDePresence(type_, quand, enregistre or quand, POINTEUR))
    JOURNEES.append(nouvelle)
    return nouvelle


def presence(journee_: Journee, type_: str, quand: datetime, enregistre=None, auteur=POINTEUR) -> EvenementDePresence:
    evenement = EvenementDePresence(type_, quand, enregistre or quand, auteur)
    journee_.evenements.append(evenement)
    return evenement


# ---------------------------------------------------------------------------------------------------------------------
# Calendrier
# ---------------------------------------------------------------------------------------------------------------------

PREMIER_JOUR = d("2026-08-24")  # lundi de la semaine 35, reprise apres la fermeture estivale
DERNIER_JOUR = d("2026-09-25")  # vendredi de la semaine 39
JOURS_OUVRES = [
    PREMIER_JOUR + timedelta(days=n)
    for n in range((DERNIER_JOUR - PREMIER_JOUR).days + 1)
    if (PREMIER_JOUR + timedelta(days=n)).weekday() < 5
]

# Journees reservees aux scenarios : (operateur, jour d'arrivee). La vie courante ne les touche pas.
RESERVES: set[tuple[str, date]] = set()
# Operateurs dont la vie courante s'arrete avant le bloc « situation du jour ».
CONGES = {("MARTIN_S2", jour) for jour in JOURS_OUVRES if d("2026-09-07") <= jour <= d("2026-09-11")}


def reserve(operateur: str, *jours: str):
    for jour in jours:
        RESERVES.add((operateur, d(jour)))


# ---------------------------------------------------------------------------------------------------------------------
# Elements : les moules neufs (PRODUIT) et les interventions (ORDRE_DE_FABRICATION)
# ---------------------------------------------------------------------------------------------------------------------

CREATION_AVANT_ETE = heure(d("2026-07-09"), "10:12")

# Un moule neuf commence en 2025 : son nom porte l'annee de sa creation, pas celle de son travail.
element(
    "PRD_FLACON", "PRODUIT", "M25-1187", "Moule 8 empreintes flacon PEHD 250 ml, canaux chauds Hasco",
    heure(d("2025-11-18"), "14:30"), disponible=(d("2026-08-24"), d("2026-09-25")),
)
element(
    "PRD_BOUCHON", "PRODUIT", "M26-0412", "Moule 4 empreintes bouchon flip-top Ø28, dévissage automatique",
    heure(d("2026-06-02"), "09:05"), disponible=(d("2026-08-24"), d("2026-09-25")),
)
element(
    "PRD_POIGNEE", "PRODUIT", "M26-0433", "Moule bi-matière poignée de valise PP + TPE",
    heure(d("2026-06-23"), "11:40"), disponible=(d("2026-08-24"), d("2026-09-18")),
)
element(
    "PRD_LUNETTES", "PRODUIT", "M26-0451", "Moule 2 empreintes branche de lunettes acétate injecté",
    heure(d("2026-07-03"), "16:20"), disponible=(d("2026-08-31"), d("2026-09-25")),
)
element(
    "PRD_POT", "PRODUIT", "M26-0467", "Moule pot crème 50 ml cosmétique, finition poli miroir A1",
    heure(d("2026-07-09"), "08:50"), disponible=(d("2026-08-24"), d("2026-09-25")),
)
element(
    "PRD_CAPOT", "PRODUIT", "M26-0478", "Moule capot de rétroviseur ABS/PC, tiroirs hydrauliques",
    heure(d("2026-08-26"), "10:15"), disponible=(d("2026-09-02"), d("2026-09-25")),
)
element(
    "PRD_BOITIER", "PRODUIT", "M26-0489", "Moule boîtier de robot pâtissier PP chargé talc",
    heure(d("2026-09-08"), "13:30"), disponible=(d("2026-09-14"), d("2026-09-25")),
)

OF_COURANTS = [
    ("OF_EJECT", "M22-0315", "Changement des éjecteurs cassés et contrôle de course", ("2026-08-24", "2026-09-04")),
    ("OF_PLANJOINT", "M21-0198", "Reprise du plan de joint bavure côté fixe", ("2026-08-24", "2026-09-11")),
    ("OF_NOYAU", "M24-0877", "Modification de noyau suite ECN client indice C", ("2026-08-25", "2026-09-18")),
    ("OF_ELECTRODES", None, "Usinage d'électrodes graphite pour reprise d'empreinte", ("2026-08-24", "2026-09-25")),
    ("OF_PREV", "M23-0560", "Maintenance préventive 500 000 cycles", ("2026-08-31", "2026-09-11")),
    ("OF_TIROIR", "M24-0902", "Réparation tiroir grippé et reprise des lardons", ("2026-09-01", "2026-09-25")),
    ("OF_TEXTURE", "M25-1033", "Retouche texturation grain cuir après casse", ("2026-09-07", "2026-09-25")),
    ("OF_REGULATION", "M22-0240", "Remplacement de la régulation et des colliers chauffants", ("2026-09-14", "2026-09-25")),
    ("OF_INSERT", None, None, ("2026-08-26", "2026-09-25")),  # element reduit a son seul numero
]

for code, reference, description, (debut, fin) in OF_COURANTS:
    element(code, "ORDRE_DE_FABRICATION", reference, description, CREATION_AVANT_ETE, disponible=(d(debut), d(fin)))

SCENARIOS_ELEMENTS = [
    ("S01", "M24-0655", "Reprise diamètre de colonne de guidage"),
    ("S02", "M25-0981", "Usinage 5 axes empreinte modifiée, travail jamais arrêté"),
    ("S03A", "M26-0301", "Usinage empreinte fixe, deux machines en parallèle"),
    ("S03B", "M26-0302", "Usinage bride de centrage, deux machines en parallèle"),
    ("S04", "M25-1102", "Électroérosion empreinte, deux puis quatre postes de front"),
    ("S05A", "M26-0350", "Polissage insert A, un seul établi pour trois OF"),
    ("S05B", "M26-0351", "Polissage insert B, un seul établi pour trois OF"),
    ("S05C", "M26-0352", "Polissage insert C, un seul établi pour trois OF"),
    ("S06", "M21-0077", "Soudure laser d'arête cassée, deux non-conformités"),
    ("S07A", "M26-0042", "OF 42 de l'exemple E2 : départ oublié, relance le lendemain"),
    ("S07B", "M26-0043", "OF 43 de l'exemple E2"),
    ("S08", "M24-0714", "Ajustage d'un moule, départ régularisé par le gestionnaire"),
    ("S09", "M25-0888", "Usinage de nuit, départ oublié (exemple E3)"),
    ("S10A", "M26-0142", "Poste de nuit complet, arrivée redondante absorbée (exemple E1)"),
    ("S10B", "M26-0143", "Poste de nuit complet, second OF lancé à 3 h (exemple E1)"),
    ("S11", "M26-0211", "Poste de nuit à cheval sur deux semaines (exemple E7)"),
    ("S12", "M23-0419", "Pupitre hors ligne, départ reçu le lendemain (exemple E5)"),
    ("S13", "M22-0315B", "Réengagé après clôture : deux passages en atelier"),
    ("S14", "M25-1150", "Essai T1 du samedi, régularisé sans présence"),
    ("S15", "M26-0199", "Métrologie, fin de contrôle corrigée"),
    ("S16", "M26-0198", "Tournage, un pointage annulé sur le mauvais OF"),
    ("S17A", "M26-0501", "Manutention sans poste, intérimaire et apprenti"),
    ("S17B", "M26-0502", "Manutention sans poste, en parallèle"),
    ("S18", "M26-0620", "Arrondi : trois postes pendant sept minutes"),
    ("S19", "M26-0655", "Soirée de retour sous le seuil (exemple E4)"),
    ("S20", "M26-0701", "Engagé, jamais pointé"),
    ("S21", "M26-0702", "Créé, jamais engagé"),
    ("S22", "M26-0777", "Polissage en cours à l'instant de la lecture"),
    ("S23A", "M24-0930", "Tournage jamais arrêté la veille"),
    ("S23B", "M24-0931", "Tournage du lendemain sur un autre tour"),
]

for code, reference, description in SCENARIOS_ELEMENTS:
    element(code, "ORDRE_DE_FABRICATION", reference, description, heure(d("2026-08-21"), "15:00"), scenario=code)


# ---------------------------------------------------------------------------------------------------------------------
# Scenarios : chaque ligne des attendus renvoie a l'un d'eux.
# ---------------------------------------------------------------------------------------------------------------------

LUN_24 = d("2026-08-24")
ENGAGEMENT_SCENARIOS = heure(LUN_24, "06:00")
for code, _, _ in SCENARIOS_ELEMENTS:
    if code not in ("S21", "S13"):
        engage(code, ENGAGEMENT_SCENARIOS)


def journee_type(operateur: str, jour: date, arrivee="07:30", pause="12:00", reprise="13:00", depart="16:30", scenario=None):
    return journee(
        operateur,
        [
            ("ARRIVEE", heure(jour, arrivee)),
            ("PAUSE", heure(jour, pause)),
            ("REPRISE", heure(jour, reprise)),
            ("DEPART", heure(jour, depart)),
        ],
        scenario=scenario,
    )


# S01 -- cas nominal : un operateur, un poste, trois heures. Bouvier, mardi 25/08.
jour = d("2026-08-25")
reserve("BOUVIER", "2026-08-25")
journee_type("BOUVIER", jour, scenario="S01")
pointe(suivi_de("S01"), "DEBUT", "BOUVIER", "TCN", heure(jour, "08:00"))
pointe(suivi_de("S01"), "FIN", "BOUVIER", "TCN", heure(jour, "11:00"))

# S16 -- annulation : un debut sur le mauvais OF (S01) annule par le gestionnaire, le bon lance deux minutes apres.
jour = d("2026-08-26")
reserve("BOUVIER", "2026-08-26")
journee_type("BOUVIER", jour, scenario="S16")
faux = pointe(suivi_de("S01"), "DEBUT", "BOUVIER", "TCN", heure(jour, "08:00"))
faux.annulation = Annulation(GESTIONNAIRE, heure(jour, "08:40"), "Mauvais OF sélectionné au pupitre")
pointe(suivi_de("S16"), "DEBUT", "BOUVIER", "TCN", heure(jour, "08:02"))
pointe(suivi_de("S16"), "FIN", "BOUVIER", "TCN", heure(jour, "12:00"))

# S02 -- pause qui scinde, depart qui tronque, cloture qui ferme : Morel, equipe d'apres-midi, mercredi 26/08.
jour = d("2026-08-26")
reserve("MOREL", "2026-08-26")
journee_type("MOREL", jour, arrivee="13:00", pause="17:00", reprise="17:30", depart="21:00", scenario="S02")
pointe(suivi_de("S02"), "DEBUT", "MOREL", "5AX", heure(jour, "14:00"))
suivi_de("S02").cloture = (heure(d("2026-08-28"), "10:00"), heure(d("2026-08-28"), "10:00"))

# S03 -- deux machines en parallele : Martin, equipe du matin, jeudi 27/08.
jour = d("2026-08-27")
reserve("MARTIN_S", "2026-08-27")
journee_type("MARTIN_S", jour, arrivee="05:00", pause="09:00", reprise="09:20", depart="13:00", scenario="S03")
pointe(suivi_de("S03A"), "DEBUT", "MARTIN_S", "5AX", heure(jour, "06:00"))
pointe(suivi_de("S03B"), "DEBUT", "MARTIN_S", "HUR", heure(jour, "07:00"))
pointe(suivi_de("S03B"), "FIN", "MARTIN_S", "HUR", heure(jour, "08:30"))
pointe(suivi_de("S03A"), "FIN", "MARTIN_S", "5AX", heure(jour, "10:00"))

# S04 -- l'erosionniste : deux pieces du meme OF sur deux machines, puis quatre postes de front. Perrin, lundi 31/08.
jour = d("2026-08-31")
reserve("PERRIN", "2026-08-31")
journee_type("PERRIN", jour, scenario="S04")
s04 = suivi_de("S04")
pointe(s04, "DEBUT", "PERRIN", "ENF", heure(jour, "08:00"))
pointe(s04, "DEBUT", "PERRIN", "FIL", heure(jour, "08:00"))
pointe(s04, "FIN", "PERRIN", "FIL", heure(jour, "10:00"))
pointe(s04, "FIN", "PERRIN", "ENF", heure(jour, "11:00"))
for code in ("ENF", "FIL", "REC", "PER"):
    pointe(s04, "DEBUT", "PERRIN", code, heure(jour, "13:30"))
for code in ("ENF", "FIL", "REC", "PER"):
    pointe(s04, "FIN", "PERRIN", code, heure(jour, "14:30"))

# S05 -- un seul etabli pour trois OF : pas de division, cout machine plein sur chacun. Bernard, mardi 01/09.
jour = d("2026-09-01")
reserve("BERNARD", "2026-09-01")
journee_type("BERNARD", jour, scenario="S05")
pointe(suivi_de("S05A"), "DEBUT", "BERNARD", "PO1", heure(jour, "08:00"))
pointe(suivi_de("S05B"), "DEBUT", "BERNARD", "PO1", heure(jour, "08:00"))
pointe(suivi_de("S05C"), "DEBUT", "BERNARD", "PO1", heure(jour, "09:00"))
for code in ("S05A", "S05B", "S05C"):
    pointe(suivi_de(code), "FIN", "BERNARD", "PO1", heure(jour, "10:00"))

# S06 -- non-conformites : l'une au milieu du travail, l'autre d'emblee le lendemain. Rollet, 02 et 03/09.
reserve("ROLLET", "2026-09-02", "2026-09-03")
jour = d("2026-09-02")
journee_type("ROLLET", jour, scenario="S06")
s06 = suivi_de("S06")
pointe(s06, "DEBUT", "ROLLET", "SOU", heure(jour, "08:00"))
pointe(s06, "NON_CONFORMITE", "ROLLET", "SOU", heure(jour, "10:00"))
pointe(s06, "DEBUT", "ROLLET", "SOU", heure(jour, "11:00"))
pointe(s06, "FIN", "ROLLET", "SOU", heure(jour, "15:00"))
jour = d("2026-09-03")
journee_type("ROLLET", jour, scenario="S06")
pointe(s06, "NON_CONFORMITE", "ROLLET", "SOU", heure(jour, "08:00"))
pointe(s06, "FIN", "ROLLET", "SOU", heure(jour, "09:30"))
s06.cloture = (heure(jour, "16:00"), heure(jour, "16:05"))

# S07 -- exemple E2 : depart oublie, journee abandonnee, fin presumee, nouvelle journee et relance. Martin, 07-08/09.
reserve("MARTIN_S", "2026-09-07", "2026-09-08")
lundi = d("2026-09-07")
mardi = d("2026-09-08")
e2 = journee(
    "MARTIN_S",
    [
        ("ARRIVEE", heure(lundi, "07:00")),
        ("PAUSE", heure(lundi, "12:00")),
        ("REPRISE", heure(lundi, "13:00")),
    ],
    scenario="S07",
)
pointe(suivi_de("S07A"), "DEBUT", "MARTIN_S", "5AX", heure(lundi, "08:00"))
pointe(suivi_de("S07B"), "DEBUT", "MARTIN_S", "HUR", heure(lundi, "09:00"))
pointe(suivi_de("S07B"), "FIN", "MARTIN_S", "HUR", heure(lundi, "16:00"))
journee("MARTIN_S", [("ARRIVEE", heure(mardi, "07:00")), ("DEPART", heure(mardi, "15:00"))], scenario="S07")
pointe(suivi_de("S07A"), "DEBUT", "MARTIN_S", "5AX", heure(mardi, "07:05"))  # relance d'une activite en cours
pointe(suivi_de("S07A"), "FIN", "MARTIN_S", "5AX", heure(mardi, "11:00"))
suivi_de("S07A").cloture = (heure(d("2026-09-11"), "17:00"), heure(d("2026-09-11"), "17:00"))

# S08 -- meme situation, mais le gestionnaire regularise le depart le lendemain matin. Vuillermoz, 07-08/09.
reserve("VUILLERMOZ", "2026-09-07", "2026-09-08")
j08 = journee(
    "VUILLERMOZ",
    [("ARRIVEE", heure(lundi, "07:00")), ("PAUSE", heure(lundi, "12:00")), ("REPRISE", heure(lundi, "13:00"))],
    scenario="S08",
)
presence(j08, "DEPART", heure(lundi, "16:30"), enregistre=heure(mardi, "08:10"), auteur=GESTIONNAIRE)
pointe(suivi_de("S08"), "DEBUT", "VUILLERMOZ", "AJU", heure(lundi, "07:30"))
journee_type("VUILLERMOZ", mardi, arrivee="07:00", depart="15:00", scenario="S08")
pointe(suivi_de("S08"), "DEBUT", "VUILLERMOZ", "AJU", heure(mardi, "07:15"))  # relance
pointe(suivi_de("S08"), "FIN", "VUILLERMOZ", "AJU", heure(mardi, "11:15"))

# S10 -- exemple E1 : poste de nuit complet, l'arrivee de 3 h est absorbee (rien n'est ecrit). Dupont, 31/08 -> 01/09.
reserve("DUPONT", "2026-08-31")
lundi = d("2026-08-31")
mardi = d("2026-09-01")
journee("DUPONT", [("ARRIVEE", heure(lundi, "20:00")), ("DEPART", heure(mardi, "08:00"))], scenario="S10")
pointe(suivi_de("S10A"), "DEBUT", "DUPONT", "UGV", heure(lundi, "20:05"))
pointe(suivi_de("S10B"), "DEBUT", "DUPONT", "5AX", heure(mardi, "03:02"))
pointe(suivi_de("S10A"), "FIN", "DUPONT", "UGV", heure(mardi, "08:00"))
pointe(suivi_de("S10B"), "FIN", "DUPONT", "5AX", heure(mardi, "08:00"))

# S09 -- exemple E3 : nuit sans depart, nouvelle arrivee le soir suivant, relance. Dupont, 14 -> 16/09.
reserve("DUPONT", "2026-09-14", "2026-09-15")
lundi = d("2026-09-14")
mardi = d("2026-09-15")
mercredi = d("2026-09-16")
journee("DUPONT", [("ARRIVEE", heure(lundi, "20:00"))], scenario="S09")
pointe(suivi_de("S09"), "DEBUT", "DUPONT", "UGV", heure(lundi, "20:05"))
journee(
    "DUPONT",
    [
        ("ARRIVEE", heure(mardi, "20:00")),
        ("PAUSE", heure(mercredi, "00:00")),
        ("REPRISE", heure(mercredi, "00:30")),
        ("DEPART", heure(mercredi, "05:00")),
    ],
    scenario="S09",
)
pointe(suivi_de("S09"), "DEBUT", "DUPONT", "UGV", heure(mardi, "20:10"))  # relance
pointe(suivi_de("S09"), "FIN", "DUPONT", "UGV", heure(mercredi, "04:00"))

# S11 -- exemple E7 : poste de nuit du dimanche 20/09 au lundi 21/09, a cheval sur les semaines 38 et 39.
reserve("DUPONT", "2026-09-20")
dimanche = d("2026-09-20")
lundi = d("2026-09-21")
journee("DUPONT", [("ARRIVEE", heure(dimanche, "20:00")), ("DEPART", heure(lundi, "08:00"))], scenario="S11")
pointe(suivi_de("S11"), "DEBUT", "DUPONT", "UGV", heure(dimanche, "20:10"))
pointe(suivi_de("S11"), "FIN", "DUPONT", "UGV", heure(lundi, "07:50"))

# S12 -- exemple E5 : pupitre hors ligne, le depart de mardi 08:30 ouvre une journee nulle. Collet, 14-15/09.
reserve("COLLET", "2026-09-14", "2026-09-15")
lundi = d("2026-09-14")
mardi = d("2026-09-15")
journee("COLLET", [("ARRIVEE", heure(lundi, "07:30"))], scenario="S12")
pointe(suivi_de("S12"), "DEBUT", "COLLET", "HUR", heure(lundi, "08:00"))
pointe(suivi_de("S12"), "FIN", "COLLET", "HUR", heure(lundi, "11:00"))
# L'arrivee implicite partage l'heure du geste ; le geste est enregistre au retour du reseau.
journee(
    "COLLET",
    [("ARRIVEE", heure(mardi, "08:30")), ("DEPART", heure(mardi, "08:30"))],
    scenario="S12",
    enregistre=heure(mardi, "08:30:04"),
)
journee_type("COLLET", mardi, arrivee="08:35", scenario="S12")

# S13 -- reengagement apres cloture : deux passages, le cout les additionne.
element_s13 = ELEMENTS["S13"]
premier = engage("S13", heure(d("2026-08-24"), "06:00"))
reserve("GIROD", "2026-08-25")
jour = d("2026-08-25")
journee_type("GIROD", jour, scenario="S13")
pointe(premier, "DEBUT", "GIROD", "AJU", heure(jour, "08:00"))
pointe(premier, "FIN", "GIROD", "AJU", heure(jour, "10:00"))
premier.cloture = (heure(jour, "17:00"), heure(jour, "17:02"))
second = engage("S13", heure(d("2026-09-09"), "07:00"))
reserve("GUYON", "2026-09-09")
jour = d("2026-09-09")
journee_type("GUYON", jour, scenario="S13")
pointe(second, "DEBUT", "GUYON", "PRS", heure(jour, "14:00"))
pointe(second, "FIN", "GUYON", "PRS", heure(jour, "16:00"))
second.cloture = (heure(d("2026-09-10"), "09:00"), heure(d("2026-09-10"), "09:00"))

# S14 -- pointage regularise sans presence : l'essai du samedi 12/09, rendu intact. Girod.
samedi = d("2026-09-12")
pointe(suivi_de("S14"), "DEBUT", "GIROD", "PRS", heure(samedi, "08:00"), enregistre=heure(d("2026-09-14"), "08:05"), auteur=GESTIONNAIRE)
pointe(suivi_de("S14"), "FIN", "GIROD", "PRS", heure(samedi, "10:00"), enregistre=heure(d("2026-09-14"), "08:06"), auteur=GESTIONNAIRE)

# S15 -- correction d'une fin : 15:00 pointe, 11:30 reel, corrige le lundi suivant. Jacquemoud, vendredi 28/08.
reserve("JACQUEMOUD", "2026-08-28")
jour = d("2026-08-28")
journee_type("JACQUEMOUD", jour, depart="15:30", scenario="S15")
pointe(suivi_de("S15"), "DEBUT", "JACQUEMOUD", "MMT", heure(jour, "09:00"))
fausse_fin = pointe(suivi_de("S15"), "FIN", "JACQUEMOUD", "MMT", heure(jour, "15:00"))
correction = heure(d("2026-08-31"), "08:30")
fausse_fin.annulation = Annulation(GESTIONNAIRE, correction, "Fin de contrôle pointée en retard")
pointe(suivi_de("S15"), "FIN", "JACQUEMOUD", "MMT", heure(jour, "11:30"), enregistre=correction, auteur=GESTIONNAIRE)

# S17 -- sans poste : un interimaire et un apprenti sans taux, puis deux OF de front sans poste. Mardi 15/09.
reserve("DAVID", "2026-09-15")
reserve("PONCET", "2026-09-15")
jour = d("2026-09-15")
journee_type("DAVID", jour, scenario="S17")
journee_type("PONCET", jour, scenario="S17")
pointe(suivi_de("S17A"), "DEBUT", "DAVID", None, heure(jour, "08:00"))
pointe(suivi_de("S17A"), "DEBUT", "PONCET", None, heure(jour, "08:00"))
pointe(suivi_de("S17B"), "DEBUT", "DAVID", None, heure(jour, "10:00"))
pointe(suivi_de("S17B"), "FIN", "DAVID", None, heure(jour, "11:00"))
pointe(suivi_de("S17A"), "FIN", "DAVID", None, heure(jour, "12:00"))
pointe(suivi_de("S17A"), "FIN", "PONCET", None, heure(jour, "12:00"))

# S18 -- arrondi : trois postes pendant sept minutes, dont la Huron requalifiee en percage. Mermet, jeudi 17/09.
reserve("MERMET", "2026-09-17")
jour = d("2026-09-17")
journee_type("MERMET", jour, arrivee="05:00", pause="09:00", reprise="09:20", depart="13:00", scenario="S18")
for code in ("UGV", "5AX", "HUR"):
    pointe(suivi_de("S18"), "DEBUT", "MERMET", code, heure(jour, "10:00"))
for code in ("UGV", "5AX", "HUR"):
    pointe(suivi_de("S18"), "FIN", "MERMET", code, heure(jour, "10:07"))

# S19 -- exemple E4 tel que le modele le traite depuis le lot 3 : retour a 19:30 absorbe, depart a 23:00 au-dela du
# seuil qui ouvre une journee nulle. Vuillermoz, lundi 21/09.
reserve("VUILLERMOZ", "2026-09-21")
jour = d("2026-09-21")
journee("VUILLERMOZ", [("ARRIVEE", heure(jour, "07:00"))], scenario="S19")
pointe(suivi_de("S19"), "DEBUT", "VUILLERMOZ", "AJU", heure(jour, "07:30"))
pointe(suivi_de("S19"), "FIN", "VUILLERMOZ", "AJU", heure(jour, "16:45"))
journee("VUILLERMOZ", [("ARRIVEE", heure(jour, "23:00")), ("DEPART", heure(jour, "23:00"))], scenario="S19")

# S23 -- une activite oubliee la veille sur un autre poste ne divise pas le lendemain : elle est ramenee a la journee
# ou elle a commence. Bouvier, lundi 21/09 (tour conventionnel jamais arrete) et mardi 22/09 (tour CN).
reserve("BOUVIER", "2026-09-21", "2026-09-22")
lundi = d("2026-09-21")
mardi = d("2026-09-22")
journee_type("BOUVIER", lundi, scenario="S23")
pointe(suivi_de("S23A"), "DEBUT", "BOUVIER", "TCV", heure(lundi, "14:00"))
journee_type("BOUVIER", mardi, scenario="S23")
pointe(suivi_de("S23B"), "DEBUT", "BOUVIER", "TCN", heure(mardi, "08:00"))
pointe(suivi_de("S23B"), "FIN", "BOUVIER", "TCN", heure(mardi, "10:00"))
suivi_de("S23A").cloture = (heure(d("2026-09-23"), "09:00"), heure(d("2026-09-23"), "09:00"))

# P01 -- sondes du seuil d'amplitude (13 h), sans pointage d'OF.
reserve("GUYON", "2026-09-22", "2026-09-23")
# Au seuil pile, pointe par l'operateur : aucune anomalie.
journee_type("GUYON", d("2026-09-22"), arrivee="06:00", pause="12:00", reprise="12:45", depart="19:00", scenario="P01")
# Depart corrige par le gestionnaire a 13 h + 0,5 s : amplitude excessive.
j = journee_type("GUYON", d("2026-09-23"), arrivee="06:00", pause="12:00", reprise="12:45", depart="18:00", scenario="P01")
depart_faux = [e for e in j.evenements if e.type == "DEPART"][0]
depart_faux.annulation = Annulation(GESTIONNAIRE, heure(d("2026-09-24"), "07:45"), "Départ réel après le démoulage de 19 h")
presence(j, "DEPART", heure(d("2026-09-23"), "19:00:00.5"), enregistre=heure(d("2026-09-24"), "07:45"), auteur=GESTIONNAIRE)
# Amplitude de 12 h 30 : sous le seuil par defaut, au-dessus si le gestionnaire le passe a 12 h (exemple E8).
reserve("MOREL", "2026-09-22")
journee_type("MOREL", d("2026-09-22"), arrivee="08:30", pause="12:30", reprise="13:00", depart="21:00", scenario="P01")

# P02 -- depart regularise tard par le gestionnaire : 14 h 30 d'amplitude, anomalie. Girod, jeudi 24/09.
reserve("GIROD", "2026-09-24")
j = journee("GIROD", [("ARRIVEE", heure(d("2026-09-24"), "07:00")), ("PAUSE", heure(d("2026-09-24"), "12:00")), ("REPRISE", heure(d("2026-09-24"), "12:30"))], scenario="P02")
presence(j, "DEPART", heure(d("2026-09-24"), "21:30"), enregistre=heure(d("2026-09-25"), "07:20"), auteur=GESTIONNAIRE)

# P03 -- une pause saisie par erreur, annulee par le gestionnaire. Bernard, jeudi 03/09.
reserve("BERNARD", "2026-09-03")
j = journee_type("BERNARD", d("2026-09-03"), pause="12:00", reprise="12:45", scenario="P03")
erreur = presence(j, "PAUSE", heure(d("2026-09-03"), "10:00"))
erreur.annulation = Annulation(GESTIONNAIRE, heure(d("2026-09-03"), "14:10"), "Pause saisie par erreur")

# P04 -- arrivee corrigee : 06:00 saisie, 07:45 reelle. Chapuis, mardi 08/09.
reserve("CHAPUIS", "2026-09-08")
j = journee("CHAPUIS", [("ARRIVEE", heure(d("2026-09-08"), "06:00")), ("PAUSE", heure(d("2026-09-08"), "09:30")), ("REPRISE", heure(d("2026-09-08"), "09:50")), ("DEPART", heure(d("2026-09-08"), "13:00"))], scenario="P04")
arrivee_fausse = j.evenements[0]
arrivee_fausse.annulation = Annulation(GESTIONNAIRE, heure(d("2026-09-08"), "10:30"), "Badgé à la place d'un collègue")
presence(j, "ARRIVEE", heure(d("2026-09-08"), "07:45"), enregistre=heure(d("2026-09-08"), "10:30"), auteur=GESTIONNAIRE)

# P05 -- journee abandonnee restee en pause : aucune fenetre a presumer. Chapuis, mercredi 16/09.
reserve("CHAPUIS", "2026-09-16")
journee("CHAPUIS", [("ARRIVEE", heure(d("2026-09-16"), "05:00")), ("PAUSE", heure(d("2026-09-16"), "09:00"))], scenario="P05")

# P06 -- une pause recue pour une journee abandonnee : arrivee implicite et pause au meme instant. Rey, 10-11/09.
reserve("REY", "2026-09-10", "2026-09-11")
journee("REY", [("ARRIVEE", heure(d("2026-09-10"), "07:30"))], scenario="P06")
j = journee(
    "REY",
    [("ARRIVEE", heure(d("2026-09-11"), "09:00")), ("PAUSE", heure(d("2026-09-11"), "09:00")), ("REPRISE", heure(d("2026-09-11"), "09:15")), ("DEPART", heure(d("2026-09-11"), "16:00"))],
    scenario="P06",
)

# P07 -- depart pointe en pause : la derniere fenetre s'arrete a la pause. Favre, vendredi 18/09.
reserve("FAVRE", "2026-09-18")
journee("FAVRE", [("ARRIVEE", heure(d("2026-09-18"), "08:00")), ("PAUSE", heure(d("2026-09-18"), "12:00")), ("DEPART", heure(d("2026-09-18"), "13:30"))], scenario="P07")

# P08 -- presence sans affectation : une journee de formation securite, aucun OF. Rey, lundi 14/09.
reserve("REY", "2026-09-14")
journee_type("REY", d("2026-09-14"), arrivee="08:00", pause="12:00", reprise="13:00", depart="17:00", scenario="P08")

# P09 -- heures du samedi matin pour tenir un delai : Girod et Vuillermoz, samedi 19/09, avec pointage.
for code in ("GIROD", "VUILLERMOZ"):
    journee(code, [("ARRIVEE", heure(d("2026-09-19"), "07:00")), ("DEPART", heure(d("2026-09-19"), "11:30"))], scenario="P09")

# S20 est engage sans aucun pointage ; S21 est cree sans jamais etre engage.


# ---------------------------------------------------------------------------------------------------------------------
# Vie courante : chaque operateur, chaque jour ouvre qui n'est ni reserve ni en conge.
# ---------------------------------------------------------------------------------------------------------------------

HORAIRES = {
    # (arrivee, debut de pause, fin de pause, depart) ; le vendredi, la journee finit une heure plus tot.
    "JOUR": ("07:30", "12:00", "13:00", "16:30"),
    "MATIN": ("05:00", "09:00", "09:20", "13:00"),
    "APREM": ("13:00", "17:00", "17:20", "21:00"),
    "NUIT": ("21:00", "01:00", "01:20", "05:00"),
}


def decale(instant: datetime, minutes: int) -> datetime:
    return instant + timedelta(minutes=minutes, seconds=random.randint(0, 59))


def elements_pointables(jour: date) -> list[Element]:
    return [e for e in ELEMENTS.values() if e.scenario is None and e.disponible and e.disponible[0] <= jour <= e.disponible[1]]


def fenetres_du_plan(jour: date, equipe: str) -> list[tuple[datetime, datetime]]:
    arrivee, pause, reprise, depart = HORAIRES[equipe]
    lendemain = jour + timedelta(days=1)
    nuit = equipe == "NUIT"
    debut = heure(jour, arrivee)
    debut_pause = heure(lendemain if nuit else jour, pause)
    fin_pause = heure(lendemain if nuit else jour, reprise)
    fin = heure(lendemain if nuit else jour, depart)
    if jour.weekday() == 4 and equipe == "JOUR":
        fin -= timedelta(hours=1)
    return [(debut, debut_pause), (fin_pause, fin)]


# Premier passage en atelier des elements courants : engages le premier jour ou ils sont pointables, a 06:00.
for code, element_ in ELEMENTS.items():
    if element_.scenario is None:
        engage(code, heure(element_.disponible[0], "04:30"))

def vie_courante():
    for operateur in OPERATEURS.values():
        if operateur.equipe == "AUCUNE":
            continue
        for jour in JOURS_OUVRES:
            if (operateur.code, jour) in RESERVES or (operateur.code, jour) in CONGES:
                continue
            if operateur.equipe == "NUIT" and jour.weekday() == 4:
                continue  # pas de nuit le vendredi
            journee_courante(operateur, jour)


def journee_courante(operateur: Operateur, jour: date):
    plan = fenetres_du_plan(jour, operateur.equipe)
    arrivee = decale(plan[0][0], random.randint(-8, 4))
    pause = decale(plan[0][1], random.randint(-3, 3))
    reprise = decale(plan[1][0], random.randint(0, 4))
    depart = decale(plan[1][1], random.randint(-5, 12))

    # Un pupitre sur vingt rejoue sa journee en differe : saisie datee a l'heure du geste, enregistree plus tard.
    differe = timedelta(hours=random.randint(1, 3)) if random.random() < 0.05 else timedelta(0)

    j = Journee(operateur)
    for type_, quand in (("ARRIVEE", arrivee), ("PAUSE", pause), ("REPRISE", reprise), ("DEPART", depart)):
        j.evenements.append(EvenementDePresence(type_, quand, quand + differe, POINTEUR))
    JOURNEES.append(j)

    postes = operateur.postes_a(jour) or [None]
    elements = elements_pointables(jour)
    curseur = arrivee + timedelta(minutes=random.randint(3, 15))
    fin_de_journee = depart - timedelta(minutes=random.randint(3, 10))

    while curseur < fin_de_journee - timedelta(minutes=30):
        element_ = random.choice(elements)
        suivi = suivi_de(element_.code)
        poste = random.choice(postes)
        duree = timedelta(minutes=random.choice([45, 60, 75, 90, 120, 150, 180, 210]))
        fin = min(curseur + duree, fin_de_journee)
        if pause <= curseur < reprise:
            curseur = reprise + timedelta(minutes=random.randint(1, 6))
            continue

        pointe(suivi, "DEBUT", operateur.code, poste, curseur, enregistre=curseur + differe)
        milieu = curseur + (fin - curseur) / 2
        if random.random() < 0.06 and milieu > curseur:
            # Piece ratee : la reprise est comptee a part, puis retour au bon travail ou arret.
            pointe(suivi, "NON_CONFORMITE", operateur.code, poste, milieu, enregistre=milieu + differe)
            if random.random() < 0.5:
                retour = milieu + (fin - milieu) / 2
                pointe(suivi, "DEBUT", operateur.code, poste, retour, enregistre=retour + differe)

        # Un operateur qui mene volontiers deux postes lance une seconde machine pendant la premiere.
        second = [p for p in postes if p is not None and p != poste]
        if operateur.parallele and second and random.random() < 0.45 and fin - curseur >= timedelta(minutes=90):
            autre = random.choice(second)
            autre_element = random.choice(elements)
            lancement = curseur + timedelta(minutes=random.randint(10, 40))
            arret = min(lancement + timedelta(minutes=random.randint(40, 150)), fin)
            pointe(suivi_de(autre_element.code), "DEBUT", operateur.code, autre, lancement, enregistre=lancement + differe)
            pointe(suivi_de(autre_element.code), "FIN", operateur.code, autre, arret, enregistre=arret + differe)

        if fin >= fin_de_journee and random.random() < 0.12:
            # Oubli d'arreter : le depart referme ce que personne n'a arrete, l'activite reste ouverte au journal.
            pass
        else:
            pointe(suivi, "FIN", operateur.code, poste, fin, enregistre=fin + differe)
        curseur = fin + timedelta(minutes=random.randint(2, 20))


vie_courante()

# Samedi 19/09 (P09) : un OF courant pointe pendant les heures du samedi.
samedi = d("2026-09-19")
pointe(suivi_de("PRD_CAPOT"), "DEBUT", "GIROD", "AJU", heure(samedi, "07:10"))
pointe(suivi_de("PRD_CAPOT"), "DEBUT", "VUILLERMOZ", "AJU", heure(samedi, "07:10"))
pointe(suivi_de("PRD_CAPOT"), "FIN", "GIROD", "AJU", heure(samedi, "11:20"))
pointe(suivi_de("PRD_CAPOT"), "FIN", "VUILLERMOZ", "AJU", heure(samedi, "11:20"))

# Clotures des elements courants dont la periode est echue : le lendemain de leur dernier jour pointable, 10:00.
for code, element_ in ELEMENTS.items():
    if element_.scenario is None and element_.disponible[1] < DERNIER_JOUR:
        suivi = suivi_de(code)
        cloture = heure(element_.disponible[1] + timedelta(days=3), "10:00")
        suivi.cloture = (cloture, cloture)

# Le moule de maintenance preventive est reengage apres sa cloture, pour une derniere intervention le 23/09.
reengage = engage("OF_PREV", heure(d("2026-09-22"), "16:00"))
chapuis_23 = [j for j in JOURNEES if j.operateur.code == "CHAPUIS" and j.debut().astimezone(PARIS).date() == d("2026-09-23")][0]
pointe(reengage, "DEBUT", "CHAPUIS", "REC", chapuis_23.actifs()[0].survenue + timedelta(minutes=1))
pointe(reengage, "FIN", "CHAPUIS", "REC", chapuis_23.actifs()[0].survenue + timedelta(minutes=46))


# ---------------------------------------------------------------------------------------------------------------------
# Situation du jour : ecrite relativement a now(), pour que le pupitre et le cout en cours aient quelque chose a montrer
# quel que soit le jour du chargement. Rey est presente et polit S22 depuis 2 h 50 ; Bernard est en pause.
# ---------------------------------------------------------------------------------------------------------------------

SITUATION_DU_JOUR = {
    "REY": {"arrivee": timedelta(hours=3), "pause": None},
    "BERNARD": {"arrivee": timedelta(hours=4), "pause": timedelta(minutes=30)},
}
S22_DEBUT = timedelta(hours=2, minutes=50)


# ---------------------------------------------------------------------------------------------------------------------
# Validation : les memes refus que le domaine, avant d'ecrire quoi que ce soit.
# ---------------------------------------------------------------------------------------------------------------------


def valide():
    # Journal de presence : automate, et aucune journee vide.
    for j in JOURNEES:
        assert j.actifs(), f"journee vide {j.id}"
        assert j.actifs()[0].type == "ARRIVEE", f"journee sans arrivee en tete {j.id}"
        j.etat()

    # Deux journees d'un meme operateur ne se chevauchent ni ne se touchent.
    par_operateur = defaultdict(list)
    for j in JOURNEES:
        par_operateur[j.operateur.code].append(j)
    for code, journees in par_operateur.items():
        journees.sort(key=lambda j: j.debut())
        for avant, apres in zip(journees, journees[1:]):
            assert avant.dernier_fait() < apres.debut(), f"chevauchement {code} {avant.debut()} / {apres.debut()}"
            # Une journee encore ouverte au moment de la suivante doit etre abandonnee a cet instant.
            if avant.depart() is None:
                assert apres.debut() > avant.debut() + SEUIL, f"arrivee absorbee ecrite {code} {apres.debut()}"

    # Journal d'atelier : automate par cle, bornes d'engagement et de cloture, habilitation a la saisie.
    for suivi in SUIVIS:
        etats = defaultdict(lambda: "ABSENTE")
        for e in suivi.actifs():
            etats[e.cle] = transition_d_activite(etats[e.cle], e.type)
        for e in suivi.evenements:
            assert e.survenue >= suivi.engagement, f"evenement avant engagement {suivi.element.code} {e.survenue}"
            if suivi.cloture:
                assert e.survenue <= suivi.cloture[0], f"evenement apres cloture {suivi.element.code} {e.survenue}"
            if e.poste:
                assert e.poste.code in e.operateur.postes_a(e.jour), f"non habilite {e.operateur.code} {e.poste.code} {e.jour}"
            assert e.survenue < MAINTENANT, "evenement futur"

    # Un element n'a qu'un suivi non cloture a la fois, et ses passages ne se recouvrent pas.
    par_element = defaultdict(list)
    for suivi in SUIVIS:
        par_element[suivi.element.code].append(suivi)
    for code, suivis in par_element.items():
        assert sum(1 for s in suivis if s.cloture is None) <= 1, f"deux suivis ouverts {code}"
        suivis.sort(key=lambda s: s.engagement)
        for avant, apres in zip(suivis, suivis[1:]):
            assert avant.cloture and avant.cloture[0] <= apres.engagement, f"passages recouvrants {code}"


valide()


# ---------------------------------------------------------------------------------------------------------------------
# Projections
# ---------------------------------------------------------------------------------------------------------------------


def etat_du_suivi(suivi: Suivi) -> str:
    if suivi.cloture:
        return "CLOTURE"
    etats = defaultdict(lambda: "ABSENTE")
    for e in suivi.actifs():
        etats[e.cle] = transition_d_activite(etats[e.cle], e.type)
    if any(etat != "ABSENTE" for etat in etats.values()):
        return "EN_COURS"
    return "INTERROMPU" if suivi.actifs() else "EN_ATTENTE"


def microsecondes(duree: timedelta) -> int:
    return (duree.days * 86_400 + duree.seconds) * 1_000_000 + duree.microseconds


# Noms : la numerotation reprend ou l'annee en etait, par type et par annee, dans l'ordre de creation.
COMPTEURS_INITIAUX = {("ORDRE_DE_FABRICATION", 2026): 183, ("PRODUIT", 2026): 30, ("PRODUIT", 2025): 46}
compteurs = dict(COMPTEURS_INITIAUX)
for element_ in sorted(ELEMENTS.values(), key=lambda e: (e.creation, e.id)):
    cle = (element_.type, element_.creation.year)
    compteurs[cle] = compteurs.get(cle, 0) + 1
    prefixe = "OF" if element_.type == "ORDRE_DE_FABRICATION" else "PRD"
    element_.nom = f"{prefixe}-{element_.creation.year:04d}-{compteurs[cle]:06d}"


# ---------------------------------------------------------------------------------------------------------------------
# Ecriture du SQL
# ---------------------------------------------------------------------------------------------------------------------


def ecrit_sql() -> str:
    lignes: list[str] = []
    ecrit = lignes.append

    ecrit("-- Jeu de donnees de recette : impeccmold, moulier de la Plastics Vallee (Oyonnax).")
    ecrit("-- Genere par src/main/docker/jeu-de-donnees/generer.py : ne pas modifier a la main, relancer le script.")
    ecrit("-- Valeurs attendues : ATTENDUS.md, a cote de ce fichier.")
    ecrit("--")
    ecrit("-- Chargement, depuis la racine du projet, l'application ayant demarre au moins une fois en profil local")
    ecrit("-- (c'est elle qui cree et migre le schema impeccmold) :")
    ecrit("--   docker compose -f src/main/docker/postgresql.yml exec -T postgresql \\")
    ecrit("--     psql -U glmproject -d glmproject -v ON_ERROR_STOP=1 < src/main/docker/jeu-de-donnees/impeccmold.sql")
    ecrit("--")
    ecrit("-- ATTENTION : le script VIDE d'abord toutes les tables metier du schema impeccmold.")
    ecrit("-- Horodatages en UTC, comme les ecrit Hibernate (hibernate.jdbc.time_zone: UTC) ; les commentaires sont")
    ecrit("-- en heure de Paris.")
    ecrit("")
    ecrit("begin;")
    ecrit("set local search_path to impeccmold;")
    ecrit("")
    ecrit(
        "truncate table evenement_d_atelier, suivi_d_atelier, evenement_de_presence, journee_de_travail, "
        "identite_evenement_atelier, operateur_poste, operateur, poste_de_travail, element_de_fabrication, "
        "compteur_d_elements_de_fabrication;"
    )
    ecrit("")
    ecrit("-- Seuil d'amplitude a sa valeur par defaut : 13 h.")
    ecrit("update parametrage set amplitude_maximale_minutes = 780, modifie_par = null, modifie_le = null where id = 1;")
    ecrit("")

    ecrit("-- Postes de travail : valeurs courantes du referentiel. Les evenements portent celles du jour de la saisie.")
    ecrit("insert into poste_de_travail (id, libelle, nature, cout_horaire) values")
    ecrit(
        ",\n".join(
            f"  ('{p.id}', {sql_texte(p.libelle)}, {sql_texte(p.nature)}, {sql_nombre(p.cout)})" for p in POSTES.values()
        )
        + ";"
    )
    ecrit("")

    ecrit("-- Operateurs : taux horaire courant. Les evenements portent celui du jour de la saisie.")
    ecrit("insert into operateur (id, nom, prenom, matricule, taux_horaire) values")
    ecrit(
        ",\n".join(
            f"  ('{o.id}', {sql_texte(o.nom)}, {sql_texte(o.prenom)}, {sql_texte(o.matricule)}, {sql_nombre(o.taux[-1][1])})"
            for o in OPERATEURS.values()
        )
        + ";"
    )
    ecrit("")

    ecrit("-- Habilitations courantes.")
    ecrit("insert into operateur_poste (operateur_id, poste_id) values")
    ecrit(
        ",\n".join(f"  ('{o.id}', '{POSTES[p].id}')" for o in OPERATEURS.values() for p in o.postes) + ";"
    )
    ecrit("")

    ecrit("-- Elements de fabrication.")
    ecrit("insert into element_de_fabrication (id, type, nom, reference, description, date_de_creation, date_de_modification) values")
    ecrit(
        ",\n".join(
            f"  ('{e.id}', '{e.type}', '{e.nom}', {sql_texte(e.reference)}, {sql_texte(e.description)}, "
            f"{sql_instant(e.creation)}, {sql_instant(e.creation)})"
            for e in sorted(ELEMENTS.values(), key=lambda e: e.nom)
        )
        + ";"
    )
    ecrit("")
    ecrit("insert into compteur_d_elements_de_fabrication (type, annee, numero) values")
    ecrit(",\n".join(f"  ('{t}', {a}, {n})" for (t, a), n in sorted(compteurs.items())) + ";")
    ecrit("")

    ecrit("-- Suivis d'atelier.")
    ecrit(
        "insert into suivi_d_atelier (id, element_id, element_nom, element_type, engagement_auteur, engagement_date, "
        "cloture_auteur, cloture_date_de_survenue, cloture_date_d_enregistrement, etat) values"
    )
    ecrit(
        ",\n".join(
            f"  ('{s.id}', '{s.element.id}', '{s.element.nom}', '{s.element.type}', '{GESTIONNAIRE}', {sql_instant(s.engagement)}, "
            + (
                f"'{GESTIONNAIRE}', {sql_instant(s.cloture[0])}, {sql_instant(s.cloture[1])}, "
                if s.cloture
                else "null, null, null, "
            )
            + f"'{etat_du_suivi(s)}')"
            for s in SUIVIS
        )
        + ";"
    )
    ecrit("")

    ecrit("-- Journal d'atelier.")
    evenements = sorted((e for s in SUIVIS for e in s.evenements), key=lambda e: (e.survenue, e.id))
    for paquet in range(0, len(evenements), 500):
        ecrit(
            "insert into evenement_d_atelier (id, suivi_id, type, operateur_id, poste_id, nature, auteur, date_de_survenue, "
            "date_d_enregistrement, annulation_auteur, annulation_date, annulation_motif, cout_horaire, taux_horaire) values"
        )
        ecrit(
            ",\n".join(
                f"  ('{e.id}', '{e.suivi.id}', '{e.type}', '{e.operateur.id}', "
                f"{sql_texte(e.poste.id if e.poste else None)}, {sql_texte(e.nature)}, '{e.auteur}', "
                f"{sql_instant(e.survenue)}, {sql_instant(e.enregistrement)}, "
                + (
                    f"'{e.annulation.auteur}', {sql_instant(e.annulation.date)}, {sql_texte(e.annulation.motif)}, "
                    if e.annulation
                    else "null, null, null, "
                )
                + f"{sql_nombre(e.cout)}, {sql_nombre(e.taux)})"
                for e in evenements[paquet : paquet + 500]
            )
            + ";"
        )
    ecrit("")

    ecrit("-- Journees de travail : etat, debut, fin, dernier_fait et amplitude sont les projections du journal.")
    ecrit(
        "insert into journee_de_travail (id, operateur_id, etat, debut, fin, dernier_fait, amplitude_microsecondes) values"
    )
    rangs = []
    for j in sorted(JOURNEES, key=lambda j: (j.debut(), j.id)):
        depart = j.depart()
        amplitude = microsecondes(depart - j.debut()) if depart else None
        rangs.append(
            f"  ('{j.id}', '{j.operateur.id}', '{j.etat()}', {sql_instant(j.debut())}, "
            f"{sql_instant(depart) if depart else 'null'}, {sql_instant(j.dernier_fait())}, {sql_nombre(amplitude)})"
        )
    ecrit(",\n".join(rangs) + ";")
    ecrit("")

    ecrit("-- Journal de presence.")
    presences = sorted(((j, e) for j in JOURNEES for e in j.evenements), key=lambda p: (p[1].survenue, p[1].id))
    for paquet in range(0, len(presences), 500):
        ecrit(
            "insert into evenement_de_presence (id, journee_id, type, auteur, date_de_survenue, date_d_enregistrement, "
            "annulation_auteur, annulation_date, annulation_motif) values"
        )
        ecrit(
            ",\n".join(
                f"  ('{e.id}', '{j.id}', '{e.type}', '{e.auteur}', {sql_instant(e.survenue)}, {sql_instant(e.enregistrement)}, "
                + (
                    f"'{e.annulation.auteur}', {sql_instant(e.annulation.date)}, {sql_texte(e.annulation.motif)})"
                    if e.annulation
                    else "null, null, null)"
                )
                for j, e in presences[paquet : paquet + 500]
            )
            + ";"
        )
    ecrit("")

    ecrit("-- Situation du jour, relative a l'instant du chargement.")
    ecrit("-- Rey : presente depuis 3 h, polit S22 depuis 2 h 50. Bernard : arrivee il y a 4 h, en pause depuis 30 min.")
    maintenant = "(now() at time zone 'utc')"
    rey = OPERATEURS["REY"]
    bernard = OPERATEURS["BERNARD"]
    j_rey = identifiant("50000000")
    j_bernard = identifiant("50000000")
    a_rey = identifiant("60000000")
    a_bernard = identifiant("60000000")
    p_bernard = identifiant("60000000")
    e_s22 = identifiant("70000000")
    s22 = suivi_de("S22")
    ecrit(
        "insert into journee_de_travail (id, operateur_id, etat, debut, fin, dernier_fait, amplitude_microsecondes) values\n"
        f"  ('{j_rey}', '{rey.id}', 'PRESENT', {maintenant} - interval '3 hours', null, {maintenant} - interval '3 hours', null),\n"
        f"  ('{j_bernard}', '{bernard.id}', 'EN_PAUSE', {maintenant} - interval '4 hours', null, {maintenant} - interval '30 minutes', null);"
    )
    ecrit(
        "insert into evenement_de_presence (id, journee_id, type, auteur, date_de_survenue, date_d_enregistrement, "
        "annulation_auteur, annulation_date, annulation_motif) values\n"
        f"  ('{a_rey}', '{j_rey}', 'ARRIVEE', '{POINTEUR}', {maintenant} - interval '3 hours', {maintenant} - interval '3 hours', null, null, null),\n"
        f"  ('{a_bernard}', '{j_bernard}', 'ARRIVEE', '{POINTEUR}', {maintenant} - interval '4 hours', {maintenant} - interval '4 hours', null, null, null),\n"
        f"  ('{p_bernard}', '{j_bernard}', 'PAUSE', '{POINTEUR}', {maintenant} - interval '30 minutes', {maintenant} - interval '30 minutes', null, null, null);"
    )
    po2 = POSTES["PO2"]
    ecrit(
        "insert into evenement_d_atelier (id, suivi_id, type, operateur_id, poste_id, nature, auteur, date_de_survenue, "
        "date_d_enregistrement, annulation_auteur, annulation_date, annulation_motif, cout_horaire, taux_horaire) values\n"
        f"  ('{e_s22}', '{s22.id}', 'DEBUT', '{rey.id}', '{po2.id}', {sql_texte(po2.nature)}, '{POINTEUR}', "
        f"{maintenant} - interval '2 hours 50 minutes', {maintenant} - interval '2 hours 50 minutes', null, null, null, "
        f"{po2.cout}, {rey.taux[-1][1]});"
    )
    ecrit(f"update suivi_d_atelier set etat = 'EN_COURS' where id = '{s22.id}';")
    ecrit("")

    ecrit("-- Identites reservees : chaque evenement ecrit, comme le fait la migration 2026/09/001.")
    ecrit("insert into identite_evenement_atelier (id, rejouable)")
    ecrit("select id, false from evenement_d_atelier union all select id, false from evenement_de_presence;")
    ecrit("")
    ecrit("commit;")
    return "\n".join(lignes) + "\n"


# ---------------------------------------------------------------------------------------------------------------------
# Valeurs attendues, recalculees selon les regles metier documentees
# ---------------------------------------------------------------------------------------------------------------------

SIX = Decimal("0.000001")
DEUX = Decimal("0.01")


def fenetres_a(j: Journee, pointages: list[datetime], maintenant: datetime):
    """Les fenetres de la journee lue a cet instant, et sa fin presumee si elle est abandonnee."""
    fenetres = j.fenetres()
    if j.depart() is not None:
        return [(a, b, False) for a, b in fenetres], None
    arrivee = j.debut()
    limite = arrivee + SEUIL
    if not maintenant > limite:
        return [(a, b, False) for a, b in fenetres], None
    dernier = j.dernier_fait()
    candidats = [p for p in pointages if arrivee <= p <= limite and p > dernier]
    fin = max(candidats) if candidats else dernier
    return [(a, b if b is not None else fin, b is None) for a, b in fenetres], fin


def contient(j: Journee, fin_presumee, instant: datetime) -> bool:
    if instant < j.debut():
        return False
    fin = j.depart() or fin_presumee
    return not (fin is not None and instant > fin)


def pointages_de(code: str) -> list[datetime]:
    return [e.survenue for s in SUIVIS for e in s.actifs() if e.operateur.code == code]


def intervalles(suivi: Suivi):
    """Repli du journal : (evenement ouvrant, categorie, debut, fin ou None)."""
    par_cle = defaultdict(list)
    for e in suivi.actifs():
        par_cle[e.cle].append(e)
    resultat = []
    for evenements in par_cle.values():
        etat = "ABSENTE"
        for rang, e in enumerate(evenements):
            etat = transition_d_activite(etat, e.type)
            fin = evenements[rang + 1].survenue if rang + 1 < len(evenements) else (suivi.cloture[0] if suivi.cloture else None)
            if etat != "ABSENTE":
                resultat.append((e, "TRAVAIL" if etat == "EN_COURS" else "NON_CONFORMITE", e.survenue, fin))
    return resultat


def reduit(intervalle, journees_de_l_operateur, maintenant):
    """Ramene aux fenetres de la journee ou l'intervalle a commence ; intact si aucune journee ne le contient."""
    e, categorie, debut, fin = intervalle
    for j, fenetres, fin_presumee in journees_de_l_operateur:
        if contient(j, fin_presumee, debut):
            parts = []
            for a, b, presume in fenetres:
                bas = max(a, debut)
                haut = b if fin is None else (fin if b is None else min(b, fin))
                if haut is None:
                    parts.append((e, categorie, bas, maintenant, False))
                elif haut > bas:
                    parts.append((e, categorie, bas, haut, presume and (fin is None or b <= fin)))
            return parts
    return [(e, categorie, debut, fin if fin is not None else maintenant, False)]


def presences_lues(maintenant):
    par_operateur = defaultdict(list)
    for j in JOURNEES:
        fenetres, fin = fenetres_a(j, pointages_de(j.operateur.code), maintenant)
        par_operateur[j.operateur.code].append((j, fenetres, fin))
    return par_operateur


def tranches_de_tout(maintenant):
    presences = presences_lues(maintenant)
    tranches = defaultdict(list)  # suivi -> tranches
    par_operateur = defaultdict(list)
    for suivi in SUIVIS:
        for intervalle in intervalles(suivi):
            for tranche in reduit(intervalle, presences[intervalle[0].operateur.code], maintenant):
                tranches[suivi.id].append(tranche)
                par_operateur[intervalle[0].operateur.code].append(tranche)
    return tranches, par_operateur


def heures(debut: datetime, fin: datetime) -> Decimal:
    millis = microsecondes(fin - debut) // 1000
    return (Decimal(millis) / Decimal(3_600_000)).quantize(SIX, rounding=ROUND_HALF_UP)


def decoupe(tranche, tranches_de_l_operateur):
    """La tranche decoupee aux bornes de tous les pointages de l'operateur, avec le nombre de postes occupes."""
    bornes = sorted({t[2] for t in tranches_de_l_operateur} | {t[3] for t in tranches_de_l_operateur})
    parts = []
    for bas, haut in zip(bornes, bornes[1:]):
        postes = {t[0].poste.code if t[0].poste else None for t in tranches_de_l_operateur if min(t[3], haut) > max(t[2], bas)}
        if not postes:
            continue
        a, b = max(tranche[2], bas), min(tranche[3], haut)
        if b > a:
            parts.append((a, b, len(postes)))
    return parts


def cout_de_revient(code: str, tranches, par_operateur):
    element_ = ELEMENTS[code]
    lignes = defaultdict(lambda: {"travail": timedelta(0), "nc": timedelta(0), "machine": Decimal(0), "mo": Decimal(0), "nc_periodes": [], "debut": None, "fin": None})
    for suivi in [s for s in SUIVIS if s.element is element_]:
        for tranche in tranches[suivi.id]:
            e, categorie, debut, fin, _ = tranche
            ligne = lignes[e.nature]
            if categorie == "TRAVAIL":
                ligne["travail"] += fin - debut
            else:
                ligne["nc"] += fin - debut
                ligne["nc_periodes"].append((debut, fin))
            ligne["debut"] = debut if ligne["debut"] is None else min(ligne["debut"], debut)
            ligne["fin"] = fin if ligne["fin"] is None else max(ligne["fin"], fin)
            for a, b, diviseur in decoupe(tranche, par_operateur[e.operateur.code]):
                h = heures(a, b)
                if e.cout is not None:
                    ligne["machine"] += e.cout * h
                if e.taux is not None:
                    ligne["mo"] += (e.taux * h / Decimal(diviseur)).quantize(SIX, rounding=ROUND_HALF_UP)
    resultat = []
    for nature in sorted(lignes, key=lambda n: (n is None, n or "")):
        ligne = lignes[nature]
        ligne["machine"] = ligne["machine"].quantize(DEUX, rounding=ROUND_HALF_UP)
        ligne["mo"] = ligne["mo"].quantize(DEUX, rounding=ROUND_HALF_UP)
        resultat.append((nature, ligne))
    return resultat


def duree_texte(duree: timedelta) -> str:
    secondes = int(duree.total_seconds())
    signe = "-" if secondes < 0 else ""
    secondes = abs(secondes)
    h, reste = divmod(secondes, 3600)
    m, s = divmod(reste, 60)
    return f"{signe}{h} h {m:02d}" + (f" min {s:02d} s" if s else "")


def iso8601(duree: timedelta) -> str:
    secondes = int(duree.total_seconds())
    if secondes == 0:
        return "PT0S"
    h, reste = divmod(secondes, 3600)
    m, s = divmod(reste, 60)
    return "PT" + (f"{h}H" if h else "") + (f"{m}M" if m else "") + (f"{s}S" if s else "")


def local(instant: datetime) -> str:
    return instant.astimezone(PARIS).strftime("%d/%m %H:%M")


def releve(maintenant):
    """Heures pointees et presumees par operateur et par jour de Paris : fenetres closes, coupees a minuit."""
    pointees = defaultdict(timedelta)
    presumees = defaultdict(timedelta)
    for j in JOURNEES:
        fenetres, _ = fenetres_a(j, pointages_de(j.operateur.code), maintenant)
        for a, b, presume in fenetres:
            if b is None:
                continue
            curseur = a
            while curseur < b:
                jour = curseur.astimezone(PARIS).date()
                minuit = datetime.combine(jour + timedelta(days=1), time(0), tzinfo=PARIS)
                fin = min(b, minuit)
                (presumees if presume else pointees)[(j.operateur.code, jour)] += fin - curseur
                curseur = fin
    return pointees, presumees


def anomalies(maintenant):
    lignes = []
    for j in JOURNEES:
        if j.etat() != "ABSENT" and maintenant > j.debut() + SEUIL:
            lignes.append((j.debut(), j.operateur, "JOURNEE_SANS_DEPART", j))
        depart = j.depart()
        if depart is not None and depart - j.debut() > SEUIL:
            lignes.append((j.debut(), j.operateur, "AMPLITUDE_EXCESSIVE", j))
    return sorted(lignes, key=lambda l: l[0], reverse=True)


def ecrit_attendus() -> str:
    tranches, par_operateur = tranches_de_tout(MAINTENANT)
    pointees, presumees = releve(MAINTENANT)
    sortie: list[str] = []
    ecrit = sortie.append

    ecrit("# Jeu de données impeccmold : valeurs attendues")
    ecrit("")
    ecrit(
        "Généré par `generer.py` avec `impeccmold.sql` : ne pas modifier à la main. Les valeurs sont recalculées par le "
        "script selon les règles de [contexte-metier.md](../../../../documentation/contexte-metier.md), sans passer par "
        "le code Java. Un écart entre ce fichier et l'API est donc un défaut de l'un ou de l'autre."
    )
    ecrit("")
    ecrit(
        f"Heures de Paris. Seuil d'amplitude : 13 h. Les valeurs ne dépendent pas de l'instant de lecture, sauf le bloc "
        f"« situation du jour » (Rey, Bernard, S22), écrit relativement à `now()` au chargement."
    )
    ecrit("")
    ecrit("## Chargement")
    ecrit("")
    ecrit("L'application doit avoir démarré au moins une fois en profil `local` : c'est elle qui crée et migre le schéma `impeccmold`. Puis, depuis la racine du projet :")
    ecrit("")
    ecrit("```bash")
    ecrit("docker compose -f src/main/docker/postgresql.yml exec -T postgresql \\")
    ecrit("  psql -U glmproject -d glmproject -v ON_ERROR_STOP=1 < src/main/docker/jeu-de-donnees/impeccmold.sql")
    ecrit("```")
    ecrit("")
    ecrit("Le script **vide d'abord toutes les tables métier** du schéma, remet le seuil à 13 h, puis charge tout dans une seule transaction. Pour modifier le jeu : éditer `generer.py`, puis `python3 generer.py`.")
    ecrit("")
    ecrit("## Contenu")
    ecrit("")
    ecrit(f"- {len(OPERATEURS)} opérateurs, {len(POSTES)} postes de travail, {len(ELEMENTS)} éléments de fabrication, {len(SUIVIS)} suivis d'atelier ;")
    ecrit(f"- {len(JOURNEES) + 2} journées de travail, {sum(len(j.evenements) for j in JOURNEES) + 3} événements de présence ;")
    ecrit(f"- {sum(len(s.evenements) for s in SUIVIS) + 1} événements d'atelier ;")
    ecrit("- du lundi 24/08/2026 (semaine 35, reprise après la fermeture d'été) au vendredi 25/09/2026 (semaine 39), plus deux samedis et un dimanche soir.")
    ecrit("")

    ecrit("## Référentiel")
    ecrit("")
    ecrit("| Opérateur | Matricule | Taux courant | Postes habilités | Métier |")
    ecrit("| --- | --- | ---: | --- | --- |")
    for o in OPERATEURS.values():
        taux_ = o.taux[-1][1]
        historique = " (25,00 avant le 14/09)" if o.code == "MERMET" else ""
        ecrit(
            f"| {o.nom} {o.prenom} | {o.matricule or '—'} | {taux_ if taux_ is not None else '—'}{historique} | "
            f"{', '.join(POSTES[p].libelle for p in o.postes) or '—'} | {o.metier} |"
        )
    ecrit("")
    ecrit("| Poste | Nature | Coût horaire |")
    ecrit("| --- | --- | ---: |")
    for p in POSTES.values():
        note = {"HUR": " (nature « Fraisage » avant le 16/09)", "FIL": " (75,00 avant le 14/09)"}.get(p.code, "")
        ecrit(f"| {p.libelle} | {p.nature} | {p.cout if p.cout is not None else '—'}{note} |")
    ecrit("")

    ecrit("## Cas limites")
    ecrit("")
    ecrit("Chaque scénario occupe ses opérateurs toute la journée : aucun pointage aléatoire ne s'y mêle, les chiffres se vérifient à la main.")
    ecrit("")
    cas = [
        ("S01", "BOUVIER mar. 25/08", "Cas nominal : tour CN 08:00 → 11:00.", "3 h, machine 195,00, MO 76,20."),
        ("S16", "BOUVIER mer. 26/08", "Un DEBUT sur S01 à 08:00 annulé (« Mauvais OF sélectionné »), le bon OF lancé à 08:02 → 12:00.", "S16 : 3 h 58. S01 inchangé : l'événement annulé ne compte pas."),
        ("S02", "MOREL mer. 26/08", "Équipe d'après-midi 13:00–21:00, pause 17:00–17:30. DEBUT 14:00 jamais arrêté ; clôture le 28/08.", "14:00–17:00 + 17:30–21:00 = 6 h 30 : la pause scinde, le départ tronque, la clôture ne rallonge pas."),
        ("S03", "MARTIN Sébastien jeu. 27/08", "5 axes 06:00 → 10:00 (S03A), Huron 07:00 → 08:30 (S03B), pause 09:00–09:20.", "S03A 3 h 40, MO 75,83 (divisé par 2 de 07:00 à 08:30 seulement) ; S03B 1 h 30, MO 19,50."),
        ("S04", "PERRIN lun. 31/08", "Enfonçage et fil sur le même OF 08:00 (fil jusqu'à 10:00, enfonçage 11:00), puis quatre postes 13:30 → 14:30.", "MO totale 111,20 = 27,80 × 4 h de présence réellement occupées : une heure n'est jamais payée deux fois."),
        ("S05", "BERNARD mar. 01/09", "Un seul établi pour trois OF (A et B 08:00 → 10:00, C 09:00 → 10:00).", "Aucune division (un seul poste) ; chaque OF porte le coût machine entier."),
        ("S06", "ROLLET 02 et 03/09", "DEBUT 08:00, NON_CONFORMITE 10:00, DEBUT 11:00, FIN 15:00 ; le lendemain NON_CONFORMITE d'emblée 08:00 → 09:30.", "Travail 5 h, non-conformité 2 h 30 en deux périodes datées."),
        ("S07", "MARTIN Sébastien 07-08/09", "Exemple E2 : départ oublié lundi, arrivée mardi 07:00 (nouvelle journée), relance de l'OF 42 à 07:05.", "OF 42 lundi 7 h dont 3 h présumées, + 3 h 55 mardi ; anomalie JOURNEE_SANS_DEPART ; relevé lundi 5 h pointées + 3 h présumées."),
        ("S08", "VUILLERMOZ 07-08/09", "Même situation, départ régularisé à 16:30 le mardi 08:10 par le gestionnaire ; relance mardi 07:15.", "Aucune anomalie ; relevé lundi 8 h 30 pointées ; établi sans coût horaire : machine 0,00."),
        ("S10", "DUPONT 31/08 → 01/09", "Exemple E1 : nuit 20:00 → 08:00, arrivée de 03:00 absorbée (rien n'est écrit), second OF à 03:02.", "Une seule journée ; relevé lundi 4 h, mardi 8 h ; MO divisée par 2 de 03:02 à 08:00."),
        ("S09", "DUPONT 14 → 16/09", "Exemple E3 : nuit sans départ, arrivée le soir suivant, relance de l'OF à 20:10.", "Lundi : 5 min présumées au relevé, 0 sur l'OF (la fin présumée est le démarrage lui-même) ; anomalie JOURNEE_SANS_DEPART."),
        ("S11", "DUPONT dim. 20/09 → lun. 21/09", "Exemple E7 : poste de nuit à cheval sur les semaines 38 et 39.", "Semaine 38 dimanche 4 h, semaine 39 lundi 8 h."),
        ("S12", "COLLET 14-15/09", "Exemple E5 : pupitre hors ligne, le départ de mardi 08:30 ouvre une journée nulle (arrivée implicite + départ), puis arrivée 08:35.", "Lundi abandonnée, fin présumée 11:00 (dernière fin d'OF) ; mardi 0 h + 6 h 55."),
        ("S13", "GIROD 25/08, GUYON 09/09", "Réengagé après clôture : ajustage 2 h au premier passage, essai 2 h au second.", "Le rapport additionne les deux passages."),
        ("S14", "GIROD sam. 12/09", "Essai régularisé par le gestionnaire le lundi, sans aucune présence ce samedi.", "Rendu intact : 2 h, alors que le relevé du samedi est vide."),
        ("S15", "JACQUEMOUD ven. 28/08", "FIN pointée 15:00, corrigée à 11:30 le 31/08 (annulation + insertion).", "2 h 30."),
        ("S17", "DAVID et PONCET mar. 15/09", "Sans poste : intérimaire sans matricule (20,80) et apprenti sans taux, 08:00 → 12:00 ; S17B de front 10:00 → 11:00.", "Ligne sans nature ; aucune division (l'absence de poste compte pour un poste) ; l'apprenti ne coûte rien."),
        ("S18", "MERMET jeu. 17/09", "Trois postes pendant 7 min, dont la Huron requalifiée « Perçage » depuis le 16/09 ; taux 26,50 depuis le 14/09.", "Arrondi : lignes Fraisage 23,92 / 2,06 et Perçage 4,90 / 1,03."),
        ("S19", "VUILLERMOZ lun. 21/09", "Exemple E4 depuis le lot 3 : parti sans pointer, retour à 19:30 absorbé, départ à 23:00 au-delà du seuil.", "Journée du matin abandonnée (fin présumée 16:45), journée nulle à 23:00 ; les 3 h 30 du soir sont perdues."),
        ("S20", "—", "Engagé, jamais pointé.", "Suivi EN_ATTENTE, rapport vide."),
        ("S21", "—", "Créé, jamais engagé.", "Aucun suivi, rapport vide (pas une erreur)."),
        ("S22", "REY, maintenant", "DEBUT il y a 2 h 50 sur l'établi polissage 2, journée ouverte il y a 3 h.", "Suivi EN_COURS ; coût croissant avec l'heure de lecture (18 €/h machine + 24 €/h MO)."),
        ("S23", "BOUVIER 21-22/09", "Tour conventionnel lancé lundi 14:00 et jamais arrêté (S23A, clôturé mercredi) ; mardi, tour CN 08:00 → 10:00 (S23B).", "S23A 2 h 30 (ramené à lundi) ; S23B 2 h, MO 50,80 : l'oubli de la veille ne divise pas le taux du lendemain."),
        ("P01", "GUYON 22-23/09, MOREL 22/09", "Amplitude de 13 h pile (pointée), de 13 h + 0,5 s (départ corrigé), et de 12 h 30.", "Seule la deuxième est en anomalie AMPLITUDE_EXCESSIVE ; la troisième le devient si le seuil passe à 12 h (E8)."),
        ("P02", "GIROD jeu. 24/09", "Départ régularisé à 21:30 le lendemain.", "Amplitude 14 h 30 : AMPLITUDE_EXCESSIVE."),
        ("P03", "BERNARD jeu. 03/09", "Une pause à 10:00 saisie par erreur, annulée.", "Relevé 8 h 15 : la pause annulée ne compte pas."),
        ("P04", "CHAPUIS mar. 08/09", "Arrivée 06:00 corrigée en 07:45.", "Relevé 4 h 55."),
        ("P05", "CHAPUIS mer. 16/09", "Journée abandonnée restée en pause.", "4 h pointées, 0 présumée ; anomalie JOURNEE_SANS_DEPART."),
        ("P06", "REY 10-11/09", "Arrivée jeudi sans rien d'autre ; une pause reçue vendredi 09:00 ouvre une journée qui commence en pause.", "Jeudi : 0 h ; vendredi 6 h 45 ; anomalie sur jeudi."),
        ("P07", "FAVRE ven. 18/09", "Départ pointé depuis la pause.", "Relevé 4 h : la fenêtre s'arrête à la pause."),
        ("P08", "REY lun. 14/09", "Formation sécurité : présente 8 h, aucun OF.", "Présence sans affectation : 8 h au relevé, rien au coût."),
        ("P09", "GIROD, VUILLERMOZ sam. 19/09", "Heures du samedi sur le moule capot de rétroviseur.", "4 h 30 au relevé du samedi."),
        ("—", "COLLET", "Déshabilité du tour conventionnel le 04/09 après y avoir pointé.", "Ses pointages antérieurs restent valorisés ; le pupitre ne lui propose plus ce tour."),
        ("—", "MARTIN Sandrine", "En congés la semaine 37.", "Relevé de la semaine 37 vide, sept jours à zéro."),
        ("—", "SERVE", "Embauché, habilité, jamais pointé.", "ABSENT au pupitre ; seul opérateur supprimable, avec la rectifieuse cylindrique pour les postes."),
    ]
    ecrit("| Cas | Qui, quand | Situation | Attendu |")
    ecrit("| --- | --- | --- | --- |")
    for code, qui, situation, attendu in cas:
        ecrit(f"| {code} | {qui} | {situation} | {attendu} |")
    ecrit("")

    ecrit("## Coût de revient par élément")
    ecrit("")
    ecrit("`GET /api/couts-de-revient/{elementId}`, rôle gestionnaire. Une ligne par nature d'opération, triées par nature, sans nature en dernier. Temps au format de l'API entre parenthèses.")
    ecrit("")
    for element_ in sorted(ELEMENTS.values(), key=lambda e: (e.scenario is None, e.scenario or "", e.nom)):
        if element_.code == "S22":
            continue
        lignes = cout_de_revient(element_.code, tranches, par_operateur)
        titre = f"{element_.scenario} — " if element_.scenario else ""
        ecrit(f"### {titre}{element_.nom} · {element_.reference or 'sans référence'}")
        ecrit("")
        ecrit(f"`{element_.id}` — {element_.description or 'sans description'}")
        ecrit("")
        if not lignes:
            ecrit("Rapport vide.")
            ecrit("")
            continue
        ecrit("| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |")
        ecrit("| --- | ---: | ---: | ---: | ---: | ---: |")
        total_machine = Decimal(0)
        total_mo = Decimal(0)
        total_travail = timedelta(0)
        total_nc = timedelta(0)
        for nature, ligne in lignes:
            total_machine += ligne["machine"]
            total_mo += ligne["mo"]
            total_travail += ligne["travail"]
            total_nc += ligne["nc"]
            ecrit(
                f"| {nature or '(sans nature)'} | {duree_texte(ligne['travail'])} ({iso8601(ligne['travail'])}) | "
                f"{duree_texte(ligne['nc'])} | {ligne['machine']} | {ligne['mo']} | {ligne['machine'] + ligne['mo']} |"
            )
        ecrit(
            f"| **Total** | **{duree_texte(total_travail)}** | **{duree_texte(total_nc)}** | **{total_machine}** | "
            f"**{total_mo}** | **{total_machine + total_mo}** |"
        )
        periodes = [p for _, ligne in lignes for p in ligne["nc_periodes"]]
        if periodes:
            ecrit("")
            ecrit("Non-conformités : " + ", ".join(f"{local(a)} → {local(b)}" for a, b in sorted(periodes)) + ".")
        ecrit("")

    ecrit("## Relevé d'heures par opérateur")
    ecrit("")
    ecrit(
        "`GET /api/synthese-heures/...` : durée pointée (fenêtres closes, pauses exclues, coupée à minuit) et, entre "
        "parenthèses, durée présumée. Une cellule vide vaut 0. Le jour du chargement, hors de ces tableaux, "
        "Bernard compte en plus 3 h 30 pointées (arrivée → pause) et Rey rien, sa fenêtre étant encore ouverte."
    )
    ecrit("")
    semaines = sorted({(jour.isocalendar()[0], jour.isocalendar()[1]) for (_, jour) in list(pointees) + list(presumees)})
    for annee, semaine in semaines:
        lundi = date.fromisocalendar(annee, semaine, 1)
        jours = [lundi + timedelta(days=n) for n in range(7)]
        ecrit(f"### Semaine {semaine} — du {lundi.strftime('%d/%m')} au {jours[-1].strftime('%d/%m/%Y')}")
        ecrit("")
        ecrit("| Opérateur | " + " | ".join(j.strftime("%a %d").replace("Mon", "lun").replace("Tue", "mar").replace("Wed", "mer").replace("Thu", "jeu").replace("Fri", "ven").replace("Sat", "sam").replace("Sun", "dim") for j in jours) + " | Total |")
        ecrit("| --- |" + " ---: |" * 8)
        for o in OPERATEURS.values():
            cellules = []
            total = timedelta(0)
            total_presume = timedelta(0)
            for jour in jours:
                p = pointees.get((o.code, jour), timedelta(0))
                q = presumees.get((o.code, jour), timedelta(0))
                total += p
                total_presume += q
                texte = duree_texte(p) if p else ""
                if q:
                    texte += f" ({duree_texte(q)})"
                cellules.append(texte)
            total_texte = duree_texte(total) + (f" ({duree_texte(total_presume)})" if total_presume else "")
            ecrit(f"| {o.nom} {o.prenom} | " + " | ".join(cellules) + f" | {total_texte} |")
        ecrit("")

    ecrit("## Anomalies")
    ecrit("")
    ecrit("`GET /api/atelier/anomalies`, la plus récente d'abord, jugées au seuil de 13 h. Les journées de la situation du jour restent sous le seuil au chargement.")
    ecrit("")
    ecrit("| Opérateur | Arrivée | Type | Journée |")
    ecrit("| --- | --- | --- | --- |")
    for debut, operateur, type_, j in anomalies(MAINTENANT):
        ecrit(f"| {operateur.nom} {operateur.prenom} | {local(debut)} | {type_} | `{j.id}` |")
    ecrit("")

    ecrit("## États des suivis d'atelier")
    ecrit("")
    ecrit("| Élément | Référence | Passage | Engagé le | Clôturé le | État |")
    ecrit("| --- | --- | ---: | --- | --- | --- |")
    rangs = defaultdict(int)
    for suivi in sorted(SUIVIS, key=lambda s: (s.element.nom, s.engagement)):
        rangs[suivi.element.code] += 1
        etat = "EN_COURS" if suivi.element.code == "S22" else etat_du_suivi(suivi)
        ecrit(
            f"| {suivi.element.nom} | {suivi.element.reference or '—'} | {rangs[suivi.element.code]} | {local(suivi.engagement)} | "
            f"{local(suivi.cloture[0]) if suivi.cloture else '—'} | {etat} |"
        )
    ecrit("")

    ecrit("## Pupitre au chargement")
    ecrit("")
    ecrit(
        "`GET /api/pupitre/referentiel` : REY est PRESENT (présente jusqu'à son arrivée + 13 h), BERNARD EN_PAUSE, tous "
        "les autres ABSENT — y compris ceux dont une journée abandonnée est restée PRESENT en base. Les éléments rendus "
        "sont ceux dont le suivi n'est pas clôturé."
    )
    ecrit("")
    return "\n".join(sortie) + "\n"


def ecrit_canonique() -> str:
    """Les memes lignes que le test de verification ecrit depuis l'application, pour un diff direct."""
    tranches, par_operateur = tranches_de_tout(MAINTENANT)
    pointees, presumees = releve(MAINTENANT)
    lignes = []
    for element_ in ELEMENTS.values():
        if element_.code == "S22":
            continue
        for nature, ligne in cout_de_revient(element_.code, tranches, par_operateur):
            lignes.append(
                f"COUT|{element_.id}|{nature or '-'}|{microsecondes(ligne['travail']) // 1000}|"
                f"{microsecondes(ligne['nc']) // 1000}|{ligne['machine']}|{ligne['mo']}"
            )
    for (code, jour) in set(pointees) | set(presumees):
        p = pointees.get((code, jour), timedelta(0))
        q = presumees.get((code, jour), timedelta(0))
        if (p or q) and jour < d("2026-09-26"):
            lignes.append(f"HEURES|{OPERATEURS[code].id}|{jour}|{microsecondes(p) // 1000}|{microsecondes(q) // 1000}")
    for _, _, type_, j in anomalies(MAINTENANT):
        lignes.append(f"ANOMALIE|{j.id}|{type_}")
    for suivi in SUIVIS:
        if suivi.element.code == "S22":
            continue
        total = sum((t[3] - t[2] for t in tranches[suivi.id]), timedelta(0))
        presume = sum((t[3] - t[2] for t in tranches[suivi.id] if t[4]), timedelta(0))
        lignes.append(f"TEMPS|{suivi.id}|{microsecondes(total) // 1000}|{microsecondes(presume) // 1000}")
    for o in OPERATEURS.values():
        etat = {"REY": "PRESENT", "BERNARD": "EN_PAUSE"}.get(o.code, "ABSENT")
        lignes.append(f"PUPITRE|{o.id}|{etat}")
    for suivi in SUIVIS:
        if suivi.cloture is None:
            lignes.append(f"SUIVI_PUPITRE|{suivi.id}")
    return "\n".join(sorted(lignes)) + "\n"


import os

if os.environ.get("CANONIQUE"):
    Path(os.environ["CANONIQUE"]).write_text(ecrit_canonique(), encoding="utf-8")

(ICI / "impeccmold.sql").write_text(ecrit_sql(), encoding="utf-8")
(ICI / "ATTENDUS.md").write_text(ecrit_attendus(), encoding="utf-8")
print("impeccmold.sql et ATTENDUS.md generes.")
