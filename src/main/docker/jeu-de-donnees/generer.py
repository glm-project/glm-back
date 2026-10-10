#!/usr/bin/env python3
"""
Jeu de donnees de recette de l'entreprise impeccmold : cinq produits a l'atelier (trois OF, deux moules), dix
operateurs et un mois de pointages, avec deux produits clotures, deux pas encore
engages et quelques FIN oubliees (activites terminees automatiquement) (du 7 septembre au 9 octobre 2026, jours ouvres).

Deterministe (graine fixe), il ecrit impeccmold.sql a cote de lui. Horodatages en secondes epoch UTC, comme les
colonnes NUMERIC(30, 9) de l'atelier.

Usage : python3 generer.py
"""

import random
import unicodedata
import uuid
from datetime import date, datetime, time, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

PARIS = ZoneInfo("Europe/Paris")
POINTEUR = "user.impeccmold"
GESTIONNAIRE = "gestionnaire.impeccmold"
DUREE_MAX = 13 * 3600
PREMIER_JOUR, DERNIER_JOUR = date(2026, 9, 7), date(2026, 10, 9)
random.seed(20260907)
uid = lambda: uuid.UUID(int=random.getrandbits(128), version=4)


def t(valeur):
    return "null" if valeur is None else f"'{str(valeur).replace(chr(39), chr(39) * 2)}'"


def epoch(jour, minutes):
    return datetime.combine(jour, time(minutes // 60, minutes % 60), tzinfo=PARIS).timestamp()


def cle(libelle):
    return "".join(c for c in unicodedata.normalize("NFD", libelle) if not unicodedata.combining(c)).lower().strip()


NATURES = {n: uid() for n in ["Usinage", "Électroérosion", "Polissage", "Ajustage", "Contrôle"]}
POSTES = [  # libelle, nature, cout horaire
    ("Centre d'usinage 5 axes", "Usinage", "85.00"),
    ("Fraiseuse UGV", "Usinage", "70.00"),
    ("Enfonçage", "Électroérosion", "65.00"),
    ("Fil", "Électroérosion", "60.00"),
    ("Établi polissage", "Polissage", "45.00"),
    ("Banc d'ajustage", "Ajustage", "50.00"),
    ("Machine tridimensionnelle", "Contrôle", "55.00"),
]
POSTES = [(uid(), *p) for p in POSTES]
NOMS = [("Rey", "Julien"), ("Bernard", "Sophie"), ("Martin", "Lucas"), ("Durand", "Emma"), ("Petit", "Hugo"),
        ("Moreau", "Léa"), ("Girard", "Thomas"), ("Roux", "Camille"), ("Fournier", "Nathan"), ("Lambert", "Chloé")]
OPERATEURS = []
for i, (nom, prenom) in enumerate(NOMS):
    postes = random.sample(POSTES, 2 if i % 3 else 3)
    OPERATEURS.append((uid(), nom, prenom, f"M{100 + i:03d}", f"{random.choice([22, 24, 26, 28, 30])}.50", postes))
ELEMENTS = [  # categorie, numero, reference, description
    ("OF", 1, "CMD-24561", "Série 2 000 capots de rétroviseur"),
    ("OF", 2, "CMD-24577", "Reprise empreinte bouchon 28 mm"),
    ("OF", 3, "CMD-24590", "Modification tiroir moule boîtier"),
    ("MOULE", 1, "M-2026-PEUG-01", "Moule 4 empreintes clip de planche de bord"),
    ("MOULE", 2, "M-2026-SEB-02", "Moule bi-matière poignée de casserole"),
    ("OF", 4, "CMD-24502", "Retouche éjecteurs moule couvercle"),  # cloture
    ("MOULE", 3, "M-2026-VAL-03", "Moule 2 empreintes grille d'aération"),  # cloture
    ("OF", 5, "CMD-24612", "Série 500 bouchons verseurs"),  # pas encore engage
    ("MOULE", 4, "M-2026-ALP-04", "Moule capot de batterie"),  # pas encore engage
]
ELEMENTS = [(uid(), c, f"{c}-2026-{n:06d}", r, d) for c, n, r, d in ELEMENTS]
ENGAGEMENT = epoch(date(2026, 9, 4), 9 * 60)
CLOTURE_JOUR = date(2026, 9, 18)
CLOTURE = epoch(CLOTURE_JOUR, 17 * 60 + 30)
ENGAGES, CLOTURES = ELEMENTS[:7], {e[0] for e in ELEMENTS[5:7]}

lignes = [
    "-- Jeu de donnees de recette impeccmold : genere par generer.py, ne pas modifier a la main.",
    "-- ATTENTION : vide d'abord les tables metier du schema impeccmold. Chargement (application demarree une fois) :",
    "--   docker compose -f src/main/docker/postgresql.yml exec -T postgresql \\",
    "--     psql -U glmproject -d glmproject -v ON_ERROR_STOP=1 < src/main/docker/jeu-de-donnees/impeccmold.sql",
    "begin;",
    "set local search_path to impeccmold;",
    "truncate table activite_d_atelier, evenement_d_atelier, pointage_ignore_d_atelier, suivi_d_atelier, operateur_poste,"
    " operateur, poste_de_travail, nature_de_travail, element_de_fabrication, compteur_d_elements_de_fabrication,"
    " categorie_de_produit;",
    "insert into categorie_de_produit (code, rang) values ('OF', 0), ('MOULE', 1);",
]
ecrit = lignes.append


def valeurs(table, colonnes, rangs):
    ecrit(f"insert into {table} ({colonnes}) values\n  " + ",\n  ".join("(" + ", ".join(r) + ")" for r in rangs) + ";")


valeurs("nature_de_travail", "id, libelle, cle", [(t(i), t(n), t(cle(n))) for n, i in NATURES.items()])
valeurs("poste_de_travail", "id, libelle, nature_id, cout_horaire", [(t(i), t(l), t(NATURES[n]), c) for i, l, n, c in POSTES])
valeurs("operateur", "id, nom, prenom, identifiant, taux_horaire", [(t(o[0]), t(o[1]), t(o[2]), t(o[3]), o[4]) for o in OPERATEURS])
valeurs("operateur_poste", "operateur_id, poste_id", [(t(o[0]), t(p[0])) for o in OPERATEURS for p in o[5]])
creation = datetime(2026, 9, 3, 14, 0).isoformat(sep=" ")
valeurs("element_de_fabrication", "id, categorie, nom, reference, description, date_de_creation, date_de_modification",
        [(t(i), t(c), t(n), t(r), t(d), t(creation), t(creation)) for i, c, n, r, d in ELEMENTS])
valeurs("compteur_d_elements_de_fabrication", "categorie, annee, numero", [("'OF'", "2026", "5"), ("'MOULE'", "2026", "4")])

evenements, activites, nb_par_suivi = [], [], {e[0]: 0 for e in ENGAGES}
oublis = 0
jour = PREMIER_JOUR
while jour <= DERNIER_JOUR:
    if jour.weekday() < 5:
        for op_id, _, _, _, taux, postes in OPERATEURS:
            if random.random() < 0.08:  # absent
                continue
            minute, fin_de_journee = random.choice([420, 450, 480]), random.choice([960, 990, 1020])
            while minute < fin_de_journee - 30:
                duree = min(random.choice([60, 90, 120, 150, 180, 240]), fin_de_journee - minute)
                if 720 <= minute + duree // 2 < 780:
                    minute = 780
                    continue
                ouverts = [e[0] for e in ENGAGES if jour <= CLOTURE_JOUR or e[0] not in CLOTURES]
                suivi = random.choice(ouverts)
                poste_id, _, nature, cout = random.choice(postes)
                categorie = "NON_CONFORMITE" if random.random() < 0.05 else "TRAVAIL"
                debut, fin = epoch(jour, minute), epoch(jour, minute + duree)
                ouvrant = uid()
                evenements.append((ouvrant, suivi, categorie if categorie == "NON_CONFORMITE" else "DEBUT", ouvrant, op_id,
                                   poste_id, NATURES[nature], cout, taux, DUREE_MAX, debut))
                # FIN oubliee, seulement sur la derniere activite du jour : l'echeance tombe avant la reprise du lendemain
                oubli = minute + duree >= fin_de_journee - 30 and suivi not in CLOTURES and oublis < 12 and random.random() < 0.03
                if oubli:
                    oublis += 1
                    fin = None
                else:
                    evenements.append((uid(), suivi, "FIN", None, op_id, poste_id, None, None, None, None, fin))
                activites.append((ouvrant, suivi, op_id, poste_id, NATURES[nature], categorie, debut, debut + DUREE_MAX, fin))
                nb_par_suivi[suivi] += 1 if oubli else 2
                minute += duree + random.choice([0, 0, 5, 10, 15])
    jour += timedelta(days=1)

cloture = lambda i: (t(GESTIONNAIRE), f"{CLOTURE:.0f}", f"{CLOTURE:.0f}") if i in CLOTURES else ("null",) * 3
valeurs("suivi_d_atelier", "id, revision, element_id, element_nom, element_categorie, engagement_auteur, engagement_date, "
        "cloture_auteur, cloture_date_de_survenue, cloture_date_d_enregistrement",
        [(t(i), str(nb_par_suivi[i] + (i in CLOTURES)), t(i), t(n), t(c), t(GESTIONNAIRE), f"{ENGAGEMENT:.0f}", *cloture(i))
         for i, c, n, _, _ in ENGAGES])
s = lambda v: "null" if v is None else str(v)
for k in range(0, len(evenements), 1000):
    valeurs("evenement_d_atelier", "id, suivi_id, type, activite_id, operateur_id, poste_id, nature_id, cout_horaire, "
            "taux_horaire, duree_max_secondes, auteur, origine, date_de_survenue, date_d_enregistrement",
            [(t(e[0]), t(e[1]), t(e[2]), t(e[3]), t(e[4]), t(e[5]), t(e[6]), s(e[7]), s(e[8]), s(e[9]), t(POINTEUR),
              "'POINTAGE'", f"{e[10]:.0f}", f"{e[10]:.0f}") for e in evenements[k:k + 1000]])
for k in range(0, len(activites), 1000):
    valeurs("activite_d_atelier", "id, suivi_id, ouverture_id, operateur_id, poste_id, nature_id, categorie, debut, echeance, fin",
            [(t(a[0]), t(a[1]), t(a[0]), t(a[2]), t(a[3]), t(a[4]), t(a[5]), f"{a[6]:.0f}", f"{a[7]:.0f}", s(a[8] and f"{a[8]:.0f}"))
             for a in activites[k:k + 1000]])
ecrit("commit;")
Path(__file__).with_name("impeccmold.sql").write_text("\n".join(lignes) + "\n", encoding="utf-8")
print(f"{len(OPERATEURS)} operateurs, {len(ELEMENTS)} produits, {len(activites)} activites, {oublis} FIN oubliees")
