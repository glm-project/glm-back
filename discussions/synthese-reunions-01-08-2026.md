# Synthèse des réunions du 01/08/2026

Sources :

- `reunion client 01-08-2026.txt` — réunion avec le client (Carlos) et l'équipe (Jean-Yves, Nicolas, Loïc).
- `reunion equipe suite reunion client du 01-08-2026.txt` — échange interne d'équipe (Jean-Yves restitue le besoin client à Nicolas).

> Les transcriptions sont des retranscriptions automatiques : certains passages sont tronqués ou approximatifs. Les points marqués « à confirmer » sont ceux où le verbatim ne permet pas de trancher avec certitude.

> **Niveaux de fiabilité employés dans ce document.** ✅ = le client l'énonce explicitement dans la réunion du 01/08. 🔁 = rapporté de seconde main dans la réunion d'équipe (Jean-Yves restitue des propos du client), donc **non reconfirmé devant le client**. 🔎 = reconstruction de ma part à partir d'un passage tronqué par la transcription automatique. ❓ = point ouvert, non tranché en réunion.

---

## 1. Objectif du produit

Une application de **suivi des temps** permettant de calculer le **coût de revient** de la fabrication d'un moule.

Deux populations d'utilisateurs :

- les **opérateurs** de l'atelier, qui pointent et déclarent sur quel travail ils sont ;
- la **direction / l'administration**, qui supervise l'avancement, le coût de revient par OF, et récupère les heures pour la paie.

Exigence structurante répétée par le client : **la simplicité**. Il ne veut pas d'ERP (« des trucs très lourds en gestion »), il veut « un truc simple, très très simple ». L'outil remplace la pointeuse actuelle, pas le processus de l'atelier.

Méthode retenue : **itératif** — une V1 volontairement réduite, améliorée au fur et à mesure.

---

## 2. Le principe central : une pointeuse à laquelle on ajoute l'OF

Le client formule le besoin ainsi : « il faut penser à une pointeuse, mais on rajoute une option OF à cette pointeuse ».

Un seul geste alimente donc **deux comptages simultanés** :

1. les **heures de présence** de l'employé (base de la paie) ;
2. le **temps passé par ordre de fabrication** (base du coût de revient).

### Parcours opérateur

| Moment           | Action                              | Effet                                                                             | Source |
| ---------------- | ----------------------------------- | --------------------------------------------------------------------------------- | ------ |
| Arrivée          | Il s'identifie                      | Pointage d'arrivée **automatique** — pas de bouton « démarrer ma journée » séparé | ✅     |
| Début de travail | Il clique sur un OF                 | Démarre le temps sur cet OF                                                       | ✅     |
| Changement       | Il clique sur un autre OF           | Bascule (ou cumule, voir multi-OF)                                                | ✅     |
| Pause déjeuner   | **Un seul bouton « pause »**        | Met en pause **tous** les OF actifs                                               | 🔁     |
| Retour           | Il reprend                          | Pause / arrêt / reprise sont le même mécanisme                                    | ✅     |
| Fin de journée   | **Un seul bouton « tout arrêter »** | Clôture la présence et tous les OF en cours                                       | 🔁     |

Point d'ergonomie tranché en réunion d'équipe : **jamais N clics pour N tâches**. Si trois OF tournent, il ne faut pas cliquer trois fois pour la pause — un bouton global, « comme une coupure générale d'électricité ».

⚠️ **Attention à la source de cette règle.** Le bouton de pause global et le bouton « tout arrêter » n'apparaissent que dans la **réunion d'équipe**, où Jean-Yves rapporte les propos du client (« il m'a dit c'est un truc simple… ne mettez qu'un bouton, pas trois »). Dans la réunion client du 01/08, la pause est au contraire décrite **au singulier** : « il met une pause, ça fait une pause automatiquement sur l'OF ». Le comportement en présence de plusieurs OF actifs n'a donc **jamais été validé directement par le client** — alors qu'il structure l'écran principal. À reconfirmer en priorité.

### Heures de présence

Elles courent de **l'arrivée dans la société au départ**, et non du premier au dernier OF. Point vérifié explicitement pendant la réunion.

### Temps non affecté à un OF (bouton « GLM »)

Un opérateur peut être présent sans avoir de travail rattaché à un OF (période creuse). Il faut malgré tout comptabiliser sa présence : une **tâche par défaut « GLM »**, non rattachée à un OF, à placer **tout en bas** de l'écran (« comme ça veut dire qu'on a toujours du boulot »).

### Non-conformité (NC)

Une pièce ratée en cours de réalisation doit être refaite. Ce temps de reprise :

- reste **rattaché au même OF** ;
- coûte le **même prix horaire** ;
- mais est comptabilisé **séparément**, en NC.

Objectif : à la clôture, savoir « combien de temps on a passé à faire du bon travail et combien à refaire ».

---

## 3. Modèle métier

### L'OF

- **Un OF, c'est un numéro. Rien d'autre.** Pas de gamme, pas d'opérations, pas de devis, pas de description dans l'application. Le client refuse explicitement d'y entrer : « je veux pas, c'est une usine à gaz ».
- Deux natures de travail, fonctionnellement identiques mais **à distinguer visuellement** :
  - les **moules neufs** — fabrication complète de A à Z, numérotés en séquence (1015, 1016, 1017…) ;
  - les **OF** — modifications sur un moule existant, plus nombreux, sous la forme « OF + numéro ».
  - Un moule neuf devient un OF dès qu'il repasse en modification après essai.
  - 🔎❓ Moules en haut de l'écran, OF en bas : **proposition de l'équipe**, la réponse du client est tronquée dans la transcription. Ce n'est pas une décision.
- **Créés dans l'application par le dirigeant lui-même ou son assistante** (« ça va être fait par moi ou par mon assistante »), pas par les opérateurs.
- **Aucune planification** : pas de date de fin prévue, pas d'estimation d'heures. « On ne planifie rien avec le logiciel. »
- **Clôture manuelle** en back-office. Un OF clôturé **disparaît des écrans opérateurs** — ils ne voient que les OF actifs.
- 🔎 Durée réelle très variable : de quelques heures à plusieurs mois. (La transcription donne « 23 mois » et « 34 mois », que je lis comme « 2-3 » et « 3-4 » mois — c'est une reconstruction, pas du verbatim.)
- ❓ Volume simultané attendu : le client annonce **10 à 15 moules neufs** _et_ **une vingtaine d'OF**, puis parle d'un espace écran pour « une vingtaine de boutons ou 15 boutons ». **Les deux chiffres ne se recoupent pas** : le total serait de 25 à 35 boutons. À clarifier — l'écran d'accueil opérateur en dépend directement.
- **Pas d'affectation d'OF par personne** : tout le monde voit la même liste. L'affectation du travail reste orale / papier, hors application (le chef d'atelier distribue les programmes). Le client sait par ailleurs à quelle commande client correspond chaque OF, mais cette information reste hors de l'application.

### La ressource (opérateur)

C'est sur l'opérateur, et non sur l'OF, que se raccroche tout le paramétrage :

- un **taux horaire** ;
- une **fonction / compétence** : fraisage, tournage, érosion, découpe à fil, dessin… ;
- une **liste de machines** qu'il est habilité à utiliser (**maximum 4 par personne**). Attention : le plafond de 4 porte sur la personne, pas sur l'OF — interrogé sur le nombre de machines que peut mobiliser un OF, le client répond « Ah oui, même plus ».

> « La machine est liée à l'opérateur, et l'opérateur a la fonction. »

C'est la fonction de l'opérateur qui permet d'agréger la synthèse d'OF par catégorie de travail.

### La machine

- Porte un **coût horaire**.
- **Jamais rattachée à un OF** (un moule passe par plusieurs machines, et le client ne veut pas gérer ça).
- **Aucune connexion technique** entre les machines-outils et l'application : l'opérateur lance sa machine, puis va pointer, ou l'inverse. Les deux ne sont pas corrélés.

### Travail en parallèle

Deux formes de parallélisme, toutes deux à supporter :

1. **Plusieurs OF simultanés** — un opérateur peut démarrer plusieurs OF en même temps.
2. **Plusieurs machines sur un même OF** — cas de l'érosion, par exemple : deux pièces du même OF sur deux machines différentes. Cas ponctuel, qui ne concerne que « deux ou trois personnes » dans l'atelier.

Ergonomie associée : au clic sur un OF, **si et seulement si** l'opérateur est paramétré sur plusieurs machines, une pop-up lui demande sur quelle(s) machine(s) il travaille. Un opérateur mono-machine garde **un seul clic**. Il peut ensuite désactiver une machine en cours de session sans arrêter le reste.

---

## 4. Calcul du coût de revient

✅ **La formule est énoncée clairement par le client**, et répétée deux fois de suite :

- coût machine : **coût horaire de chaque machine** active, **non divisé** ;
- coût humain : **taux horaire de l'opérateur divisé par le nombre de machines** qu'il utilise simultanément (« son horaire est divisé par le nombre de machines qu'il utilise » ; « s'il n'utilise qu'une, il n'est pas divisé »).

Logique : ce qui intéresse le client n'est pas le coût d'une personne mais le **coût par nature de travail** (coût du fraisage, coût de l'érosion). Répartir le temps humain entre les machines actives évite de compter plusieurs fois la même heure de présence.

❓ **Ce qui reste ouvert n'est pas la règle, mais l'objection soulevée ensuite par Nicolas** : diviser le taux humain fausse le coût réel de la personne (« ce qui nous intéresse, c'est pas le coût d'une personne… le problème c'est que si la personne travaille une heure sur deux machines, son coût est divisé par deux »). L'échange s'interrompt sans conclusion — c'est le passage le plus dégradé de la transcription. À reprendre, ainsi que le cas non abordé : **que se passe-t-il quand les machines actives simultanément relèvent d'OF différents ?**

---

## 5. Écrans

Revue des 4 pages de la maquette :

### 5.1 Accueil / tableau de bord direction — **à retravailler**

- **Supprimer** le pourcentage de complétion : sans estimation d'heures, il n'a pas de sens (« complexifie pas les choses pour rien »).
- **Conserver / ajouter** des indicateurs graphiques simples :
  - heures cumulées par OF en cours (« sur cet OF on a déjà passé 100 heures ») ;
  - courbe des non-conformités.
- Volontairement peu de graphiques, très simples.

### 5.2 « Pilotage » — **rejetée en l'état** ❓

Le client dit « moi, cette page, non » — mais il enchaîne immédiatement par « par contre, cette page-là, on peut sortir graphiquement le nombre d'heures actuellement sur OF », ce qui ressort ensuite qualifié d'« écran d'accueil ». **Suppression pure ou transformation en tableau de bord graphique : la transcription ne permet pas de trancher**, et le nombre final de pages (3 ou 4) n'est donc pas établi. Ce qui est certain : la page telle qu'elle est maquettée, avec son pourcentage de complétion, est rejetée.

### 5.3 Temps réel — **conservée**

Vue « qui est là et qui fait quoi », avec **code couleur** :

- ✅ vert : présent, en train de travailler sur un OF ;
- ✅ rouge : a quitté la boîte ;
- 🔎 orange : proposé par l'équipe pour « parti déjeuner » ; la réponse du client est tronquée dans la transcription. Le client évoque par ailleurs une quatrième couleur (rose) sans qu'on sache à quoi elle correspond.

Usage principal : voir d'un coup d'œil, le matin, qui est présent sans traverser l'atelier. On affiche le **numéro d'OF** et le statut, pas le détail.

### 5.4 Historique par employé — **conservée, mais back-office**

- Vue **hebdomadaire** (lundi : arrivé à 6h20, pause, …).
- Destinataire : **l'assistante / la comptable**, pour transmettre les heures au comptable de paie. Elle accède à **tous** les employés.
- Point à confirmer : le client hésite sur l'accès de l'opérateur à son propre historique. À trancher.

---

## 6. Oublis de pointage

Cas reconnu comme inévitable et **non traité par la V1 côté opérateur**. Décisions :

- ce n'est **pas** l'opérateur qui corrige son propre pointage (risque de fraude) ;
- une **interface d'administration** permet de saisir a posteriori : « le mec a bossé sur tel OF de telle heure à telle heure » ;
- côté processus, le client se dit prêt à gérer à la main : l'opérateur signale, on lui dit d'aller pointer, et on ajoute le complément en fin de journée.

L'équipe s'engage à **proposer des solutions ergonomiques** (option « pointage en retard » avec saisie de l'heure de début, par exemple).

---

## 7. Identification des opérateurs

Longue discussion, conclusion nette.

| Piste                  | Verdict               | Motif                                                                                                                                       |
| ---------------------- | --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| Empreinte digitale     | Écartée pour la V1    | Capteur à acheter + coût de développement / intégration ; mains sales en atelier (contournable par 3-4 empreintes enregistrées)             |
| Reconnaissance faciale | Écartée               | Technologies propriétaires ; la meilleure (Apple) imposerait d'acheter un iPad ; solutions Windows moins fiables et contournables par photo |
| Reconnaissance vocale  | Écartée               | Bruit de l'atelier                                                                                                                          |
| Badge / clé physique   | Écarté                | Se prête aussi facilement qu'un code ; badge + code = double contrainte sans gain                                                           |
| Smartphone personnel   | Écarté                | « J'ai oublié mon téléphone », et un collègue peut pointer avec le téléphone d'un autre                                                     |
| **Code personnel**     | **Retenu pour la V1** | C'est déjà le fonctionnement actuel (ex. taper « 049 » : on/off) ; le moins onéreux                                                         |

Argument qui a emporté la décision : dès lors qu'un moyen de secours faible existe (le code), l'authentification forte ne protège plus rien — autant partir directement sur le code, plus simple pour tout le monde. « Le risque zéro n'existe pas. »

**Dissuasion plutôt que blocage** : l'application conserve l'historique complet des connexions ; idée évoquée de prendre une photo au moment du login à titre purement dissuasif (sans reconnaissance). Détection d'anomalies « à la Google Photos » : explicitement classée **second temps, clairement secondaire**.

---

## 8. Matériel et ergonomie du poste de pointage

- **Grand écran tactile** piloté par un **PC Windows**, en remplacement de la pointeuse actuelle. Application **web**. Fonctionnement actuel à remplacer : l'opérateur tape son code (ex. « 049 »), ce qui démarre, puis le retape, ce qui arrête — « on/off, on/off tout le temps ».
- **Tablettes écartées** : environnement mécanique (poussière, graisse, solvants), elles « n'ont pas duré longtemps ».
- **Plusieurs postes** : l'atelier fait 1000 m², le client envisage au moins **deux PC** (entrée + fond d'atelier), utilisables simultanément. Aucune contrainte technique côté équipe.
- L'écran tactile sert aussi, quand il est inactif, à dérouler un PowerPoint de présentation de GLM pour les clients de passage — à garder en tête pour la mise en veille.

### Règles d'affichage

- **Un OF = un bouton portant son numéro.** Pas de description, pas de libellé, pas de texte superflu.
- **Rien qui défile.** L'opérateur s'identifie, il voit tout d'un coup, « tac clac ».
- Distinction graphique claire moules / OF, peu d'informations, peu de détails.

---

## 9. Hébergement, réseau et continuité de service

Sujet le plus sensible pour le client.

- L'application est **hébergée à distance, en France** — pas de serveur physique chez le client, contrairement à son réflexe initial.
- Sa crainte : une coupure Internet ou une panne fait perdre les pointages, donc les heures des employés (« c'est le gros bordel… pour les heures travaillées des employés »).
- **Priorité affichée par le client, actée par l'équipe : la continuité de service.**
- Réponses apportées :
  - sauvegardes et pare-feu côté hébergement ; au pire, perte limitée aux pointages du jour ;
  - **offline-first** : en cas de coupure, les pointages sont enregistrés dans une **base locale du poste**, puis **synchronisés** au retour du réseau. Réaction du client : « voilà, ça, ça me plaît ».
- Contexte sécurité : le client subit déjà des réticences de ses propres clients sur les accès distants (SAV constructeurs de machines). Argument rassurant retenu : les OF et moules stockés ne sont **rattachés à aucun client final**, ils ne portent aucune donnée sensible.

---

## 10. Divergences entre les deux transcriptions

À lever explicitement, car elles portent sur le périmètre :

1. **La notion de machine.** La réunion d'équipe indique que le client n'en voulait pas — « il n'y a même pas une notion de machine, c'est moi qui suis parti plus loin », le client ayant refusé d'affecter des tâches par machine et par personne (« usine à gaz »). La réunion client du 01/08 réintroduit clairement machines, compétences et coûts machine. **Lecture retenue** : la machine existe bien dans le modèle, mais **rattachée à l'opérateur** (paramétrage de la ressource), **jamais à l'OF** — c'est cela que le client refusait.
2. **Un OF à la fois ou plusieurs ?** L'échange d'équipe hésite. La réunion client tranche : **plusieurs OF simultanés**, et même plusieurs machines sur un même OF.
3. **Moule vs OF.** L'échange d'équipe conclut qu'il n'y a « pas de notion de moule, c'est la même chose que l'OF ». La réunion client maintient **deux dénominations à distinguer visuellement**, pour un traitement fonctionnel identique.

---

## 11. Points ouverts

| #   | Sujet                                                           | Attendu                                                                                                                                 |
| --- | --------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | Formule de coût quand un opérateur est sur plusieurs machines   | Valider la règle exacte avec le client (§4), notamment le cas machines/OF différents                                                    |
| 2   | Rattrapage des oublis de pointage                               | L'équipe propose des solutions ergonomiques ; validation client                                                                         |
| 3   | Accès de l'opérateur à son propre historique hebdomadaire       | Trancher (le client dit d'abord non, puis semble accepter)                                                                              |
| 4   | Contenu exact du tableau historique hebdomadaire                | À affiner avec l'assistante                                                                                                             |
| 5   | Colonnes complémentaires de la synthèse d'OF                    | Montant du devis et montant d'achats : champs de saisie libres, non reliés au reste — confirmer                                         |
| 6   | Résolution / définition de l'écran de pointage actuel           | **Information non obtenue** : à la question posée, le client répond « je sais pas vous dire ». À redemander (marque évoquée, sans plus) |
| 7   | Comportement de la pause et de l'arrêt avec plusieurs OF actifs | Reconfirmer le bouton global directement avec le client (§2) — jamais validé de première main                                           |
| 8   | Nombre de boutons à afficher simultanément                      | Lever la contradiction 10-15 moules + 20 OF vs « une vingtaine de boutons » (§3)                                                        |
| 9   | Sort de la page « Pilotage »                                    | Supprimée ou transformée en tableau de bord graphique ? (§5.2)                                                                          |
| 10  | Ordre d'affichage moules / OF                                   | « Moules en haut » n'est qu'une proposition non validée (§3)                                                                            |
| 11  | Périmètre exact de la V1                                        | Découpage itératif à formaliser sur la base de cette synthèse                                                                           |

---

## 12. Ce qui est explicitement hors périmètre

- Toute **planification** (dates de fin, charge prévisionnelle, estimation d'heures).
- Le **contenu des OF** : gamme, opérations, devis détaillé.
- L'**affectation** des OF aux opérateurs dans l'application.
- Toute **liaison technique** entre l'application et les machines-outils.
- La **biométrie** et la détection d'anomalies d'identification (reportées après la V1).
