# Contexte métier

Décrit les bounded contexts métier du projet et leur rôle. Les règles de code (architecture, DDD, tests, conventions) sont dans `glm-back/AGENTS.md`.

Toutes les données métier sont isolées par entreprise cliente : chaque entreprise a son propre schéma PostgreSQL, désigné par le claim `tenant` du token. Aucun agrégat ne porte donc d'identifiant d'entreprise — l'isolation est assurée par l'infrastructure, décrite dans [multitenancy.md](multitenancy.md).

**L'application s'adresse à un maximum d'entreprises clientes.** GLM sert de trame, pas de spécification : aucun concept propre à son métier n'est obligatoire dans le modèle. Les notions qu'une autre entreprise pourrait ne pas avoir — une référence externe, un poste de travail, une fonction d'usinage — sont toutes facultatives, et leur absence redonne un comportement cohérent plutôt qu'un cas dégradé.

## elementdefabrication

Gère les éléments de fabrication. Un `ElementDeFabrication` est rangé dans une `Categorie` que l'entreprise a déclarée dans `categoriedeproduit` — `MOULE` et `OF` chez le client de référence — et la porte comme une valeur : les éléments de deux catégories ne diffèrent que par elle, par leur préfixe de nommage et par leur série de numérotation. Le nom est toujours produit par le domaine à la création, par numérotation automatique propre à la catégorie et à l'année, préfixée du code de la catégorie (`MOULE-2026-000001`) — l'API ne le fournit jamais. Une catégorie non déclarée est refusée (409 `categorie-inconnue`).

« Produit » est un terme volontairement générique : l'application s'adresse à plusieurs entreprises clientes, dont les métiers nomment différemment ce qu'elles fabriquent (des moules, chez le client de référence).

La `Fiche` porte ce que l'utilisateur peut renseigner et réviser. Ses deux champs sont **facultatifs** : un élément de fabrication se réduit légitimement à son seul numéro. La `Reference` est l'identifiant que l'entreprise donne elle-même à l'élément dans son propre système (numéro de moule, référence de plan) ; elle est **unique par entreprise** quand elle est renseignée, l'unicité étant portée par une contrainte du schéma du tenant. PostgreSQL considérant les `NULL` comme distincts, autant d'éléments que nécessaire peuvent rester sans référence, et deux entreprises peuvent employer la même.

La garde d'unicité vit dans `ElementsDeFabricationService`, qui lit le détenteur d'une référence par le port `ElementDeFabricationRepository.idPourReference` et lève `ReferenceDejaUtiliseeException` (409). La contrainte en base est le filet de dernier recours : deux créations strictement concurrentes de la même référence produiraient un 500 plutôt qu'un 409, cas assumé puisque la création est le fait du dirigeant ou de son assistante.

La création et la modification passent par des commandes d'action (`ElementDeFabricationToCreate`, `ElementDeFabricationToUpdate`) construites par l'adapter primaire et orchestrées par `ElementsDeFabricationService`, service de domaine pur qui porte les ports repository, compteur, catégories déclarées et horloge.

Le repository et le compteur sont persistés en PostgreSQL, dans le schéma de l'entreprise courante : la numérotation repart donc de 1 pour chaque entreprise, et deux entreprises peuvent porter le même nom d'élément. Les catégories, donc les préfixes, sont propres à chaque entreprise : `CategoriesDeclarees` les lit dans son schéma, par une entité en lecture seule de `categorie_de_produit`.

### Points ouverts

1. **Relation produit ↔ ordre de fabrication.** Le client décrit un enchaînement (« ce moule neuf, une fois testé, s'il y a une opération à faire dessus, ça se transforme en OF ») mais ne demande jamais le lien, et l'imposer exclurait les modifications sur des produits antérieurs à l'application. Le jour où ce lien sera ajouté, il ne concernera que les ordres de fabrication : les deux types cesseront de ne différer que par leur valeur, ce qui rouvrira la question de scinder l'agrégat unique — aujourd'hui justifié précisément parce qu'ils ne diffèrent par aucun champ.
2. **Suppression.** Le client ne parle jamais de supprimer, seulement de clôturer. Dès que les temps seront saisis, la suppression d'un élément qui en porte devra être interdite : elle empêcherait de relire les faits et leurs coûts.
3. **Numérotation.** Les préfixes sont désormais les catégories de chaque entreprise. L'année et le reset annuel du format `MOULE-2026-000001` n'ont en revanche aucune source client, alors que des ordres de fabrication durant plusieurs mois traversent les millésimes.
4. **Critère de lecture.** `ElementDeFabricationCriteria` ne filtre que par période de création et la liste est paginée. Aucun écran décrit par le client ne filtre ainsi ; le seul critère cité est « actifs seulement ». Côté atelier, ce point est traité — la période y est devenue facultative.

## categoriedeproduit

Gère le **référentiel des catégories de produit** : les familles dans lesquelles l'entreprise range ce qu'elle fabrique. Le client de référence en a deux, les moules et les OF ; une autre entreprise en nommera d'autres. C'est pourquoi aucune catégorie n'est créée d'office : une entreprise neuve commence sans, et son gestionnaire déclare les siennes.

Une `CategorieDeProduit` se réduit à un **code** (`MOULE`, `OF`) et à un **rang**. Le code est à la fois ce qui s'affiche et le préfixe du nom des éléments qui s'y créent : il n'y a pas de libellé à part, donc pas deux désignations à garder d'accord. Il **ne se renomme jamais**, puisqu'il entre dans la clé qui fabrique les noms ; son motif est celui du préfixe d'un nom d'élément, des lettres majuscules sans accent.

Le **rang** porte l'ordre d'affichage choisi par l'entreprise : celui des boutons de création, des filtres et des zones du pupitre. Une catégorie nouvelle se range en dernier, pour que l'ordre déjà choisi ne bouge pas. Le gestionnaire réordonne en donnant **l'ordre entier**, jamais par déplacements relatifs : il doit citer chaque catégorie une fois et une seule. Une liste qui en omet une, en cite une deux fois ou en cite une inconnue — déclarée ou supprimée entre-temps par quelqu'un d'autre — est refusée plutôt que complétée au hasard.

**Une catégorie qui range des produits ne se supprime pas** : leur nom porte son code, et ils resteraient rangés dans une famille disparue. La règle vit dans le domaine, derrière le port `CategoriesUtilisees`, dont l'adapter lit la table `element_de_fabrication` par une entité en lecture seule — sans jamais importer `elementdefabrication`. Une catégorie vide, elle, se supprime et peut être déclarée à nouveau.

## naturedetravail

Gère le **référentiel des natures de travail** : les métiers exercés dans l'atelier, soudage, tournage, fraisage, dessin. Jusqu'ici, chaque poste portait sa nature en texte libre, et une faute de frappe (« Soudage » d'un côté, « Soudure » de l'autre) suffisait à couper en deux les rapports d'un même métier. L'entreprise déclare désormais ses natures une fois, et aucune n'est créée d'office.

Une `NatureDeTravail` porte un **identifiant** et un **libellé**. Le libellé est unique à la casse, aux accents et aux espaces près : sa **clé** (minuscules, sans accents, espaces réduits) est ce qui distingue deux natures, et « Soudage » et « soudâge » ne peuvent pas coexister. La liste se lit dans l'ordre de cette clé, donc par ordre alphabétique sans égard aux accents, et dit pour chaque nature si elle sert déjà.

Le gestionnaire peut **renommer** une nature, y compris pour n'en corriger que la casse ou les accents. Ceux qui se servent d'une nature n'en retiendront que l'**identifiant** : un renommage ne modifiera que la ligne de la nature, jamais l'historique, et le nouveau libellé s'affichera partout, rapports passés compris. Une nature **ne se supprime pas** tant qu'un poste la porte, et **plus jamais** dès qu'un pointage l'a recopiée : le journal n'en garde que l'identifiant. La liste dit, pour chacune, si elle sert.

## parametrage

Gère les **réglages que l'entreprise fixe elle-même**, sans développeur : un seul jeu pour toute l'entreprise, que le gestionnaire modifie et que tout utilisateur lit. GLM est une trame, et une donnée qui varie d'un client à l'autre ne s'écrit pas en constante.

Le premier réglage est la **durée max d'une activité** : le temps au bout duquel une activité que rien n'a terminée se termine automatiquement (voir « La fin automatique à l'échéance » dans `atelier`). Elle vaut **treize heures** tant que l'entreprise ne l'a pas fixée, et reste comprise entre une heure et vingt-quatre heures. Un réglage jamais fixé n'est pas recopié en base : la valeur par défaut n'existe qu'à un endroit, le domaine. `atelier` et le référentiel du pupitre ne la connaissent pas : ils la reçoivent par un port du noyau partagé, que `parametrage` implémente.

Le second est le **logo de l'entreprise**, qui s'affiche en en-tête de la supervision, du pupitre et des PDF, ajusté à la case de chaque en-tête sans être déformé. C'est une image PNG ou JPEG qui tient dans 256 x 256 pixels, dans les proportions de son choix (256 x 100 pour un logo en longueur), de 50 Ko au plus, sans recadrage ni conversion par GLM. Le refus dit ce qui ne va pas, avec la valeur reçue, pour que le gestionnaire corrige son fichier. Sa **version** est l'empreinte de son contenu : elle entre dans l'adresse de l'image, que le navigateur garde en cache tant que le logo ne change pas.

Le `Parametrage` n'a pas d'identifiant : chaque schéma d'entreprise porte une seule ligne, créée avec lui. Un réglage n'est donc jamais créé ni supprimé, seulement modifié.

## atelier

Gère l'exécution en atelier de ce que `elementdefabrication` a déclaré. Le gestionnaire y met un élément en atelier, les opérateurs y pointent leurs activités, le gestionnaire clôture et régularise la fin des activités oubliées.

Le contexte porte le `SuiviDAtelier` d'un élément engagé et son journal d'activité. Les temps se lisent dans
ses activités interprétées, avec leurs bornes et leur échéance.

**Le journal d'événements est la source de vérité.** L'agrégat se reconstruit par le repli du journal, trié par date de survenue ; les projections d'activités sont réconciliées à chaque écriture pour les lectures. C'est l'insertion rétroactive qui l'impose — un temps juste exige qu'un pointage hors ligne compte à l'heure où il a eu lieu, pas à l'heure où il arrive, et un modèle à compteurs ne sait pas revenir en arrière. Chaque événement porte donc un `Horodatage` bitemporel : sa date de survenue, métier, et sa date d'enregistrement, technique.

**Une régularisation est un acte du gestionnaire, conservé sur le fait, pas un écart de dates ni une identité d'auteur.** Chaque événement du journal d'un élément porte son origine : `POINTAGE` pour tout ce qui passe par la route des pointages, quels que soient le rôle de celui qui pointe et l'heure de geste fournie, `REGULARISATION` pour une régularisation, même saisie à l'heure du fait. `estUneRegularisation()` lit cette origine. L'écart entre les deux dates ne suffit pas : un pupitre resté hors ligne rejoue ses pointages après coup sans que le gestionnaire soit intervenu. Cet écart reste la lecture d'une saisie différée. Le booléen jumeau `estSaisiParUnTiers` a été retiré avec le passage à l'identifiant : l'`Auteur` vient du jeton et l'opérateur du référentiel, et rien ne relie encore les deux — le comparer n'aurait plus produit qu'une réponse toujours vraie. Il reviendra avec le lot « utilisateur connecté ».

### La pause et le travail non facturable

La pause est un geste du pupitre : il envoie une fin par activité actionnable, puis une nouvelle ouverture à
la reprise, en travail ou en non conformité. Le serveur reçoit les faits d'activité correspondants.

**GLM n'est pas un concept du modèle.** C'est le nom que le client de référence donne à son travail non facturable,
sur un projet interne par exemple, qu'il veut déclarer manuellement (« De toute façon il y aura ce bouton GLM »).
Ce travail sera déclaré par le superviseur sous la forme d'un OF de type Perso. La création et ce sous-type feront
l'objet d'un chantier séparé ; la supervision actuelle ne crée aucune activité sans élément.

### Le temps des activités

Atelier ne rend aucune durée. Il tient les activités que le journal interprète, avec leur début, leur fin réelle ou leur
échéance, et laisse les lecteurs les compter : la synthèse des heures additionne le temps opérationnel d'un opérateur
sur une semaine, le coût de revient valorise le temps passé sur un élément. Une activité se termine à sa fin réelle — une
`FIN` pointée, une régularisation ou la clôture —, sinon automatiquement à son échéance, avec une anomalie (voir
ci-dessous). Une activité en cours ne compte rien.

La pause de midi scinde le travail par le journal de l'élément : une fin à midi, un début à la reprise. Un travail jamais
arrêté ne court pas pour autant jusqu'au lendemain : il se termine à son échéance. À son retour, l'opérateur qui pointe un
`DEBUT` après l'échéance ouvre une nouvelle activité, sans prolonger l'ancienne ; avant l'échéance, ce `DEBUT` est ignoré
et l'activité continue. Une fin oubliée se rattrape par une régularisation du gestionnaire, qui remplace la fin
automatique.

### La fin automatique à l'échéance

Une activité encore en cours ne compte rien. Oubliée, elle ne court pas pour autant indéfiniment : **son échéance est son début plus la durée max d'une activité** (`Echeance`), en heures écoulées, jamais en heures d'horloge murale — le passage à l'heure d'été ou d'hiver ne l'allonge ni ne la raccourcit. Elle est atteinte dès que l'instant est supérieur ou égal à ce terme. Une activité que rien n'a terminée avant son échéance est **terminée automatiquement** à cet instant, et porte une **anomalie** : c'est la `FIN_AUTOMATIQUE` (`finAutomatique`), la seule anomalie que le gestionnaire traite. Le coût de revient dit `AnomalieDuPointage` ce qui rend un pointage suspect : `FIN_AUTOMATIQUE` en est la seule valeur, et c'est la même, sans type partagé. Avec les treize heures par défaut, un travail à 8 h sans aucune fin, lu à 20 h 59, est en cours ; lu à 21 h, ou le lendemain, il est terminé à 21 h ; la durée fixée à 8 h, il est terminé à 16 h. La durée est le réglage de l'entreprise (voir `parametrage`) : `atelier` la reçoit par un port du noyau partagé (`MaximumActivityDurations`), que lit aussi le pupitre, et **la copie sur le pointage qui ouvre l'activité**. Une activité garde donc la durée lue à la réception de son début : le gestionnaire qui la change ne modifie que les activités dont le début est reçu ensuite, sans rétroactivité. Limite connue : c'est la réception qui compte, pas l'heure du geste ; un début pointé hors ligne avant le changement et reçu après prend la nouvelle durée, et le gestionnaire rattrape l'écart par la régularisation. La copie est nécessaire parce que les projections d'activité sont recalculées à chaque geste du suivi : une durée lue en direct à ce moment-là s'appliquerait à une activité déjà ouverte.

**Rien n'est écrit.** Ni événement de clôture automatique ni traitement planifié : l'interprétation du journal donne des activités qui ne dépendent que des faits, avec leur fin réelle quand un geste ou la clôture les a terminées. Seule leur lecture, à un **instant d'évaluation** explicite, décide si une activité sans fin réelle est encore en cours ou déjà terminée automatiquement. Cet instant vient de l'horloge du service applicatif. La première lecture après une indisponibilité retrouve donc la même borne, sans rattrapage.

**La même échéance gouverne la réception**, sur les seules heures métier :

- une fin pointée **avant l'échéance** termine l'activité à son heure, même reçue après elle : une fin pointée à 17 h et reçue le lendemain donne 9 h, et retire l'anomalie ;
- une fin pointée **à l'échéance ou après** est ignorée (`APRES_ECHEANCE`) : l'activité garde sa borne de 21 h et son anomalie, et l'audit garde la trace du pointage. Un geste pile à l'échéance n'emporte donc plus sur la fin automatique ;
- un `DEBUT` ou une `NON_CONFORMITE` pointé à l'échéance ou après est accepté : l'activité échue compte comme terminée et la nouvelle s'ouvre à son heure. Travail à 8 h, non conformité à 23 h : rien n'est compté entre 21 h et 23 h ;
- une clôture postérieure à l'échéance ne prolonge rien ;
- **seul le gestionnaire** établit une fin réelle au-delà de l'échéance, par la régularisation : la fin automatique est une borne par défaut, pas un plafond.

Chaque ouverture crée une activité distincte, avec sa propre échéance : travail à 8 h, puis fin et non conformité à 12 h, donnent 4 h de travail terminées à 12 h et une non conformité en cours jusqu'à 1 h le lendemain.

### La règle de réception

Un atelier pointe hors ligne, depuis plusieurs pupitres : un double appui, deux pupitres sur le même opérateur ou un pointage hors ligne arrivé en retard peuvent contredire le journal. **Le serveur décide à l'arrivée du pointage**, premier arrivé premier servi, au lieu de garder les deux faits et de demander au gestionnaire de trancher. Un pointage incohérent n'entre pas au journal : il part dans une table d'audit, sans écran. La réponse, 409 `pointage-ignore`, ne s'affiche pas à l'opérateur ; le pupitre retire l'effet local du pointage et se recale sur le référentiel. Un refus aurait fait perdre un geste réel sans que personne puisse l'expliquer, et une interprétation silencieuse aurait chiffré un temps que personne n'a validé.

Le pupitre ne pointe que trois choses : `DEBUT`, `NON_CONFORMITE` et `FIN`. Un pointage ne désigne aucune activité ; la **clé** est l'opérateur, l'élément et le poste, et une clé a au plus une activité en cours. Une `FIN` ferme l'activité en cours de sa clé. Après les contrôles habituels (opérateur, poste, habilitation, élément clôturé), le serveur juge dans cet ordre :

1. **`ANTERIEUR`** : l'heure du geste précède celle du dernier pointage accepté de la clé, régularisations comprises (une heure égale passe) ; ou c'est une `FIN` qui n'est pas postérieure au début de l'activité qu'elle fermerait, car **aucune activité n'a une durée nulle** ;
2. **l'échéance**, jugée sur l'heure du geste ;
3. **le tableau** :

| État de la clé                                              | `DEBUT`                  | `NON_CONFORMITE`         | `FIN`                                                                                                      |
| ----------------------------------------------------------- | ------------------------ | ------------------------ | ---------------------------------------------------------------------------------------------------------- |
| Rien en cours (jamais ouverte, terminée, clôturée ou échue) | accepté                  | accepté                  | ignoré : `APRES_ECHEANCE` si la dernière activité de la clé est échue et sans fin, sinon `AUCUNE_ACTIVITE` |
| Activité en cours (travail ou NC)                           | ignoré : `DEJA_EN_COURS` | ignoré : `DEJA_EN_COURS` | accepté                                                                                                    |

Les quatre raisons d'audit sont `DEJA_EN_COURS` (un double appui), `AUCUNE_ACTIVITE` (un deuxième arrêt),
`APRES_ECHEANCE` (un arrêt à l'échéance ou après, la durée max d'activité après le début) et `ANTERIEUR` (un pointage hors ligne arrivé après un
pointage plus récent). Travail à 7 h et arrêt à 12 h, puis un passage en non conformité pointé à 10 h qui arrive après :
il est ignoré, et le travail reste compté de 7 h à 12 h.

Une fin survenue avant la clôture de l'élément, mais reçue après elle, est enregistrée à son heure : la clôture ne prime
pas sur un geste qui l'a précédée. Survenue après, elle est ignorée, la clôture ayant déjà arrêté l'activité encore en cours (`AUCUNE_ACTIVITE`, ou
`APRES_ECHEANCE` si l'activité était déjà échue). Un `DEBUT` ou une `NON_CONFORMITE` sur un élément clôturé reste refusé (409 `suivi-d-atelier-cloture`) : c'est
le seul refus que l'opérateur voit.

Le journal se relit par heure du geste, la fin avant l'ouverture à heure égale, puis par identifiant. Grâce à
`ANTERIEUR`, c'est équivalent à l'ordre d'arrivée pour les pointages du pupitre, et une régularisation arrivée tard
tombe à sa place.

### L'activité, un opérateur sur un poste de travail

`CleDActivite` est le couple (`OperateurId`, `Optional<PosteDeTravailId>`). Le poste de travail est ce que l'opérateur engage en pointant : une machine chez le client de référence, un établi, un four, une salle ailleurs.

- Il est **facultatif** : une entreprise sans parc machine laisse l'`Optional` vide et retrouve une activité unique par opérateur.
- C'est **lui**, et non la nature de l'opération, qui identifie l'activité. L'érosionniste qui met deux pièces du même élément sur deux machines mène ainsi deux activités indépendantes — cas que le client détaille explicitement, et qu'une clé fondée sur la fonction refuserait.

La `NatureDOperation` (fraisage, tournage, érosion, dessin) est **recopiée du poste** au moment de la saisie, par le port `PostesConnus` : un poste requalifié plus tard ne réécrit pas l'histoire de l'atelier. Le journal n'en retient que l'**identifiant** dans le référentiel des natures, comme il ne retient que celui de l'opérateur et du poste, et en relit le libellé courant : renommer une nature ne touche que sa ligne, et un ordre pointé avant et après le renommage ne compte qu'une nature. Comme `operateur_id` et `poste_id`, `nature_id` ne porte aucune clé étrangère ; c'est `naturedetravail` qui refuse de supprimer une nature pointée. Elle vient du poste et non de la personne, parce que c'est le poste qui dit quel métier s'y exerce — un opérateur polyvalent déclenche un pointage par poste. Elle ne porte **aucun invariant** : elle n'est qu'un axe d'agrégation pour la synthèse par catégorie de travail, et elle reste facultative comme le poste.

### Le référentiel, atteint par identifiant

Le journal ne retient que des **identifiants** — `OperateurId`, `PosteDeTravailId` —, jamais un libellé, à la seule exception de la nature. C'est ce qui permet à toute agrégation à venir de compter une personne pour une personne, là où deux orthographes d'un texte libre en auraient compté deux.

Rien d'autre n'en est copié, à la différence du nom de l'élément figé à l'engagement, et la raison est symétrique : un élément renommé ne doit pas réécrire son histoire, alors qu'une fiche d'opérateur corrigée doit s'afficher corrigée sur toutes les feuilles de temps, y compris les anciennes. Les libellés sont donc **relus à chaque lecture** par `OperateursConnus` et `PostesConnus`, dont l'accès par ensemble résout un journal entier en une requête. `AnnuaireDAtelier` porte ce résultat le temps d'une lecture.

La contrepartie de ce choix : **ni un opérateur ni un poste ayant servi à pointer ne se supprime**. La règle vit dans les deux référentiels, derrière un port qui lit le journal d'atelier.

**L'habilitation est la seule règle dure du contexte.** Un pointage sur un poste où l'opérateur n'est pas déclaré est refusé (409), par `Habilitations`. La règle ne joue que lorsqu'un poste est fourni : une entreprise sans parc machine n'a aucune habilitation à déclarer et retrouve son comportement nominal. Elle joue en revanche sur les **deux** écritures du journal — pointage et régularisation —, sans quoi le back-office contournerait ce que le pupitre applique.

### Non conformité

Une pièce ratée se refait, sur le même élément et au même tarif, mais comptée à part. Un pointage est un `DEBUT` (une activité de travail), une `NON_CONFORMITE` (une activité de non conformité) ou une `FIN` ; il ne dit ni intention ni cible. Passer en non conformité, puis reprendre du bon travail, se pointe donc par des paires de gestes à la même heure, la `FIN` d'abord : `FIN` puis `NON_CONFORMITE`, puis `FIN` puis `DEBUT`. Une activité s'identifie par son pointage ouvrant (`ActiviteId`). Un `DEBUT` pendant une activité en cours ne la relance pas : il est ignoré (`DEJA_EN_COURS`). À la clôture, on sait « combien de temps on a passé à faire du bon travail et combien à refaire ».

### La régularisation de la fin

Le gestionnaire ne réécrit pas le journal : il n'annule ni ne corrige un pointage. Il fait une seule chose, **régulariser la fin d'une activité échue**, celle qu'aucune fin n'a terminée avant son échéance. Il donne l'heure à laquelle la fin a réellement eu lieu ; elle peut dépasser l'échéance. L'opérateur, le poste et le type se déduisent de l'activité. La régularisation ne passe pas par la règle de réception.

Elle est refusée si l'activité n'est pas une fin automatique (`activite-non-echue`), si une régularisation la vise déjà (`activite-deja-regularisee`), si l'heure est dans le futur, ne suit pas le début (`fin-avant-debut`) ou dépasse le plus tôt du début suivant sur la clé et de la clôture (`fin-apres-borne`, `borneDeFin` du dossier). Le client fournit l'identifiant de la saisie ; un renvoi répond comme un succès et n'écrit rien de plus.

Le dossier d'une fin automatique ne contient que l'élément, l'activité échue, les pointages de sa clé et `borneDeFin`.

**La clôture ne fige rien pour le gestionnaire.** Elle ferme le pointage aux opérateurs ; la régularisation reste admise, et la clôture elle-même se déplace ou se rouvre. Le seul invariant qui subsiste est de cohérence, pas de permission : aucun événement daté après la clôture.

### Le temps réparti

Deux mesures se distinguent par leur cumul :

- **temps effectif** — la durée réelle passée sur un élément. Deux postes pendant 1 h font 2 h effectives. Atelier ne la calcule plus (`TempsDAtelierService` et `SuiviDAtelier.intervalles` n'existent plus) : elle se lit dans le temps passé du coût de revient et dans la durée par élément de la synthèse des heures.
- **temps réparti** — la même heure d'opérateur divisée par le **nombre de postes de travail** occupés simultanément par ses activités terminées, tous éléments confondus. Il sert au coût de revient.

Le **temps opérationnel** de `syntheseheures` cumule les portions d'activités terminées, réelles ou automatiques,
coupées aux minuits locaux. Les activités en cours restent visibles sans durée comptabilisée. Voir la section
`syntheseheures`.

Le diviseur est bien le nombre de postes, et non le nombre d'activités ou d'éléments : le client énonce la règle deux fois de suite — coût horaire de chaque machine active non divisé, taux horaire de l'opérateur divisé par le nombre de machines qu'il utilise. Un opérateur sur trois éléments avec une seule machine n'est donc pas divisé.

**Le journal enregistre les faits d'activité.** Le réparti traverse les agrégats — la fin d'une activité sur un
second élément peut changer la part déjà attribuée au premier —, il est donc calculé à la lecture par le coût.
Atelier projette l'interprétation des faits ; les lecteurs y accèdent par leurs ports et entités propres et
ajoutent l'évaluation temporelle, le calendrier ou la valorisation de leur contexte.

### Frontière avec elementdefabrication

`elementdefabrication` étant annoté `@BusinessContext`, l'atelier ne l'importe jamais. Il déclare sa propre identité, `ElementEngage`, dont le nom et la catégorie sont **copiés à l'engagement** : c'est ce qu'affiche l'écran d'atelier, et un élément renommé plus tard ne doit pas réécrire l'histoire de l'atelier. Le port `ElementsEngageables` porte cette frontière. La catégorie copiée est une valeur libre, `CategorieDElement`, et non une liste fermée : chaque entreprise déclare les siennes dans `categoriedeproduit`, et l'atelier — comme le pupitre, la synthèse et le coût de revient — la restitue sans la connaître.

**Mettre un élément en atelier est un geste métier explicite du back-office**, distinct de sa création : tout ce qui est créé n'est pas forcément à faire, et c'est cet acte qui fait apparaître l'élément sur l'écran des opérateurs. C'est aussi ce qui donne un sens à l'invariant « aucun événement antérieur à l'engagement ».

L'écran des opérateurs veut tous les éléments actifs d'un coup, sans rien qui défile et sans notion de date : la période de `SuiviDAtelierCriteria` est donc **facultative**, et ne sert qu'aux écrans de back-office.

Les quatre couches existent ; `suivi_d_atelier` et `evenement_d_atelier` persistent le suivi et ses faits dans le
schéma de l'entreprise courante. Le domaine rapproche les projections `activite_d_atelier` à chaque écriture.
Elles portent les bornes indépendantes de la lecture et servent les filtres sans reconstruire toute l'entreprise.
Le suivi se reconstruit depuis ses faits, et non depuis les projections.

L'atelier écrit aussi l'audit des pointages ignorés, `pointage_ignore_d_atelier` : une ligne par pointage que la règle de
réception a écarté. La table n'a **ni clé ni contrainte**, seulement un index sur l'identifiant : deux lignes pour un même
renvoi sont acceptées, et rien ne doit empêcher un pointage ignoré de s'écrire. Elle ne sert qu'à retrouver un renvoi
et à relire en base ce que la règle a écarté ; aucun contexte voisin ne la lit.

Le modèle relationnel permet aux lecteurs de sélectionner les activités par **recouvrement**, et de lire
séparément le journal brut pour le relevé. Chaque lecteur possède ses entités JPA en lecture seule sur les
tables du propriétaire, comme `atelier` le fait sur `element_de_fabrication`. Les bornes projetées servent
les rapports sans rejouer les gestes ; les faits du journal restent consultables avec leur identité.

`ElementsEngageables` lit la table `element_de_fabrication` par une entité en lecture seule propre à l'atelier, sans jamais importer le contexte voisin. `OperateursConnus`, `PostesConnus` et `Habilitations` font de même sur `operateur`, `poste_de_travail` et `operateur_poste`.

L'API est décrite par OpenAPI (`/swagger-ui.html`) et par [atelier-api.md](atelier-api.md), qui porte ce que la spec ne peut pas dire.

### La supervision de l'atelier

`GET /api/atelier/supervision` rend une lecture complète de l'entreprise : tous les opérateurs et leurs métiers
courants, et les activités sans fin réelle, en cours ou terminées automatiquement. Une fin automatique reste à traiter
après une reprise ou la clôture ; elle disparaît quand une fin réelle, pointée avant l'échéance ou régularisée, retire
son anomalie. Les activités terminées réellement ne sont plus supervisées.

La route lit les projections d'atelier, sans repli des journaux, à un seul instant d'évaluation fourni par l'horloge
applicative. Le classement visuel et les compteurs appartiennent au consommateur. Le
[guide d'intégration](atelier-api.md#la-supervision-de-latelier-en-une-lecture-complète-rôles-user-et-gestionnaire)
porte le contrat détaillé et les limites de cohérence face aux écritures concurrentes.

### Points ouverts

1. **Régulariser après une dé-habilitation est refusé.** L'habilitation étant vérifiée sur les deux écritures du journal, un gestionnaire ne peut plus régulariser la fin d'une activité sur un poste dont l'opérateur a été retiré depuis. Le cas est assumé — il ferme la porte au contournement —, mais il empêcherait le rattrapage des activités concernées : à rouvrir si le client le rencontre.
2. **Le coût de revient monétaire est sorti du contexte** : `coutderevient` lit les activités projetées par
   atelier et les tarifs du fait ouvrant, par ses propres ports en lecture seule. `atelier` capture ces
   tarifs ; le coût les combine. Reste ouvert le **coût par période** (par opérateur, par poste, par mois), qui
   n'a pas de demande client formulée. Le partage suit le verbatim client : taux humain divisé par postes
   distincts occupés par les activités terminées, coût machine entier.
3. **La pause, la reprise et l'arrêt global appartiennent au pupitre.** La pause termine les activités
   actionnables et mémorise celles à reprendre ; la reprise ouvre de nouvelles activités. L'arrêt global termine
   les activités encore actionnables et efface durablement la mémoire de reprise. Le serveur reçoit les gestes
   d'activité correspondants ; les activités expirées sont exclues de ces commandes. Voir
   l'[ADR 0002](adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md) pour la frontière serveur/pupitre.
4. **La déclaration du travail non facturable.** Le client veut son bouton GLM, placé en bas de l'écran, pour déclarer à la main le travail qu'il ne facture pas. Il sera déclaré par le superviseur comme un OF de type Perso. Sa création, le sous-type et le rattachement éventuel à un projet interne feront l'objet d'un chantier distinct.
5. **Le cycle de vie de l'élément lui-même.** La clôture existe côté atelier, sur le suivi. Reste à trancher si l'élément de fabrication porte en propre un statut, ou si son activité se lit entièrement par la présence ou l'absence d'un suivi non clôturé.
6. **Aucune garde d'unicité en base** sur « un seul suivi non clôturé par élément », contrairement à ce que `elementdefabrication` fait pour la `Reference`. La règle vit dans le service, mais une contrainte partielle transformerait en 500 un état que le domaine admet aujourd'hui : rouvrir la clôture d'un suivi dont l'élément a été réengagé depuis. À trancher côté domaine avant de poser la contrainte.
7. **L'écriture du journal rapproche par identifiant**, ce qui coûte une lecture indexée de la collection à chaque pointage. Si un journal devenait assez long pour que cette lecture pèse, la sortie est un upsert natif gardé (`on conflict (id) do update ... where ... is distinct from ...`), qui épargne à PostgreSQL toute version de tuple sur les lignes inchangées — au prix d'une scission permanente entre lecture JPA et écriture JDBC.
8. **L'audit reste à faire.** Les pointages ignorés se lisent en base, sans endpoint ni écran ; les actes du gestionnaire ne sont pas audités, et ceux qui seraient incohérents ne sont pas refusés au-delà de la borne de fin.

## postedetravail

Gère le **référentiel de ce sur quoi les opérateurs pointent**. Un `PosteDeTravail` porte un `Libelle` et une `NatureDeTravail` : « Tour 1 » sert à tourner, « Poste de soudure » à souder. Il porte aussi, facultativement, un `CoutHoraire`, nul pour un poste qui ne demande que de la main d'œuvre : le contexte se contente de le stocker et de le restituer, le calcul du coût de revient restant un lot à part. `atelier` le lit désormais, mais seulement pour le copier sur chaque événement du journal au moment de la saisie — sans jamais le recalculer ni le combiner. C'est la capture, pas la valorisation.

Le terme reste volontairement générique, comme dans l'atelier : une machine chez le client de référence, un établi, un four, une salle ailleurs.

**Le libellé est unique par entreprise.** C'est ce qui fait du référentiel un référentiel : sans lui, rien ne relierait le « Tour 1 » saisi par Dupont au « Tour 1 » saisi par Martin. La garde vit dans `PostesDeTravailService`, sur le patron de `ElementsDeFabricationService.verifierReferenceLibre` ; la contrainte du schéma est le filet de dernier recours.

**La nature est obligatoire ici**, alors qu'elle reste facultative dans l'atelier. Ce n'est pas une contradiction : l'atelier doit fonctionner pour une entreprise sans parc machine ni métiers distincts, qui n'ouvrira simplement pas cet écran. Mais un poste qui serait déclaré sans dire quel travail s'y fait ne servirait à rien — c'est précisément ce que ce contexte apporte.

**Le poste ne porte que l'identifiant de sa nature**, choisie dans le référentiel de `naturedetravail` : la `NatureDuPoste` réunit cet identifiant et le libellé courant, relu par jointure à chaque lecture. Renommer une nature renomme donc celle de tous ses postes sans toucher leur ligne. Le port `NaturesDeclarees` lit le référentiel par la donnée. Pendant la transition du front, le libellé saisi en texte reste accepté : il désigne la nature de même clé (casse, accents et espaces ignorés), déclarée à la volée si elle manque — chemin déprécié, retiré par glm-back#130.

**Un poste encore habilité ne se supprime pas** : cela laisserait des opérateurs pointer sur du vide. La règle vit dans le domaine, derrière le port `PostesEnUsage`, dont l'adapter lit la table `operateur_poste` par une entité en lecture seule — sans jamais importer `operateur`, annoté `@BusinessContext`.

**Un poste sur lequel du temps a été pointé ne se supprime plus du tout**, et ce second refus est définitif là où le premier se lève en retirant l'habilitation : le journal d'atelier ne retenant que l'identifiant du poste, sa disparition laisserait des heures de travail sans machine. Le port `PostesPointes` lit `evenement_d_atelier` de la même façon.

## operateur

Gère le **référentiel des personnes qui pointent**. Un `Operateur` porte son nom, son prénom, un identifiant facultatif, un taux horaire facultatif, et l'ensemble des postes sur lesquels il est habilité.

### Le métier vient du poste, jamais de la personne

C'est le point décisif de ces deux contextes, et il est fondé sur ce que le client décrit.

Un opérateur polyvalent — soudeur **et** tourneur — déclenche **deux démarrages** sur le pupitre : un sur le poste de soudure, l'autre sur le tour. Deux temps courent alors en parallèle, et chacun sait de quel métier il relève parce que le poste le dit. C'est exactement ce que l'atelier modélise déjà : sa `CleDActivite` est le couple (opérateur, poste), et son javadoc énonce que « c'est le poste, et non la nature de l'opération, qui distingue deux activités menées de front ». Le verbatim client le confirme sur un cas voisin : deux pièces du même OF sur deux machines différentes.

Il s'ensuit que **la nature appartient au poste**. Déclarer un métier sur la personne stockerait la même information deux fois, avec la possibilité qu'elles se contredisent — un tourneur habilité sur une fraiseuse. Les métiers d'un opérateur se **déduisent** donc de ses postes : `ProfilDOperateur.natures()` rend les natures de ses habilitations, triées. Personne ne les saisit.

La phrase du client « la machine est liée à l'opérateur, et l'opérateur a la fonction » dit **où se saisit** le paramétrage — sur la ligne de l'opérateur, on liste ses postes —, pas d'où la nature se déduit au moment du pointage.

**Un opérateur dont un fait historique d'activité existe ne se supprime pas.** Le journal conserve son identifiant pour lire l'histoire ; `OperateursQuiOntPointe` lit les faits par une entité propre en lecture seule.

### Identité et identifiant

L'identité (nom, prénom) est **unique par entreprise**. L'**identifiant** est le code que l'entreprise donne elle-même à ses collaborateurs : **facultatif**, car toutes n'en attribuent pas, et **unique dès qu'il est renseigné** — patron exact de `elementdefabrication.Reference`, `NULL` distincts compris, donc autant d'opérateurs sans identifiant que nécessaire.

### Frontière avec postedetravail

`postedetravail` étant annoté `@BusinessContext`, ce contexte ne l'importe jamais : il déclare ses propres `PosteHabilitableId`, `LibelleDePoste` et `NatureDeTravail`, et lit la table voisine par une entité en lecture seule, sur le patron d'`ElementEngageableEntity`.

**Rien n'est copié**, à la différence de l'atelier qui copie nom et catégorie à l'engagement. La raison est symétrique : l'atelier copie parce qu'un élément renommé ne doit pas réécrire son histoire, alors qu'ici aucun historique ne pend à un poste — un poste renommé doit s'afficher renommé partout. L'opérateur ne stocke donc que l'identifiant, et le port `PostesHabilitables` n'expose que `parIds`, pour qu'une page entière se résolve en une requête.

### Points ouverts

1. **Les gestionnaires ne sont pas déclarés.** Leur fiche n'aurait aucun usage tant que l'authentification n'est pas tranchée : l'`Auteur` d'une saisie vient du jeton, pas d'un référentiel. À rouvrir avec ce sujet.
2. **Aucun plafond sur le nombre de postes par personne**, alors que le client énonce « maximum 4 machines par personne ». Une donnée de paramétrage ne s'écrit pas en constante du domaine, et GLM est une trame : une autre entreprise en habilitera six. Si le plafond doit être tenu, il viendra d'un port.
3. **Montants.** Coût horaire du poste et taux horaire de l'opérateur existent sur les deux agrégats (facultatifs ; le taux strictement positif, le coût positif ou nul pour un poste qui ne demande que de la main d'œuvre), `atelier` les copie sur chaque événement du journal au moment de la saisie, et `coutderevient` les valorise. La boucle est fermée : un opérateur sans taux horaire ne coûte rien en main d'œuvre, et le rapport ne l'invente pas. Un poste à 0 € de l'heure coûte 0 € de machine : c'est un tarif connu, pas un tarif absent.
4. **Utilisateur connecté.** La réflexion de principe dans [strategie/authentification-pointage.md](strategie/authentification-pointage.md) distingue l’**identité d’appareil** du pupitre et l’**identification** de l’opérateur au geste — par un code, qui désigne sans prouver, ou par une signature, qui prouve. L'`Auteur` du jeton cessant dès lors de désigner une personne, c'est la **qualité de l'identification** portée par l'événement qui permettra l’audit, et c’est elle qui rouvrira `estSaisiParUnTiers`. Restent ouverts le régime du code — ouvert à tous ou réservé à l'exception —, le matériel des pupitres, et la validation juridique de l'empreinte.

## feuilledetemps

Première **projection transverse** du projet : un contexte purement lecteur, qui ne possède aucune table et
recalcule tout à chaque appel. Il répond à une seule question — _qu'a fait cette personne cette semaine, jour par
jour_ — à partir des activités interprétées par `atelier`.

### Pourquoi il n'est pas dans atelier

`atelier` manipule des instants et s'interdit le calendrier : aucun `ZoneId` ni `LocalDate` n'entre dans ce
contexte. La feuille ramène les activités aux jours de l'entreprise et aux semaines ISO. Une équipe de nuit
compte sur deux jours, et une activité du dimanche peut recouvrir le lundi de la semaine suivante. Minuit
répartit au calendrier sans créer de geste ni de fin métier : une activité terminée de 20 h à 8 h donne
4 h puis 8 h. Les indications en cours gardent leurs règles ci-dessous.

### La lecture passe par la base, jamais par un import

`atelier`, `operateur` et `postedetravail` étant annotés `@BusinessContext`, ce contexte déclare ses propres entités
JPA `@Immutable`. Il lit la projection `activite_d_atelier`, avec l'élément porté par le suivi, et l'identité de
l'opérateur dans le référentiel. Il ne rejoue aucun journal et ne propose aucune écriture.

Le filet est le scénario Cucumber : il écrit par l'API d'atelier puis lit la feuille. Fins pointées, fins
automatiques, régularisations et clôtures restituent l'interprétation du propriétaire. Le lecteur compare l'échéance projetée à son instant de lecture puis découpe
les activités au calendrier de l'entreprise.

### Ce que la feuille montre

Sept jours toujours, du lundi au dimanche de la semaine ISO demandée, vides compris. L'année est celle des
semaines ISO, qui diffère de l'année civile à ses bornes : la semaine 1 de 2026 commence le 29 décembre 2025.
La semaine est toujours explicite, jamais « la semaine courante ».

Chaque jour rend les portions d'activité : élément, poste et nature facultatifs, catégorie travail ou
non-conformité, début et fin éventuelle. La sélection porte sur les activités qui **recouvrent** la semaine,
même commencées avant elle et sans pointage de la semaine. Une régularisation peut établir une fin bien au-delà
de la durée max d'une activité, voire de la semaine : aucune borne basse fixe sur le début ne permet de les retrouver toutes.

La feuille accepte un instant `evaluation` facultatif et rend celui effectivement utilisé. Sans paramètre,
l'heure du serveur est relevée une seule fois. Cet instant gouverne l'expiration et les jours atteints par les
activités en cours. Transmettre le même instant à la synthèse assure la même décision d'expiration dans le relevé. Les faits connus restent interprétés même postérieurs à cet instant ;
une écriture entre les appels peut les modifier, donc l'instant commun ne garantit pas un instantané commun.
Un instant passé est accepté. La borne future est l'heure du serveur plus deux minutes, incluse ; elle est vérifiée
avec un seul relevé d'horloge. Un dépassement ou un instant fourni vide ou mal formé répond 400, sans rapport.

Une activité avec fin réelle est `TERMINEE`, même si cette fin dépasse
l'échéance. À défaut, elle est `EN_COURS` avant l'échéance et `TERMINEE_AUTOMATIQUEMENT` dès celle-ci, à cette borne,
avec son anomalie visible par l'état.
La feuille ne calcule aucune durée. Chaque portion garde l'identité stable, l'état et les bornes de l'activité entière,
avec une fin seulement pour les deux états terminés. Les portions terminées sont coupées aux minuits locaux et aux
limites de la semaine ; les bornes de l'activité restent intactes.

Une activité en cours rend une indication sans fin sur chacun des jours atteints à l'instant de lecture, dans la
semaine : commencée dimanche à 22 h et lue lundi à 1 h, elle apparaît lundi avec son début entier, sans fin à minuit.
Une fin lundi à 3 h remplace ensuite cette indication par les portions terminées, 2 h dimanche et 3 h lundi.
La feuille nomme l'élément, jamais le suivi : un élément réengagé après clôture reste le même élément. Ni libellé
de poste ni fiche d'élément ici — la synthèse des heures les porte. Les activités sont triées par début de portion,
élément puis identité stable.

### Points ouverts

1. **Le fuseau horaire est fixé à `Europe/Paris`** par un adapter, sur le patron des préfixes d'éléments de
   fabrication. Il passe déjà par un port : le jour où une entreprise cliente vit ailleurs, seul l'adapter change.
2. **Aucune restriction sur qui lit la feuille de qui.** `USER` et `GESTIONNAIRE` lisent l'historique de n'importe
   quel opérateur, faute de lien entre un utilisateur authentifié et une fiche du référentiel. À rouvrir avec le lot
   « utilisateur connecté », qui ramènera aussi `estSaisiParUnTiers` côté atelier.

## coutderevient

Seconde **projection transverse** du projet, après `feuilledetemps` : un contexte purement lecteur, qui ne possède
aucune table et recalcule tout à chaque appel. Il répond à une seule question — _combien cet élément a-t-il coûté,
et en quoi_ — et c'est ce qui le sépare d'`atelier`.

### Pourquoi il n'est pas dans atelier

`atelier` s'interdit explicitement le calcul du coût de revient. Il **capture** : il copie sur chaque événement du
journal la nature de l'opération, le coût horaire du poste et le taux horaire de l'opérateur, exactement pour que
ces valeurs soient figées au moment de la saisie — mais aucune arithmétique ne les combine chez lui. Le découpage
était annoncé dès le lot des montants ; c'est ici qu'il se réalise.

La séparation n'est pas cosmétique. Le coût **traverse les agrégats** : un nouveau pointage sur un second élément
change la part de main d'œuvre déjà attribuée au premier. Un agrégat d'atelier ne peut pas porter une valeur que le
journal d'un autre agrégat modifie ; seule une projection le peut.

### Ce que le rapport montre

Une ligne par nature d'opération, plus une ligne sans nature pour une entreprise sans parc machine.
Chaque ligne sépare travail et non conformité, ses périodes datées, machine et main d'œuvre.
L'élément connu mais jamais engagé rend un rapport vide ; un réengagement additionne ses passages.

Le rapport comptabilise seulement les activités terminées. Une activité encore en cours est entièrement
exclue du temps, des coûts et du diviseur ; `activitesEnCours` explique leur nombre.
Sans fin réelle, l'activité devient comptabilisable dès son échéance projetée par atelier, borne incluse, jusqu'à cette
borne fixe, même lors d'une lecture ultérieure. `finsAutomatiques` expose ses périodes et l'anomalie active.
Les fins pointées, les régularisations et les clôtures sont relues selon l'interprétation d'atelier, sans
fermeture à l'heure de lecture. Une régularisation peut établir plus que la durée max d'une activité.

### Partage et arrondi

Le coût de chaque machine court en entier. Le taux humain se partage selon les postes distincts occupés
par les activités terminées d'un même opérateur, tous éléments confondus. Plusieurs activités sur un même
poste comptent un poste, et sans poste représente un poste. Le découpage aux changements d'occupation
limite le partage aux chevauchements. La charge couvre l'union du travail valorisé et de l'occupation lue.
L’acquisition comprend les activités adjacentes et prolonge sa couverture sur les activités terminées
découvertes, jusqu’à lire la fenêtre entière ; un relais sur un même poste ne coupe pas son arrondi.

Pour A de 08 à 10 h sur fraiseuse, B sur tour ouverte depuis 09 h et 20 €/h humain, A vaut 40 € et B reste
exclue. Si B se termine à 11 h, le rapport recalculé porte 30 € humain sur chaque activité.
Le coût humain s'arrondit une fois par **fenêtre de partage**, période où l'opérateur occupe le même ensemble
de postes, puis se répartit en centimes entiers entre les activités. Le centime restant va au plus fort reste,
puis à l'activité commencée la première : 1 h à 35 €/h sur trois postes vaut 11,67 + 11,67 + 11,66 = 35,00 €,
quels que soient les éléments. La machine s'arrondit une fois par activité. La ligne et le rapport ne font
qu'additionner des montants **déjà arrondis**, pour que chaque total soit exactement la somme affichée
([ADR 0004](adr/0004-split-the-operator-cost-to-the-cent.md)).

### Valeurs

Les durées et montants portent leur seule `valeur`, toujours connue, zéro compris. Le zéro d'un taux absent reste
indépendant du diviseur : le rapport n'invente aucun tarif.

### Lecture et accès

Le coût lit les projections d'activités d'atelier par ses entités JPA `@Immutable`, sans import entre
contextes et sans repli concurrent du journal. Les tarifs sont ceux du fait ouvrant, figés à la saisie,
jamais ceux du référentiel courant.
L'occupation est sélectionnée par recouvrement, même commencée avant la période valorisée : une
régularisation peut dépasser la durée max d'une activité, donc aucune borne basse fixe sur le début n'est sûre.

L'horloge est relevée une seule fois par rapport et cet instant est rendu dans `evaluation`.
L'instant gouverne l'expiration ; les faits connus restent lus, même postérieurs. Cette route ne prend
pas de paramètre d'évaluation et ne promet pas un instantané face aux écritures concurrentes.
Le rapport est réservé au `GESTIONNAIRE`, car il expose des coûts issus des taux horaires humains.

### Points ouverts

Le coût par période (opérateur, poste ou mois) et le périmètre d'équipe du gestionnaire restent à définir
lorsqu'une demande métier les nécessitera.

## syntheseheures

Projection transverse purement lectrice : _combien de temps opérationnel cette personne a-t-elle accompli cette
semaine, sur quels éléments, jour par jour_. Elle recalcule ses durées depuis les activités interprétées par
`atelier` et ne calcule aucun montant.

### Pourquoi il n'est pas dans atelier

Atelier manipule des instants et ne connaît ni fuseau ni jour calendaire. La synthèse ramène ses activités au
calendrier de l'entreprise, comme `feuilledetemps`, puis additionne les portions terminées. Les deux lectures
possèdent leurs modèles et leurs adapters ; leurs scénarios Cucumber partagent les mêmes tableaux de faits.

### Ce que le relevé montre

Sept jours toujours, du lundi au dimanche de la semaine ISO explicite, vides compris. L'année est celle des
semaines ISO, différente de l'année civile à ses bornes. Un poste de nuit se coupe aux minuits locaux,
y compris entre deux semaines ; le changement d’heure conserve la durée réellement écoulée. Dimanche
22 h à lundi 3 h donne 2 h puis 3 h dans les deux semaines ISO ; minuit ne termine pas l’activité.

Les activités sont sélectionnées par **recouvrement**, même commencées avant la semaine et sans pointage en son
sein. Une régularisation peut établir une fin supérieure à la durée max d'une activité, voire à une semaine : aucune borne basse fixe
sur le début ne les retrouve toutes. La synthèse reçoit `evaluation` facultatif et rend l'instant effectivement
utilisé pour l'expiration et le découpage des activités en cours. Sans paramètre, l'heure du serveur est relevée
une seule fois. Le client transmet le même instant aux deux lectures pour composer le relevé.
Les faits connus restent interprétés, même postérieurs à cet instant. L'instant commun assure la même décision
d'expiration ; une écriture entre les appels peut changer les faits lus, sans instantané commun garanti.
Un instant passé est accepté. La borne
future est l'heure du serveur plus deux minutes, incluse ; elle est vérifiée avec un seul relevé d'horloge.
Un dépassement ou un instant fourni vide ou mal formé répond 400, sans rapport.

Une fin réelle conserve sa borne, même régularisée au-delà de l'échéance. Sans elle, l'activité ne produit aucune
durée avant son échéance et compte dès celle-ci jusqu'à sa fin automatique. Les fins pointées, les fins automatiques, les
régularisations et les clôtures sont celles d'atelier, projetées en base.

Les durées se **cumulent par élément** : deux éléments simultanés de 08 h à 09 h portent chacun une heure,
le jour et la semaine deux heures. La NC est comprise une seule fois dans la durée totale et exposée aussi à part.
Chaque total porte sa `valeur`, toujours connue. Les sommes des jours et des éléments sont égales à la durée
opérationnelle de la semaine. Une activité en cours conserve son élément sur chaque jour atteint, même sans pointage dans
la semaine, sans durée comptabilisée.

Les éléments rendus portent une activité ou un pointage dans la semaine, par première apparition puis nom.
Un réengagement reste le même élément. Le nom et la catégorie viennent du suivi ; référence et description sont
relues au référentiel, et peuvent être absentes. Les couples poste/nature suivent leur première apparition,
activité et pointage confondus ; l'absence de poste ou de nature est nominale.

Le **journal brut** est lu indépendamment des activités : tous les pointages de l'opérateur datés de la
semaine sont rendus, même sans activité, avec leur identité et leur type. Leur ordre est l'heure métier, puis la fin
avant l'ouverture, puis l'identité du pointage. L'heure d'enregistrement ne départage jamais.
Chaque élément et chaque poste nommés au journal trouvent leur fiche ou leur libellé dans la synthèse.

### La lecture passe par la base, jamais par un import

La synthèse déclare ses propres entités JPA `@Immutable` sur `activite_d_atelier`, `suivi_d_atelier`,
`evenement_d_atelier` et les référentiels. Elle n'importe aucun contexte métier voisin, ne rejoue aucun journal
et ne possède aucune table. Les activités sont lues en une requête, le journal en deux requêtes groupées ;
Les fiches et les postes sont aussi résolus par lots. Cucumber écrit réellement dans atelier puis lit le relevé,
ce qui confronte la projection à son propriétaire.

### Points ouverts

1. **Le fuseau reste fixé à `Europe/Paris`** par un adapter derrière son port.
2. **Les heures supplémentaires ne sont pas calculées**, faute de règle fournie par le client.
3. **Aucune restriction sur qui lit le relevé de qui** : `USER` et `GESTIONNAIRE` lisent les opérateurs de leur
   entreprise, faute de lien entre utilisateur authentifié et fiche d'opérateur.

## pupitre

Quatrième **projection transverse** du projet : un contexte purement lecteur, qui ne possède aucune table et
recalcule tout à chaque appel. Il répond à une seule question — _que doit garder sur disque le poste d'atelier pour
continuer à collecter sans réseau_ — et rend la réponse en un seul appel, `GET /api/pupitre/referentiel`.

### Pourquoi une route à part

Le pupitre de `glm-front` est offline-first : journal IndexedDB par entreprise, file d'attente FIFO, rejeu à
l'identique. Le chemin d'**écriture** est servi depuis longtemps par `atelier` — l'identifiant de chaque geste naît
au pupitre, un rejeu rend 200 sans dupliquer quand l'identifiant figure déjà dans les événements, et la règle de réception ignore (409 `pointage-ignore`) un pointage qui ne s'accorde pas à l'état de sa clé. Le chemin de
**lecture**, lui, se reconstituait en traversant deux collections paginées, `GET /api/operateurs` puis
`GET /api/atelier/suivis`, cent par cent, à chaque synchronisation — au démarrage, sur l'événement réseau, toutes
les trente secondes, après chaque capture, à la fermeture d'une fenêtre opérateur.

Quatre défauts en découlaient, tous du ressort du back :

- **un assemblage de pages.** Rien ne garantissait que deux pages venaient du même état de la base ; le front
  compensait par des gardes — total stable, pas de doublon, pas de page vide — qui ne rattrapent pas un remplacement
  de même taille entre deux pages. La route supprime les pages, donc ces gardes ; elle ne rend pas pour autant un
  instantané, voir plus bas ;
- **aucune date.** Le pupitre ne savait pas de quand datait ce qu'il affichait, seulement s'il était connecté ;
- **des montants sur un écran d'atelier partagé.** `RestOperateur` porte le taux horaire ; le pupitre le recevait et
  le jetait, alors que `coutderevient` le réserve au gestionnaire ;
- **N requêtes là où une suffit**, avec un plafond de page à cent.

### Ce que la route rend, et ce qu'elle ne rend pas

Les opérateurs désignables — identité, identifiant, postes habilités —, les éléments encore pointables — identité,
nom d'atelier, référence, catégorie, état, activités en cours —, `genereLe` et la durée maximale d'une activité (`dureeMaximaleDActivite`, ISO 8601 : `PT13H` par défaut, la valeur fixée par le gestionnaire sinon), que le pupitre lit au lieu de la coder. Un opérateur sans activité
reste rendu avec toutes ses habilitations.

La liste des opérateurs dépend du référentiel et des habilitations, indépendamment des pointages.
Les activités viennent de la projection d'atelier ; les opérateurs restent désignables sans activité.

Elle ne rend **ni montant** (taux horaire, coût horaire : les entités de lecture ne les mappent même pas), **ni
journal d'événements**, **ni élément clôturé**, ni métadonnée d'engagement ou de clôture.

### `genereLe` date l'évaluation

Le pupitre peut donc dire « référentiel du 14/09 à 09:31 » et mesurer son retard. Cette date change à chaque appel, y
compris quand rien n'a bougé : elle dit quand le serveur a produit la réponse, pas quand le référentiel a changé
pour la dernière fois. Dater le dernier changement supposerait d'horodater les modifications d'`operateur`,
`poste_de_travail` et `operateur_poste` — trois tables sans colonne de modification, et une date de modification est
une donnée du domaine, qui ouvrirait deux agrégats voisins. Écarté tant que rien ne le demande.

### La non-pagination est le choix, pas un oubli

C'est la pagination qui imposait au front ses gardes sur les totaux, les doublons et les pages vides. Tout est ici lu
en **un appel et une transaction unique** : il n'y a plus de pages à recoudre. Le volume est borné par la taille de
l'atelier. `genereLe` donne un instant commun d'expiration aux activités lues ; les écritures concurrentes
peuvent toutefois modifier les données entre les requêtes.

La transaction ne fait pas pour autant de la réponse un instantané. La route a demandé une **lecture répétable** de
sa livraison au 21/09/2026, précisément pour cela — sous `READ COMMITTED`, chaque requête prend son propre
instantané, et la lecture des opérateurs peut ignorer un opérateur qu'une activité de la lecture suivante désigne.
Cette demande n'a jamais pu être honorée : Hibernate pose le schéma du tenant dès l'acquisition de la connexion, ce
qui prend un instantané, et PostgreSQL refuse ensuite tout changement d'isolation. La route répondait `500` à chaque
appel. L'isolation a donc été retirée et l'écart assumé — il porte sur deux collections lues à quelques
millisecondes d'intervalle, et se résorbe au rafraîchissement suivant du cache. La récupérer supposerait de
s'approprier l'acquisition de connexion du multi-tenant ; le détail est dans le
[AGENTS.md du contexte `pupitre`](../src/main/java/com/glm/glmback/pupitre/AGENTS.md).

### La lecture passe par la base, jamais par un import

`atelier`, `operateur`, `postedetravail` et `elementdefabrication` étant annotés `@BusinessContext`, ce contexte
porte ses propres entités JPA en lecture seule. Il lit l'interprétation projetée dans `activite_d_atelier`, sans
rejouer le journal d'atelier : seules les activités interprétables sans fin réelle peuvent être courantes.
L'échéance est inclusive : à début plus la durée de l'activité pile, elle disparaît de la liste. L'identité de l'activité, rendue dans `ouverture`, est celle de son pointage ouvrant. Le suivi est `EN_COURS` si l'une de ces activités l'est à `genereLe`,
sinon `INTERROMPU` s'il porte un pointage, sinon `EN_ATTENTE`.

Les scénarios Cucumber écrivent par l'API d'atelier puis relisent par le référentiel, avec échéance et régularisation :
ils vérifient les colonnes réellement partagées entre les deux contextes.

### Le nom vient du suivi, la référence du référentiel

Le nom de l'élément est celui copié à l'engagement : un élément renommé ne réécrit pas l'histoire de l'atelier. La
`Reference`, elle, est un libellé courant, relu à chaque lecture comme les identités d'opérateurs — c'est ce que le
front préfère afficher sur sa tuile quand l'entreprise en attribue une. Un élément **supprimé** du référentiel
laisse donc sa tuile intacte, privée de sa seule référence.

### Points ouverts

1. **Aucune lecture conditionnelle.** Le rafraîchissement toutes les trente secondes renvoie le corps entier. Un
   `ETag` supposerait une version qui ne bouge qu'au changement, donc les colonnes de modification écartées
   ci-dessus. À rouvrir si le volume le justifie.
2. **La quarantaine des gestes refusés reste à faire**, côté serveur : un rejeu refusé pour raison métier —
   habilitation retirée, suivi clôturé — ne vit aujourd'hui que dans le journal local du pupitre.
   La [réflexion d’authentification](strategie/authentification-pointage.md) envisage cette quarantaine ;
   elle n’est pas une fonctionnalité livrée. Les refus du [catalogue courant](codes-erreur.md) restent
   durables dans le journal local. Un pointage ignoré par la règle de réception n'est pas un refus à reprendre : il est audité en base et le pupitre se recale.
3. **Le client Keycloak du pupitre n'existe pas dans le realm.** `glm-front` attend `pupitre_device`, avec le
   device grant activé et le client scope `glmproject` — sans lui, le jeton ne porte pas de claim `tenant` et toute
   la surface `/api/**` répond 403. C'est la dernière pièce d'infrastructure avant qu'un pupitre déployé puisse
   s'enrôler.
