# Jeu de données impeccmold : valeurs attendues

Généré par `generer.py` avec `impeccmold.sql` : ne pas modifier à la main. Les valeurs sont recalculées par le script selon les règles de [contexte-metier.md](../../../../documentation/contexte-metier.md), sans passer par le code Java. Un écart entre ce fichier et l'API est donc un défaut de l'un ou de l'autre.

Heures de Paris. Seuil d'amplitude : 13 h. Les valeurs ne dépendent pas de l'instant de lecture, sauf le bloc « situation du jour » (Rey, Bernard, S22), écrit relativement à `now()` au chargement.

## Chargement

L'application doit avoir démarré au moins une fois en profil `local` : c'est elle qui crée et migre le schéma `impeccmold`. Puis, depuis la racine du projet :

```bash
docker compose -f src/main/docker/postgresql.yml exec -T postgresql \
  psql -U glmproject -d glmproject -v ON_ERROR_STOP=1 < src/main/docker/jeu-de-donnees/impeccmold.sql
```

Le script **vide d'abord toutes les tables métier** du schéma, remet le seuil à 13 h, puis charge tout dans une seule transaction. Pour modifier le jeu : éditer `generer.py`, puis `python3 generer.py`.

## Contenu

- 20 opérateurs, 17 postes de travail, 46 éléments de fabrication, 47 suivis d'atelier ;
- 472 journées de travail, 1856 événements de présence ;
- 4068 événements d'atelier ;
- du lundi 24/08/2026 (semaine 35, reprise après la fermeture d'été) au vendredi 25/09/2026 (semaine 39), plus deux samedis et un dimanche soir.

## Référentiel

| Opérateur | Matricule | Taux courant | Postes habilités | Métier |
| --- | --- | ---: | --- | --- |
| DUPONT Jean-Marc | M0412 | 28.50 | Fraiseuse UGV DMG HSC 75, Centre d'usinage 5 axes Hermle C42, Fraiseuse conventionnelle Huron | Fraiseur UGV, poste de nuit |
| MARTIN Sébastien | M0418 | 26.00 | Centre d'usinage 5 axes Hermle C42, Fraiseuse conventionnelle Huron, Fraiseuse UGV DMG HSC 75 | Fraiseur |
| BERNARD Nathalie | M0425 | 24.20 | Établi polissage 1, Établi polissage 2 | Polisseuse |
| PERRIN Christophe | M0431 | 27.80 | Enfonçage Charmilles Form 30, Érosion fil AgieCharmilles CUT 200, Rectifieuse plane Jones & Shipman 540, Perceuse à forets profonds | Érosionniste |
| GIROD Pascal | M0302 | 31.00 | Établi ajustage montage, Presse d'essai Arburg 220 t, Soudure laser Alpha, Tridimensionnelle Zeiss Contura | Mouliste, chef d'atelier |
| BOUVIER Mickaël | M0447 | 25.40 | Tour CN Mazak QT 250, Tour conventionnel Cazeneuve HB 575 | Tourneur |
| JACQUEMOUD Élodie | M0452 | 23.60 | Tridimensionnelle Zeiss Contura | Contrôleuse métrologie |
| ROLLET Thierry | M0355 | 29.90 | Soudure laser Alpha, Établi ajustage montage | Soudeur laser |
| MOREL Damien | M0461 | 24.80 | Centre d'usinage 5 axes Hermle C42, Fraiseuse UGV DMG HSC 75 | Fraiseur, équipe d'après-midi |
| CHAPUIS Kévin | M0470 | 22.50 | Rectifieuse plane Jones & Shipman 540, Perceuse à forets profonds | Rectifieur |
| VUILLERMOZ Anthony | M0388 | 26.70 | Établi ajustage montage, Presse d'essai Arburg 220 t | Mouliste ajusteur |
| MARTIN Sandrine | M0483 | 21.90 | Établi polissage 1, Établi polissage 2 | Polisseuse |
| FAVRE Julien | M0215 | 32.40 | Poste CAO 1 | Dessinateur-projeteur |
| GUYON Laurent | M0399 | 27.10 | Presse d'essai Arburg 220 t, Établi ajustage montage | Régleur essais |
| MERMET Bruno | M0490 | 26.50 (25,00 avant le 14/09) | Fraiseuse UGV DMG HSC 75, Centre d'usinage 5 axes Hermle C42, Fraiseuse conventionnelle Huron | Fraiseur |
| COLLET Yannick | M0501 | 23.10 | Fraiseuse conventionnelle Huron | Fraiseur conventionnel |
| PONCET Mathis | A0026 | — | Établi ajustage montage | Apprenti mouliste |
| DAVID Lucas | — | 20.80 | — | Intérimaire manutention |
| REY Céline | M0512 | 24.00 | Établi polissage 2, Établi ajustage montage | Polisseuse ajusteuse |
| SERVE Olivier | M0520 | 26.00 | Tour CN Mazak QT 250, Rectifieuse plane Jones & Shipman 540 | Tourneur, embauché le 28/09 |

| Poste | Nature | Coût horaire |
| --- | --- | ---: |
| Fraiseuse UGV DMG HSC 75 | Fraisage | 95.00 |
| Centre d'usinage 5 axes Hermle C42 | Fraisage | 110.00 |
| Fraiseuse conventionnelle Huron | Perçage | 42.00 (nature « Fraisage » avant le 16/09) |
| Tour CN Mazak QT 250 | Tournage | 65.00 |
| Tour conventionnel Cazeneuve HB 575 | Tournage | 38.00 |
| Enfonçage Charmilles Form 30 | Électroérosion enfonçage | 72.00 |
| Érosion fil AgieCharmilles CUT 200 | Électroérosion fil | 80.00 (75,00 avant le 14/09) |
| Rectifieuse plane Jones & Shipman 540 | Rectification | 55.00 |
| Perceuse à forets profonds | Perçage | 50.00 |
| Établi polissage 1 | Polissage | 18.00 |
| Établi polissage 2 | Polissage | 18.00 |
| Établi ajustage montage | Ajustage | — |
| Soudure laser Alpha | Soudure | 85.00 |
| Tridimensionnelle Zeiss Contura | Contrôle | 60.00 |
| Presse d'essai Arburg 220 t | Essai | 90.00 |
| Poste CAO 1 | Conception | 35.00 |
| Rectifieuse cylindrique Studer | Rectification | 58.00 |

## Cas limites

Chaque scénario occupe ses opérateurs toute la journée : aucun pointage aléatoire ne s'y mêle, les chiffres se vérifient à la main.

| Cas | Qui, quand | Situation | Attendu |
| --- | --- | --- | --- |
| S01 | BOUVIER mar. 25/08 | Cas nominal : tour CN 08:00 → 11:00. | 3 h, machine 195,00, MO 76,20. |
| S16 | BOUVIER mer. 26/08 | Un DEBUT sur S01 à 08:00 annulé (« Mauvais OF sélectionné »), le bon OF lancé à 08:02 → 12:00. | S16 : 3 h 58. S01 inchangé : l'événement annulé ne compte pas. |
| S02 | MOREL mer. 26/08 | Équipe d'après-midi 13:00–21:00, pause 17:00–17:30. DEBUT 14:00 jamais arrêté ; clôture le 28/08. | 14:00–17:00 + 17:30–21:00 = 6 h 30 : la pause scinde, le départ tronque, la clôture ne rallonge pas. |
| S03 | MARTIN Sébastien jeu. 27/08 | 5 axes 06:00 → 10:00 (S03A), Huron 07:00 → 08:30 (S03B), pause 09:00–09:20. | S03A 3 h 40, MO 75,83 (divisé par 2 de 07:00 à 08:30 seulement) ; S03B 1 h 30, MO 19,50. |
| S04 | PERRIN lun. 31/08 | Enfonçage et fil sur le même OF 08:00 (fil jusqu'à 10:00, enfonçage 11:00), puis quatre postes 13:30 → 14:30. | MO totale 111,20 = 27,80 × 4 h de présence réellement occupées : une heure n'est jamais payée deux fois. |
| S05 | BERNARD mar. 01/09 | Un seul établi pour trois OF (A et B 08:00 → 10:00, C 09:00 → 10:00). | Aucune division (un seul poste) ; chaque OF porte le coût machine entier. |
| S06 | ROLLET 02 et 03/09 | DEBUT 08:00, NON_CONFORMITE 10:00, DEBUT 11:00, FIN 15:00 ; le lendemain NON_CONFORMITE d'emblée 08:00 → 09:30. | Travail 5 h, non-conformité 2 h 30 en deux périodes datées. |
| S07 | MARTIN Sébastien 07-08/09 | Exemple E2 : départ oublié lundi, arrivée mardi 07:00 (nouvelle journée), relance de l'OF 42 à 07:05. | OF 42 lundi 7 h dont 3 h présumées, + 3 h 55 mardi ; anomalie JOURNEE_SANS_DEPART ; relevé lundi 5 h pointées + 3 h présumées. |
| S08 | VUILLERMOZ 07-08/09 | Même situation, départ régularisé à 16:30 le mardi 08:10 par le gestionnaire ; relance mardi 07:15. | Aucune anomalie ; relevé lundi 8 h 30 pointées ; établi sans coût horaire : machine 0,00. |
| S10 | DUPONT 31/08 → 01/09 | Exemple E1 : nuit 20:00 → 08:00, arrivée de 03:00 absorbée (rien n'est écrit), second OF à 03:02. | Une seule journée ; relevé lundi 4 h, mardi 8 h ; MO divisée par 2 de 03:02 à 08:00. |
| S09 | DUPONT 14 → 16/09 | Exemple E3 : nuit sans départ, arrivée le soir suivant, relance de l'OF à 20:10. | Lundi : 5 min présumées au relevé, 0 sur l'OF (la fin présumée est le démarrage lui-même) ; anomalie JOURNEE_SANS_DEPART. |
| S11 | DUPONT dim. 20/09 → lun. 21/09 | Exemple E7 : poste de nuit à cheval sur les semaines 38 et 39. | Semaine 38 dimanche 4 h, semaine 39 lundi 8 h. |
| S12 | COLLET 14-15/09 | Exemple E5 : pupitre hors ligne, le départ de mardi 08:30 ouvre une journée nulle (arrivée implicite + départ), puis arrivée 08:35. | Lundi abandonnée, fin présumée 11:00 (dernière fin d'OF) ; mardi 0 h + 6 h 55. |
| S13 | GIROD 25/08, GUYON 09/09 | Réengagé après clôture : ajustage 2 h au premier passage, essai 2 h au second. | Le rapport additionne les deux passages. |
| S14 | GIROD sam. 12/09 | Essai régularisé par le gestionnaire le lundi, sans aucune présence ce samedi. | Rendu intact : 2 h, alors que le relevé du samedi est vide. |
| S15 | JACQUEMOUD ven. 28/08 | FIN pointée 15:00, corrigée à 11:30 le 31/08 (annulation + insertion). | 2 h 30. |
| S17 | DAVID et PONCET mar. 15/09 | Sans poste : intérimaire sans matricule (20,80) et apprenti sans taux, 08:00 → 12:00 ; S17B de front 10:00 → 11:00. | Ligne sans nature ; aucune division (l'absence de poste compte pour un poste) ; l'apprenti ne coûte rien. |
| S18 | MERMET jeu. 17/09 | Trois postes pendant 7 min, dont la Huron requalifiée « Perçage » depuis le 16/09 ; taux 26,50 depuis le 14/09. | Arrondi : lignes Fraisage 23,92 / 2,06 et Perçage 4,90 / 1,03. |
| S19 | VUILLERMOZ lun. 21/09 | Exemple E4 depuis le lot 3 : parti sans pointer, retour à 19:30 absorbé, départ à 23:00 au-delà du seuil. | Journée du matin abandonnée (fin présumée 16:45), journée nulle à 23:00 ; les 3 h 30 du soir sont perdues. |
| S20 | — | Engagé, jamais pointé. | Suivi EN_ATTENTE, rapport vide. |
| S21 | — | Créé, jamais engagé. | Aucun suivi, rapport vide (pas une erreur). |
| S22 | REY, maintenant | DEBUT il y a 2 h 50 sur l'établi polissage 2, journée ouverte il y a 3 h. | Suivi EN_COURS ; coût croissant avec l'heure de lecture (18 €/h machine + 24 €/h MO). |
| S23 | BOUVIER 21-22/09 | Tour conventionnel lancé lundi 14:00 et jamais arrêté (S23A, clôturé mercredi) ; mardi, tour CN 08:00 → 10:00 (S23B). | S23A 2 h 30 (ramené à lundi) ; S23B 2 h, MO 50,80 : l'oubli de la veille ne divise pas le taux du lendemain. |
| P01 | GUYON 22-23/09, MOREL 22/09 | Amplitude de 13 h pile (pointée), de 13 h + 0,5 s (départ corrigé), et de 12 h 30. | Seule la deuxième est en anomalie AMPLITUDE_EXCESSIVE ; la troisième le devient si le seuil passe à 12 h (E8). |
| P02 | GIROD jeu. 24/09 | Départ régularisé à 21:30 le lendemain. | Amplitude 14 h 30 : AMPLITUDE_EXCESSIVE. |
| P03 | BERNARD jeu. 03/09 | Une pause à 10:00 saisie par erreur, annulée. | Relevé 8 h 15 : la pause annulée ne compte pas. |
| P04 | CHAPUIS mar. 08/09 | Arrivée 06:00 corrigée en 07:45. | Relevé 4 h 55. |
| P05 | CHAPUIS mer. 16/09 | Journée abandonnée restée en pause. | 4 h pointées, 0 présumée ; anomalie JOURNEE_SANS_DEPART. |
| P06 | REY 10-11/09 | Arrivée jeudi sans rien d'autre ; une pause reçue vendredi 09:00 ouvre une journée qui commence en pause. | Jeudi : 0 h ; vendredi 6 h 45 ; anomalie sur jeudi. |
| P07 | FAVRE ven. 18/09 | Départ pointé depuis la pause. | Relevé 4 h : la fenêtre s'arrête à la pause. |
| P08 | REY lun. 14/09 | Formation sécurité : présente 8 h, aucun OF. | Présence sans affectation : 8 h au relevé, rien au coût. |
| P09 | GIROD, VUILLERMOZ sam. 19/09 | Heures du samedi sur le moule capot de rétroviseur. | 4 h 30 au relevé du samedi. |
| — | COLLET | Déshabilité du tour conventionnel le 04/09 après y avoir pointé. | Ses pointages antérieurs restent valorisés ; le pupitre ne lui propose plus ce tour. |
| — | MARTIN Sandrine | En congés la semaine 37. | Relevé de la semaine 37 vide, sept jours à zéro. |
| — | SERVE | Embauché, habilité, jamais pointé. | ABSENT au pupitre ; seul opérateur supprimable, avec la rectifieuse cylindrique pour les postes. |

## Coût de revient par élément

`GET /api/couts-de-revient/{elementId}`, rôle gestionnaire. Une ligne par nature d'opération, triées par nature, sans nature en dernier. Temps au format de l'API entre parenthèses.

### S01 — OF-2026-000193 · M24-0655

`30000000-0000-4000-8000-000000000017` — Reprise diamètre de colonne de guidage

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Tournage | 3 h 00 (PT3H) | 0 h 00 | 195.00 | 76.20 | 271.20 |
| **Total** | **3 h 00** | **0 h 00** | **195.00** | **76.20** | **271.20** |

### S02 — OF-2026-000194 · M25-0981

`30000000-0000-4000-8000-000000000018` — Usinage 5 axes empreinte modifiée, travail jamais arrêté

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 6 h 30 (PT6H30M) | 0 h 00 | 715.00 | 161.20 | 876.20 |
| **Total** | **6 h 30** | **0 h 00** | **715.00** | **161.20** | **876.20** |

### S03A — OF-2026-000195 · M26-0301

`30000000-0000-4000-8000-000000000019` — Usinage empreinte fixe, deux machines en parallèle

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 3 h 40 (PT3H40M) | 0 h 00 | 403.33 | 75.83 | 479.16 |
| **Total** | **3 h 40** | **0 h 00** | **403.33** | **75.83** | **479.16** |

### S03B — OF-2026-000196 · M26-0302

`30000000-0000-4000-8000-000000000020` — Usinage bride de centrage, deux machines en parallèle

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 1 h 30 (PT1H30M) | 0 h 00 | 63.00 | 19.50 | 82.50 |
| **Total** | **1 h 30** | **0 h 00** | **63.00** | **19.50** | **82.50** |

### S04 — OF-2026-000197 · M25-1102

`30000000-0000-4000-8000-000000000021` — Électroérosion empreinte, deux puis quatre postes de front

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Perçage | 1 h 00 (PT1H) | 0 h 00 | 50.00 | 6.95 | 56.95 |
| Rectification | 1 h 00 (PT1H) | 0 h 00 | 55.00 | 6.95 | 61.95 |
| Électroérosion enfonçage | 4 h 00 (PT4H) | 0 h 00 | 288.00 | 62.55 | 350.55 |
| Électroérosion fil | 3 h 00 (PT3H) | 0 h 00 | 225.00 | 34.75 | 259.75 |
| **Total** | **9 h 00** | **0 h 00** | **618.00** | **111.20** | **729.20** |

### S05A — OF-2026-000198 · M26-0350

`30000000-0000-4000-8000-000000000022` — Polissage insert A, un seul établi pour trois OF

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Polissage | 2 h 00 (PT2H) | 0 h 00 | 36.00 | 48.40 | 84.40 |
| **Total** | **2 h 00** | **0 h 00** | **36.00** | **48.40** | **84.40** |

### S05B — OF-2026-000199 · M26-0351

`30000000-0000-4000-8000-000000000023` — Polissage insert B, un seul établi pour trois OF

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Polissage | 2 h 00 (PT2H) | 0 h 00 | 36.00 | 48.40 | 84.40 |
| **Total** | **2 h 00** | **0 h 00** | **36.00** | **48.40** | **84.40** |

### S05C — OF-2026-000200 · M26-0352

`30000000-0000-4000-8000-000000000024` — Polissage insert C, un seul établi pour trois OF

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Polissage | 1 h 00 (PT1H) | 0 h 00 | 18.00 | 24.20 | 42.20 |
| **Total** | **1 h 00** | **0 h 00** | **18.00** | **24.20** | **42.20** |

### S06 — OF-2026-000201 · M21-0077

`30000000-0000-4000-8000-000000000025` — Soudure laser d'arête cassée, deux non-conformités

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Soudure | 5 h 00 (PT5H) | 2 h 30 | 637.50 | 224.25 | 861.75 |
| **Total** | **5 h 00** | **2 h 30** | **637.50** | **224.25** | **861.75** |

Non-conformités : 02/09 10:00 → 02/09 11:00, 03/09 08:00 → 03/09 09:30.

### S07A — OF-2026-000202 · M26-0042

`30000000-0000-4000-8000-000000000026` — OF 42 de l'exemple E2 : départ oublié, relance le lendemain

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 10 h 55 (PT10H55M) | 0 h 00 | 1200.83 | 205.83 | 1406.66 |
| **Total** | **10 h 55** | **0 h 00** | **1200.83** | **205.83** | **1406.66** |

### S07B — OF-2026-000203 · M26-0043

`30000000-0000-4000-8000-000000000027` — OF 43 de l'exemple E2

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 6 h 00 (PT6H) | 0 h 00 | 252.00 | 78.00 | 330.00 |
| **Total** | **6 h 00** | **0 h 00** | **252.00** | **78.00** | **330.00** |

### S08 — OF-2026-000204 · M24-0714

`30000000-0000-4000-8000-000000000028` — Ajustage d'un moule, départ régularisé par le gestionnaire

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 12 h 00 (PT12H) | 0 h 00 | 0.00 | 320.40 | 320.40 |
| **Total** | **12 h 00** | **0 h 00** | **0.00** | **320.40** | **320.40** |

### S09 — OF-2026-000205 · M25-0888

`30000000-0000-4000-8000-000000000029` — Usinage de nuit, départ oublié (exemple E3)

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 7 h 20 (PT7H20M) | 0 h 00 | 696.67 | 209.00 | 905.67 |
| **Total** | **7 h 20** | **0 h 00** | **696.67** | **209.00** | **905.67** |

### S10A — OF-2026-000206 · M26-0142

`30000000-0000-4000-8000-000000000030` — Poste de nuit complet, arrivée redondante absorbée (exemple E1)

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 11 h 55 (PT11H55M) | 0 h 00 | 1132.08 | 268.85 | 1400.93 |
| **Total** | **11 h 55** | **0 h 00** | **1132.08** | **268.85** | **1400.93** |

### S10B — OF-2026-000207 · M26-0143

`30000000-0000-4000-8000-000000000031` — Poste de nuit complet, second OF lancé à 3 h (exemple E1)

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 4 h 58 (PT4H58M) | 0 h 00 | 546.33 | 70.78 | 617.11 |
| **Total** | **4 h 58** | **0 h 00** | **546.33** | **70.78** | **617.11** |

### S11 — OF-2026-000208 · M26-0211

`30000000-0000-4000-8000-000000000032` — Poste de nuit à cheval sur deux semaines (exemple E7)

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 11 h 40 (PT11H40M) | 0 h 00 | 1108.33 | 332.50 | 1440.83 |
| **Total** | **11 h 40** | **0 h 00** | **1108.33** | **332.50** | **1440.83** |

### S12 — OF-2026-000209 · M23-0419

`30000000-0000-4000-8000-000000000033` — Pupitre hors ligne, départ reçu le lendemain (exemple E5)

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 3 h 00 (PT3H) | 0 h 00 | 126.00 | 69.30 | 195.30 |
| **Total** | **3 h 00** | **0 h 00** | **126.00** | **69.30** | **195.30** |

### S13 — OF-2026-000210 · M22-0315B

`30000000-0000-4000-8000-000000000034` — Réengagé après clôture : deux passages en atelier

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 2 h 00 (PT2H) | 0 h 00 | 0.00 | 62.00 | 62.00 |
| Essai | 2 h 00 (PT2H) | 0 h 00 | 180.00 | 54.20 | 234.20 |
| **Total** | **4 h 00** | **0 h 00** | **180.00** | **116.20** | **296.20** |

### S14 — OF-2026-000211 · M25-1150

`30000000-0000-4000-8000-000000000035` — Essai T1 du samedi, régularisé sans présence

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Essai | 2 h 00 (PT2H) | 0 h 00 | 180.00 | 62.00 | 242.00 |
| **Total** | **2 h 00** | **0 h 00** | **180.00** | **62.00** | **242.00** |

### S15 — OF-2026-000212 · M26-0199

`30000000-0000-4000-8000-000000000036` — Métrologie, fin de contrôle corrigée

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Contrôle | 2 h 30 (PT2H30M) | 0 h 00 | 150.00 | 59.00 | 209.00 |
| **Total** | **2 h 30** | **0 h 00** | **150.00** | **59.00** | **209.00** |

### S16 — OF-2026-000213 · M26-0198

`30000000-0000-4000-8000-000000000037` — Tournage, un pointage annulé sur le mauvais OF

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Tournage | 3 h 58 (PT3H58M) | 0 h 00 | 257.83 | 100.75 | 358.58 |
| **Total** | **3 h 58** | **0 h 00** | **257.83** | **100.75** | **358.58** |

### S17A — OF-2026-000214 · M26-0501

`30000000-0000-4000-8000-000000000038` — Manutention sans poste, intérimaire et apprenti

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| (sans nature) | 8 h 00 (PT8H) | 0 h 00 | 0.00 | 83.20 | 83.20 |
| **Total** | **8 h 00** | **0 h 00** | **0.00** | **83.20** | **83.20** |

### S17B — OF-2026-000215 · M26-0502

`30000000-0000-4000-8000-000000000039` — Manutention sans poste, en parallèle

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| (sans nature) | 1 h 00 (PT1H) | 0 h 00 | 0.00 | 20.80 | 20.80 |
| **Total** | **1 h 00** | **0 h 00** | **0.00** | **20.80** | **20.80** |

### S18 — OF-2026-000216 · M26-0620

`30000000-0000-4000-8000-000000000040` — Arrondi : trois postes pendant sept minutes

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fraisage | 0 h 14 (PT14M) | 0 h 00 | 23.92 | 2.06 | 25.98 |
| Perçage | 0 h 07 (PT7M) | 0 h 00 | 4.90 | 1.03 | 5.93 |
| **Total** | **0 h 21** | **0 h 00** | **28.82** | **3.09** | **31.91** |

### S19 — OF-2026-000217 · M26-0655

`30000000-0000-4000-8000-000000000041` — Soirée de retour sous le seuil (exemple E4)

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 9 h 15 (PT9H15M) | 0 h 00 | 0.00 | 246.98 | 246.98 |
| **Total** | **9 h 15** | **0 h 00** | **0.00** | **246.98** | **246.98** |

### S20 — OF-2026-000218 · M26-0701

`30000000-0000-4000-8000-000000000042` — Engagé, jamais pointé

Rapport vide.

### S21 — OF-2026-000219 · M26-0702

`30000000-0000-4000-8000-000000000043` — Créé, jamais engagé

Rapport vide.

### S23A — OF-2026-000221 · M24-0930

`30000000-0000-4000-8000-000000000045` — Tournage jamais arrêté la veille

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Tournage | 2 h 30 (PT2H30M) | 0 h 00 | 95.00 | 63.50 | 158.50 |
| **Total** | **2 h 30** | **0 h 00** | **95.00** | **63.50** | **158.50** |

### S23B — OF-2026-000222 · M24-0931

`30000000-0000-4000-8000-000000000046` — Tournage du lendemain sur un autre tour

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Tournage | 2 h 00 (PT2H) | 0 h 00 | 130.00 | 50.80 | 180.80 |
| **Total** | **2 h 00** | **0 h 00** | **130.00** | **50.80** | **180.80** |

### OF-2026-000184 · M22-0315

`30000000-0000-4000-8000-000000000008` — Changement des éjecteurs cassés et contrôle de course

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 20 h 55 min 19 s (PT20H55M19S) | 0 h 00 | 0.00 | 361.90 | 361.90 |
| Conception | 7 h 37 min 19 s (PT7H37M19S) | 0 h 00 | 266.77 | 246.95 | 513.72 |
| Contrôle | 3 h 00 (PT3H) | 0 h 09 min 46 s | 189.77 | 74.64 | 264.41 |
| Essai | 7 h 51 min 18 s (PT7H51M18S) | 0 h 00 | 706.95 | 210.64 | 917.59 |
| Fraisage | 17 h 11 min 11 s (PT17H11M11S) | 0 h 00 | 1362.48 | 318.44 | 1680.92 |
| Perçage | 0 h 45 (PT45M) | 0 h 00 | 37.50 | 16.88 | 54.38 |
| Polissage | 13 h 54 min 10 s (PT13H54M10S) | 0 h 00 | 250.25 | 326.96 | 577.21 |
| Rectification | 3 h 45 (PT3H45M) | 0 h 00 | 206.25 | 84.38 | 290.63 |
| Soudure | 5 h 38 min 19 s (PT5H38M19S) | 0 h 00 | 479.28 | 171.39 | 650.67 |
| Tournage | 1 h 51 min 51 s (PT1H51M51S) | 0 h 00 | 70.84 | 43.06 | 113.90 |
| Électroérosion enfonçage | 2 h 00 (PT2H) | 0 h 00 | 144.00 | 41.70 | 185.70 |
| Électroérosion fil | 2 h 00 (PT2H) | 0 h 00 | 150.00 | 38.23 | 188.23 |
| (sans nature) | 1 h 22 min 30 s (PT1H22M30S) | 0 h 22 min 30 s | 0.00 | 36.40 | 36.40 |
| **Total** | **87 h 51 min 57 s** | **0 h 32 min 16 s** | **3864.09** | **1971.57** | **5835.66** |

Non-conformités : 25/08 09:09 → 25/08 09:31, 27/08 11:47 → 27/08 11:57.

### OF-2026-000185 · M21-0198

`30000000-0000-4000-8000-000000000009` — Reprise du plan de joint bavure côté fixe

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 29 h 31 min 55 s (PT29H31M55S) | 0 h 37 min 30 s | 0.00 | 638.23 | 638.23 |
| Conception | 6 h 56 min 52 s (PT6H56M52S) | 0 h 00 | 243.17 | 225.11 | 468.28 |
| Contrôle | 15 h 00 (PT15H) | 0 h 30 | 930.00 | 402.80 | 1332.80 |
| Essai | 3 h 57 min 31 s (PT3H57M31S) | 0 h 00 | 356.27 | 106.68 | 462.95 |
| Fraisage | 28 h 56 min 51 s (PT28H56M51S) | 1 h 37 min 30 s | 2429.48 | 665.87 | 3095.35 |
| Perçage | 1 h 23 min 30 s (PT1H23M30S) | 0 h 00 | 69.58 | 30.07 | 99.65 |
| Polissage | 20 h 57 min 45 s (PT20H57M45S) | 2 h 53 min 06 s | 429.25 | 545.18 | 974.43 |
| Rectification | 7 h 16 min 13 s (PT7H16M13S) | 0 h 00 | 399.87 | 155.97 | 555.84 |
| Soudure | 16 h 47 min 01 s (PT16H47M1S) | 0 h 22 min 30 s | 1458.48 | 524.01 | 1982.49 |
| Tournage | 12 h 53 min 46 s (PT12H53M46S) | 0 h 00 | 658.08 | 318.36 | 976.44 |
| Électroérosion enfonçage | 3 h 00 (PT3H) | 0 h 00 | 216.00 | 52.13 | 268.13 |
| Électroérosion fil | 2 h 27 min 19 s (PT2H27M19S) | 0 h 00 | 184.15 | 45.02 | 229.17 |
| (sans nature) | 5 h 00 (PT5H) | 0 h 00 | 0.00 | 104.00 | 104.00 |
| **Total** | **154 h 08 min 43 s** | **6 h 00 min 36 s** | **7374.33** | **3813.43** | **11187.76** |

Non-conformités : 25/08 10:58 → 25/08 11:21, 27/08 14:06 → 27/08 14:25, 28/08 09:50 → 28/08 10:28, 31/08 10:41 → 31/08 12:00, 31/08 13:53 → 31/08 14:38, 31/08 14:22 → 31/08 15:37, 01/09 07:22 → 01/09 07:33, 03/09 08:09 → 03/09 08:39, 07/09 06:49 → 07/09 07:19, 09/09 04:08 → 09/09 04:20.

### OF-2026-000186 · M24-0877

`30000000-0000-4000-8000-000000000010` — Modification de noyau suite ECN client indice C

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 21 h 46 min 35 s (PT21H46M35S) | 0 h 00 | 0.00 | 366.39 | 366.39 |
| Conception | 13 h 35 min 37 s (PT13H35M37S) | 1 h 20 min 35 s | 522.78 | 483.95 | 1006.73 |
| Contrôle | 10 h 10 min 13 s (PT10H10M13S) | 0 h 00 | 610.22 | 252.22 | 862.44 |
| Essai | 5 h 59 min 28 s (PT5H59M28S) | 0 h 15 | 561.70 | 176.50 | 738.20 |
| Fraisage | 53 h 08 min 45 s (PT53H8M45S) | 0 h 22 min 30 s | 4389.91 | 1277.59 | 5667.50 |
| Perçage | 15 h 42 min 37 s (PT15H42M37S) | 0 h 00 | 718.88 | 348.89 | 1067.77 |
| Polissage | 16 h 22 min 02 s (PT16H22M2S) | 0 h 13 min 39 s | 298.71 | 382.15 | 680.86 |
| Rectification | 3 h 37 min 37 s (PT3H37M37S) | 0 h 45 | 240.73 | 103.76 | 344.49 |
| Soudure | 12 h 01 min 53 s (PT12H1M53S) | 0 h 00 | 1022.67 | 359.74 | 1382.41 |
| Tournage | 6 h 50 min 40 s (PT6H50M40S) | 0 h 00 | 287.09 | 168.48 | 455.57 |
| Électroérosion enfonçage | 3 h 02 min 14 s (PT3H2M14S) | 0 h 00 | 218.68 | 69.61 | 288.29 |
| Électroérosion fil | 1 h 45 (PT1H45M) | 0 h 45 | 195.00 | 69.50 | 264.50 |
| (sans nature) | 11 h 45 min 46 s (PT11H45M46S) | 0 h 00 | 0.00 | 244.67 | 244.67 |
| **Total** | **175 h 48 min 27 s** | **3 h 41 min 44 s** | **9066.37** | **4303.45** | **13369.82** |

Non-conformités : 27/08 03:57 → 27/08 04:19, 27/08 15:15 → 27/08 15:30, 31/08 13:03 → 31/08 14:24, 02/09 16:06 → 02/09 16:19, 03/09 11:08 → 03/09 11:53, 15/09 11:03 → 15/09 11:48.

### OF-2026-000187 · sans référence

`30000000-0000-4000-8000-000000000011` — Usinage d'électrodes graphite pour reprise d'empreinte

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 42 h 27 min 30 s (PT42H27M30S) | 1 h 10 min 19 s | 0.00 | 762.78 | 762.78 |
| Conception | 16 h 03 min 12 s (PT16H3M12S) | 0 h 00 | 561.87 | 520.13 | 1082.00 |
| Contrôle | 17 h 22 min 18 s (PT17H22M18S) | 0 h 00 | 1042.30 | 441.42 | 1483.72 |
| Essai | 21 h 35 (PT21H35M) | 0 h 00 | 1942.50 | 603.76 | 2546.26 |
| Fraisage | 61 h 14 min 20 s (PT61H14M20S) | 0 h 53 min 45 s | 5197.94 | 1385.26 | 6583.20 |
| Perçage | 28 h 06 min 04 s (PT28H6M4S) | 1 h 08 min 21 s | 1430.00 | 692.93 | 2122.93 |
| Polissage | 24 h 22 min 12 s (PT24H22M12S) | 1 h 53 min 02 s | 472.57 | 599.62 | 1072.19 |
| Rectification | 16 h 56 min 12 s (PT16H56M12S) | 0 h 22 min 30 s | 952.14 | 381.29 | 1333.43 |
| Soudure | 16 h 35 min 31 s (PT16H35M31S) | 1 h 07 min 30 s | 1505.94 | 539.75 | 2045.69 |
| Tournage | 24 h 58 min 46 s (PT24H58M46S) | 0 h 29 min 53 s | 1276.08 | 642.54 | 1918.62 |
| Électroérosion enfonçage | 2 h 15 (PT2H15M) | 0 h 00 | 162.00 | 62.55 | 224.55 |
| Électroérosion fil | 0 h 37 min 30 s (PT37M30S) | 0 h 37 min 30 s | 93.75 | 34.75 | 128.50 |
| (sans nature) | 14 h 35 min 23 s (PT14H35M23S) | 0 h 00 | 0.00 | 303.47 | 303.47 |
| **Total** | **287 h 08 min 59 s** | **7 h 42 min 51 s** | **14637.09** | **6970.25** | **21607.34** |

Non-conformités : 25/08 08:22 → 25/08 08:44, 25/08 11:12 → 25/08 11:57, 25/08 13:45 → 25/08 14:04, 25/08 14:47 → 25/08 15:17, 27/08 16:11 → 27/08 16:32, 28/08 09:12 → 28/08 09:57, 28/08 09:40 → 28/08 10:25, 04/09 15:08 → 04/09 15:32, 07/09 11:16 → 07/09 11:54, 08/09 10:48 → 08/09 11:18, 09/09 15:30 → 09/09 16:00, 10/09 08:16 → 10/09 08:53, 10/09 20:19 → 10/09 20:56, 22/09 08:33 → 22/09 08:55, 25/09 12:46 → 25/09 13:03.

### OF-2026-000188 · M23-0560

`30000000-0000-4000-8000-000000000012` — Maintenance préventive 500 000 cycles

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 21 h 11 min 06 s (PT21H11M6S) | 0 h 00 | 0.00 | 394.85 | 394.85 |
| Contrôle | 7 h 51 min 12 s (PT7H51M12S) | 0 h 00 | 471.20 | 203.84 | 675.04 |
| Essai | 4 h 25 min 55 s (PT4H25M55S) | 0 h 00 | 398.87 | 118.33 | 517.20 |
| Fraisage | 29 h 28 min 03 s (PT29H28M3S) | 0 h 24 min 41 s | 2794.89 | 688.71 | 3483.60 |
| Perçage | 2 h 00 (PT2H) | 0 h 00 | 100.00 | 45.00 | 145.00 |
| Polissage | 12 h 11 min 17 s (PT12H11M17S) | 0 h 30 | 228.39 | 305.55 | 533.94 |
| Rectification | 3 h 10 min 20 s (PT3H10M20S) | 0 h 00 | 174.47 | 76.67 | 251.14 |
| Soudure | 2 h 30 (PT2H30M) | 0 h 00 | 212.50 | 76.40 | 288.90 |
| Tournage | 8 h 33 min 24 s (PT8H33M24S) | 0 h 00 | 379.15 | 209.16 | 588.31 |
| Électroérosion enfonçage | 2 h 10 min 10 s (PT2H10M10S) | 0 h 00 | 156.20 | 60.31 | 216.51 |
| Électroérosion fil | 7 h 17 min 55 s (PT7H17M55S) | 0 h 00 | 547.40 | 150.78 | 698.18 |
| (sans nature) | 5 h 06 min 32 s (PT5H6M32S) | 0 h 00 | 0.00 | 106.26 | 106.26 |
| **Total** | **105 h 55 min 54 s** | **0 h 54 min 41 s** | **5463.07** | **2435.86** | **7898.93** |

Non-conformités : 31/08 16:11 → 31/08 16:36, 10/09 08:27 → 10/09 08:57.

### OF-2026-000189 · M24-0902

`30000000-0000-4000-8000-000000000013` — Réparation tiroir grippé et reprise des lardons

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 41 h 21 min 18 s (PT41H21M18S) | 1 h 02 min 24 s | 0.00 | 729.15 | 729.15 |
| Conception | 4 h 40 min 46 s (PT4H40M46S) | 0 h 00 | 163.78 | 151.61 | 315.39 |
| Contrôle | 8 h 36 min 58 s (PT8H36M58S) | 0 h 00 | 516.97 | 211.12 | 728.09 |
| Essai | 7 h 08 min 16 s (PT7H8M16S) | 0 h 00 | 642.40 | 191.98 | 834.38 |
| Fraisage | 36 h 27 min 14 s (PT36H27M14S) | 0 h 00 | 3210.56 | 804.28 | 4014.84 |
| Perçage | 11 h 18 min 44 s (PT11H18M44S) | 0 h 11 min 15 s | 509.11 | 261.08 | 770.19 |
| Polissage | 26 h 34 min 42 s (PT26H34M42S) | 0 h 00 | 478.41 | 621.91 | 1100.32 |
| Rectification | 2 h 27 min 27 s (PT2H27M27S) | 0 h 00 | 135.16 | 54.65 | 189.81 |
| Soudure | 3 h 25 min 10 s (PT3H25M10S) | 0 h 00 | 290.65 | 105.18 | 395.83 |
| Tournage | 7 h 15 (PT7H15M) | 0 h 00 | 370.00 | 181.85 | 551.85 |
| Électroérosion enfonçage | 2 h 00 min 21 s (PT2H21S) | 0 h 00 | 144.42 | 55.76 | 200.18 |
| Électroérosion fil | 3 h 29 min 51 s (PT3H29M51S) | 0 h 00 | 279.80 | 69.66 | 349.46 |
| (sans nature) | 8 h 42 min 21 s (PT8H42M21S) | 0 h 00 | 0.00 | 181.08 | 181.08 |
| **Total** | **163 h 28 min 08 s** | **1 h 13 min 39 s** | **6741.26** | **3619.31** | **10360.57** |

Non-conformités : 03/09 15:39 → 03/09 16:05, 07/09 11:55 → 07/09 12:06, 16/09 13:04 → 16/09 13:17, 18/09 07:52 → 18/09 08:15.

### OF-2026-000190 · M25-1033

`30000000-0000-4000-8000-000000000014` — Retouche texturation grain cuir après casse

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 15 h 43 min 11 s (PT15H43M11S) | 2 h 11 min 15 s | 0.00 | 336.76 | 336.76 |
| Conception | 13 h 58 min 31 s (PT13H58M31S) | 0 h 11 min 15 s | 495.70 | 458.87 | 954.57 |
| Contrôle | 11 h 39 min 46 s (PT11H39M46S) | 0 h 00 | 699.77 | 292.55 | 992.32 |
| Essai | 16 h 09 min 31 s (PT16H9M31S) | 0 h 00 | 1454.27 | 461.33 | 1915.60 |
| Fraisage | 23 h 20 min 46 s (PT23H20M46S) | 0 h 37 min 30 s | 2053.25 | 552.27 | 2605.52 |
| Perçage | 16 h 24 min 56 s (PT16H24M56S) | 1 h 29 | 822.14 | 421.20 | 1243.34 |
| Polissage | 18 h 23 min 05 s (PT18H23M5S) | 0 h 00 | 330.92 | 432.87 | 763.79 |
| Rectification | 11 h 37 min 31 s (PT11H37M31S) | 0 h 00 | 639.39 | 252.71 | 892.10 |
| Soudure | 6 h 37 min 04 s (PT6H37M4S) | 0 h 45 | 626.26 | 223.73 | 849.99 |
| Tournage | 7 h 50 min 36 s (PT7H50M36S) | 0 h 00 | 388.32 | 199.22 | 587.54 |
| Électroérosion enfonçage | 0 h 52 (PT52M) | 0 h 00 | 62.40 | 12.05 | 74.45 |
| (sans nature) | 5 h 30 (PT5H30M) | 0 h 00 | 0.00 | 114.40 | 114.40 |
| **Total** | **148 h 06 min 57 s** | **5 h 14** | **7572.42** | **3757.96** | **11330.38** |

Non-conformités : 07/09 08:21 → 07/09 08:43, 08/09 08:52 → 08/09 10:07, 09/09 11:38 → 09/09 12:14, 11/09 09:07 → 11/09 09:52, 16/09 10:17 → 16/09 10:28, 21/09 11:07 → 21/09 12:00, 23/09 10:19 → 23/09 11:04, 24/09 05:42 → 24/09 05:57, 24/09 11:48 → 24/09 12:00.

### OF-2026-000191 · M22-0240

`30000000-0000-4000-8000-000000000015` — Remplacement de la régulation et des colliers chauffants

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 10 h 46 min 28 s (PT10H46M28S) | 0 h 00 | 0.00 | 298.20 | 298.20 |
| Conception | 2 h 00 (PT2H) | 0 h 00 | 70.00 | 64.80 | 134.80 |
| Contrôle | 8 h 00 min 18 s (PT8H18S) | 0 h 00 | 480.30 | 203.27 | 683.57 |
| Essai | 1 h 14 min 39 s (PT1H14M39S) | 0 h 00 | 111.98 | 33.22 | 145.20 |
| Fraisage | 22 h 55 min 33 s (PT22H55M33S) | 0 h 00 | 2396.35 | 507.53 | 2903.88 |
| Perçage | 11 h 30 min 12 s (PT11H30M12S) | 0 h 11 min 15 s | 545.58 | 244.69 | 790.27 |
| Polissage | 14 h 42 min 38 s (PT14H42M38S) | 0 h 17 min 44 s | 270.11 | 347.32 | 617.43 |
| Rectification | 2 h 44 min 57 s (PT2H44M57S) | 0 h 00 | 151.20 | 69.80 | 221.00 |
| Soudure | 3 h 24 min 58 s (PT3H24M58S) | 0 h 00 | 290.37 | 103.52 | 393.89 |
| Tournage | 6 h 11 min 17 s (PT6H11M17S) | 0 h 00 | 307.07 | 157.18 | 464.25 |
| Électroérosion enfonçage | 2 h 47 (PT2H47M) | 0 h 00 | 200.40 | 38.69 | 239.09 |
| Électroérosion fil | 2 h 52 min 05 s (PT2H52M5S) | 0 h 37 min 30 s | 279.44 | 97.11 | 376.55 |
| (sans nature) | 11 h 19 min 15 s (PT11H19M15S) | 0 h 00 | 0.00 | 235.47 | 235.47 |
| **Total** | **100 h 29 min 20 s** | **1 h 06 min 29 s** | **5102.80** | **2400.80** | **7503.60** |

Non-conformités : 14/09 11:44 → 14/09 12:02, 22/09 05:24 → 22/09 05:35, 23/09 14:16 → 23/09 14:54.

### OF-2026-000192 · sans référence

`30000000-0000-4000-8000-000000000016` — sans description

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 23 h 38 min 28 s (PT23H38M28S) | 0 h 00 | 0.00 | 488.53 | 488.53 |
| Conception | 16 h 22 min 43 s (PT16H22M43S) | 0 h 00 | 573.25 | 530.67 | 1103.92 |
| Contrôle | 11 h 58 min 44 s (PT11H58M44S) | 0 h 23 min 34 s | 742.32 | 314.85 | 1057.17 |
| Essai | 14 h 23 min 10 s (PT14H23M10S) | 0 h 00 | 1294.75 | 402.95 | 1697.70 |
| Fraisage | 60 h 08 min 33 s (PT60H8M33S) | 0 h 00 | 4994.75 | 1390.66 | 6385.41 |
| Perçage | 24 h 28 min 02 s (PT24H28M2S) | 0 h 11 min 15 s | 1123.50 | 590.51 | 1714.01 |
| Polissage | 35 h 05 min 46 s (PT35H5M46S) | 0 h 00 | 631.73 | 827.19 | 1458.92 |
| Rectification | 8 h 47 min 27 s (PT8H47M27S) | 0 h 00 | 483.50 | 180.31 | 663.81 |
| Soudure | 7 h 58 min 18 s (PT7H58M18S) | 1 h 11 min 53 s | 779.45 | 277.50 | 1056.95 |
| Tournage | 10 h 50 min 47 s (PT10H50M47S) | 0 h 00 | 529.52 | 260.55 | 790.07 |
| Électroérosion enfonçage | 4 h 30 (PT4H30M) | 0 h 00 | 324.00 | 125.10 | 449.10 |
| Électroérosion fil | 4 h 22 min 57 s (PT4H22M57S) | 0 h 00 | 336.19 | 121.83 | 458.02 |
| (sans nature) | 10 h 24 min 36 s (PT10H24M36S) | 0 h 00 | 0.00 | 216.53 | 216.53 |
| **Total** | **232 h 59 min 32 s** | **1 h 46 min 43 s** | **11812.96** | **5727.18** | **17540.14** |

Non-conformités : 03/09 14:47 → 03/09 14:59, 11/09 14:18 → 11/09 15:30, 24/09 16:01 → 24/09 16:24.

### PRD-2025-000047 · M25-1187

`30000000-0000-4000-8000-000000000001` — Moule 8 empreintes flacon PEHD 250 ml, canaux chauds Hasco

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 60 h 02 min 27 s (PT60H2M27S) | 0 h 00 | 0.00 | 896.29 | 896.29 |
| Conception | 16 h 30 min 29 s (PT16H30M29S) | 1 h 30 | 630.28 | 583.46 | 1213.74 |
| Contrôle | 18 h 27 min 53 s (PT18H27M53S) | 0 h 00 | 1107.88 | 454.34 | 1562.22 |
| Essai | 22 h 28 min 43 s (PT22H28M43S) | 0 h 00 | 2023.07 | 632.71 | 2655.78 |
| Fraisage | 70 h 07 min 53 s (PT70H7M53S) | 0 h 40 min 47 s | 6065.63 | 1589.77 | 7655.40 |
| Perçage | 13 h 47 min 05 s (PT13H47M5S) | 0 h 48 min 15 s | 674.53 | 337.75 | 1012.28 |
| Polissage | 24 h 02 min 24 s (PT24H2M24S) | 0 h 00 | 432.72 | 556.35 | 989.07 |
| Rectification | 12 h 01 min 50 s (PT12H1M50S) | 0 h 00 | 661.68 | 249.19 | 910.87 |
| Soudure | 11 h 35 min 38 s (PT11H35M38S) | 0 h 00 | 985.48 | 346.66 | 1332.14 |
| Tournage | 7 h 43 min 50 s (PT7H43M50S) | 0 h 28 min 06 s | 385.28 | 197.40 | 582.68 |
| Électroérosion enfonçage | 3 h 40 min 24 s (PT3H40M24S) | 0 h 00 | 264.48 | 81.77 | 346.25 |
| Électroérosion fil | 5 h 38 min 04 s (PT5H38M4S) | 0 h 00 | 435.89 | 102.59 | 538.48 |
| (sans nature) | 24 h 23 min 43 s (PT24H23M43S) | 0 h 00 | 0.00 | 507.42 | 507.42 |
| **Total** | **290 h 30 min 23 s** | **3 h 27 min 08 s** | **13666.92** | **6535.70** | **20202.62** |

Non-conformités : 25/08 09:03 → 25/08 10:33, 04/09 11:34 → 04/09 12:02, 10/09 19:17 → 10/09 19:28, 16/09 07:49 → 16/09 08:04, 21/09 12:28 → 21/09 13:01, 23/09 08:31 → 23/09 09:00.

### PRD-2026-000031 · M26-0412

`30000000-0000-4000-8000-000000000002` — Moule 4 empreintes bouchon flip-top Ø28, dévissage automatique

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 49 h 58 min 50 s (PT49H58M50S) | 0 h 45 | 0.00 | 999.15 | 999.15 |
| Conception | 8 h 29 min 06 s (PT8H29M6S) | 0 h 18 min 45 s | 307.91 | 285.04 | 592.95 |
| Contrôle | 20 h 35 min 32 s (PT20H35M32S) | 0 h 00 | 1235.53 | 495.23 | 1730.76 |
| Essai | 22 h 57 min 12 s (PT22H57M12S) | 0 h 37 min 51 s | 2122.58 | 647.98 | 2770.56 |
| Fraisage | 49 h 25 min 48 s (PT49H25M48S) | 1 h 39 min 35 s | 4620.38 | 1137.73 | 5758.11 |
| Perçage | 27 h 31 min 25 s (PT27H31M25S) | 0 h 50 min 29 s | 1325.11 | 667.06 | 1992.17 |
| Polissage | 30 h 13 min 23 s (PT30H13M23S) | 0 h 00 | 544.02 | 712.52 | 1256.54 |
| Rectification | 6 h 45 min 21 s (PT6H45M21S) | 0 h 30 | 399.07 | 167.26 | 566.33 |
| Soudure | 11 h 12 min 50 s (PT11H12M50S) | 0 h 00 | 953.18 | 336.12 | 1289.30 |
| Tournage | 15 h 44 min 03 s (PT15H44M3S) | 0 h 00 | 663.64 | 388.88 | 1052.52 |
| Électroérosion enfonçage | 5 h 28 min 54 s (PT5H28M54S) | 0 h 00 | 394.68 | 123.90 | 518.58 |
| Électroérosion fil | 7 h 56 min 18 s (PT7H56M18S) | 0 h 00 | 605.07 | 193.35 | 798.42 |
| (sans nature) | 22 h 49 min 32 s (PT22H49M32S) | 0 h 00 | 0.00 | 474.77 | 474.77 |
| **Total** | **279 h 08 min 15 s** | **4 h 41 min 40 s** | **13171.17** | **6628.99** | **19800.16** |

Non-conformités : 25/08 19:19 → 25/08 20:04, 03/09 06:41 → 03/09 07:11, 04/09 19:40 → 04/09 20:16, 14/09 11:08 → 14/09 11:26, 17/09 14:19 → 17/09 15:04, 18/09 17:33 → 18/09 17:51, 22/09 04:10 → 22/09 05:00, 25/09 14:18 → 25/09 14:56.

### PRD-2026-000032 · M26-0433

`30000000-0000-4000-8000-000000000003` — Moule bi-matière poignée de valise PP + TPE

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 43 h 43 min 20 s (PT43H43M20S) | 1 h 26 min 06 s | 0.00 | 793.81 | 793.81 |
| Conception | 14 h 26 min 05 s (PT14H26M5S) | 0 h 00 | 505.22 | 467.69 | 972.91 |
| Contrôle | 12 h 12 min 06 s (PT12H12M6S) | 0 h 00 | 732.10 | 306.46 | 1038.56 |
| Essai | 13 h 27 min 08 s (PT13H27M8S) | 0 h 00 | 1210.70 | 373.26 | 1583.96 |
| Fraisage | 43 h 55 min 12 s (PT43H55M12S) | 0 h 41 min 15 s | 3637.39 | 1065.69 | 4703.08 |
| Perçage | 7 h 40 min 18 s (PT7H40M18S) | 0 h 00 | 383.58 | 177.90 | 561.48 |
| Polissage | 31 h 35 min 26 s (PT31H35M26S) | 0 h 00 | 568.63 | 722.54 | 1291.17 |
| Rectification | 12 h 29 min 02 s (PT12H29M2S) | 0 h 22 min 30 s | 707.24 | 305.19 | 1012.43 |
| Soudure | 6 h 00 min 19 s (PT6H19S) | 0 h 00 | 510.45 | 182.89 | 693.34 |
| Tournage | 17 h 39 min 58 s (PT17H39M58S) | 1 h 00 | 844.95 | 467.64 | 1312.59 |
| Électroérosion enfonçage | 4 h 19 min 32 s (PT4H19M32S) | 0 h 00 | 311.44 | 67.08 | 378.52 |
| Électroérosion fil | 3 h 55 min 41 s (PT3H55M41S) | 0 h 00 | 299.77 | 77.69 | 377.46 |
| (sans nature) | 15 h 14 min 54 s (PT15H14M54S) | 1 h 45 | 0.00 | 353.57 | 353.57 |
| **Total** | **226 h 39 min 01 s** | **5 h 14 min 51 s** | **9711.47** | **5361.41** | **15072.88** |

Non-conformités : 26/08 11:37 → 26/08 12:07, 27/08 01:49 → 27/08 02:00, 31/08 09:52 → 31/08 10:15, 10/09 14:43 → 10/09 15:31, 14/09 13:34 → 14/09 13:49, 17/09 08:29 → 17/09 09:29, 17/09 11:50 → 17/09 12:12, 18/09 09:24 → 18/09 11:09.

### PRD-2026-000033 · M26-0451

`30000000-0000-4000-8000-000000000004` — Moule 2 empreintes branche de lunettes acétate injecté

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 27 h 00 min 43 s (PT27H43S) | 1 h 09 min 49 s | 0.00 | 414.37 | 414.37 |
| Conception | 6 h 27 min 56 s (PT6H27M56S) | 0 h 21 min 39 s | 238.92 | 221.17 | 460.09 |
| Contrôle | 16 h 41 min 50 s (PT16H41M50S) | 0 h 00 | 1001.83 | 434.63 | 1436.46 |
| Essai | 20 h 14 min 16 s (PT20H14M16S) | 0 h 00 | 1821.40 | 565.62 | 2387.02 |
| Fraisage | 23 h 21 min 22 s (PT23H21M22S) | 2 h 32 min 30 s | 2298.81 | 592.84 | 2891.65 |
| Perçage | 15 h 35 min 24 s (PT15H35M24S) | 0 h 00 | 681.23 | 385.25 | 1066.48 |
| Polissage | 31 h 14 min 50 s (PT31H14M50S) | 0 h 00 | 562.45 | 730.27 | 1292.72 |
| Rectification | 9 h 04 min 36 s (PT9H4M36S) | 1 h 00 | 554.22 | 226.88 | 781.10 |
| Soudure | 8 h 20 min 02 s (PT8H20M2S) | 0 h 00 | 708.38 | 249.18 | 957.56 |
| Tournage | 17 h 38 min 20 s (PT17H38M20S) | 0 h 00 | 998.03 | 448.03 | 1446.06 |
| Électroérosion enfonçage | 5 h 40 (PT5H40M) | 0 h 00 | 408.00 | 126.95 | 534.95 |
| Électroérosion fil | 2 h 18 min 40 s (PT2H18M40S) | 0 h 00 | 177.39 | 48.03 | 225.42 |
| (sans nature) | 5 h 51 min 04 s (PT5H51M4S) | 0 h 00 | 0.00 | 121.70 | 121.70 |
| **Total** | **189 h 29 min 03 s** | **5 h 03 min 58 s** | **9450.66** | **4564.92** | **14015.58** |

Non-conformités : 03/09 18:21 → 03/09 18:43, 09/09 15:22 → 09/09 16:32, 14/09 06:51 → 14/09 07:44, 21/09 16:04 → 21/09 16:26, 23/09 08:40 → 23/09 08:57, 24/09 08:37 → 24/09 09:37, 24/09 19:16 → 24/09 20:16.

### PRD-2026-000034 · M26-0467

`30000000-0000-4000-8000-000000000005` — Moule pot crème 50 ml cosmétique, finition poli miroir A1

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 50 h 36 min 49 s (PT50H36M49S) | 0 h 00 | 0.00 | 884.97 | 884.97 |
| Conception | 13 h 13 min 50 s (PT13H13M50S) | 0 h 11 min 15 s | 469.63 | 434.74 | 904.37 |
| Contrôle | 26 h 08 min 53 s (PT26H8M53S) | 0 h 47 min 55 s | 1616.80 | 671.09 | 2287.89 |
| Essai | 18 h 38 min 33 s (PT18H38M33S) | 0 h 00 | 1677.83 | 502.39 | 2180.22 |
| Fraisage | 80 h 39 min 41 s (PT80H39M41S) | 1 h 00 | 7339.81 | 1877.28 | 9217.09 |
| Perçage | 32 h 40 min 01 s (PT32H40M1S) | 0 h 15 | 1522.75 | 761.35 | 2284.10 |
| Polissage | 27 h 19 min 25 s (PT27H19M25S) | 0 h 00 | 491.82 | 642.83 | 1134.65 |
| Rectification | 16 h 56 min 05 s (PT16H56M5S) | 0 h 22 min 30 s | 952.03 | 355.85 | 1307.88 |
| Soudure | 4 h 44 min 08 s (PT4H44M8S) | 0 h 00 | 402.52 | 143.49 | 546.01 |
| Tournage | 25 h 33 min 28 s (PT25H33M28S) | 0 h 00 | 1059.53 | 633.34 | 1692.87 |
| Électroérosion enfonçage | 2 h 26 min 37 s (PT2H26M37S) | 0 h 00 | 175.94 | 58.29 | 234.23 |
| Électroérosion fil | 1 h 30 (PT1H30M) | 0 h 00 | 120.00 | 28.03 | 148.03 |
| (sans nature) | 14 h 00 min 48 s (PT14H48S) | 0 h 00 | 0.00 | 291.48 | 291.48 |
| **Total** | **314 h 28 min 18 s** | **2 h 36 min 40 s** | **15828.66** | **7285.13** | **23113.79** |

Non-conformités : 28/08 08:02 → 28/08 08:39, 28/08 11:19 → 28/08 11:42, 31/08 09:10 → 31/08 09:32, 01/09 12:13 → 01/09 12:36, 02/09 11:33 → 02/09 11:58, 17/09 05:27 → 17/09 05:42, 25/09 13:47 → 25/09 13:58.

### PRD-2026-000035 · M26-0478

`30000000-0000-4000-8000-000000000006` — Moule capot de rétroviseur ABS/PC, tiroirs hydrauliques

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 25 h 36 min 08 s (PT25H36M8S) | 0 h 00 | 0.00 | 599.34 | 599.34 |
| Conception | 21 h 59 min 30 s (PT21H59M30S) | 0 h 00 | 769.71 | 712.53 | 1482.24 |
| Contrôle | 16 h 48 min 50 s (PT16H48M50S) | 0 h 11 min 15 s | 1020.08 | 423.77 | 1443.85 |
| Essai | 6 h 30 (PT6H30M) | 1 h 30 | 720.00 | 213.60 | 933.60 |
| Fraisage | 41 h 18 min 05 s (PT41H18M5S) | 1 h 11 min 15 s | 3703.28 | 984.65 | 4687.93 |
| Perçage | 7 h 54 min 03 s (PT7H54M3S) | 0 h 10 min 28 s | 364.64 | 186.16 | 550.80 |
| Polissage | 29 h 30 min 35 s (PT29H30M35S) | 0 h 00 | 531.18 | 684.01 | 1215.19 |
| Rectification | 7 h 01 (PT7H1M) | 0 h 00 | 385.92 | 174.81 | 560.73 |
| Soudure | 5 h 39 min 15 s (PT5H39M15S) | 0 h 05 min 45 s | 488.75 | 173.63 | 662.38 |
| Tournage | 9 h 24 min 25 s (PT9H24M25S) | 0 h 45 | 460.21 | 257.99 | 718.20 |
| Électroérosion enfonçage | 0 h 45 (PT45M) | 0 h 00 | 54.00 | 20.85 | 74.85 |
| Électroérosion fil | 1 h 00 (PT1H) | 0 h 00 | 80.00 | 27.80 | 107.80 |
| (sans nature) | 7 h 10 min 47 s (PT7H10M47S) | 0 h 13 min 35 s | 0.00 | 154.05 | 154.05 |
| **Total** | **180 h 37 min 38 s** | **4 h 07 min 19 s** | **8577.77** | **4613.19** | **13190.96** |

Non-conformités : 08/09 10:11 → 08/09 10:33, 08/09 16:09 → 08/09 16:22, 09/09 08:11 → 09/09 08:30, 10/09 08:25 → 10/09 09:10, 10/09 10:46 → 10/09 10:57, 14/09 11:52 → 14/09 11:58, 17/09 12:36 → 17/09 12:46, 18/09 13:47 → 18/09 14:32, 23/09 18:05 → 23/09 18:35, 25/09 10:26 → 25/09 11:11.

### PRD-2026-000036 · M26-0489

`30000000-0000-4000-8000-000000000007` — Moule boîtier de robot pâtissier PP chargé talc

| Nature | Travail | Non-conformité | Machine | Main d'œuvre | Total |
| --- | ---: | ---: | ---: | ---: | ---: |
| Ajustage | 20 h 00 min 02 s (PT20H2S) | 0 h 02 min 51 s | 0.00 | 490.02 | 490.02 |
| Conception | 3 h 49 min 35 s (PT3H49M35S) | 0 h 00 | 133.92 | 123.98 | 257.90 |
| Contrôle | 3 h 52 min 33 s (PT3H52M33S) | 0 h 00 | 232.55 | 91.47 | 324.02 |
| Essai | 8 h 15 min 19 s (PT8H15M19S) | 0 h 00 | 742.98 | 220.88 | 963.86 |
| Fraisage | 25 h 04 min 26 s (PT25H4M26S) | 0 h 00 | 2524.21 | 537.73 | 3061.94 |
| Perçage | 15 h 18 min 30 s (PT15H18M30S) | 0 h 00 | 660.48 | 345.94 | 1006.42 |
| Polissage | 9 h 14 min 02 s (PT9H14M2S) | 0 h 00 | 166.21 | 215.43 | 381.64 |
| Rectification | 11 h 40 min 09 s (PT11H40M9S) | 0 h 00 | 641.80 | 263.68 | 905.48 |
| Soudure | 7 h 32 min 20 s (PT7H32M20S) | 0 h 00 | 640.81 | 231.39 | 872.20 |
| Tournage | 2 h 00 (PT2H) | 0 h 00 | 130.00 | 50.80 | 180.80 |
| Électroérosion fil | 1 h 00 (PT1H) | 0 h 00 | 80.00 | 27.80 | 107.80 |
| (sans nature) | 2 h 00 (PT2H) | 0 h 00 | 0.00 | 41.60 | 41.60 |
| **Total** | **109 h 46 min 56 s** | **0 h 02 min 51 s** | **5952.96** | **2640.72** | **8593.68** |

Non-conformités : 25/09 11:55 → 25/09 11:58.

## Relevé d'heures par opérateur

`GET /api/synthese-heures/...` : durée pointée (fenêtres closes, pauses exclues, coupée à minuit) et, entre parenthèses, durée présumée. Une cellule vide vaut 0. Le jour du chargement, hors de ces tableaux, Bernard compte en plus 3 h 30 pointées (arrivée → pause) et Rey rien, sa fenêtre étant encore ouverte.

### Semaine 35 — du 24/08 au 30/08/2026

| Opérateur | lun 24 | mar 25 | mer 26 | jeu 27 | ven 28 | sam 29 | dim 30 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| DUPONT Jean-Marc | 3 h 00 min 40 s | 7 h 48 min 21 s | 7 h 44 min 26 s | 7 h 44 min 15 s | 4 h 35 |  |  | 30 h 52 min 42 s |
| MARTIN Sébastien | 7 h 45 min 57 s | 7 h 47 min 48 s | 7 h 37 min 18 s | 7 h 40 | 7 h 35 min 14 s |  |  | 38 h 26 min 17 s |
| BERNARD Nathalie | 8 h 19 min 05 s | 8 h 02 min 57 s | 7 h 59 min 24 s | 8 h 13 | 6 h 54 min 17 s |  |  | 39 h 28 min 43 s |
| PERRIN Christophe | 8 h 03 min 10 s | 7 h 59 min 08 s | 7 h 55 min 28 s | 8 h 02 min 12 s | 7 h 01 min 27 s |  |  | 39 h 01 min 25 s |
| GIROD Pascal | 7 h 59 min 39 s | 8 h 00 | 8 h 07 min 03 s | 8 h 11 min 58 s | 6 h 59 min 16 s |  |  | 39 h 17 min 56 s |
| BOUVIER Mickaël | 8 h 11 min 08 s | 8 h 00 | 8 h 00 | 8 h 09 min 49 s | 7 h 12 min 24 s |  |  | 39 h 33 min 21 s |
| JACQUEMOUD Élodie | 8 h 09 min 57 s | 8 h 09 min 13 s | 7 h 55 min 32 s | 7 h 50 min 52 s | 7 h 00 |  |  | 39 h 05 min 34 s |
| ROLLET Thierry | 8 h 10 min 30 s | 7 h 52 min 19 s | 7 h 57 min 57 s | 8 h 07 min 01 s | 6 h 55 min 36 s |  |  | 39 h 03 min 23 s |
| MOREL Damien | 7 h 44 min 45 s | 7 h 42 min 51 s | 7 h 30 | 7 h 37 min 02 s | 7 h 41 min 14 s |  |  | 38 h 15 min 52 s |
| CHAPUIS Kévin | 7 h 35 min 08 s | 7 h 45 min 20 s | 7 h 45 min 26 s | 7 h 42 min 14 s | 7 h 50 min 54 s |  |  | 38 h 39 min 02 s |
| VUILLERMOZ Anthony | 8 h 07 min 53 s | 8 h 15 min 04 s | 8 h 05 min 21 s | 8 h 02 min 32 s | 7 h 04 min 47 s |  |  | 39 h 35 min 37 s |
| MARTIN Sandrine | 7 h 53 min 47 s | 8 h 09 min 39 s | 8 h 06 min 34 s | 8 h 13 min 35 s | 6 h 56 min 54 s |  |  | 39 h 20 min 29 s |
| FAVRE Julien | 7 h 59 min 22 s | 8 h 05 min 20 s | 7 h 49 min 24 s | 8 h 14 min 42 s | 6 h 48 min 54 s |  |  | 38 h 57 min 42 s |
| GUYON Laurent | 8 h 09 min 50 s | 8 h 08 min 19 s | 8 h 11 min 08 s | 8 h 05 min 55 s | 7 h 04 min 17 s |  |  | 39 h 39 min 29 s |
| MERMET Bruno | 7 h 45 min 28 s | 7 h 44 min 24 s | 7 h 40 min 02 s | 7 h 58 min 15 s | 7 h 55 min 27 s |  |  | 39 h 03 min 36 s |
| COLLET Yannick | 8 h 08 min 16 s | 8 h 02 min 15 s | 7 h 58 min 12 s | 8 h 00 min 46 s | 7 h 06 min 24 s |  |  | 39 h 15 min 53 s |
| PONCET Mathis | 8 h 07 min 08 s | 7 h 55 min 29 s | 8 h 17 min 41 s | 7 h 59 min 11 s | 7 h 06 min 56 s |  |  | 39 h 26 min 25 s |
| DAVID Lucas | 8 h 12 min 51 s | 8 h 11 min 18 s | 8 h 05 min 21 s | 7 h 58 min 04 s | 6 h 54 min 20 s |  |  | 39 h 21 min 54 s |
| REY Céline | 8 h 03 min 08 s | 8 h 11 min 19 s | 8 h 03 min 03 s | 8 h 10 min 33 s | 6 h 59 min 54 s |  |  | 39 h 27 min 57 s |
| SERVE Olivier |  |  |  |  |  |  |  | 0 h 00 |

### Semaine 36 — du 31/08 au 06/09/2026

| Opérateur | lun 31 | mar 01 | mer 02 | jeu 03 | ven 04 | sam 05 | dim 06 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| DUPONT Jean-Marc | 4 h 00 | 10 h 57 min 50 s | 7 h 35 min 21 s | 7 h 41 min 41 s | 4 h 33 min 51 s |  |  | 34 h 48 min 43 s |
| MARTIN Sébastien | 7 h 46 min 38 s | 7 h 48 min 56 s | 7 h 43 min 22 s | 7 h 45 min 45 s | 7 h 49 min 07 s |  |  | 38 h 53 min 48 s |
| BERNARD Nathalie | 7 h 59 min 18 s | 8 h 00 | 8 h 14 min 13 s | 8 h 15 | 7 h 00 min 22 s |  |  | 39 h 28 min 53 s |
| PERRIN Christophe | 8 h 00 | 8 h 01 min 18 s | 7 h 55 min 57 s | 8 h 03 min 40 s | 7 h 04 min 37 s |  |  | 39 h 05 min 32 s |
| GIROD Pascal | 8 h 01 min 39 s | 8 h 07 min 45 s | 8 h 12 min 47 s | 8 h 01 min 16 s | 7 h 03 min 27 s |  |  | 39 h 26 min 54 s |
| BOUVIER Mickaël | 8 h 06 min 57 s | 8 h 02 min 40 s | 8 h 05 min 43 s | 8 h 00 min 17 s | 7 h 17 min 31 s |  |  | 39 h 33 min 08 s |
| JACQUEMOUD Élodie | 8 h 00 min 46 s | 8 h 06 min 24 s | 8 h 13 min 24 s | 8 h 00 min 20 s | 6 h 57 min 35 s |  |  | 39 h 18 min 29 s |
| ROLLET Thierry | 8 h 09 min 23 s | 7 h 50 min 14 s | 8 h 00 | 8 h 00 | 7 h 14 min 26 s |  |  | 39 h 14 min 03 s |
| MOREL Damien | 7 h 47 min 14 s | 7 h 48 min 19 s | 7 h 43 min 36 s | 7 h 31 min 29 s | 7 h 37 min 14 s |  |  | 38 h 27 min 52 s |
| CHAPUIS Kévin | 7 h 46 min 19 s | 7 h 36 min 24 s | 7 h 40 min 01 s | 7 h 51 min 37 s | 7 h 39 min 47 s |  |  | 38 h 34 min 08 s |
| VUILLERMOZ Anthony | 8 h 10 | 8 h 15 min 28 s | 7 h 50 min 27 s | 8 h 04 min 48 s | 6 h 54 |  |  | 39 h 14 min 43 s |
| MARTIN Sandrine | 8 h 04 min 41 s | 8 h 09 min 16 s | 8 h 07 min 28 s | 7 h 52 min 46 s | 7 h 06 min 58 s |  |  | 39 h 21 min 09 s |
| FAVRE Julien | 8 h 06 min 11 s | 8 h 13 min 33 s | 8 h 08 min 58 s | 7 h 54 min 21 s | 7 h 01 min 57 s |  |  | 39 h 25 |
| GUYON Laurent | 7 h 56 min 12 s | 8 h 02 min 01 s | 7 h 58 min 15 s | 8 h 13 min 33 s | 7 h 00 min 25 s |  |  | 39 h 10 min 26 s |
| MERMET Bruno | 7 h 42 min 40 s | 7 h 44 min 12 s | 7 h 36 min 32 s | 7 h 33 min 19 s | 7 h 51 min 07 s |  |  | 38 h 27 min 50 s |
| COLLET Yannick | 8 h 09 min 31 s | 8 h 02 min 41 s | 7 h 55 min 56 s | 8 h 08 min 22 s | 7 h 02 min 45 s |  |  | 39 h 19 min 15 s |
| PONCET Mathis | 8 h 06 min 58 s | 7 h 55 min 24 s | 7 h 54 min 12 s | 8 h 01 min 47 s | 6 h 58 min 52 s |  |  | 38 h 57 min 13 s |
| DAVID Lucas | 7 h 56 min 10 s | 8 h 11 min 44 s | 8 h 03 min 29 s | 8 h 02 min 24 s | 7 h 09 min 02 s |  |  | 39 h 22 min 49 s |
| REY Céline | 7 h 56 min 37 s | 8 h 01 min 45 s | 8 h 07 min 34 s | 8 h 11 min 09 s | 7 h 11 min 23 s |  |  | 39 h 28 min 28 s |
| SERVE Olivier |  |  |  |  |  |  |  | 0 h 00 |

### Semaine 37 — du 07/09 au 13/09/2026

| Opérateur | lun 07 | mar 08 | mer 09 | jeu 10 | ven 11 | sam 12 | dim 13 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| DUPONT Jean-Marc | 3 h 06 min 17 s | 7 h 38 min 20 s | 7 h 38 min 18 s | 7 h 31 min 50 s | 4 h 40 min 49 s |  |  | 30 h 35 min 34 s |
| MARTIN Sébastien | 5 h 00 (3 h 00) | 8 h 00 | 7 h 53 min 11 s | 7 h 35 min 24 s | 7 h 54 min 02 s |  |  | 36 h 22 min 37 s (3 h 00) |
| BERNARD Nathalie | 8 h 18 min 33 s | 7 h 51 min 08 s | 8 h 01 min 56 s | 8 h 05 min 14 s | 7 h 02 min 04 s |  |  | 39 h 18 min 55 s |
| PERRIN Christophe | 8 h 14 min 54 s | 8 h 02 min 16 s | 8 h 00 min 06 s | 7 h 57 min 51 s | 7 h 15 min 11 s |  |  | 39 h 30 min 18 s |
| GIROD Pascal | 8 h 09 min 34 s | 8 h 05 min 54 s | 8 h 08 min 47 s | 7 h 59 min 46 s | 7 h 04 min 38 s |  |  | 39 h 28 min 39 s |
| BOUVIER Mickaël | 7 h 50 min 16 s | 8 h 06 min 59 s | 8 h 08 min 51 s | 8 h 09 min 24 s | 7 h 13 min 38 s |  |  | 39 h 29 min 08 s |
| JACQUEMOUD Élodie | 8 h 02 min 20 s | 8 h 01 min 45 s | 7 h 57 min 50 s | 7 h 58 min 05 s | 7 h 06 min 55 s |  |  | 39 h 06 min 55 s |
| ROLLET Thierry | 8 h 06 min 48 s | 7 h 57 min 16 s | 8 h 08 min 07 s | 8 h 09 min 54 s | 7 h 15 min 30 s |  |  | 39 h 37 min 35 s |
| MOREL Damien | 7 h 34 min 26 s | 7 h 37 min 31 s | 7 h 29 min 38 s | 7 h 53 min 58 s | 7 h 39 |  |  | 38 h 14 min 33 s |
| CHAPUIS Kévin | 7 h 45 min 44 s | 4 h 55 | 7 h 44 min 07 s | 7 h 52 min 38 s | 7 h 45 min 04 s |  |  | 36 h 02 min 33 s |
| VUILLERMOZ Anthony | 8 h 30 | 7 h 00 | 7 h 59 min 53 s | 8 h 00 min 15 s | 7 h 12 min 30 s |  |  | 38 h 42 min 38 s |
| MARTIN Sandrine |  |  |  |  |  |  |  | 0 h 00 |
| FAVRE Julien | 8 h 11 min 06 s | 7 h 59 min 19 s | 8 h 12 min 31 s | 8 h 09 min 46 s | 7 h 08 min 18 s |  |  | 39 h 41 |
| GUYON Laurent | 8 h 03 min 09 s | 7 h 50 min 02 s | 8 h 00 | 7 h 59 min 43 s | 7 h 07 min 39 s |  |  | 39 h 00 min 33 s |
| MERMET Bruno | 7 h 48 min 08 s | 7 h 43 min 47 s | 7 h 40 min 12 s | 7 h 35 min 23 s | 7 h 57 min 10 s |  |  | 38 h 44 min 40 s |
| COLLET Yannick | 8 h 06 min 24 s | 8 h 18 min 30 s | 7 h 59 min 15 s | 7 h 52 min 32 s | 7 h 02 min 23 s |  |  | 39 h 19 min 04 s |
| PONCET Mathis | 8 h 05 min 22 s | 8 h 16 min 42 s | 8 h 04 min 29 s | 8 h 08 min 03 s | 6 h 58 min 27 s |  |  | 39 h 33 min 03 s |
| DAVID Lucas | 7 h 56 min 05 s | 8 h 09 min 20 s | 7 h 57 min 48 s | 8 h 06 min 54 s | 7 h 09 min 52 s |  |  | 39 h 19 min 59 s |
| REY Céline | 8 h 13 min 08 s | 8 h 10 min 15 s | 8 h 10 min 18 s |  | 6 h 45 |  |  | 31 h 18 min 41 s |
| SERVE Olivier |  |  |  |  |  |  |  | 0 h 00 |

### Semaine 38 — du 14/09 au 20/09/2026

| Opérateur | lun 14 | mar 15 | mer 16 | jeu 17 | ven 18 | sam 19 | dim 20 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| DUPONT Jean-Marc |  (0 h 05) | 4 h 00 | 7 h 27 min 20 s | 7 h 57 min 55 s | 4 h 44 min 45 s |  | 4 h 00 | 28 h 10 (0 h 05) |
| MARTIN Sébastien | 7 h 34 min 31 s | 7 h 30 min 49 s | 7 h 32 min 48 s | 7 h 41 min 23 s | 7 h 36 min 44 s |  |  | 37 h 56 min 15 s |
| BERNARD Nathalie | 8 h 02 min 26 s | 8 h 07 min 29 s | 8 h 02 min 11 s | 8 h 02 min 06 s | 6 h 54 min 14 s |  |  | 39 h 08 min 26 s |
| PERRIN Christophe | 8 h 01 min 12 s | 8 h 06 min 37 s | 8 h 15 min 04 s | 7 h 55 min 38 s | 7 h 09 min 50 s |  |  | 39 h 28 min 21 s |
| GIROD Pascal | 8 h 11 min 30 s | 8 h 02 min 42 s | 8 h 11 min 39 s | 7 h 59 min 46 s | 7 h 13 | 4 h 30 |  | 44 h 08 min 37 s |
| BOUVIER Mickaël | 8 h 00 min 54 s | 7 h 49 min 22 s | 8 h 12 min 42 s | 8 h 10 min 49 s | 6 h 52 min 09 s |  |  | 39 h 05 min 56 s |
| JACQUEMOUD Élodie | 8 h 11 min 50 s | 8 h 08 min 07 s | 8 h 01 min 40 s | 8 h 00 min 15 s | 6 h 57 min 20 s |  |  | 39 h 19 min 12 s |
| ROLLET Thierry | 8 h 13 min 04 s | 8 h 03 min 40 s | 8 h 08 min 16 s | 8 h 19 min 56 s | 7 h 05 min 35 s |  |  | 39 h 50 min 31 s |
| MOREL Damien | 7 h 43 | 7 h 51 min 51 s | 7 h 51 min 03 s | 7 h 45 min 18 s | 7 h 43 min 30 s |  |  | 38 h 54 min 42 s |
| CHAPUIS Kévin | 7 h 47 min 04 s | 7 h 36 min 09 s | 4 h 00 | 7 h 53 min 53 s | 7 h 35 min 12 s |  |  | 34 h 52 min 18 s |
| VUILLERMOZ Anthony | 8 h 12 min 31 s | 8 h 00 min 52 s | 8 h 00 min 42 s | 8 h 00 min 19 s | 6 h 59 min 09 s | 4 h 30 |  | 43 h 43 min 33 s |
| MARTIN Sandrine | 7 h 59 min 35 s | 8 h 09 min 34 s | 7 h 57 min 31 s | 8 h 11 min 19 s | 6 h 53 min 46 s |  |  | 39 h 11 min 45 s |
| FAVRE Julien | 8 h 06 min 44 s | 8 h 16 min 25 s | 8 h 10 min 51 s | 8 h 09 min 24 s | 4 h 00 |  |  | 36 h 43 min 24 s |
| GUYON Laurent | 7 h 59 min 05 s | 8 h 05 min 48 s | 7 h 49 min 51 s | 8 h 10 min 32 s | 6 h 55 min 18 s |  |  | 39 h 00 min 34 s |
| MERMET Bruno | 7 h 40 min 46 s | 7 h 46 min 14 s | 7 h 44 min 33 s | 7 h 40 | 7 h 51 min 08 s |  |  | 38 h 42 min 41 s |
| COLLET Yannick |  (3 h 30) | 6 h 55 | 8 h 00 min 16 s | 7 h 55 min 13 s | 7 h 05 min 38 s |  |  | 29 h 56 min 07 s (3 h 30) |
| PONCET Mathis | 8 h 13 min 48 s | 8 h 00 | 8 h 08 min 40 s | 8 h 09 min 32 s | 7 h 02 min 57 s |  |  | 39 h 34 min 57 s |
| DAVID Lucas | 8 h 07 min 23 s | 8 h 00 | 7 h 53 min 17 s | 7 h 56 min 45 s | 7 h 08 min 04 s |  |  | 39 h 05 min 29 s |
| REY Céline | 8 h 00 | 7 h 55 min 04 s | 8 h 01 min 05 s | 7 h 57 min 18 s | 7 h 03 min 28 s |  |  | 38 h 56 min 55 s |
| SERVE Olivier |  |  |  |  |  |  |  | 0 h 00 |

### Semaine 39 — du 21/09 au 27/09/2026

| Opérateur | lun 21 | mar 22 | mer 23 | jeu 24 | ven 25 | sam 26 | dim 27 | Total |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| DUPONT Jean-Marc | 10 h 56 min 11 s | 7 h 43 min 46 s | 7 h 50 min 36 s | 7 h 44 min 16 s | 4 h 35 min 37 s |  |  | 38 h 50 min 26 s |
| MARTIN Sébastien | 7 h 47 min 01 s | 7 h 41 min 41 s | 7 h 35 min 06 s | 7 h 47 min 58 s | 7 h 52 min 41 s |  |  | 38 h 44 min 27 s |
| BERNARD Nathalie | 8 h 08 min 37 s | 7 h 57 min 25 s | 7 h 47 min 56 s | 8 h 02 min 31 s | 7 h 17 min 49 s |  |  | 39 h 14 min 18 s |
| PERRIN Christophe | 7 h 58 min 19 s | 8 h 00 min 51 s | 7 h 55 min 25 s | 7 h 54 min 58 s | 7 h 04 min 29 s |  |  | 38 h 54 min 02 s |
| GIROD Pascal | 8 h 02 min 41 s | 8 h 00 min 21 s | 8 h 04 min 03 s | 14 h 00 | 7 h 05 min 07 s |  |  | 45 h 12 min 12 s |
| BOUVIER Mickaël | 8 h 00 | 8 h 00 | 8 h 03 min 50 s | 8 h 02 min 30 s | 6 h 54 min 36 s |  |  | 39 h 00 min 56 s |
| JACQUEMOUD Élodie | 8 h 03 min 10 s | 7 h 56 min 08 s | 8 h 02 min 21 s | 8 h 02 min 31 s | 6 h 57 min 32 s |  |  | 39 h 01 min 42 s |
| ROLLET Thierry | 8 h 15 min 17 s | 8 h 07 min 45 s | 8 h 07 min 40 s | 8 h 15 min 10 s | 7 h 01 min 36 s |  |  | 39 h 47 min 28 s |
| MOREL Damien | 7 h 39 min 37 s | 12 h 00 | 7 h 51 min 41 s | 7 h 49 min 35 s | 7 h 55 min 55 s |  |  | 43 h 16 min 48 s |
| CHAPUIS Kévin | 7 h 49 min 42 s | 7 h 40 min 43 s | 7 h 55 min 25 s | 7 h 37 min 27 s | 7 h 47 min 12 s |  |  | 38 h 50 min 29 s |
| VUILLERMOZ Anthony |  (9 h 45) | 8 h 02 min 32 s | 8 h 05 min 27 s | 7 h 59 min 06 s | 7 h 03 min 18 s |  |  | 31 h 10 min 23 s (9 h 45) |
| MARTIN Sandrine | 8 h 00 min 14 s | 7 h 58 min 05 s | 8 h 08 min 54 s | 8 h 05 min 41 s | 6 h 48 min 30 s |  |  | 39 h 01 min 24 s |
| FAVRE Julien | 8 h 00 min 58 s | 8 h 17 min 31 s | 8 h 07 min 47 s | 8 h 03 min 54 s | 7 h 10 min 35 s |  |  | 39 h 40 min 45 s |
| GUYON Laurent | 8 h 12 min 17 s | 12 h 15 | 12 h 15 | 7 h 59 min 56 s | 7 h 09 min 45 s |  |  | 47 h 51 min 58 s |
| MERMET Bruno | 7 h 38 min 28 s | 7 h 38 min 07 s | 7 h 48 min 04 s | 7 h 52 min 40 s | 7 h 51 min 26 s |  |  | 38 h 48 min 45 s |
| COLLET Yannick | 7 h 54 min 08 s | 7 h 57 min 12 s | 8 h 03 min 47 s | 7 h 52 min 44 s | 6 h 53 min 38 s |  |  | 38 h 41 min 29 s |
| PONCET Mathis | 8 h 08 min 27 s | 8 h 12 min 24 s | 8 h 12 min 02 s | 8 h 11 min 55 s | 6 h 57 min 08 s |  |  | 39 h 41 min 56 s |
| DAVID Lucas | 7 h 58 min 50 s | 8 h 05 min 40 s | 8 h 04 min 09 s | 8 h 10 min 12 s | 7 h 05 min 45 s |  |  | 39 h 24 min 36 s |
| REY Céline | 8 h 10 min 01 s | 7 h 56 min 57 s | 7 h 55 min 37 s | 7 h 51 min 31 s | 6 h 54 min 29 s |  |  | 38 h 48 min 35 s |
| SERVE Olivier |  |  |  |  |  |  |  | 0 h 00 |

## Anomalies

`GET /api/atelier/anomalies`, la plus récente d'abord, jugées au seuil de 13 h. Les journées de la situation du jour restent sous le seuil au chargement.

| Opérateur | Arrivée | Type | Journée |
| --- | --- | --- | --- |
| GIROD Pascal | 24/09 07:00 | AMPLITUDE_EXCESSIVE | `50000000-0000-4000-8000-000000000033` |
| GUYON Laurent | 23/09 06:00 | AMPLITUDE_EXCESSIVE | `50000000-0000-4000-8000-000000000031` |
| VUILLERMOZ Anthony | 21/09 07:00 | JOURNEE_SANS_DEPART | `50000000-0000-4000-8000-000000000026` |
| CHAPUIS Kévin | 16/09 05:00 | JOURNEE_SANS_DEPART | `50000000-0000-4000-8000-000000000036` |
| DUPONT Jean-Marc | 14/09 20:00 | JOURNEE_SANS_DEPART | `50000000-0000-4000-8000-000000000014` |
| COLLET Yannick | 14/09 07:30 | JOURNEE_SANS_DEPART | `50000000-0000-4000-8000-000000000017` |
| REY Céline | 10/09 07:30 | JOURNEE_SANS_DEPART | `50000000-0000-4000-8000-000000000037` |
| MARTIN Sébastien | 07/09 07:00 | JOURNEE_SANS_DEPART | `50000000-0000-4000-8000-000000000009` |

## États des suivis d'atelier

| Élément | Référence | Passage | Engagé le | Clôturé le | État |
| --- | --- | ---: | --- | --- | --- |
| OF-2026-000184 | M22-0315 | 1 | 24/08 04:30 | 07/09 10:00 | CLOTURE |
| OF-2026-000185 | M21-0198 | 1 | 24/08 04:30 | 14/09 10:00 | CLOTURE |
| OF-2026-000186 | M24-0877 | 1 | 25/08 04:30 | 21/09 10:00 | CLOTURE |
| OF-2026-000187 | — | 1 | 24/08 04:30 | — | EN_COURS |
| OF-2026-000188 | M23-0560 | 1 | 31/08 04:30 | 14/09 10:00 | CLOTURE |
| OF-2026-000188 | M23-0560 | 2 | 22/09 16:00 | — | INTERROMPU |
| OF-2026-000189 | M24-0902 | 1 | 01/09 04:30 | — | INTERROMPU |
| OF-2026-000190 | M25-1033 | 1 | 07/09 04:30 | — | EN_COURS |
| OF-2026-000191 | M22-0240 | 1 | 14/09 04:30 | — | INTERROMPU |
| OF-2026-000192 | — | 1 | 26/08 04:30 | — | EN_COURS |
| OF-2026-000193 | M24-0655 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000194 | M25-0981 | 1 | 24/08 06:00 | 28/08 10:00 | CLOTURE |
| OF-2026-000195 | M26-0301 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000196 | M26-0302 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000197 | M25-1102 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000198 | M26-0350 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000199 | M26-0351 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000200 | M26-0352 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000201 | M21-0077 | 1 | 24/08 06:00 | 03/09 16:00 | CLOTURE |
| OF-2026-000202 | M26-0042 | 1 | 24/08 06:00 | 11/09 17:00 | CLOTURE |
| OF-2026-000203 | M26-0043 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000204 | M24-0714 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000205 | M25-0888 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000206 | M26-0142 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000207 | M26-0143 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000208 | M26-0211 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000209 | M23-0419 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000210 | M22-0315B | 1 | 24/08 06:00 | 25/08 17:00 | CLOTURE |
| OF-2026-000210 | M22-0315B | 2 | 09/09 07:00 | 10/09 09:00 | CLOTURE |
| OF-2026-000211 | M25-1150 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000212 | M26-0199 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000213 | M26-0198 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000214 | M26-0501 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000215 | M26-0502 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000216 | M26-0620 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000217 | M26-0655 | 1 | 24/08 06:00 | — | INTERROMPU |
| OF-2026-000218 | M26-0701 | 1 | 24/08 06:00 | — | EN_ATTENTE |
| OF-2026-000220 | M26-0777 | 1 | 24/08 06:00 | — | EN_COURS |
| OF-2026-000221 | M24-0930 | 1 | 24/08 06:00 | 23/09 09:00 | CLOTURE |
| OF-2026-000222 | M24-0931 | 1 | 24/08 06:00 | — | INTERROMPU |
| PRD-2025-000047 | M25-1187 | 1 | 24/08 04:30 | — | EN_COURS |
| PRD-2026-000031 | M26-0412 | 1 | 24/08 04:30 | — | EN_COURS |
| PRD-2026-000032 | M26-0433 | 1 | 24/08 04:30 | 21/09 10:00 | CLOTURE |
| PRD-2026-000033 | M26-0451 | 1 | 31/08 04:30 | — | EN_COURS |
| PRD-2026-000034 | M26-0467 | 1 | 24/08 04:30 | — | INTERROMPU |
| PRD-2026-000035 | M26-0478 | 1 | 02/09 04:30 | — | EN_COURS |
| PRD-2026-000036 | M26-0489 | 1 | 14/09 04:30 | — | INTERROMPU |

## Pupitre au chargement

`GET /api/pupitre/referentiel` : REY est PRESENT (présente jusqu'à son arrivée + 13 h), BERNARD EN_PAUSE, tous les autres ABSENT — y compris ceux dont une journée abandonnée est restée PRESENT en base. Les éléments rendus sont ceux dont le suivi n'est pas clôturé.

