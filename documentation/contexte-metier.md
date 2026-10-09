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

## parametrage

Gère les **réglages que l'entreprise fixe elle-même**, sans développeur : un seul jeu pour toute l'entreprise, que le gestionnaire modifie et que tout utilisateur lit. GLM est une trame, et une donnée qui varie d'un client à l'autre ne s'écrit pas en constante.

Le premier réglage est la **durée max d'une activité** : le temps au bout duquel une activité que rien n'a terminée se termine automatiquement (voir « La fin automatique à l'échéance » dans `atelier`). Elle vaut **treize heures** tant que l'entreprise ne l'a pas fixée, et reste comprise entre une heure et vingt-quatre heures. Un réglage jamais fixé n'est pas recopié en base : la valeur par défaut n'existe qu'à un endroit, le domaine.

Le `Parametrage` n'a pas d'identifiant : chaque schéma d'entreprise porte une seule ligne, créée avec lui. Un réglage n'est donc jamais créé ni supprimé, seulement modifié.

## atelier

Gère l'exécution en atelier de ce que `elementdefabrication` a déclaré. Le gestionnaire y met un élément en atelier, les opérateurs y pointent leurs activités, le gestionnaire clôture et corrige.

Le contexte porte le `SuiviDAtelier` d'un élément engagé et son journal d'activité. Les temps se lisent dans
ses activités interprétées, avec leurs bornes, leur échéance et les séquences en conflit.

**Le journal d'événements est la source de vérité.** L'agrégat se reconstruit par le repli du journal, trié par date de survenue ; les projections d'activités et de conflits sont réconciliées à chaque écriture pour les lectures. C'est la correction qui l'impose — un temps juste exige que la saisie oubliée compte à l'heure où elle a eu lieu, pas à l'heure où on la rattrape, et un modèle à compteurs ne sait pas revenir en arrière. Chaque événement porte donc un `Horodatage` bitemporel : sa date de survenue, métier, et sa date d'enregistrement, technique.

**Une régularisation est un acte du gestionnaire, conservé sur le fait, pas un écart de dates ni une identité d'auteur.** Chaque événement du journal d'un élément porte son origine : `POINTAGE` pour tout ce qui passe par la route des pointages, quels que soient le rôle de celui qui pointe et l'heure de geste fournie, `REGULARISATION` pour une régularisation ou le remplaçant d'une correction, même saisis à l'heure du fait. `estUneRegularisation()` lit cette origine. L'écart entre les deux dates ne suffit pas : un pupitre resté hors ligne rejoue ses pointages après coup sans que le gestionnaire soit intervenu. Cet écart reste la lecture d'une saisie différée. Le booléen jumeau `estSaisiParUnTiers` a été retiré avec le passage à l'identifiant : l'`Auteur` vient du jeton et l'opérateur du référentiel, et rien ne relie encore les deux — le comparer n'aurait plus produit qu'une réponse toujours vraie. Il reviendra avec le lot « utilisateur connecté ».

### La pause et le travail non facturable

La pause est un geste du pupitre : il envoie une fin ciblée par activité actionnable, puis une nouvelle ouverture à
la reprise, en travail ou en non conformité. Le serveur reçoit les faits d'activité correspondants.

**GLM n'est pas un concept du modèle.** C'est le nom que le client de référence donne à son travail non facturable,
sur un projet interne par exemple, qu'il veut déclarer manuellement (« De toute façon il y aura ce bouton GLM »).
Ce travail sera déclaré par le superviseur sous la forme d'un OF de type Perso. La création et ce sous-type feront
l'objet d'un chantier séparé ; la supervision actuelle ne crée aucune activité sans élément.

### Le temps effectif, celui des seules activités

`TempsDAtelierService.tempsEffectif` rend les intervalles des activités d'un élément, tels que le journal les interprète (`SuiviDAtelier.intervalles`), à l'instant d'évaluation. Une activité se termine à sa fin réelle — un geste qui la termine, ou la clôture —, sinon automatiquement à son échéance, avec une anomalie (voir ci-dessous).

La route rend l'identité stable de chaque activité et ses bornes : une activité en cours ou à résoudre reste sans
fin ni durée à comptabiliser ; `finAutomatique` signale l'anomalie d'une activité terminée à son échéance,
`aResoudre` la dépendance à un conflit. L'horloge du service applicatif fournit l'instant de cette lecture.

La pause de midi scinde le travail par le journal de l'élément : une fin à midi, un début à la reprise. Corriger une heure de pause fausse demande donc une correction par activité, sur sa fin et sur son début.

Un travail jamais arrêté ne court pas pour autant jusqu'au lendemain : il se termine à son échéance, et l'opérateur qui reclique sur l'élément à son retour ouvre une nouvelle activité, sans prolonger l'ancienne. Une fin oubliée se rattrape par une régularisation du gestionnaire, qui remplace la fin automatique.

### La fin automatique à l'échéance

Une activité encore en cours ne compte rien. Oubliée, elle ne court pas pour autant indéfiniment : **son échéance est son début plus 13 heures écoulées** (`Echeance`), jamais 13 heures d'horloge murale — le passage à l'heure d'été ou d'hiver ne l'allonge ni ne la raccourcit. Une activité que rien n'a terminée avant son échéance est **terminée automatiquement** à cet instant, et porte une **anomalie**. Ce sens restreint est la nature `FIN_AUTOMATIQUE` des anomalies de pointage (`finAutomatique`) : le gestionnaire traite aussi les `CONFLIT`, et le coût de revient dit `AnomalieDuPointage` ce qui rend un pointage suspect ou incomplet (`FIN_AUTOMATIQUE` est commun, `CONFLIT` correspond à `A_RESOUDRE`, `PARTAGE_INCONNU` n'a pas d'équivalent en atelier), sans type partagé. Travail à 8 h sans aucune fin : lu à 20 h 59, il est en cours ; lu à 21 h, ou le lendemain, il est terminé à 21 h. Le délai est la règle de l'atelier, pas un paramètre de l'entreprise.

**Rien n'est écrit.** Ni événement de clôture automatique ni traitement planifié : l'interprétation du journal donne des activités qui ne dépendent que des faits, avec leur fin réelle quand un geste ou la clôture les a terminées. Seule leur lecture, à un **instant d'évaluation** explicite, décide si une activité sans fin réelle est encore en cours ou déjà terminée automatiquement. Cet instant vient de l'horloge du service applicatif. La première lecture après une indisponibilité retrouve donc la même borne, sans rattrapage.

**La même échéance gouverne l'interprétation**, sans instant de lecture, sur les seules heures métier :

- un geste pointé **au plus tard à l'échéance** de l'activité qu'il vise la termine à son heure, même reçu après elle : une fin pointée à 17 h et reçue le lendemain donne 9 h, et retire l'anomalie. Un geste pile à l'échéance l'emporte sur la fin automatique ;
- une fin pointée **après l'échéance** est conservée : l'activité garde sa borne de 21 h et son anomalie, sans conflit ni qualification supplémentaire du pointage dans le journal ;
- une transition pointée après l'échéance de sa cible laisse à celle-ci sa borne automatique et ouvre la nouvelle activité à son heure : travail à 8 h, non conformité visant ce travail à 23 h, rien n'est compté entre 21 h et 23 h ;
- une relance après l'échéance laisse le même trou, et une clôture postérieure à l'échéance ne prolonge rien ;
- **seul le gestionnaire** établit une fin réelle au-delà de l'échéance, par une fin ou une transition régularisée : la fin automatique est une borne par défaut, pas un plafond.

Chaque transition ouvre une activité distincte, avec sa propre échéance : travail à 8 h puis non conformité à 12 h donnent 4 h de travail terminées à 12 h et une non conformité en cours jusqu'à 1 h le lendemain. Corriger un début recalcule tout : à 22 h, un début corrigé de 8 h à 12 h repousse l'échéance à 1 h, et l'activité redevient en cours, sans anomalie.

### Les séquences en conflit

Un atelier pointe hors ligne, depuis plusieurs pupitres, et le gestionnaire rattrape après coup : deux faits peuvent se contredire, et le second arriver avant le premier. Travail A à 8 h, passage de A en non conformité à 12 h, fin de A à 17 h : la fin dit A en cours jusqu'à 17 h, la transition dit qu'elle a cessé à 12 h. **Le serveur ne choisit pas.** Il ne refuse aucun des deux, ne rattache pas la fin à la non conformité et n'ignore pas la transition, quel que soit leur ordre d'arrivée : les faits sont conservés, et leur séquence est **en conflit** jusqu'à ce que le gestionnaire corrige ou annule ce qui est faux. Un refus aurait fait dépendre le journal de l'ordre de réception, et fait perdre un geste réel ; une interprétation silencieuse aurait chiffré un temps que personne n'a validé.

Se contredisent : un geste qui vise une activité remplacée avant son heure, déjà terminée par une fin — le double appui sur « arrêter » compris —, pas encore ouverte à son heure ou dont l'ouverture est annulée ; une transition vers sa propre catégorie, qui serait une relance déguisée ; une transition qui vise une activité échue alors qu'une autre est en cours sur le poste, et qu'elle ne peut ouvrir sans la terminer. Ne se contredisent pas : un geste qui vise une activité seulement échue, que la règle des 13 h suffit à lire, ou pointé pile à son échéance.

La contradiction couvre ce qui sépare le début de la cible de l'heure du geste. La cible, l'activité qu'ouvre le geste et toute activité du même poste qui chevauche cette zone sont **à résoudre** : ni en cours, ni terminées, sans durée ni coût chiffrés, et la fin automatique ne les tranche pas. Les autres gardent leur lecture, en particulier la nouvelle ouverture pointée après le conflit, seule action qu'un pupitre propose encore sur ce poste. L'état de l'élément se juge sur ses seules activités interprétables ; le conflit se lit à part, dans la réponse du suivi, et ne se stocke jamais : il disparaît au recalcul dès que les faits redeviennent cohérents. L'historique ne garde que les pointages et les corrections.

La correction d'un ouvrant conserve l'identité de son activité. Elle ne peut donc pas la déplacer vers un autre couple opérateur/poste tant qu'un geste actif la vise depuis l'ancien : ce serait une cible incohérente, pas une contradiction de séquence. Le gestionnaire corrige ou annule d'abord ce geste, puis déplace l'ouvrant.

Une fin survenue avant la clôture de l'élément, mais reçue après elle, est enregistrée à son heure : la clôture ne prime pas sur un geste qui l'a précédée. Survenue après, elle n'arrête plus rien.

### L'activité, un opérateur sur un poste de travail

`CleDActivite` est le couple (`OperateurId`, `Optional<PosteDeTravailId>`). Le poste de travail est ce que l'opérateur engage en pointant : une machine chez le client de référence, un établi, un four, une salle ailleurs.

- Il est **facultatif** : une entreprise sans parc machine laisse l'`Optional` vide et retrouve une activité unique par opérateur.
- C'est **lui**, et non la nature de l'opération, qui identifie l'activité. L'érosionniste qui met deux pièces du même élément sur deux machines mène ainsi deux activités indépendantes — cas que le client détaille explicitement, et qu'une clé fondée sur la fonction refuserait.

La `NatureDOperation` (fraisage, tournage, érosion, dessin) est **recopiée du poste** au moment de la saisie, par le port `PostesConnus`, comme le nom de l'élément est recopié à l'engagement : un poste requalifié plus tard ne réécrit pas l'histoire de l'atelier. Elle vient du poste et non de la personne, parce que c'est le poste qui dit quel métier s'y exerce — un opérateur polyvalent déclenche un pointage par poste. Elle ne porte **aucun invariant** : elle n'est qu'un axe d'agrégation pour la synthèse par catégorie de travail, et elle reste facultative comme le poste.

### Le référentiel, atteint par identifiant

Le journal ne retient que des **identifiants** — `OperateurId`, `PosteDeTravailId` —, jamais un libellé, à la seule exception de la nature. C'est ce qui permet à toute agrégation à venir de compter une personne pour une personne, là où deux orthographes d'un texte libre en auraient compté deux.

Rien d'autre n'en est copié, à la différence du nom de l'élément figé à l'engagement, et la raison est symétrique : un élément renommé ne doit pas réécrire son histoire, alors qu'une fiche d'opérateur corrigée doit s'afficher corrigée sur toutes les feuilles de temps, y compris les anciennes. Les libellés sont donc **relus à chaque lecture** par `OperateursConnus` et `PostesConnus`, dont l'accès par ensemble résout un journal entier en une requête. `AnnuaireDAtelier` porte ce résultat le temps d'une lecture.

La contrepartie de ce choix : **ni un opérateur ni un poste ayant servi à pointer ne se supprime**. La règle vit dans les deux référentiels, derrière un port qui lit le journal d'atelier.

**L'habilitation est la seule règle dure du contexte.** Un pointage sur un poste où l'opérateur n'est pas déclaré est refusé (409), par `Habilitations`. La règle ne joue que lorsqu'un poste est fourni : une entreprise sans parc machine n'a aucune habilitation à déclarer et retrouve son comportement nominal. Elle joue en revanche sur les **trois** écritures du journal — pointage, régularisation et correction —, sans quoi le back-office contournerait ce que le pupitre applique.

### Non conformité

Une pièce ratée se refait, sur le même élément et au même tarif, mais comptée à part. Chaque pointage dit son **intention** (`IntentionDePointage`) : une **ouverture** crée une activité, en travail ou en non conformité ; une **transition** remplace l'activité qu'elle vise par une activité distincte de l'autre catégorie ; une **fin** termine l'activité qu'elle vise, et elle seule. Passer en non conformité, puis reprendre du bon travail, se pointe donc par deux transitions, un `NON_CONFORMITE` puis un `DEBUT` qui visent chacun l'activité qu'ils remplacent. L'activité visée se désigne par l'identité de son pointage ouvrant (`ActiviteId`), que garde le remplaçant d'une correction. Une ouverture sur une activité déjà en cours la **relance** : la période précédente s'arrête à l'heure du geste, une nouvelle commence, et l'opérateur qui revient sur un élément resté ouvert n'est jamais bloqué. Un geste qui contredit le journal — il vise une activité déjà terminée, déjà remplacée ou annulée, ou change une activité vers sa propre catégorie — n'est jamais refusé ni rattaché à une autre activité que sa cible : il est conservé, et sa séquence est en conflit. À la clôture, on sait « combien de temps on a passé à faire du bon travail et combien à refaire ».

### Les trois actes de correction

| Situation      | Acte         | Effet                                                |
| -------------- | ------------ | ---------------------------------------------------- |
| Saisie oubliée | `regularise` | insertion d'un événement daté dans le passé          |
| Saisie en trop | `annule`     | marquage de l'événement fautif, qui reste au journal |
| Saisie fausse  | `corrige`    | annulation **et** insertion, en un seul acte         |

Ces trois actes s'appliquent aux faits du suivi d'atelier.

Un journal ne se réécrit pas : l'événement erroné reste, porteur d'une `Annulation` qui trace qui a corrigé, quand et pourquoi. Le repli écarte les annulés, puis déroule l'automate.

`corrige` n'est pas la composition des deux autres, et c'est le point non évident : le remplaçant d'un début corrigé garde l'activité qu'il ouvrait, et les gestes qui la visaient y restent rattachés. Annuler ce début puis régulariser sa version corrigée ouvrirait une autre activité, et laisserait en conflit la fin qui visait la première.

Ce sont aussi les actes qui **résolvent une séquence en conflit** : annuler la transition erronée, ou corriger la fin qui visait l'activité remplacée pour qu'elle termine sa remplaçante. Aucun n'est refusé parce qu'il crée ou laisse une contradiction ; une résolution en plusieurs actes traverse donc des états intermédiaires en conflit, et le recalcul retire le conflit dès que les faits redeviennent cohérents.

**La clôture ne fige rien pour le gestionnaire.** Elle ferme le pointage aux opérateurs ; régularisation, annulation et correction restent admises, et la clôture elle-même se déplace ou s'annule. Le seul invariant qui subsiste est de cohérence, pas de permission : aucun événement daté après la clôture.

### Le temps réparti

Deux mesures se distinguent par leur cumul :

- **temps effectif** — la durée réelle passée sur un élément, telle que la produit `TempsDAtelierService`. Deux postes pendant 1 h font 2 h effectives.
- **temps réparti** — la même heure d'opérateur divisée par le **nombre de postes de travail** occupés simultanément par ses activités terminées, tous éléments confondus. Il sert au coût de revient.

Le **temps opérationnel** de `syntheseheures` cumule les portions d'activités terminées, réelles ou automatiques,
coupées aux minuits locaux. Les activités en cours restent visibles sans durée comptabilisée ; les valeurs
dépendant d'une activité à résoudre restent incomplètes sans chiffre. Voir la section `syntheseheures`.

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

L'atelier projette aussi ses séquences en conflit, pour que les lecteurs les retrouvent après un redémarrage :
`sequence_en_conflit` porte le suivi et le couple opérateur/poste, `pointage_en_conflit` les identités ordonnées de
ses faits actifs, et `activite_d_atelier` le rattachement ordonné de ses activités à résoudre. Une séquence peut
n'avoir aucune activité, notamment après l'annulation d'un ouvrant encore visé. Ces tables sont écrites uniquement
par atelier, depuis `SuiviDAtelier.conflits()` ; elles sont rapprochées à chaque écriture et disparaissent après
résolution. Elles ne servent jamais à reconstituer le suivi, qui rejoue son journal.

Chaque activité à résoudre porte aussi sa `fin_au_plus_tard`, indépendante de la lecture : son échéance ou la
plus tardive fin ou transition régularisée qui la vise, limitée par la clôture. Elle borne ses jours possibles
sans lui donner de fin réelle. La correction, l'annulation et la clôture réécrivent la projection ; la résolution
retire cette borne avec le conflit, sans historique d'anomalie artificiel.

Le modèle relationnel permet aux lecteurs de sélectionner les activités par **recouvrement**, et de lire
séparément le journal brut pour le relevé. Chaque lecteur possède ses entités JPA en lecture seule sur les
tables du propriétaire, comme `atelier` le fait sur `element_de_fabrication`. Les bornes projetées servent
les rapports sans rejouer les gestes ; les faits du journal restent consultables avec leur identité, intention
et cible.

`ElementsEngageables` lit la table `element_de_fabrication` par une entité en lecture seule propre à l'atelier, sans jamais importer le contexte voisin. `OperateursConnus`, `PostesConnus` et `Habilitations` font de même sur `operateur`, `poste_de_travail` et `operateur_poste`.

L'API est décrite par OpenAPI (`/swagger-ui.html`) et par [atelier-api.md](atelier-api.md), qui porte ce que la spec ne peut pas dire.

### La supervision de l'atelier

`GET /api/atelier/supervision` rend une lecture complète de l'entreprise : tous les opérateurs et leurs métiers
courants, les activités interprétables en cours ou terminées automatiquement, et les séquences en conflit avec
les descriptions de leurs activités à résoudre, y compris les séquences vides. Une fin automatique reste à traiter
après une relance ou la clôture ; elle disparaît quand une fin recevable retire son anomalie. Les activités terminées
réellement ne sont plus supervisées.

La route lit les projections d'atelier, sans repli des journaux, à un seul instant d'évaluation fourni par l'horloge
applicative. Le classement visuel et les compteurs appartiennent au consommateur. Le
[guide d'intégration](atelier-api.md#la-supervision-de-latelier-en-une-lecture-complète-rôles-user-et-gestionnaire)
porte le contrat détaillé et les limites de cohérence face aux écritures concurrentes.

### Points ouverts

1. **Régulariser après une dé-habilitation est refusé.** L'habilitation étant vérifiée sur les trois écritures du journal, un gestionnaire ne peut plus rattraper une saisie oubliée sur un poste dont l'opérateur a été retiré depuis. Le cas est assumé pour ce lot — il ferme la porte au contournement —, mais il empêcherait le rattrapage des activités concernées : à rouvrir si le client le rencontre.
2. **Le coût de revient monétaire est sorti du contexte** : `coutderevient` lit les activités projetées par
   atelier et les tarifs du fait ouvrant actif, par ses propres ports en lecture seule. `atelier` capture ces
   tarifs ; le coût les combine. Reste ouvert le **coût par période** (par opérateur, par poste, par mois), qui
   n'a pas de demande client formulée. Le partage suit le verbatim client : taux humain divisé par postes
   distincts occupés par les activités terminées, coût machine entier.
3. **La pause, la reprise et l'arrêt global appartiennent au pupitre.** La pause termine les activités
   actionnables et mémorise celles à reprendre ; la reprise ouvre de nouvelles activités. L'arrêt global termine
   les activités encore actionnables et efface durablement la mémoire de reprise. Le serveur reçoit les gestes
   d'activité correspondants ; les activités en conflit ou expirées sont exclues de ces commandes. Voir
   l'[ADR 0002](adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md) pour la frontière serveur/pupitre.
4. **La déclaration du travail non facturable.** Le client veut son bouton GLM, placé en bas de l'écran, pour déclarer à la main le travail qu'il ne facture pas. Il sera déclaré par le superviseur comme un OF de type Perso. Sa création, le sous-type et le rattachement éventuel à un projet interne feront l'objet d'un chantier distinct.
5. **Le cycle de vie de l'élément lui-même.** La clôture existe côté atelier, sur le suivi. Reste à trancher si l'élément de fabrication porte en propre un statut, ou si son activité se lit entièrement par la présence ou l'absence d'un suivi non clôturé.
6. **Aucune garde d'unicité en base** sur « un seul suivi non clôturé par élément », contrairement à ce que `elementdefabrication` fait pour la `Reference`. La règle vit dans le service, mais une contrainte partielle transformerait en 500 un état que le domaine admet aujourd'hui : rouvrir la clôture d'un suivi dont l'élément a été réengagé depuis. À trancher côté domaine avant de poser la contrainte.
7. **L'écriture du journal rapproche par identifiant**, ce qui coûte une lecture indexée de la collection à chaque pointage. Si un journal devenait assez long pour que cette lecture pèse, la sortie est un upsert natif gardé (`on conflict (id) do update ... where ... is distinct from ...`), qui épargne à PostgreSQL toute version de tuple sur les lignes inchangées — au prix d'une scission permanente entre lecture JPA et écriture JDBC.

## postedetravail

Gère le **référentiel de ce sur quoi les opérateurs pointent**. Un `PosteDeTravail` porte un `Libelle` et une `NatureDeTravail` : « Tour 1 » sert à tourner, « Poste de soudure » à souder. Il porte aussi, facultativement, un `CoutHoraire` : le contexte se contente de le stocker et de le restituer, le calcul du coût de revient restant un lot à part. `atelier` le lit désormais, mais seulement pour le copier sur chaque événement du journal au moment de la saisie — sans jamais le recalculer ni le combiner. C'est la capture, pas la valorisation.

Le terme reste volontairement générique, comme dans l'atelier : une machine chez le client de référence, un établi, un four, une salle ailleurs.

**Le libellé est unique par entreprise.** C'est ce qui fait du référentiel un référentiel : sans lui, rien ne relierait le « Tour 1 » saisi par Dupont au « Tour 1 » saisi par Martin. La garde vit dans `PostesDeTravailService`, sur le patron de `ElementsDeFabricationService.verifierReferenceLibre` ; la contrainte du schéma est le filet de dernier recours.

**La nature est obligatoire ici**, alors qu'elle reste facultative dans l'atelier. Ce n'est pas une contradiction : l'atelier doit fonctionner pour une entreprise sans parc machine ni métiers distincts, qui n'ouvrira simplement pas cet écran. Mais un poste qui serait déclaré sans dire quel travail s'y fait ne servirait à rien — c'est précisément ce que ce contexte apporte.

**Un poste encore habilité ne se supprime pas** : cela laisserait des opérateurs pointer sur du vide. La règle vit dans le domaine, derrière le port `PostesEnUsage`, dont l'adapter lit la table `operateur_poste` par une entité en lecture seule — sans jamais importer `operateur`, annoté `@BusinessContext`.

**Un poste sur lequel du temps a été pointé ne se supprime plus du tout**, et ce second refus est définitif là où le premier se lève en retirant l'habilitation : le journal d'atelier ne retenant que l'identifiant du poste, sa disparition laisserait des heures de travail sans machine. Le port `PostesPointes` lit `evenement_d_atelier` de la même façon.

## operateur

Gère le **référentiel des personnes qui pointent**. Un `Operateur` porte son nom, son prénom, un identifiant facultatif, un taux horaire facultatif, et l'ensemble des postes sur lesquels il est habilité.

### Le métier vient du poste, jamais de la personne

C'est le point décisif de ces deux contextes, et il est fondé sur ce que le client décrit.

Un opérateur polyvalent — soudeur **et** tourneur — déclenche **deux démarrages** sur le pupitre : un sur le poste de soudure, l'autre sur le tour. Deux temps courent alors en parallèle, et chacun sait de quel métier il relève parce que le poste le dit. C'est exactement ce que l'atelier modélise déjà : sa `CleDActivite` est le couple (opérateur, poste), et son javadoc énonce que « c'est le poste, et non la nature de l'opération, qui distingue deux activités menées de front ». Le verbatim client le confirme sur un cas voisin : deux pièces du même OF sur deux machines différentes.

Il s'ensuit que **la nature appartient au poste**. Déclarer un métier sur la personne stockerait la même information deux fois, avec la possibilité qu'elles se contredisent — un tourneur habilité sur une fraiseuse. Les métiers d'un opérateur se **déduisent** donc de ses postes : `ProfilDOperateur.natures()` rend les natures de ses habilitations, triées. Personne ne les saisit.

La phrase du client « la machine est liée à l'opérateur, et l'opérateur a la fonction » dit **où se saisit** le paramétrage — sur la ligne de l'opérateur, on liste ses postes —, pas d'où la nature se déduit au moment du pointage.

**Un opérateur dont un fait historique d'activité existe ne se supprime pas**, même si ce fait est annulé. Le journal conserve son identifiant pour lire l'histoire ; `OperateursQuiOntPointe` lit les faits par une entité propre en lecture seule, sans filtre d'annulation.

### Identité et identifiant

L'identité (nom, prénom) est **unique par entreprise**. L'**identifiant** est le code que l'entreprise donne elle-même à ses collaborateurs : **facultatif**, car toutes n'en attribuent pas, et **unique dès qu'il est renseigné** — patron exact de `elementdefabrication.Reference`, `NULL` distincts compris, donc autant d'opérateurs sans identifiant que nécessaire.

### Frontière avec postedetravail

`postedetravail` étant annoté `@BusinessContext`, ce contexte ne l'importe jamais : il déclare ses propres `PosteHabilitableId`, `LibelleDePoste` et `NatureDeTravail`, et lit la table voisine par une entité en lecture seule, sur le patron d'`ElementEngageableEntity`.

**Rien n'est copié**, à la différence de l'atelier qui copie nom et catégorie à l'engagement. La raison est symétrique : l'atelier copie parce qu'un élément renommé ne doit pas réécrire son histoire, alors qu'ici aucun historique ne pend à un poste — un poste renommé doit s'afficher renommé partout. L'opérateur ne stocke donc que l'identifiant, et le port `PostesHabilitables` n'expose que `parIds`, pour qu'une page entière se résolve en une requête.

### Points ouverts

1. **Les gestionnaires ne sont pas déclarés.** Leur fiche n'aurait aucun usage tant que l'authentification n'est pas tranchée : l'`Auteur` d'une saisie vient du jeton, pas d'un référentiel. À rouvrir avec ce sujet.
2. **Aucun plafond sur le nombre de postes par personne**, alors que le client énonce « maximum 4 machines par personne ». Une donnée de paramétrage ne s'écrit pas en constante du domaine, et GLM est une trame : une autre entreprise en habilitera six. Si le plafond doit être tenu, il viendra d'un port.
3. **Montants.** Coût horaire du poste et taux horaire de l'opérateur existent sur les deux agrégats (facultatifs, strictement positifs), `atelier` les copie sur chaque événement du journal au moment de la saisie, et `coutderevient` les valorise. La boucle est fermée : un opérateur sans taux horaire ne coûte rien en main d'œuvre, et le rapport ne l'invente pas.
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
4 h puis 8 h. Les indications en cours et les plages possibles à résoudre gardent leurs règles ci-dessous.

### La lecture passe par la base, jamais par un import

`atelier`, `operateur` et `postedetravail` étant annotés `@BusinessContext`, ce contexte déclare ses propres entités
JPA `@Immutable`. Il lit la projection `activite_d_atelier`, avec l'élément porté par le suivi, et l'identité de
l'opérateur dans le référentiel. Il ne rejoue aucun journal et ne propose aucune écriture.

Le filet est le scénario Cucumber : il écrit par l'API d'atelier puis lit la feuille. Relances, transitions
ciblées, fins reçues tardivement, régularisations, corrections, annulations et clôtures restituent
l'interprétation du propriétaire. Le lecteur compare l'échéance projetée à son instant de lecture puis découpe
les activités au calendrier de l'entreprise.

### Ce que la feuille montre

Sept jours toujours, du lundi au dimanche de la semaine ISO demandée, vides compris. L'année est celle des
semaines ISO, qui diffère de l'année civile à ses bornes : la semaine 1 de 2026 commence le 29 décembre 2025.
La semaine est toujours explicite, jamais « la semaine courante ».

Chaque jour rend les portions d'activité : élément, poste et nature facultatifs, catégorie travail ou
non-conformité, début et fin éventuelle. La sélection porte sur les activités qui **recouvrent** la semaine,
même commencées avant elle et sans pointage de la semaine. Une régularisation peut établir une fin bien au-delà
de 13 h, voire de la semaine : aucune borne basse fixe sur le début ne permet de les retrouver toutes.

La feuille accepte un instant `evaluation` facultatif et rend celui effectivement utilisé. Sans paramètre,
l'heure du serveur est relevée une seule fois. Cet instant gouverne l'expiration et les jours atteints par les
activités en cours et par les plages possibles à résoudre. Transmettre le même instant à la synthèse assure la
même décision d'expiration dans le relevé. Les faits connus restent interprétés même postérieurs à cet instant ;
une écriture entre les appels peut les modifier, donc l'instant commun ne garantit pas un instantané commun.
Un instant passé est accepté. La borne future est l'heure du serveur plus deux minutes, incluse ; elle est vérifiée
avec un seul relevé d'horloge. Un dépassement ou un instant fourni vide ou mal formé répond 400, sans rapport.

Une activité avec fin réelle est `TERMINEE`, même si cette fin dépasse
l'échéance. À défaut, elle est `EN_COURS` avant l'échéance et `TERMINEE_AUTOMATIQUEMENT` dès celle-ci, à cette borne,
avec son anomalie visible par l'état. Une activité en conflit est `A_RESOUDRE`, sans fin : l'échéance ne la tranche pas.
La feuille ne calcule aucune durée. Chaque portion garde l'identité stable, l'état et les bornes de l'activité entière,
avec une fin seulement pour les deux états terminés. Les portions terminées sont coupées aux minuits locaux et aux
limites de la semaine ; les bornes de l'activité restent intactes.

Une activité en cours rend une indication sans fin sur chacun des jours atteints à l'instant de lecture, dans la
semaine : commencée dimanche à 22 h et lue lundi à 1 h, elle apparaît lundi avec son début entier, sans fin à minuit.
Une fin lundi à 3 h remplace ensuite cette indication par les portions terminées, 2 h dimanche et 3 h lundi.
Une activité à résoudre figure sur chaque jour de sa plage possible : de son début jusqu'à la fin au plus tard
projetée par atelier, borne exclusive, limitée par l'instant d'évaluation. Chaque portion reste sans fin réelle
ni durée ; elle conserve son identité originale et la borne possible entière. Un lundi sans pointage local porte
ainsi le conflit commencé dimanche, même si une régularisation étend sa plage au-delà d'une semaine.

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
Les fins recevables, transitions, corrections, annulations et clôtures sont relues selon l'interprétation
d'atelier, sans fermeture à l'heure de lecture. Une régularisation peut établir plus de treize heures.

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

### Valeurs à résoudre

Les durées et montants portent une complétude distincte : travail, non conformité, machine et main d'œuvre.
Une catégorie certaine garde sa valeur si l'autre est incertaine. Chaque total complet porte son chiffre,
zéro compris ; chaque total incomplet est dépourvu de chiffre, même quand une partie est certaine.

Une activité à résoudre rend ses valeurs propres concernées inconnues. Pour les autres activités terminées,
le partage humain dépend de toute la plage possible factuelle `[debut, finAuPlusTard)`, bornée à l'évaluation,
sans chercher une portion commune aux chronologies. La machine et le temps certains restent connus.
Si le poste incertain est déjà occupé avec certitude, il ne change pas le nombre de postes distincts :
aucune incertitude humaine n'en découle. Le zéro d'un taux absent reste indépendant du diviseur.

`conflits` expose les séquences de l'élément et toutes les séquences responsables d'une valeur incomplète,
y compris celles d'autres éléments. Les activités gardent leur identité originale, les pointages leurs
identités actives. Un conflit sans activité à résoudre reste visible mais ne rend pas les montants inconnus.
Les annulations et corrections d'atelier recalculent ces dépendances et rétablissent la complétude.

### Lecture et accès

Le coût lit les projections d'activités d'atelier par ses entités JPA `@Immutable`, sans import entre
contextes et sans repli concurrent du journal. Les tarifs sont ceux du fait ouvrant actif figés à la saisie,
jamais ceux du référentiel courant. L'identité d'origine de l'activité reste stable après correction.
L'occupation est sélectionnée par recouvrement, même commencée avant la période valorisée : une
régularisation peut dépasser treize heures, donc aucune borne basse fixe sur le début n'est sûre.

L'horloge est relevée une seule fois par rapport et cet instant est rendu dans `evaluation`.
L'instant gouverne l'expiration et borne les plages possibles à résoudre ; les faits connus restent lus, même postérieurs. Cette route ne prend
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
sein. Une régularisation peut établir une fin supérieure à 13 h, voire à une semaine : aucune borne basse fixe
sur le début ne les retrouve toutes. La synthèse reçoit `evaluation` facultatif et rend l'instant effectivement
utilisé pour l'expiration, le découpage des activités en cours et les plages possibles à résoudre. Sans paramètre, l'heure du serveur est relevée
une seule fois. Le client transmet le même instant aux deux lectures pour composer le relevé.
Les faits connus restent interprétés, même postérieurs à cet instant. L'instant commun assure la même décision
d'expiration ; une écriture entre les appels peut changer les faits lus, sans instantané commun garanti.
Un instant passé est accepté. La borne
future est l'heure du serveur plus deux minutes, incluse ; elle est vérifiée avec un seul relevé d'horloge.
Un dépassement ou un instant fourni vide ou mal formé répond 400, sans rapport.

Une fin réelle conserve sa borne, même régularisée au-delà de l'échéance. Sans elle, l'activité ne produit aucune
durée avant son échéance et compte dès celle-ci jusqu'à sa fin automatique. Les décisions de relance, transition,
fin tardive, régularisation, correction, annulation et clôture sont celles d'atelier, projetées en base.
Une activité à résoudre reste sans fin et n'est jamais réinterprétée par la synthèse. Sa plage possible,
issue des faits d'atelier, atteint chaque jour jusqu'à la première borne entre `finAuPlusTard` et `evaluation`,
fin exclusive, même sans pointage local. Une clôture peut la limiter mais jamais la prolonger.

La synthèse rend les séquences en conflit dont une activité ou un pointage est rendu dans la semaine,
avec les identités des activités et des pointages concernés, même sans activité ni poste. Elle relit la projection
d'atelier, sans interprétation concurrente. Une séquence sans activité à résoudre laisse les totaux complets.
Une correction ou annulation retire le conflit dès que les faits redeviennent cohérents.

Les durées se **cumulent par élément** : deux éléments simultanés de 08 h à 09 h portent chacun une heure,
le jour et la semaine deux heures. La NC est comprise une seule fois dans la durée totale et exposée aussi à part.
Chaque total porte `complete` et, seulement s'il est complet, sa `valeur`. Un total journalier, d'élément ou
hebdomadaire dépendant d'une activité à résoudre reste incomplet **sans aucun chiffre**, même si une activité
certaine de 2 h existe à côté. La complétude de la part de NC dépend seulement des NC : un conflit de travail
laisse une NC certaine chiffrée. Les jours et éléments indépendants restent chiffrés. Quand ils sont complets,
les sommes des jours et des éléments sont égales à la durée opérationnelle de la semaine. Une activité en cours
conserve son élément sur chaque jour atteint, même sans pointage dans la semaine, sans durée comptabilisée
et sans rendre un total incomplet.

Les éléments rendus portent une activité ou un pointage dans la semaine, par première apparition puis nom.
Un réengagement reste le même élément. Le nom et la catégorie viennent du suivi ; référence et description sont
relues au référentiel, et peuvent être absentes. Les couples poste/nature suivent leur première apparition,
activité et pointage confondus ; l'absence de poste ou de nature est nominale.

Le **journal brut** est lu indépendamment des activités : tous les pointages actifs de l'opérateur datés de la
semaine sont rendus, même sans activité interprétable, avec leur identité, intention et cible éventuelle. Leur ordre est l'heure métier, puis l'intention
(fin, transition, ouverture), puis l'identité du pointage. L'heure d'enregistrement ne départage jamais.
Chaque élément et chaque poste nommés au journal trouvent leur fiche ou leur libellé dans la synthèse.

### La lecture passe par la base, jamais par un import

La synthèse déclare ses propres entités JPA `@Immutable` sur `activite_d_atelier`, `suivi_d_atelier`,
`evenement_d_atelier`, `sequence_en_conflit`, `pointage_en_conflit` et les référentiels. Elle n'importe aucun contexte métier voisin, ne rejoue aucun journal
et ne possède aucune table. Les activités sont lues en une requête, le journal en deux requêtes groupées ;
les conflits et leurs activités en deux requêtes groupées par opérateur, puis filtrés sur les identités rendues.
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
au pupitre, `identite_evenement_atelier` le réserve, un rejeu strict rend 200 sans dupliquer. Le chemin de
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
nom d'atelier, référence, type, état, activités en cours, conflits —, et `genereLe`. Un opérateur sans activité
reste rendu avec toutes ses habilitations.

La liste des opérateurs dépend du référentiel et des habilitations, indépendamment des pointages.
Les activités et les conflits viennent des projections d'atelier ; les opérateurs restent désignables sans activité.

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
L'échéance est inclusive : à début plus 13 h pile, l'activité disparaît de la liste. La correction d'un début
déplace cette échéance et conserve l'identité de l'activité, rendue dans `ouverture` ; le pupitre peut donc viser
la même activité après cette correction. Le suivi est `EN_COURS` si l'une de ces activités l'est à `genereLe`,
sinon `INTERROMPU` s'il porte un événement actif, sinon `EN_ATTENTE`.

Chaque suivi rend aussi `conflits` : le couple opérateur/poste, les identités stables des activités à résoudre
et les identités des pointages, dans leur ordre métier. Cette liste vient de `sequence_en_conflit` et
`pointage_en_conflit`, sans repli local. Une séquence sans activité reste rendue ; un poste absent reste absent.
Un rejeu ne la duplique pas, une résolution la retire à l’écriture suivante. Aucune de ces activités n’est
actionnable, mais une nouvelle ouverture cohérente peut être en cours à côté du conflit.

Les scénarios Cucumber écrivent par l'API d'atelier puis relisent par le référentiel, avec correction, annulation,
échéance et conflit : ils vérifient les colonnes réellement partagées entre les deux contextes.

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
   durables dans le journal local. Une contradiction conservée en conflit est une acceptation, distincte d’un refus.
3. **Le client Keycloak du pupitre n'existe pas dans le realm.** `glm-front` attend `pupitre_device`, avec le
   device grant activé et le client scope `glmproject` — sans lui, le jeton ne porte pas de claim `tenant` et toute
   la surface `/api/**` répond 403. C'est la dernière pièce d'infrastructure avant qu'un pupitre déployé puisse
   s'enrôler.
