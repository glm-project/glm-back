# Bornes de fin de journée : départ oublié et poste de nuit

> **Statut : proposition à challenger.** Décisions prises le 24/09/2026 ; le découpage du poste de nuit à minuit est explicitement révisable.
> Heures en heure locale (Europe/Paris) pour la lisibilité ; l'API échange des instants UTC. Seuil d'amplitude pris à **13 h** dans tous les exemples.

**Ce qu'on attend de vous**

- valider ou contester chaque décision D1 à D12 : la colonne « À challenger » donne l'objection la plus forte qu'on leur connaît ;
- vérifier les chiffres des exemples E1 à E8, et le tableau des pointages aujourd'hui refusés (§2) ;
- répondre aux questions ouvertes (§5) ;
- dire si le découpage en lots (§4) permet d'itérer.

## 1. Le sujet

Un opérateur qui part **sans rien pointer** laisse sa journée de travail ouverte. Aujourd'hui, rien ne la referme, sauf une régularisation du gestionnaire, qui doit encore s'apercevoir de l'oubli.

Le client a posé deux règles (synthèse des réunions du 01/08, §6) :

- l'oubli est inévitable ;
- **ce n'est pas l'opérateur qui corrige**, c'est le gestionnaire, après coup.

Une contrainte s'y ajoute : **un opérateur peut travailler de nuit**, par exemple de 20 h à 8 h. Aucune règle calendaire (minuit, heure de fermeture) ne peut donc fermer une journée.

### Ce qui se passe aujourd'hui

Exemple : Dupont arrive lundi à 7 h, démarre l'OF 42 à 8 h, fait une pause de 12 h à 13 h, termine l'OF 43 à 16 h et part à 17 h **sans rien pointer**. Il revient mardi à 7 h.

| Où                         | Ce qu'on constate                                                                                            |
| -------------------------- | ------------------------------------------------------------------------------------------------------------ |
| Journée de lundi           | reste ouverte, sans fin                                                                                      |
| Pupitre, lundi 21 h        | Dupont affiché **présent**                                                                                   |
| Arrivée de mardi 7 h       | refusée par le serveur, masquée par le pupitre : **l'heure d'arrivée de mardi n'est enregistrée nulle part** |
| Pointages de mardi         | comptés dans la journée de lundi                                                                             |
| OF 42, mardi 7 h 05        | « démarrer » refusé : l'OF est toujours en cours depuis lundi                                                |
| Temps passé sur l'OF 42    | 8 h → 12 h, puis 13 h → **sans fin** : la nuit est comptée                                                   |
| Coût de revient de l'OF 42 | valorisé jusqu'à l'instant de lecture, nuit comprise                                                         |
| Relevé des heures, lundi   | **0 h** (seules les périodes fermées comptent)                                                               |
| Relevé des heures, mardi   | 0 h                                                                                                          |

Quatre problèmes, un schéma chacun.

**Problème 1 : une journée sans départ n'a pas de fin.**

![Problème 1 : une journée sans départ n'a pas de fin](bornes-de-fin-de-journee/01-probleme-journee-sans-fin.svg)

**Problème 2 : l'arrivée du lendemain est perdue.**

![Problème 2 : l'arrivée du lendemain est perdue](bornes-de-fin-de-journee/02-probleme-arrivee-perdue.svg)

**Problème 3 : l'OF resté ouvert bloque la reprise et fausse le coût.**

![Problème 3 : l'OF resté ouvert bloque la reprise et fausse le coût](bornes-de-fin-de-journee/03-probleme-of-bloque.svg)

**Problème 4 : personne ne voit l'oubli.**

![Problème 4 : personne ne voit l'oubli](bornes-de-fin-de-journee/04-probleme-oubli-invisible.svg)

## 2. Décisions

| #   | Décision                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | À challenger                                                                                                                                                |
| --- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| D1  | **Amplitude maximale** d'une journée, paramétrée par entreprise depuis un écran gestionnaire. 13 h par défaut. Valeur strictement comprise entre 0 et 24 h.                                                                                                                                                                                                                                                                                                                                                                                                                                                                | 13 h ne tient pas compte d'une entreprise en 2 × 12 h avec heures supplémentaires. Faut-il une valeur par équipe plutôt que par entreprise ?                |
| D2  | Une journée sans départ dont l'amplitude depuis l'arrivée, pauses comprises, dépasse le seuil est **abandonnée**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | Mesurer depuis l'arrivée pénalise une très longue journée légitime, qui apparaîtra en anomalie.                                                             |
| D3  | **Journée abandonnée : le geste ouvre une nouvelle journée.** Tout geste (arrivée, pause, reprise, départ) reçu pour une journée abandonnée ouvre une nouvelle journée à l'heure du geste, puis s'applique. Au pupitre, l'opérateur est absent et ne peut que saisir une arrivée. Cas particulier de D12.                                                                                                                                                                                                                                                                                                                  | Ouvrir une journée à partir d'une pause ou d'un départ crée des journées vides ou nulles (E5). Vaut-il mieux les montrer que les refuser ?                  |
| D4  | Sous le seuil, une arrivée est **redondante** : l'opérateur est déjà là. Elle est absorbée : réponse positive, rien n'est ajouté. C'est ce qui permet à un opérateur de nuit de se réidentifier à 3 h.                                                                                                                                                                                                                                                                                                                                                                                                                     | Un retour en soirée sous le seuil est absorbé dans la même journée (E4).                                                                                    |
| D5  | Une journée abandonnée reçoit une **fin présumée** : le dernier fait connu, qu'il s'agisse d'un pointage de présence ou d'OF, survenu entre l'arrivée et l'arrivée + seuil. Elle est calculée, jamais enregistrée, et affichée comme présumée.                                                                                                                                                                                                                                                                                                                                                                             | La fin présumée sous-estime fortement une nuit sans geste (E3).                                                                                             |
| D6  | Le relevé des heures sépare les heures **pointées** et les heures **présumées**, par jour et par semaine.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | Deux totaux : l'assistante saura-t-elle lequel transmettre ?                                                                                                |
| D7  | Un poste de nuit est **découpé à minuit** : sur deux jours, et sur deux semaines du dimanche au lundi. Pas de catégorie « heures de nuit ». _Révisable._                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Le découpage à minuit disperse un poste de nuit sur deux semaines de paie.                                                                                  |
| D8  | Deux journées d'un même opérateur ne se chevauchent jamais. Une régularisation qui le provoquerait est refusée **au gestionnaire**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Le refus du chevauchement peut bloquer une régularisation légitime si la journée suivante a été mal ouverte.                                                |
| D9  | « Démarrer » un OF déjà en cours le **relance** au lieu d'être refusé. Même chose pour une non-conformité déjà en cours.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | Une relance efface le signal d'un « démarrer » en double, qui pouvait révéler une erreur.                                                                   |
| D10 | L'état d'un OF reste « en cours » tant que son journal le dit. Seule l'activité est présentée **« non arrêtée »** quand la journée de l'opérateur est terminée.                                                                                                                                                                                                                                                                                                                                                                                                                                                            | Un OF « en cours » sur lequel personne ne travaille peut induire le back-office en erreur.                                                                  |
| D11 | Le gestionnaire dispose d'une **liste des anomalies** : journées abandonnées sans départ, journées fermées dont l'amplitude dépasse le seuil, pointages signalés (D12).                                                                                                                                                                                                                                                                                                                                                                                                                                                    | Sans notification, personne ne consultera peut-être la liste.                                                                                               |
| D12 | **L'opérateur n'est jamais empêché de pointer ni de commencer son activité.** Un pointage ne rend d'erreur que s'il ne se rattache à rien : chaque geste est enregistré dès qu'il peut l'être, et signalé s'il est inhabituel (tableau ci-dessous). **Exceptions : un OF clôturé**, qui n'est plus pointable, et l'opérateur en est informé par un message ; **un geste qu'on ne sait rattacher à rien** (opérateur, poste ou OF inconnu, geste hors séquence, identifiant réutilisé), qui reste refusé (décision du 26/09/2026, lot 8c abandonné). Les actes du gestionnaire, eux, peuvent être refusés avec explication. | Un pointage signalé compte immédiatement, avant que le gestionnaire l'ait vu. Et le pupitre ne peut plus signaler à l'opérateur une erreur de manipulation. |

### Presque aucun pointage ne rend d'erreur

Un pointage est un geste de l'opérateur : arrivée, pause, reprise, départ, démarrer, non-conformité, arrêter. Le pupitre reçoit un succès, sauf sur un OF clôturé et sur un geste qu'on ne sait rattacher à rien (voir le tableau). Le serveur choisit l'une de ces issues, en préférant toujours celle qui fait compter le temps :

- **enregistré** : le cas normal. Un geste inhabituel (habilitation retirée) est enregistré quand même, et **signalé** en anomalie ;
- **absorbé** : le geste ne change rien. Rien n'est ajouté au journal, mais son identifiant reste réservé pour que le rejeu hors ligne reste sans doublon ;
- **redressé** : le serveur complète ou corrige le geste avant de l'enregistrer (arrivée implicite, heure ramenée), et il trace ce qu'il a fait ;
- **refusé** : réservé aux gestes qu'on ne peut rattacher à rien (opérateur, poste ou OF inconnu du serveur, geste hors séquence, identifiant réutilisé). Le refus reste dans le journal local du pupitre ; le gestionnaire régularise s'il le faut. Une mise en attente côté serveur (lot 8c) a été écartée le 26/09/2026.

![Un pointage ne rend d'erreur que s'il ne se rattache à rien](bornes-de-fin-de-journee/12-aucun-pointage-refuse.svg)

Voici tous les refus qu'un pointage peut rencontrer aujourd'hui (catalogue [codes-erreur.md](../codes-erreur.md)), et ce qu'ils deviennent :

| Pointage                                                            | Refusé aujourd'hui par                                                                 | Exemple concret                                            | Devient                                                                                                 |
| ------------------------------------------------------------------- | -------------------------------------------------------------------------------------- | ---------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| Arrivée d'un opérateur déjà présent (sous le seuil)                 | `journee-de-travail-deja-ouverte`                                                      | réidentification à 3 h (E1)                                | **absorbé**                                                                                             |
| Arrivée ou autre geste sur une journée abandonnée                   | `journee-de-travail-deja-ouverte`, `transition-de-presence-interdite`                  | retour le lendemain (E2), pupitre en retard (E5)           | **redressé** : nouvelle journée (D3)                                                                    |
| Pause, reprise ou départ sans aucune journée ouverte                | `aucune-journee-de-travail-en-cours`                                                   | départ pressé par un opérateur jamais arrivé               | **redressé** : arrivée implicite à l'heure du geste, puis le geste                                      |
| Pause déjà en pause, reprise déjà présent                           | `transition-de-presence-interdite`                                                     | double appui sur « pause »                                 | **absorbé**                                                                                             |
| Geste de présence rejoué dans le désordre qui casse l'enchaînement  | `transition-de-presence-interdite`                                                     | deux pupitres, l'un hors ligne                             | **absorbé** s'il est redondant, sinon **refusé** (409)                                                  |
| Démarrer un OF déjà en cours                                        | `transition-d-atelier-interdite`                                                       | retour sur l'OF 42 mardi 07:05 (E2)                        | **enregistré** comme une relance (D9)                                                                   |
| Arrêter un OF qui n'est pas en cours                                | `transition-d-atelier-interdite`                                                       | double appui sur « arrêter »                               | **absorbé**                                                                                             |
| Arrêter un OF clôturé entre-temps                                   | `suivi-d-atelier-cloture`                                                              | la clôture a déjà arrêté l'activité                        | **absorbé**                                                                                             |
| Démarrer ou non-conformité sur un OF clôturé entre-temps            | `suivi-d-atelier-cloture`                                                              | pupitre hors ligne pendant la clôture                      | **refusé avec un message** : « OF clôturé, vous ne pouvez plus pointer dessus » — seule exception à D12 |
| Pointage sur un poste dont l'habilitation a été retirée entre-temps | `operateur-non-habilite`                                                               | pupitre hors ligne pendant le retrait                      | **enregistré et signalé** : le temps compte, le gestionnaire peut l'annuler                             |
| Pointage daté avant l'engagement de l'OF                            | `evenement-anterieur-a-l-engagement`                                                   | horloge du pupitre en retard                               | **redressé** : ramené à l'heure d'engagement, écart signalé en anomalie                                 |
| Opérateur, poste ou OF inconnu du serveur                           | `operateur-introuvable`, `poste-de-travail-introuvable`, `suivi-d-atelier-introuvable` | référentiel du pupitre périmé, fiche supprimée entre-temps | **refusé** (404)                                                                                        |
| Pointage daté dans le futur                                         | `date-de-survenue-future`                                                              | horloge du pupitre en avance                               | **redressé** : ramené à l'heure de réception, écart signalé en anomalie                                 |
| Deux saisies simultanées sur le même OF ou la même journée          | `saisie-concurrente`                                                                   | deux opérateurs pointent au même instant                   | **enregistré** : le serveur réessaie lui-même                                                           |
| Même identifiant de geste, contenu différent                        | `identifiant-evenement-reutilise`                                                      | défaut du pupitre                                          | **refusé** (409)                                                                                        |

Un OF clôturé est la seule exception montrée à l'opérateur : il n'est plus visible au pupitre, et un pointage qui l'atteint encore (pupitre hors ligne pendant la clôture) est refusé avec un message qui l'explique à l'opérateur. Arrêter une activité sur cet OF reste absorbé, puisque la clôture l'a déjà arrêtée.

Restent aussi des erreurs, parce que ce ne sont pas des pointages :

- une requête malformée ou non authentifiée : c'est un défaut du pupitre, jamais un geste de l'opérateur ;
- les actes du gestionnaire : régularisation, correction, clôture. Ils peuvent être refusés avec explication (D8), puisque celui qui les saisit peut corriger sur le champ.

Le seuil décide si une journée est encore en cours. Il doit rester sous 24 h : c'est ce qui garantit qu'un retour le lendemain à la même heure soit toujours une nouvelle arrivée.

![Le seuil d'amplitude décide si une journée est encore en cours](bornes-de-fin-de-journee/05-solution-seuil.svg)

États de présence de l'opérateur, et ce que le pupitre propose dans chacun :

![États de présence de l'opérateur et ce que le pupitre propose](bornes-de-fin-de-journee/10-etats-de-presence.svg)

## 3. Exemples

### E1 — Poste de nuit complet

| Heure      | Geste                         | Effet                                              |
| ---------- | ----------------------------- | -------------------------------------------------- |
| lun. 20:00 | Dupont s'identifie            | arrivée, nouvelle journée                          |
| lun. 20:05 | démarre OF 42 sur fraiseuse 1 |                                                    |
| mar. 03:00 | s'identifie à nouveau         | arrivée redondante (7 h < 13 h) : **même journée** |
| mar. 03:02 | démarre OF 43 sur le tour     |                                                    |
| mar. 08:00 | « tout arrêter »              | fin OF 42, fin OF 43, départ                       |

| Résultat      | Valeur                                           |
| ------------- | ------------------------------------------------ |
| Journées      | **une seule**, de lun. 20:00 à mar. 08:00 (12 h) |
| Relevé, lundi | 4 h pointées                                     |
| Relevé, mardi | 8 h pointées                                     |
| Anomalie      | aucune                                           |

![Le poste de nuit reste une seule journée](bornes-de-fin-de-journee/07-solution-poste-de-nuit.svg)

### E2 — Départ oublié, journée de jour

| Heure      | Geste                       |
| ---------- | --------------------------- |
| lun. 07:00 | arrivée                     |
| lun. 08:00 | démarre OF 42 (fraiseuse 1) |
| lun. 09:00 | démarre OF 43 (fraiseuse 2) |
| lun. 12:00 | pause                       |
| lun. 13:00 | reprise                     |
| lun. 16:00 | termine OF 43               |
| lun. 17:00 | part **sans rien pointer**  |
| mar. 07:00 | s'identifie                 |
| mar. 07:05 | démarre OF 42 (fraiseuse 1) |

La journée devient abandonnée lundi à 20:00 (07:00 + 13 h). Le dernier fait connu est la fin de l'OF 43 à 16:00.

| Où                  | Aujourd'hui                  | Avec la proposition                      |
| ------------------- | ---------------------------- | ---------------------------------------- |
| Pupitre, lun. 21:00 | présent                      | **absent**, seule l'arrivée est proposée |
| Arrivée mar. 07:00  | perdue                       | **nouvelle journée** ouverte à 07:00     |
| OF 42, mar. 07:05   | refusé                       | **relancé**                              |
| Temps OF 42, lundi  | 8–12 h, puis 13 h → sans fin | 8–12 h + 13–16 h (présumé) = 7 h         |
| Coût OF 42, lundi   | nuit valorisée               | 7 h valorisées                           |
| Relevé, lundi       | 0 h                          | **5 h pointées + 3 h présumées**         |
| Anomalies           | rien                         | « journée sans départ » : Dupont, lundi  |

Le gestionnaire régularise ensuite un départ lundi à 17:00 :

| Où                 | Après la régularisation        |
| ------------------ | ------------------------------ |
| Relevé, lundi      | **9 h pointées**, 0 h présumée |
| Temps OF 42, lundi | 8–12 h + 13–17 h = 8 h         |
| Anomalies          | la ligne disparaît             |

![Fin présumée, nouvelle journée et relance de l'OF, avant et après régularisation](bornes-de-fin-de-journee/06-solution-fin-presumee.svg)

### E3 — Départ oublié, poste de nuit

| Heure      | Geste                  |
| ---------- | ---------------------- |
| lun. 20:00 | arrivée                |
| lun. 20:05 | démarre OF 42          |
| mar. 08:00 | part sans rien pointer |
| mar. 20:00 | s'identifie            |

| Résultat                            | Valeur                                                                                              |
| ----------------------------------- | --------------------------------------------------------------------------------------------------- |
| Abandonnée à partir de              | mar. 09:00 (20:00 + 13 h)                                                                           |
| Pupitre, mar. 10:00                 | absent                                                                                              |
| Arrivée mar. 20:00                  | nouvelle journée                                                                                    |
| Fin présumée de la journée de lundi | lun. 20:05 : le démarrage de l'OF est le dernier fait connu                                         |
| Relevé                              | lundi : 5 min présumées. **Forte sous-estimation**, signalée en anomalie jusqu'à la régularisation. |

C'est la limite assumée de la fin présumée : elle n'invente jamais d'heures. Une nuit sans aucun geste laisse peu de faits connus.

### E4 — Retour en soirée sous le seuil

| Heure      | Geste                  | Effet                                                 |
| ---------- | ---------------------- | ----------------------------------------------------- |
| lun. 07:00 | arrivée                |                                                       |
| lun. 17:00 | part sans rien pointer |                                                       |
| lun. 19:30 | revient, s'identifie   | 12 h 30 < 13 h : arrivée redondante, **même journée** |
| lun. 23:00 | « tout arrêter »       | départ                                                |

| Résultat      | Valeur                                                       |
| ------------- | ------------------------------------------------------------ |
| Journée       | 07:00 → 23:00, 16 h                                          |
| Relevé, lundi | 16 h pointées, **dont 2 h 30 que Dupont n'a pas passées là** |
| Anomalie      | « amplitude excessive » (16 h > 13 h)                        |

Aucune règle automatique ne distingue ce cas d'une longue journée : c'est au gestionnaire de trancher. **Aujourd'hui, il ne peut pas le corriger** (voir lot 7).

![Limite : le retour en soirée sous le seuil](bornes-de-fin-de-journee/09-limite-retour-en-soiree.svg)

### E5 — Geste tardif d'un pupitre en retard

Le pupitre, hors ligne depuis lundi après-midi, affiche encore Dupont présent. Mardi à 08:30, Dupont appuie sur « départ ».

| Heure      | Geste reçu par le serveur | Effet                                                                                              |
| ---------- | ------------------------- | -------------------------------------------------------------------------------------------------- |
| lun. 07:00 | arrivée                   | journée de lundi                                                                                   |
| mar. 08:30 | départ                    | journée de lundi abandonnée depuis lun. 20:00 : **arrivée implicite à 08:30, puis départ à 08:30** |

| Résultat         | Valeur                                                      |
| ---------------- | ----------------------------------------------------------- |
| Refus            | aucun                                                       |
| Journée de mardi | 08:30 → 08:30, durée nulle                                  |
| Journée de lundi | abandonnée, fin présumée au dernier fait connu, en anomalie |

Le même geste reçu sous forme de pause ouvre une journée qui commence en pause.

![Un geste tardif n'est jamais refusé](bornes-de-fin-de-journee/08-solution-geste-tardif.svg)

### E6 — Régularisation qui chevaucherait la journée suivante

Suite de E2 : la journée de mardi a été ouverte à 07:00.

| Régularisation par le gestionnaire         | Résultat                                            |
| ------------------------------------------ | --------------------------------------------------- |
| départ lundi 17:00                         | accepté                                             |
| départ mardi 08:00 sur la journée de lundi | **refusé** : chevauchement avec la journée de mardi |

### E7 — Poste de nuit à cheval sur deux semaines

Arrivée dimanche 17 mai à 20:00, départ lundi 18 mai à 08:00 (voir le schéma de E1).

| Relevé                  | Heures pointées |
| ----------------------- | --------------- |
| Semaine 20, dimanche 17 | 4 h             |
| Semaine 21, lundi 18    | 8 h             |

### E8 — Le gestionnaire change le seuil

Le seuil passe à 10 h. Dupont arrive à 07:00 et oublie son départ.

| Heure | Geste       | Effet                                 |
| ----- | ----------- | ------------------------------------- |
| 07:00 | arrivée     |                                       |
| 17:30 | s'identifie | 10 h 30 > 10 h : **nouvelle journée** |

Le changement vaut pour les gestes qui suivent. Sur une journée **non régularisée**, il peut aussi déplacer la fin présumée, puisque la recherche du dernier fait connu s'arrête à l'arrivée + seuil.

## 4. Découpage en lots

![Découpage en lots et dépendances](bornes-de-fin-de-journee/11-carte-des-lots.svg)

| Lot | Intitulé                                                                      | Exemples couverts          | Dépend de |
| --- | ----------------------------------------------------------------------------- | -------------------------- | --------- |
| 1   | Relance d'un OF en cours                                                      | E2 (mar. 07:05)            | —         |
| 2   | Paramétrage de l'amplitude maximale                                           | E8                         | —         |
| 3   | Journée abandonnée : nouvelle arrivée, gestes tardifs, chevauchement, pupitre | E1, E2, E3, E5, E6, E8     | 2         |
| 4   | Fin présumée : temps passé et coût de revient                                 | E2, E3                     | 3         |
| 5   | Relevés : heures pointées / présumées, poste de nuit                          | E1, E2, E7                 | 4         |
| 6   | Liste des anomalies                                                           | E2, E3, E4                 | 3         |
| 7   | Scinder une journée _(à instruire)_                                           | E4                         | 6         |
| 8   | Aucun pointage refusé : absorption, redressement, signalement                 | tableau des refus (§2), E5 | 6         |

### Lot 1 — Relance d'un OF en cours

- **Pourquoi :** un opérateur qui revient sur un OF resté ouvert ne doit jamais être bloqué.
- **Règle :** « démarrer » une activité déjà en cours la relance. La période précédente s'arrête et une nouvelle commence. Même chose pour une non-conformité déjà en cours.
- **Acceptation :**
  - E2, mardi 07:05 : accepté ;
  - le temps de lundi reste borné par la journée de lundi ;
  - un double appui ne change pas le total.
- **Touche :** atelier, pupitre, coût de revient (leurs relectures du journal d'OF).

### Lot 2 — Paramétrage de l'amplitude maximale

- **Pourquoi :** D1. Le seuil dépend de l'entreprise (dérogations, équipes de 12 h).
- **Règles :**
  - 13 h par défaut pour chaque entreprise ;
  - modifiable par le gestionnaire seul ;
  - valeur strictement entre 0 et 24 h ;
  - aucun effet rétroactif sur les journées déjà découpées.
- **Acceptation :**
  - lecture de 13 h sur une entreprise neuve ;
  - modification à 10 h ;
  - refus de 0 h et de 24 h ;
  - un opérateur ne peut pas modifier.
- **Touche :** nouveau contexte de paramétrage, avec écran gestionnaire côté front.

### Lot 3 — Journée abandonnée

- **Pourquoi :** D2, D3, D4, D8. C'est le cœur : ne plus perdre l'arrivée du lendemain, ne jamais refuser l'opérateur.
- **Règles :**
  - un geste d'un opérateur dont la journée est abandonnée ouvre une nouvelle journée à l'heure du geste ;
  - sous le seuil, une arrivée est redondante ;
  - une régularisation qui fait chevaucher deux journées est refusée ;
  - le pupitre affiche absent l'opérateur dont la journée est abandonnée, et reçoit l'heure jusqu'à laquelle chaque présent reste présent, pour basculer seul hors ligne.
- **Acceptation :** E1 (une seule journée), E2 (nouvelle journée mardi 07:00), E3, E5 (aucun refus), E6 (refus au gestionnaire), E8.
- **Touche :** atelier, pupitre. Côté front : bascule hors ligne sur l'heure reçue, et proposer l'arrivée seule.

### Lot 4 — Fin présumée dans le temps passé et le coût de revient

- **Pourquoi :** D5. La nuit ne doit plus être comptée ni valorisée.
- **Règle :** une journée abandonnée est fermée à son dernier fait connu, marqué présumé. Une journée non abandonnée reste ouverte (travail en cours).
- **Acceptation :**
  - E2 : OF 42 = 7 h, dont 3 h présumées ;
  - E3 : 5 min ;
  - le coût de revient ne valorise plus la nuit ;
  - une régularisation remplace le présumé par le pointé.
- **Touche :** atelier (temps effectif), coût de revient.

### Lot 5 — Relevés

- **Pourquoi :** D6, D7. L'assistante doit voir ce qui reste à confirmer avant de transmettre à la paie.
- **Règles :**
  - chaque jour et chaque semaine portent des heures pointées et des heures présumées ;
  - le poste de nuit est découpé à minuit ;
  - l'historique hebdomadaire signale les périodes présumées.
- **Acceptation :** E1 (4 h + 8 h), E2 (5 h pointées + 3 h présumées, puis 9 h après régularisation), E7 (deux semaines).
- **Touche :** synthèse des heures, feuille de temps.

### Lot 6 — Liste des anomalies

- **Pourquoi :** D11. Sans elle, la fin présumée cacherait l'oubli au lieu de le signaler.
- **Règles :**
  - réservée au gestionnaire ;
  - trois types : « journée sans départ » (abandonnée), « amplitude excessive » (journée fermée au-delà du seuil) et « pointage signalé » (D12, ajouté par le lot 8, sur sa propre liste) ;
  - triée par date, paginée ;
  - une ligne disparaît dès que la régularisation la résout.
- **Acceptation :** E2 (apparaît, puis disparaît après régularisation), E3, E4.
- **Touche :** atelier, avec un écran back-office côté front.

### Lot 7 — Scinder une journée _(à instruire)_

- **Pourquoi :** E4. Avec les actes actuels, le gestionnaire ne peut pas corriger un retour en soirée absorbé dans la même journée.
  - Insérer un départ à 17:00 seul rend le départ de 23:00 impossible à suivre.
  - Insérer une arrivée à 19:30 seule donne deux arrivées de suite.
  - La correction ne remplace qu'un événement à la fois.
- **Piste :** un acte qui insère plusieurs événements d'un coup, validés une seule fois comme la correction, ou un acte « scinder la journée à 17:00 / 19:30 ».
- **À trancher :** quelle forme d'acte, et si la liste des anomalies propose l'action directement.

### Lot 8 — Aucun pointage refusé

- **Pourquoi :** D12. Les lots 1 et 3 couvrent la relance et la journée abandonnée ; celui-ci couvre tous les autres refus du tableau.
- **Règles :**
  - chaque refus du tableau est remplacé par l'issue indiquée ;
  - un pointage signalé compte immédiatement et apparaît dans les pointages signalés ;
  - seuls restent refusés l'OF clôturé, le référentiel inconnu (404), le geste hors séquence non redondant et l'identifiant réutilisé (409).
- **Découpage livré :** 8a (absorptions, arrivée implicite, saisie concurrente rejouée), 8b (pointages signalés). **8c (mise en attente) abandonné** le 26/09/2026 : un opérateur, un poste ou un OF inconnu doit rester un 404, et les deux autres refus restent des erreurs.
- **Acceptation :** chaque ligne du tableau devient un scénario, qui vérifie l'issue attendue.
- **Touche :** atelier (présence et suivi), liste des anomalies. Côté front : n'afficher que le refus de l'OF clôturé, retirer de la politique de rejeu tous les autres refus.

## 5. Questions ouvertes

1. Découper le poste de nuit à minuit est acté mais **révisable** : l'alternative est de le rattacher au jour où il commence.
2. Heures de nuit majorées : non demandées. À rouvrir si la paie en a besoin.
3. Lot 7 : forme de l'acte de scission.
4. Faut-il une notification au gestionnaire, ou la liste des anomalies suffit-elle ?
5. ~~Un pointage mis en attente : faut-il en informer l'opérateur ?~~ Close : pas de mise en attente (lot 8c abandonné).
6. ~~Que peut faire le gestionnaire d'un pointage mis en attente ?~~ Close, même raison.
7. Un pointage d'OF sans aucune présence reste hors de ce sujet.

## 6. Liens

- [contexte-metier.md](../contexte-metier.md) — sections `atelier`, `feuilledetemps`, `syntheseheures`, `coutderevient`, `pupitre`.
- [authentification-pointage.md](authentification-pointage.md) — identification au pupitre et fonctionnement hors ligne.
- [Synthèse des réunions du 01/08/2026](../../discussions/synthese-reunions-01-08-2026.md), §2 et §6.
