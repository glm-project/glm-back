# Contexte métier

Décrit les bounded contexts métier du projet et leur rôle. Les règles de code (architecture, DDD, tests, conventions) sont dans `glm-back/AGENTS.md`.

Toutes les données métier sont isolées par entreprise cliente : chaque entreprise a son propre schéma PostgreSQL, désigné par le claim `tenant` du token. Aucun agrégat ne porte donc d'identifiant d'entreprise — l'isolation est assurée par l'infrastructure, décrite dans [multitenancy.md](multitenancy.md).

**L'application s'adresse à un maximum d'entreprises clientes.** GLM sert de trame, pas de spécification : aucun concept propre à son métier n'est obligatoire dans le modèle. Les notions qu'une autre entreprise pourrait ne pas avoir — une référence externe, un poste de travail, une fonction d'usinage — sont toutes facultatives, et leur absence redonne un comportement cohérent plutôt qu'un cas dégradé.

## elementdefabrication

Gère les éléments de fabrication. Un `ElementDeFabrication` porte son `TypeDElementDeFabrication` — ordre de fabrication ou produit — comme une valeur : les deux ne diffèrent aujourd'hui que par ce type, par leur préfixe de nommage et par leur série de numérotation. Le nom est toujours produit par le domaine à la création, par numérotation automatique propre au type et à l'année — l'API ne le fournit jamais.

« Produit » est un terme volontairement générique : l'application s'adresse à plusieurs entreprises clientes, dont les métiers nomment différemment ce qu'elles fabriquent (des moules, chez le client de référence).

La `Fiche` porte ce que l'utilisateur peut renseigner et réviser. Ses deux champs sont **facultatifs** : un élément de fabrication se réduit légitimement à son seul numéro. La `Reference` est l'identifiant que l'entreprise donne elle-même à l'élément dans son propre système (numéro de moule, référence de plan) ; elle est **unique par entreprise** quand elle est renseignée, l'unicité étant portée par une contrainte du schéma du tenant. PostgreSQL considérant les `NULL` comme distincts, autant d'éléments que nécessaire peuvent rester sans référence, et deux entreprises peuvent employer la même.

La garde d'unicité vit dans `ElementsDeFabricationService`, qui lit le détenteur d'une référence par le port `ElementDeFabricationRepository.idPourReference` et lève `ReferenceDejaUtiliseeException` (409). La contrainte en base est le filet de dernier recours : deux créations strictement concurrentes de la même référence produiraient un 500 plutôt qu'un 409, cas assumé puisque la création est le fait du dirigeant ou de son assistante.

La création et la modification passent par des commandes d'action (`ElementDeFabricationToCreate`, `ElementDeFabricationToUpdate`) construites par l'adapter primaire et orchestrées par `ElementsDeFabricationService`, service de domaine pur qui porte les ports repository, compteur, préfixes et horloge.

Le repository et le compteur sont persistés en PostgreSQL, dans le schéma de l'entreprise courante : la numérotation repart donc de 1 pour chaque entreprise, et deux entreprises peuvent porter le même nom d'élément. Les préfixes restent en dur dans `InMemoryPrefixesDElementsDeFabrication`, donc communs à toutes les entreprises.

### Points ouverts

1. **Relation produit ↔ ordre de fabrication.** Le client décrit un enchaînement (« ce moule neuf, une fois testé, s'il y a une opération à faire dessus, ça se transforme en OF ») mais ne demande jamais le lien, et l'imposer exclurait les modifications sur des produits antérieurs à l'application. Le jour où ce lien sera ajouté, il ne concernera que les ordres de fabrication : les deux types cesseront de ne différer que par leur valeur, ce qui rouvrira la question de scinder l'agrégat unique — aujourd'hui justifié précisément parce qu'ils ne diffèrent par aucun champ.
2. **Suppression.** Le client ne parle jamais de supprimer, seulement de clôturer. Dès que les temps seront saisis, la suppression d'un élément qui en porte devra être interdite : elle détruirait des heures de paie.
3. **Numérotation et préfixes.** Les préfixes sont figés pour toutes les entreprises, ce qui contredit la cible multi-clients. L'année et le reset annuel du format `PRD-2026-000001` n'ont par ailleurs aucune source client, alors que des ordres de fabrication durant plusieurs mois traversent les millésimes.
4. **Critère de lecture.** `ElementDeFabricationCriteria` ne filtre que par période de création et la liste est paginée. Aucun écran décrit par le client ne filtre ainsi ; le seul critère cité est « actifs seulement ». Côté atelier, ce point est traité — la période y est devenue facultative.

## atelier

Gère l'exécution en atelier de ce que `elementdefabrication` a déclaré. Le gestionnaire y met un élément en atelier, les opérateurs y pointent leur présence et leur travail, le gestionnaire clôture et corrige.

Le contexte porte **deux agrégats** : la `JourneeDeTravail` d'un opérateur et le `SuiviDAtelier` d'un élément engagé. Ils partagent un même langage — opérateur, auteur, horodatage, annulation, régularisation. Le temps réellement passé sur un élément, lui, ne se lit que dans son journal : aucune présence ne le borne. Les séparer en deux contextes obligerait à dupliquer ces value objects, que le shared kernel ne peut pas accueillir puisqu'il est en anglais.

**Le journal d'événements est la source de vérité.** L'état d'un agrégat et ses intervalles de temps ne sont jamais stockés : ils se déduisent du repli du journal, trié par date de survenue. C'est la correction qui l'impose — un temps juste exige que la saisie oubliée compte à l'heure où elle a eu lieu, pas à l'heure où on la rattrape, et un modèle à compteurs ne sait pas revenir en arrière. Chaque événement porte donc un `Horodatage` bitemporel : sa date de survenue, métier, et sa date d'enregistrement, technique.

**Une régularisation est un acte du gestionnaire, conservé sur le fait, pas un écart de dates ni une identité d'auteur.** Chaque événement du journal d'un élément porte son origine : `POINTAGE` pour tout ce qui passe par la route des pointages, quels que soient le rôle de celui qui pointe et l'heure de geste fournie, `REGULARISATION` pour une régularisation ou le remplaçant d'une correction, même saisis à l'heure du fait. `estUneRegularisation()` lit cette origine. L'écart entre les deux dates ne suffit pas : un pupitre resté hors ligne rejoue ses pointages après coup sans que le gestionnaire soit intervenu. Cet écart reste la lecture d'une saisie différée, et la présence y reconnaît encore sa régularisation. Le booléen jumeau `estSaisiParUnTiers` a été retiré avec le passage à l'identifiant : l'`Auteur` vient du jeton et l'opérateur du référentiel, et rien ne relie encore les deux — le comparer n'aurait plus produit qu'une réponse toujours vraie. Il reviendra avec le lot « utilisateur connecté ».

### La présence, de l'arrivée au départ

Le client décrit son besoin comme « une pointeuse à laquelle on rajoute une option OF », et il a corrigé explicitement l'équipe sur ce point : les heures de présence courent de l'arrivée dans la société au départ, **jamais du premier au dernier élément travaillé**. L'objectif du produit reste de savoir qui travaille sur quoi et combien un OF a coûté en temps de travail : **la présence ne sert pas à payer**.

`JourneeDeTravail` porte donc son propre journal, d'`ARRIVEE` et de `DEPART`. Ses bornes sont l'arrivée et le départ, **pas le jour calendaire** : aucun fuseau horaire n'entre dans le domaine, et une équipe de nuit ou un retour en soirée ouvre simplement une seconde journée. Elle expose deux mesures, que l'absence de pause rend égales pour une journée d'une seule venue :

- `amplitude()` — de l'arrivée au départ ;
- `fenetres()` — les périodes de présence, une par venue.

**La pause n'est pas un fait de présence** ([ADR 0002](adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md)) : un opérateur en pause reste présent, et le relevé de présence compte sa pause.

**Une journée sans départ est abandonnée** quand son amplitude depuis l'arrivée dépasse le seuil paramétré par l'entreprise (contexte `parametrage`, 13 h par défaut) — strictement : un geste au seuil pile reste dans la journée. Aucune règle calendaire ne peut fermer une journée, puisqu'un poste de nuit court de 20 h à 8 h ; le seuil, lui, reste sous 24 h, ce qui garantit qu'un retour le lendemain à la même heure soit une nouvelle arrivée. L'abandon se juge sur l'heure du geste, jamais sur sa réception : un pupitre hors ligne qui rejoue un geste de la veille le voit rangé dans la bonne journée.

- **Une arrivée sous le seuil est absorbée** : l'opérateur est déjà là, rien n'est ajouté, la journée en cours est rendue comme un rejeu. C'est ce qui permet à l'opérateur de nuit de se réidentifier à 3 h.
- **Un geste reçu pour une journée abandonnée en ouvre une nouvelle** : une arrivée implicite à l'heure du geste, sous un identifiant du serveur réservé comme celui d'une régularisation, puis le geste lui-même. Une arrivée égarée sur la route des pointages s'y réduit à l'arrivée implicite ; un départ tardif donne une journée de durée nulle. La journée abandonnée reste telle quelle, sans départ, à régulariser.
- **Deux journées d'un même opérateur ne se chevauchent jamais.** L'étendue d'une journée va de son premier à son dernier fait connu ; une régularisation ou une correction qui ferait toucher deux étendues est refusée au gestionnaire (`chevauchement-de-journees`). Les gestes de l'opérateur ne sont jamais refusés pour cette raison. La recherche passe par la projection `dernier_fait`, écrite comme `debut` et `fin`.
- À instant égal, dans le journal de présence, **l'arrivée passe devant** : l'arrivée implicite partage l'heure du geste qu'elle précède.

**La liste des anomalies** (`GET /api/atelier/anomalies`, gestionnaire seul) signale deux types de journées, jugées à l'instant de lecture avec le seuil courant et jamais stockées : `JOURNEE_SANS_DEPART`, abandonnée au-delà du seuil, et `AMPLITUDE_EXCESSIVE`, fermée au-delà du seuil. La plus récente d'abord, filtrable par opérateur et par type. Une ligne disparaît dès que la régularisation la résout ; changer le seuil fait apparaître ou disparaître des lignes sans migration. La requête s'appuie sur les projections `etat`, `debut` et `amplitude_microsecondes` — à la précision des horodatages de la base, pour qu'un dépassement d'une demi-seconde reste un dépassement.

Depuis le lot 3, une amplitude excessive ne naît plus d'un geste de l'opérateur : un départ pointé au-delà du seuil ouvre une nouvelle journée, et c'est la journée du matin, sans départ, qui est signalée. Elle ne vient que d'un acte du gestionnaire — un départ régularisé ou corrigé tard.

**Le départ est un fait de l'opérateur, écrit une seule fois ; la pause n'existe pas pour le serveur, le pupitre la traduit en fins d'activité.** Le départ n'est jamais recopié dans le journal des éléments, et il ne termine aucune activité : une activité oubliée se termine à son échéance, ou à la fin que le gestionnaire régularise. La pause, elle, se lit dans le journal de chaque élément : le client la décrit comme l'arrêt et la reprise du travail — « pause / arrêt / reprise sont le même mécanisme » —, et le pupitre y pointe une fin par activité en cours, puis un début, ou une non conformité, à la reprise. Le serveur ne reçoit que des fins et des débuts.

La **présence sans affectation** — le temps de présence sans élément rattaché — n'est pas un élément fictif : c'est le résidu de la présence moins le temps affecté, calculé à la lecture. La présence est comptée dès l'identification, que l'opérateur ait ou non pointé sur un élément.

**GLM n'est pas un concept du modèle.** C'est le nom que le client de référence donne à son travail non facturable — sur un projet interne, par exemple —, qu'il veut déclarer manuellement (« De toute façon il y aura ce bouton GLM »). Ce travail n'est pas encore modélisé, et la présence sans affectation n'en est pas : un opérateur présent sans activité pointée ne fait pas pour autant du travail non facturable. C'est le principe posé en tête de ce document : GLM sert de trame, pas de spécification.

### Le temps effectif, celui des seules activités

`TempsDAtelierService.tempsEffectif` rend les intervalles des activités d'un élément, tels que le journal les interprète (`SuiviDAtelier.intervalles`), à l'instant d'évaluation. **Aucune présence ne les borne** : un départ ne termine rien, et aucune fin de journée n'est présumée. Une activité se termine à sa fin réelle — un geste qui la termine, ou la clôture —, sinon automatiquement à son échéance, avec une anomalie (voir ci-dessous).

La pause de midi scinde le travail par le journal de l'élément : une fin à midi, un début à la reprise. Corriger une heure de pause fausse demande donc une correction par activité, sur sa fin et sur son début.

Un travail jamais arrêté ne court pas pour autant jusqu'au lendemain : il se termine à son échéance, et l'opérateur qui reclique sur l'élément à son retour ouvre une nouvelle activité, sans prolonger l'ancienne. Une fin oubliée se rattrape par une régularisation du gestionnaire, qui remplace la fin automatique.

### La fin automatique à l'échéance

Une activité encore en cours ne compte rien. Oubliée, elle ne court pas pour autant indéfiniment : **son échéance est son début plus 13 heures écoulées** (`Echeance`), jamais 13 heures d'horloge murale — le passage à l'heure d'été ou d'hiver ne l'allonge ni ne la raccourcit. Une activité que rien n'a terminée avant son échéance est **terminée automatiquement** à cet instant, et porte une **anomalie**. Travail à 8 h sans aucune fin : lu à 20 h 59, il est en cours ; lu à 21 h, ou le lendemain, il est terminé à 21 h. Le délai est la règle de l'atelier, pas un paramètre de l'entreprise.

**Rien n'est écrit.** Ni événement de clôture automatique ni traitement planifié : l'interprétation du journal donne des activités qui ne dépendent que des faits, avec leur fin réelle quand un geste ou la clôture les a terminées. Seule leur lecture, à un **instant d'évaluation** explicite, décide si une activité sans fin réelle est encore en cours ou déjà terminée automatiquement. Cet instant vient de l'horloge du service applicatif. La première lecture après une indisponibilité retrouve donc la même borne, sans rattrapage.

**La même échéance gouverne l'interprétation**, sans instant de lecture, sur les seules heures métier :

- un geste pointé **au plus tard à l'échéance** de l'activité qu'il vise la termine à son heure, même reçu après elle : une fin pointée à 17 h et reçue le lendemain donne 9 h, et retire l'anomalie. Un geste pile à l'échéance l'emporte sur la fin automatique ;
- une fin pointée **après l'échéance** est conservée sans effet : l'activité garde sa borne de 21 h et son anomalie, sans que la séquence soit en conflit ;
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

Ces trois actes existent sur les deux agrégats : une heure d'arrivée fausse se corrige comme un début de travail faux.

Un journal ne se réécrit pas : l'événement erroné reste, porteur d'une `Annulation` qui trace qui a corrigé, quand et pourquoi. Le repli écarte les annulés, puis déroule l'automate.

`corrige` n'est pas la composition des deux autres, et c'est le point non évident : le remplaçant d'un début corrigé garde l'activité qu'il ouvrait, et les gestes qui la visaient y restent rattachés. Annuler ce début puis régulariser sa version corrigée ouvrirait une autre activité, et laisserait en conflit la fin qui visait la première.

Ce sont aussi les actes qui **résolvent une séquence en conflit** : annuler la transition erronée, ou corriger la fin qui visait l'activité remplacée pour qu'elle termine sa remplaçante. Aucun n'est refusé parce qu'il crée ou laisse une contradiction ; une résolution en plusieurs actes traverse donc des états intermédiaires en conflit, et le recalcul retire le conflit dès que les faits redeviennent cohérents.

**La clôture ne fige rien pour le gestionnaire.** Elle ferme le pointage aux opérateurs ; régularisation, annulation et correction restent admises, et la clôture elle-même se déplace ou s'annule. Le seul invariant qui subsiste est de cohérence, pas de permission : aucun événement daté après la clôture.

### Le temps réparti

Deux mesures coexisteront, qui ne s'additionnent pas de la même façon :

- **temps effectif** — la durée réelle passée sur un élément, telle que la produit `TempsDAtelierService`. Deux postes pendant 1 h font 2 h effectives.
- **temps réparti** — la même heure d'opérateur divisée par le **nombre de postes de travail** qu'il occupait simultanément, tous éléments confondus. Il ne servira qu'au coût de revient.

Une troisième lecture en dérive sans rien ajouter au journal : le **temps opérationnel** de `syntheseheures`, qui
s'additionne comme l'effectif mais écarte un début hors de toute journée et se coupe à minuit (voir la section
`syntheseheures`).

Le diviseur est bien le nombre de postes, et non le nombre d'activités ou d'éléments : le client énonce la règle deux fois de suite — coût horaire de chaque machine active non divisé, taux horaire de l'opérateur divisé par le nombre de machines qu'il utilise. Un opérateur sur trois éléments avec une seule machine n'est donc pas divisé.

**Le journal n'enregistre que l'effectif.** Le réparti traverse les agrégats — un nouveau pointage sur un second élément change la part déjà attribuée sur le premier —, il ne peut donc être qu'une fonction de projection, calculée à la lecture. `TempsDAtelierService` produit pour cela des intervalles complets, et pas seulement l'état courant dont l'écran a besoin aujourd'hui : c'est la couture sur laquelle les tableaux de bord se brancheront.

### Frontière avec elementdefabrication

`elementdefabrication` étant annoté `@BusinessContext`, l'atelier ne l'importe jamais. Il déclare sa propre identité, `ElementEngage`, dont le nom et le type sont **copiés à l'engagement** : c'est ce qu'affiche l'écran d'atelier, et un élément renommé plus tard ne doit pas réécrire l'histoire de l'atelier. Le port `ElementsEngageables` porte cette frontière.

**Mettre un élément en atelier est un geste métier explicite du back-office**, distinct de sa création : tout ce qui est créé n'est pas forcément à faire, et c'est cet acte qui fait apparaître l'élément sur l'écran des opérateurs. C'est aussi ce qui donne un sens à l'invariant « aucun événement antérieur à l'engagement ».

L'écran des opérateurs veut tous les éléments actifs d'un coup, sans rien qui défile et sans notion de date : la période de `SuiviDAtelierCriteria` est donc **facultative**, et ne sert qu'aux écrans de back-office.

Les quatre couches existent désormais, et les deux agrégats sont persistés en PostgreSQL, dans le schéma de l'entreprise courante. Chacun occupe deux tables : la ligne de l'agrégat et son journal ; le suivi y ajoute la projection de ses activités. Le journal restant la seule source de vérité, l'état n'est jamais stocké _comme état_ — mais des **projections** sont écrites à chaque écriture et jamais relues pour reconstruire l'agrégat : `etat`, `debut` et `fin` pour la journée, la table `activite_d_atelier` pour le suivi. Celle-ci ne porte que ce qui ne dépend pas de l'instant — début, échéance, fin réelle — et le filtre des états du tableau d'atelier la juge à l'instant de la lecture. Sans elles, filtrer l'écran d'atelier sur les états ou retrouver la journée contenant un instant obligerait à ramener toute l'entreprise en mémoire.

L'atelier projette aussi ses séquences en conflit, pour que les lecteurs les retrouvent après un redémarrage :
`sequence_en_conflit` porte le suivi et le couple opérateur/poste, `pointage_en_conflit` les identités ordonnées de
ses faits actifs, et `activite_d_atelier` le rattachement ordonné de ses activités à résoudre. Une séquence peut
n'avoir aucune activité, notamment après l'annulation d'un ouvrant encore visé. Ces tables sont écrites uniquement
par atelier, depuis `SuiviDAtelier.conflits()` ; elles sont rapprochées à chaque écriture et disparaissent après
résolution. Elles ne servent jamais à reconstituer le suivi, qui rejoue son journal.

Le modèle est relationnel plutôt qu'un journal sérialisé en `jsonb`, parce que les projections à venir — coût de revient, paie, synthèses — filtrent et groupent sur des attributs d'**événement** à travers tous les agrégats : un index les sert directement, là où un document devrait être désérialisé en entier pour être presque tout jeté. Les index `(operateur, date_de_survenue)` et `(poste, date_de_survenue)` sont posés dès maintenant à cette fin, et un contexte lecteur n'aura qu'à poser dessus une entité en lecture seule, comme `atelier` le fait déjà sur `element_de_fabrication`.

`ElementsEngageables` lit la table `element_de_fabrication` par une entité en lecture seule propre à l'atelier, sans jamais importer le contexte voisin. `OperateursConnus`, `PostesConnus` et `Habilitations` font de même sur `operateur`, `poste_de_travail` et `operateur_poste`.

L'API est décrite par OpenAPI (`/swagger-ui.html`) et par [atelier-api.md](atelier-api.md), qui porte ce que la spec ne peut pas dire.

### Points ouverts

1. **Régulariser après une dé-habilitation est refusé.** L'habilitation étant vérifiée sur les trois écritures du journal, un gestionnaire ne peut plus rattraper une saisie oubliée sur un poste dont l'opérateur a été retiré depuis. Le cas est assumé pour ce lot — il ferme la porte au contournement —, mais il laisserait un trou dans la paie s'il se produisait : à rouvrir si le client le rencontre.
2. **Quelle mesure alimente la paie ? Fermé le 28/09/2026** : aucune, la présence ne sert pas à payer ([ADR 0002](adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md)). `amplitude()` et `fenetres()` restent exposées tant que la présence existe ; sa suppression est un chantier suivant.
3. **Le coût de revient monétaire est sorti du contexte**, comme prévu : il vit dans `coutderevient`, qui lit le journal d'`atelier` par port en lecture seule. `atelier` continue de ne rien calculer — il copie le coût horaire du poste et le taux horaire de l'opérateur sur l'événement, et s'arrête là. Reste ouvert le **coût par période** (par opérateur, par poste, par mois), qui n'a pas de demande client formulée, et l'objection de Nicolas sur la division du taux humain : le modèle retient le verbatim client — taux divisé par le nombre de postes, coût machine jamais divisé —, elle n'a jamais été reprise en réunion.
4. **Le bouton de pause global n'a jamais été validé de première main** — point **déplacé au pupitre**. Il ne vient que de la réunion d'équipe ; dans la réunion client, la pause est décrite au singulier, sur un seul élément. Le serveur ne connaissant plus la pause ([ADR 0002](adr/0002-let-the-pupitre-turn-a-pause-into-activity-stops.md)), c'est le pupitre qui porte le bouton global et la question : voir l'ADR du front et l'`AGENTS.md` du contexte `atelier` du pupitre.
5. **La déclaration du travail non facturable.** Le client veut son bouton GLM, placé en bas de l'écran, pour déclarer à la main le travail qu'il ne facture pas. Rien ne le modélise encore : le mode de déclaration, le rattachement éventuel à un projet interne et la coexistence avec d'autres activités feront l'objet d'une spec à part. La présence sans affectation n'y répond pas : ce n'est pas un travail déclaré.
6. **Le cycle de vie de l'élément lui-même.** La clôture existe côté atelier, sur le suivi. Reste à trancher si l'élément de fabrication porte en propre un statut, ou si son activité se lit entièrement par la présence ou l'absence d'un suivi non clôturé.
7. **Aucune garde d'unicité en base** sur « un seul suivi non clôturé par élément », contrairement à ce que `elementdefabrication` fait pour la `Reference`. La règle vit dans le service, mais une contrainte partielle transformerait en 500 un état que le domaine admet aujourd'hui : rouvrir la clôture d'un suivi dont l'élément a été réengagé depuis. À trancher côté domaine avant de poser la contrainte. « Une seule journée ouverte par opérateur » n'est plus une règle : une journée abandonnée reste sans départ pendant que la suivante est ouverte.
8. **L'écriture du journal rapproche par identifiant**, ce qui coûte une lecture indexée de la collection à chaque pointage. Si un journal devenait assez long pour que cette lecture pèse, la sortie est un upsert natif gardé (`on conflict (id) do update ... where ... is distinct from ...`), qui épargne à PostgreSQL toute version de tuple sur les lignes inchangées — au prix d'une scission permanente entre lecture JPA et écriture JDBC.
9. **Départ oublié, poste de nuit, pointages jamais refusés.** Une journée sans départ n'a aujourd'hui pas de borne haute : elle absorbe la nuit, bloque l'arrivée du lendemain et fausse le coût de revient. Les décisions — amplitude maximale paramétrable, journée abandonnée, fin présumée, relance d'un OF, aucun pointage d'opérateur refusé sauf sur un OF clôturé — et leur découpage en huit lots sont dans [strategie/bornes-de-fin-de-journee.md](strategie/bornes-de-fin-de-journee.md). Elles touchent aussi `feuilledetemps`, `syntheseheures`, `coutderevient` et `pupitre`. Lots livrés : 1, la relance d'une activité en cours ; 2, le paramétrage de l'amplitude maximale (contexte `parametrage`) ; 3, la journée abandonnée, l'arrivée absorbée et le refus du chevauchement ; 4, la fin présumée dans le temps effectif et le coût de revient ; 5, les heures pointées et présumées des relevés ; 6, la liste des anomalies ; 8a, les pointages absorbés (geste redondant, arrêt sans effet, OF clôturé), l'arrivée implicite d'un geste sans journée et le rejeu d'une saisie concurrente. Les lots 8b (pointages signalés) et 8c (mise en attente) sont abandonnés : habilitation retirée, date antérieure à l'engagement ou future, référentiel inconnu, geste hors séquence et identifiant réutilisé restent des refus.

## postedetravail

Gère le **référentiel de ce sur quoi les opérateurs pointent**. Un `PosteDeTravail` porte un `Libelle` et une `NatureDeTravail` : « Tour 1 » sert à tourner, « Poste de soudure » à souder. Il porte aussi, facultativement, un `CoutHoraire` : le contexte se contente de le stocker et de le restituer, le calcul du coût de revient restant un lot à part. `atelier` le lit désormais, mais seulement pour le copier sur chaque événement du journal au moment de la saisie — sans jamais le recalculer ni le combiner. C'est la capture, pas la valorisation.

Le terme reste volontairement générique, comme dans l'atelier : une machine chez le client de référence, un établi, un four, une salle ailleurs.

**Le libellé est unique par entreprise.** C'est ce qui fait du référentiel un référentiel : sans lui, rien ne relierait le « Tour 1 » saisi par Dupont au « Tour 1 » saisi par Martin. La garde vit dans `PostesDeTravailService`, sur le patron de `ElementsDeFabricationService.verifierReferenceLibre` ; la contrainte du schéma est le filet de dernier recours.

**La nature est obligatoire ici**, alors qu'elle reste facultative dans l'atelier. Ce n'est pas une contradiction : l'atelier doit fonctionner pour une entreprise sans parc machine ni métiers distincts, qui n'ouvrira simplement pas cet écran. Mais un poste qui serait déclaré sans dire quel travail s'y fait ne servirait à rien — c'est précisément ce que ce contexte apporte.

**Un poste encore habilité ne se supprime pas** : cela laisserait des opérateurs pointer sur du vide. La règle vit dans le domaine, derrière le port `PostesEnUsage`, dont l'adapter lit la table `operateur_poste` par une entité en lecture seule — sans jamais importer `operateur`, annoté `@BusinessContext`.

**Un poste sur lequel du temps a été pointé ne se supprime plus du tout**, et ce second refus est définitif là où le premier se lève en retirant l'habilitation : le journal d'atelier ne retenant que l'identifiant du poste, sa disparition laisserait des heures de travail sans machine. Le port `PostesPointes` lit `evenement_d_atelier` de la même façon.

## operateur

Gère le **référentiel des personnes qui pointent**. Un `Operateur` porte son nom, son prénom, un matricule facultatif, un taux horaire facultatif, et l'ensemble des postes sur lesquels il est habilité.

### Le métier vient du poste, jamais de la personne

C'est le point décisif de ces deux contextes, et il est fondé sur ce que le client décrit.

Un opérateur polyvalent — soudeur **et** tourneur — déclenche **deux démarrages** sur le pupitre : un sur le poste de soudure, l'autre sur le tour. Deux temps courent alors en parallèle, et chacun sait de quel métier il relève parce que le poste le dit. C'est exactement ce que l'atelier modélise déjà : sa `CleDActivite` est le couple (opérateur, poste), et son javadoc énonce que « c'est le poste, et non la nature de l'opération, qui distingue deux activités menées de front ». Le verbatim client le confirme sur un cas voisin : deux pièces du même OF sur deux machines différentes.

Il s'ensuit que **la nature appartient au poste**. Déclarer un métier sur la personne stockerait la même information deux fois, avec la possibilité qu'elles se contredisent — un tourneur habilité sur une fraiseuse. Les métiers d'un opérateur se **déduisent** donc de ses postes : `ProfilDOperateur.natures()` rend les natures de ses habilitations, triées. Personne ne les saisit.

La phrase du client « la machine est liée à l'opérateur, et l'opérateur a la fonction » dit **où se saisit** le paramétrage — sur la ligne de l'opérateur, on liste ses postes —, pas d'où la nature se déduit au moment du pointage.

**Un opérateur qui a pointé ne se supprime pas**, sur un élément comme en présence : le journal d'atelier et les journées de travail ne retiennent que son identifiant, et sa disparition laisserait des heures sans personne à payer. Le port `OperateursQuiOntPointe` lit les deux tables de l'atelier par des entités en lecture seule.

### Identité et matricule

L'identité (nom, prénom) est **unique par entreprise**. Le **matricule** est l'identifiant que l'entreprise donne elle-même à ses collaborateurs : **facultatif**, car toutes n'en attribuent pas, et **unique dès qu'il est renseigné** — patron exact de `elementdefabrication.Reference`, `NULL` distincts compris, donc autant d'opérateurs sans matricule que nécessaire.

### Frontière avec postedetravail

`postedetravail` étant annoté `@BusinessContext`, ce contexte ne l'importe jamais : il déclare ses propres `PosteHabilitableId`, `LibelleDePoste` et `NatureDeTravail`, et lit la table voisine par une entité en lecture seule, sur le patron d'`ElementEngageableEntity`.

**Rien n'est copié**, à la différence de l'atelier qui copie nom et type à l'engagement. La raison est symétrique : l'atelier copie parce qu'un élément renommé ne doit pas réécrire son histoire, alors qu'ici aucun historique ne pend à un poste — un poste renommé doit s'afficher renommé partout. L'opérateur ne stocke donc que l'identifiant, et le port `PostesHabilitables` n'expose que `parIds`, pour qu'une page entière se résolve en une requête.

### Points ouverts

1. **Les gestionnaires ne sont pas déclarés.** Leur fiche n'aurait aucun usage tant que l'authentification n'est pas tranchée : l'`Auteur` d'une saisie vient du jeton, pas d'un référentiel. À rouvrir avec ce sujet.
2. **Aucun plafond sur le nombre de postes par personne**, alors que le client énonce « maximum 4 machines par personne ». Une donnée de paramétrage ne s'écrit pas en constante du domaine, et GLM est une trame : une autre entreprise en habilitera six. Si le plafond doit être tenu, il viendra d'un port.
3. **Montants.** Coût horaire du poste et taux horaire de l'opérateur existent sur les deux agrégats (facultatifs, strictement positifs), `atelier` les copie sur chaque événement du journal au moment de la saisie, et `coutderevient` les valorise. La boucle est fermée : un opérateur sans taux horaire ne coûte rien en main d'œuvre, et le rapport ne l'invente pas.
4. **Utilisateur connecté.** Tranché sur le principe, dans [strategie/authentification-pointage.md](strategie/authentification-pointage.md) : le pupitre porte une **identité d'appareil** et aucune session humaine, l'opérateur est **identifié** au geste — par un code, qui désigne sans prouver, ou par une signature, qui prouve. L'`Auteur` du jeton cessant dès lors de désigner une personne, c'est la **qualité de l'identification** portée par l'événement qui vaudra pour la paie, et c'est elle qui rouvrira `estSaisiParUnTiers`. Restent ouverts le régime du code — ouvert à tous ou réservé à l'exception —, le matériel des pupitres, et la validation juridique de l'empreinte.

## feuilledetemps

Première **projection transverse** du projet : un contexte purement lecteur, qui ne possède aucune table et
recalcule tout à chaque appel. Il répond à une seule question — _qu'a fait cette personne cette semaine, jour par
jour_ — à partir des activités interprétées par `atelier`.

### Pourquoi il n'est pas dans atelier

`atelier` manipule des instants et s'interdit le calendrier : aucun `ZoneId` ni `LocalDate` n'entre dans ce
contexte. La feuille ramène les activités aux jours de l'entreprise et aux semaines ISO. Une équipe de nuit
compte sur deux jours, et une activité du dimanche peut recouvrir le lundi de la semaine suivante.

### La lecture passe par la base, jamais par un import

`atelier`, `operateur` et `postedetravail` étant annotés `@BusinessContext`, ce contexte déclare ses propres entités
JPA `@Immutable`. Il lit la projection `activite_d_atelier`, avec l'élément porté par le suivi, et l'identité de
l'opérateur dans le référentiel. Il ne rejoue aucun journal et ne propose aucune écriture.

Le filet est le scénario Cucumber : il écrit par l'API d'atelier puis lit la feuille. Relances, transitions
ciblées, fins reçues tardivement, régularisations, corrections, annulations et clôtures restituent
l'interprétation du propriétaire. Les règles ajoutées sont l'état à l'instant de lecture et le découpage calendaire.

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
activités en cours. Le même instant peut être transmis à la synthèse pour composer le relevé ; les faits connus
restent interprétés même postérieurs à cet instant, sans lecture historique ni transaction commune garantie.
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
Une activité à résoudre indique actuellement son jour de début ; son étendue calendaire sera complétée avec la
borne de conflit dans la tranche suivante du chantier.

La feuille nomme l'élément, jamais le suivi : un élément réengagé après clôture reste le même élément. Ni libellé
de poste ni fiche d'élément ici — la synthèse des heures les porte. Les activités sont triées par début de portion,
élément puis identité stable. Le contrat de la feuille ne porte aucune présence ni temps présumé.

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

Une ligne par nature d'opération — fraisage, tournage, érosion —, plus une ligne sans nature pour ce qui a été
pointé sans poste. **La nuit d'une journée abandonnée n'est jamais valorisée** : chaque venue sans départ au-delà du
seuil reçoit une fin présumée, calculée sur les pointages déjà lus pour le rapport. Une venue fermée
plus de 24 h après son arrivée la reçoit aussi ([D13](strategie/bornes-de-fin-de-journee.md), issue #59). La forme du
rapport ne change pas. Chaque ligne porte le temps de bon travail, le temps de reprise de non conformité **avec ses
périodes datées**, et le coût séparé en machine et main d'œuvre.

L'entrée se fait par l'**élément de fabrication**, non par son suivi d'atelier : un élément réengagé après clôture
additionne ses passages. Un élément connu mais jamais engagé rend un rapport vide, ce qui est une réponse et non une
erreur.

### Les deux règles de valorisation

Le client les énonce deux fois de suite, et elles ne se ressemblent pas :

- le **coût horaire de chaque poste actif court en entier**, même quand l'opérateur en mène plusieurs de front —
  deux machines pendant une heure coûtent deux heures de machine ;
- le **taux horaire de l'opérateur est divisé** par le nombre de postes qu'il occupait à cet instant, tous éléments
  confondus — une personne ne peut pas être payée deux fois la même heure.

Le diviseur compte donc des **postes**, jamais des éléments ni des activités. Un opérateur sur trois éléments avec
une seule machine n'est pas divisé ; un pointage sans poste compte pour un poste, ce qui redonne un diviseur de un à
une entreprise sans parc machine.

**Le découpage se fait aux bornes de tous les pointages de l'opérateur.** Un diviseur pris sur l'intervalle entier
serait faux dès que deux pointages ne commencent pas ensemble : sur une fraiseuse lancée à 9 h et un tour lancé à
10 h, seule l'heure commune se divise. C'est ce que `ChargeDeLOperateur` produit — des sous-périodes où le nombre de
postes occupés ne change pas.

### L'arrondi est une décision, pas un détail

Les tranches se somment à l'échelle de travail, la **ligne** arrondit au centime une seule fois, et le total du
rapport est la somme de lignes **déjà arrondies**. Sans cette discipline, l'écran afficherait un total qui n'est pas
la somme de ce qu'il montre — ce qu'aucun gestionnaire n'accepte d'un rapport de coût.

### Ce contexte porte une horloge

Comme les relevés depuis le lot 5 des bornes de fin de journée. Ici, un travail non terminé n'a pas de durée : le coût de revient d'un élément en
cours n'a de sens qu'arrêté à l'instant de la lecture. Deux appels espacés sur un élément en cours ne rendent donc
pas la même chose, et la description OpenAPI de la route le dit.

Le reste suit les règles d'`atelier` à la lettre : le temps brut est ramené aux fenêtres de présence de **la journée
où il a commencé**, si bien qu'un départ referme ce que personne n'a arrêté ; la pause de midi, elle, est pointée
dans le journal de l'élément, par une fin et un début. Un
début qui ne tombe dans aucune journée connue est **rendu intact** — c'est le choix d'`atelier`, et non celui de la
feuille de temps qui l'écarte : ici, l'anomalie doit rester chiffrée plutôt que disparaître du coût.

### La lecture passe par la base, jamais par un import

`atelier`, `elementdefabrication`, `operateur` et `postedetravail` étant annotés `@BusinessContext`, ce contexte
déclare ses propres entités JPA en lecture seule sur leurs tables. Il rejoue donc **sa propre** version du repli du
journal d'atelier, et — pour la troisième fois du projet — du repli de présence.

Ces deux replis sont **tolérants**, comme ceux de `syntheseheures` : un geste que l'automate refuse —
un départ sans arrivée, une fin sans activité en cours — est ignoré, et le rapport se calcule sur ce qui reste. Le
calcul du coût ne doit jamais répondre `500` (issue #54). À instant égal, l'arrivée passe devant : l'arrivée implicite
d'un geste tardif partage l'heure de ce geste, et la base rendait les deux dans l'ordre de leurs identifiants.

Cette duplication est assumée, pour la même raison que dans `feuilledetemps` : le partage passerait soit par un
import interdit, soit par le shared kernel, qui est en anglais. Le filet est le scénario Cucumber, qui pointe par
l'API d'`atelier` et relit par celle du coût de revient.

Aucun changelog n'a été nécessaire : les index `(operateur, date_de_survenue)` et `(poste, date_de_survenue)`
d'`evenement_d_atelier`, posés dès l'origine pour « les projections transverses des contextes à venir », servent
exactement ce lot.

### Le rapport est réservé au gestionnaire

Il dérive des taux horaires des opérateurs, que le pupitre n'a aucune raison de voir. `USER` reçoit 403.

### Points ouverts

1. **Le coût par période reste à faire** — par opérateur, par poste, par mois. La même couture le porterait, mais
   aucune demande client n'a été formulée.
2. **La lecture d'occupation n'a pas de borne basse.** Toute activité recouvrant l'élément ayant commencé avant sa
   fin, la borne haute suffit à la correction ; mais la requête reste peu sélective sur un opérateur au long
   historique. La sortie, si elle pèse, est une borne basse calculée sur la plus ancienne activité encore ouverte,
   ce qui suppose une colonne que le journal n'a pas.
3. **Aucune restriction sur quel élément un gestionnaire peut chiffrer**, faute de notion d'équipe ou de périmètre.
   Même point ouvert que sur la feuille de temps, à rouvrir avec le lot « utilisateur connecté ».
4. **Le rapport ne dit pas ce qui n'a pas de présence.** Un pointage dont le début ne tombe dans aucune journée est
   valorisé comme les autres, sans que rien ne le signale sur la ligne. L'anomalie reste visible sur
   `GET /api/atelier/suivis/{id}/temps-effectif` ; l'exposer ici demanderait un indicateur par ligne.

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
y compris entre deux semaines ; le changement d'heure conserve la durée réellement écoulée.

Les activités sont sélectionnées par **recouvrement**, même commencées avant la semaine et sans pointage en son
sein. Une régularisation peut établir une fin supérieure à 13 h, voire à une semaine : aucune borne basse fixe
sur le début ne les retrouve toutes. La synthèse reçoit `evaluation` facultatif et rend l'instant effectivement
utilisé pour l'expiration et le découpage des activités en cours. Sans paramètre, l'heure du serveur est relevée
une seule fois. Le client transmet le même instant aux deux lectures pour composer le relevé.
Les faits connus restent interprétés, même postérieurs à cet instant ; le contrat ne garantit ni lecture
historique ni transaction commune face aux écritures concurrentes. Un instant fourni vide ou mal formé répond
400, sans rapport.

Une fin réelle conserve sa borne, même régularisée au-delà de l'échéance. Sans elle, l'activité ne produit aucune
durée avant son échéance et compte dès celle-ci jusqu'à sa fin automatique. Les décisions de relance, transition,
fin tardive, régularisation, correction, annulation et clôture sont celles d'atelier, projetées en base.
Une activité à résoudre reste sans fin et n'est jamais réinterprétée par la synthèse ; les conflits et la
complétude des totaux seront restitués dans la tranche suivante du relevé.

Les durées se **cumulent par élément** : deux éléments simultanés de 08 h à 09 h portent chacun une heure,
le jour et la semaine deux heures. La NC est comprise une seule fois dans la durée totale et exposée aussi à part.
Les sommes des jours et des éléments sont égales à la durée opérationnelle de la semaine. Une activité en cours
conserve son élément sur chaque jour atteint, même sans pointage dans la semaine, sans durée comptabilisée.

Les éléments rendus portent une activité ou un pointage dans la semaine, par première apparition puis nom.
Un réengagement reste le même élément. Le nom et le type viennent du suivi ; référence et description sont
relues au référentiel, et peuvent être absentes. Les couples poste/nature suivent leur première apparition,
activité et pointage confondus ; l'absence de poste ou de nature est nominale.

Le **journal brut** est lu indépendamment des activités : tous les pointages actifs de l'opérateur datés de la
semaine sont rendus, même sans activité interprétable. Leur ordre est l'heure métier, puis l'intention
(fin, transition, ouverture), puis l'identité du pointage. L'heure d'enregistrement ne départage jamais.
Chaque élément et chaque poste nommés au journal trouvent leur fiche ou leur libellé dans la synthèse.

### La lecture passe par la base, jamais par un import

La synthèse déclare ses propres entités JPA `@Immutable` sur `activite_d_atelier`, `suivi_d_atelier`,
`evenement_d_atelier` et les référentiels. Elle n'importe aucun contexte métier voisin, ne rejoue aucun journal
et ne possède aucune table. Les activités sont lues en une requête, le journal en deux requêtes groupées ;
les fiches et les postes sont aussi résolus par lots. Cucumber écrit réellement dans atelier puis lit le relevé,
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

Les opérateurs désignables — identité, matricule, postes habilités —, les éléments encore pointables — identité,
nom d'atelier, référence, type, état, activités en cours, conflits —, et `genereLe`. Un opérateur sans activité
reste rendu avec toutes ses habilitations.

La route ne rend aucun état ni échéance de présence : `operateurs[].etat` et `presentJusqua` sont retirés.
Elle ne lit plus les journées ni le paramétrage. La suppression globale de la présence appartient à un lot suivant.

Elle ne rend **ni montant** (taux horaire, coût horaire : les entités de lecture ne les mappent même pas), **ni
journal d'événements**, **ni élément clôturé**, ni métadonnée d'engagement ou de clôture.

### `genereLe` est la version, et c'est une date

Le pupitre peut donc dire « référentiel du 14/09 à 09:31 » et mesurer son retard. Elle change à chaque appel, y
compris quand rien n'a bougé : elle dit quand le serveur a produit la réponse, pas quand le référentiel a changé
pour la dernière fois. Dater le dernier changement supposerait d'horodater les modifications d'`operateur`,
`poste_de_travail` et `operateur_poste` — trois tables sans colonne de modification, et une date de modification est
une donnée du domaine, qui ouvrirait deux agrégats voisins. Écarté tant que rien ne le demande.

### La non-pagination est le choix, pas un oubli

C'est la pagination qui imposait au front ses gardes sur les totaux, les doublons et les pages vides. Tout est ici lu
en **un appel et une transaction unique** : il n'y a plus de pages à recoudre. Le volume est borné par la taille de
l'atelier.

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
   `strategie/authentification-pointage.md` en fait une exigence. Le lot 8 de
   [strategie/bornes-de-fin-de-journee.md](strategie/bornes-de-fin-de-journee.md) l'a écartée le 26/09/2026 (lots 8b
   et 8c abandonnés) : habilitation retirée, date inhabituelle, référentiel inconnu, geste hors séquence et
   identifiant réutilisé restent refusés, et ne vivent que dans le journal local du pupitre.
3. **Le client Keycloak du pupitre n'existe pas dans le realm.** `glm-front` attend `pupitre_device`, avec le
   device grant activé et le client scope `glmproject` — sans lui, le jeton ne porte pas de claim `tenant` et toute
   la surface `/api/**` répond 403. C'est la dernière pièce d'infrastructure avant qu'un pupitre déployé puisse
   s'enrôler.

## parametrage

Porte le **paramétrage d'une entreprise** : pour l'instant, la seule **amplitude maximale** d'une journée de travail (décision D1 de [strategie/bornes-de-fin-de-journee.md](strategie/bornes-de-fin-de-journee.md)). Au-delà de cette durée depuis l'arrivée, une journée sans départ sera abandonnée (lot 3). Le gestionnaire la fixe ; l'opérateur la lit.

**La valeur par défaut n'est pas une constante du code.** Elle est semée en base, 13 h, par le changelog qui crée la table, dans chaque schéma d'entreprise. C'est la règle du dépôt pour toute donnée de paramétrage : le domaine la reçoit par un port. Une entreprise neuve reçoit donc sa ligne en même temps que son schéma, et aucune lecture ne tombe jamais sur une valeur absente.

**L'amplitude se compte à la minute et reste strictement sous 24 h.** C'est ce qui garantit qu'un retour le lendemain à la même heure soit toujours une nouvelle arrivée, même pour un poste de nuit. La règle vit dans `AmplitudeMaximale` ; la validation de la requête la répète pour répondre 400 plutôt que 500, et deux contraintes du schéma servent de filet.

**Seule la dernière modification est tracée** — auteur, lu dans le jeton, et instant. Changer le seuil peut déplacer la fin présumée d'une journée non régularisée (exemple E8) : savoir qui l'a changé, et quand, suffit à l'expliquer. Un historique complet n'est pas demandé.

### Points ouverts

1. **Les autres paramètres de l'entreprise** — fuseau horaire des relevés, préfixes des éléments de fabrication — vivent encore en configuration ou en constante. Ils ont vocation à rejoindre ce contexte.
