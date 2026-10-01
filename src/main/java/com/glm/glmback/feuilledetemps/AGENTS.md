# Bounded context `feuilledetemps`

Les règles communes sont dans [glm-back/AGENTS.md](../../../../../../../AGENTS.md), les raisons métier dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md).

## Responsabilité et vocabulaire

Ramener les activités interprétées par atelier au calendrier de l'entreprise : un opérateur, une semaine ISO
explicite, sept jours du lundi au dimanche, vides compris. Ce contexte est purement lecteur et ne possède aucune table.

`FeuilleDeTemps` porte l'opérateur résolu, la semaine et ses `JourDeLaSemaine`. Chaque jour porte des
`IntervalleDActivite` : affectation (`Activite`), bornes de portion (`Plage`) et identité, état et bornes de
l'activité entière (`ActiviteLue`). `ActiviteInterpretee` est la projection reçue du port et évaluée à la lecture.

Atelier possède le pointage, sa correction et l'interprétation. La feuille évalue les activités projetées et les
découpe au calendrier ; les durées appartiennent à la synthèse et la valorisation au coût. Le référentiel possède
l'opérateur ; la feuille expose les identifiants de poste et d'élément sans lire leurs libellés.

## Invariants

- `ActivitesDeLOperateur` lit `activite_d_atelier` par une entité propre `@Immutable`, jointe au suivi pour son élément.
  Aucun import des contextes `atelier`, `operateur` ou `postedetravail`, tous annotés `@BusinessContext`.
- La sélection porte sur le recouvrement de la semaine par le début et la fin réelle, la fin au plus tard ou l'échéance. Aucune borne
  basse fixe du début : une régularisation peut établir plus de 13 h, voire plus d'une semaine.
- `FeuillesDeTempsService` reçoit l'instant facultatif `evaluation`. Sans lui, l'heure du serveur est relevée
  une seule fois ; l'instant utilisé gouverne l'expiration, le découpage des activités en cours et les plages
  possibles à résoudre, et la réponse
  le rend. Passer le même instant à la feuille et à la synthèse assure la même décision d'expiration.
  Les faits connus restent interprétés, même postérieurs à cet instant. Une écriture entre les deux appels peut
  changer les faits lus : l'instant commun règle l'expiration, sans garantir un instantané commun.
- Un instant passé est accepté ; la limite future est l'heure du serveur plus deux minutes, incluse.
  Le serveur est échantillonné une seule fois, y compris avec un instant explicite. Au-delà, la lecture répond
  400 `evaluation-future`. Un instant fourni vide ou mal formé répond aussi 400, sans rapport.
- L'échéance est celle projetée par atelier ; le lecteur la compare à `evaluation`. Les états sont `TERMINEE`,
  `TERMINEE_AUTOMATIQUEMENT`, `EN_COURS`, `A_RESOUDRE`. Une fin réelle est conservée,
  même au-delà de l'échéance. Sans elle, l'échéance atteinte termine automatiquement l'activité à cette borne.
  Une activité à résoudre reste sans fin ; l'échéance ne résout pas le conflit.
- Chaque portion conserve l'identité stable, l'état et les bornes de l'activité entière. La fin n'existe que pour
  les deux états terminés. Le découpage aux minuits locaux et aux limites de semaine ne déplace jamais ces bornes.
- Une activité en cours rend une indication sans fin sur chaque jour atteint à l'instant de lecture, dans la semaine.
  Son début entier permet de lire « en cours depuis dimanche » sur lundi, sans durée à compter.
- Une activité à résoudre figure sur tous les jours de sa plage possible, depuis son début jusqu'à sa
  `finAuPlusTard` projetée par atelier, limitée par `evaluation`. La fin de cette plage est exclusive.
  Chaque portion reste sans fin réelle ni durée, garde l'identité originale et rend la borne possible entière.
  Un jour sans pointage local peut la porter ; la sélection retrouve aussi un début antérieur à la semaine.
- Une activité porte l'élément, jamais le suivi : un élément réengagé après clôture reste le même élément.
- Les portions sont triées par début, élément puis identité stable de l'activité.
- `DecoupageCalendaire` est le seul détenteur du calendrier. Le fuseau passe par `FuseauHoraireDeLEntreprise` ;
  son adapter rend actuellement `Europe/Paris`.

## Couture et noms techniques

`src/test/features/feuille_de_temps.feature` écrit réellement par l'API d'atelier puis lit la feuille : relances,
transitions ciblées, fins tardives, régularisations, corrections, annulations et clôtures doivent rendre les faits
projetés par leur propriétaire. `DecoupageCalendaireTest` prouve les semaines ISO et les minuits locaux, y compris
les changements d'heure.

Spring et Hibernate utilisent les noms simples des beans et entités : les noms propres à la feuille évitent les
collisions avec les propriétaires. Les records métier peuvent partager le vocabulaire des autres contextes.

## Lecture d’un poste de nuit

Minuit répartit une activité au calendrier, sans produire de geste ni de fin métier. Une activité terminée
de 20 h à 8 h donne des portions de 4 h puis de 8 h ; dimanche 22 h à lundi 3 h donne 2 h puis 3 h dans
les deux semaines ISO. Les activités en cours gardent une indication sans durée ; celles à résoudre
suivent leur plage possible bornée à l’évaluation.
