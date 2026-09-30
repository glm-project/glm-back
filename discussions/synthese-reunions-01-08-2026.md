# Synthèse des réunions du 01/08/2026 — contenus indépendants conservés

Sources : [extraits client](<reunion client 01-08-2026.md>) et
[extraits d’équipe](<reunion equipe suite reunion client du 01-08-2026.md>), issus de retranscriptions automatiques.
Cette synthèse éditoriale sélectionne les sujets conservés ; elle ne constitue ni une citation intégrale ni une
spécification actuelle. Les coupures sont explicites dans les deux documents d’extraits, sans attribution de locuteurs.
Les formulations tronquées, objections et divergences d’août restent des incertitudes historiques. Les contrats
actuels sont décrits chez leurs [propriétaires](../documentation/contexte-metier.md).

Niveaux de fiabilité : ✅ propos explicite du client ; 🔁 restitution en réunion d’équipe, non reconfirmée devant
le client en août ; 🔎 reconstruction éditoriale d’un passage tronqué ; ❓ sujet non tranché à cette date.

## Objectif et simplicité

Suivre le travail des opérateurs et le coût de revient de la fabrication, avec une direction qui consulte les
rapports. Le client demande une application simple et une progression itérative, plutôt qu’un processus de
planification. L’équipe discute les débuts et arrêts de tâches, la liste commune et les commandes globales.

## Pause et gestes globaux : portée des sources d’août

Le client rapproche pause, arrêt et reprise d’un OF. L’équipe rapporte l’intérêt d’un bouton global pour plusieurs
tâches et d’un arrêt global. La confirmation directement par le client pour plusieurs tâches manque dans ces
sources historiques ; les décisions ultérieures appartiennent aux documents actuels.

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
- ❓ Volume simultané attendu : le client annonce **10 à 15 moules neufs** _et_ **une vingtaine d'OF**, puis parle d'un espace écran pour « une vingtaine de boutons ou 15 boutons ». **Les deux chiffres ne se recoupent pas** : le total serait de 25 à 35 boutons. L’incertitude concernait l’écran d’accueil opérateur en août.
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

Logique : ce qui intéresse le client n'est pas le coût d'une personne mais le **coût par nature de travail** (coût du fraisage, coût de l'érosion). Répartir le temps humain entre les machines actives évite de compter plusieurs fois la même heure de travail.

❓ **Ce qui reste ouvert n'est pas la règle, mais l'objection soulevée ensuite par Nicolas** : diviser le taux humain fausse le coût réel de la personne (« ce qui nous intéresse, c'est pas le coût d'une personne… le problème c'est que si la personne travaille une heure sur deux machines, son coût est divisé par deux »). L'échange s'interrompt sans conclusion — c'est le passage le plus dégradé de la transcription. L’incertitude historique porte aussi sur le cas non abordé : **que se passe-t-il quand les machines actives simultanément relèvent d'OF différents ?**

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

## Corrections externes

Le fragment client conservé indique une administration capable d’établir qu’un opérateur a travaillé sur un OF
de telle heure à telle heure. Il exclut une correction par la personne elle-même. Ce constat d’août reste distinct
du contrat de régularisation, correction et annulation livré depuis.

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
- Sa crainte : une coupure Internet ou une panne fait perdre les pointages, donc les heures des employés .
- **Priorité affichée par le client, actée par l'équipe : la continuité de service.**
- Réponses apportées :
  - sauvegardes et pare-feu côté hébergement ; au pire, perte limitée aux pointages du jour ;
  - **offline-first** : en cas de coupure, les pointages sont enregistrés dans une **base locale du poste**, puis **synchronisés** au retour du réseau. Réaction du client : « voilà, ça, ça me plaît ».
- Contexte sécurité : le client subit déjà des réticences de ses propres clients sur les accès distants (SAV constructeurs de machines). Argument rassurant retenu : les OF et moules stockés ne sont **rattachés à aucun client final**, ils ne portent aucune donnée sensible.

---

## 10. Divergences entre les deux transcriptions

Divergences historiques entre ces deux sources :

1. **La notion de machine.** La réunion d'équipe indique que le client n'en voulait pas — « il n'y a même pas une notion de machine, c'est moi qui suis parti plus loin », le client ayant refusé d'affecter des tâches par machine et par personne (« usine à gaz »). La réunion client du 01/08 réintroduit clairement machines, compétences et coûts machine. **Lecture retenue** : la machine existe bien dans le modèle, mais **rattachée à l'opérateur** (paramétrage de la ressource), **jamais à l'OF** — c'est cela que le client refusait.
2. **Un OF à la fois ou plusieurs ?** L'échange d'équipe hésite. La réunion client tranche : **plusieurs OF simultanés**, et même plusieurs machines sur un même OF.
3. **Moule vs OF.** L'échange d'équipe conclut qu'il n'y a « pas de notion de moule, c'est la même chose que l'OF ». La réunion client maintient **deux dénominations à distinguer visuellement**, pour un traitement fonctionnel identique.

---

## Incertitudes historiques encore visibles dans les extraits

- L’objection sur le coût humain dans l’échange client reste tronquée ; elle ne tranche pas les règles actuelles.
- Le volume de moules et d’OF annoncé ne se recoupe pas avec le nombre de boutons évoqué.
- La page « pilotage » est rejetée dans sa forme initiale ; la suppression ou transformation exacte reste incertaine en août.
- La place respective des moules et OF à l’écran est une proposition d’équipe dont la réponse est tronquée.
- La biométrie est d’abord privilégiée dans la restitution d’équipe ; le client conclut au code pour commencer.
- Les promesses de sauvegarde, d’hébergement et de synchronisation sont des propos historiques, sans preuve de déploiement.

Les extraits préservent les discussions sur le multi-OF, les machines, les tarifs, les NC, les références,
l’ergonomie, le matériel, l’identification et la continuité de service. Le travail interne non facturable est
évoqué par son bouton propre ; son modèle reste une question distincte chez son propriétaire actuel.
