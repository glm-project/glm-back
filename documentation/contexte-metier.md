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

Le contexte porte **deux agrégats** : la `JourneeDeTravail` d'un opérateur et le `SuiviDAtelier` d'un élément engagé. Ils partagent un même langage — opérateur, auteur, horodatage, annulation, régularisation — et le temps réellement passé sur un élément se lit au croisement des deux. Les séparer en deux contextes obligerait à dupliquer ces value objects, que le shared kernel ne peut pas accueillir puisqu'il est en anglais.

**Le journal d'événements est la source de vérité.** L'état d'un agrégat et ses intervalles de temps ne sont jamais stockés : ils se déduisent du repli du journal, trié par date de survenue. C'est la correction qui l'impose — un temps juste exige que la saisie oubliée compte à l'heure où elle a eu lieu, pas à l'heure où on la rattrape, et un modèle à compteurs ne sait pas revenir en arrière. Chaque événement porte donc un `Horodatage` bitemporel : sa date de survenue, métier, et sa date d'enregistrement, technique.

**Une régularisation se reconnaît à l'écart entre ces deux dates, pas à l'identité de l'auteur.** `estUneRegularisation()` dit que la saisie est différée — et c'est le seul fait exposé : l'option de pointage en retard, qui laisse l'opérateur saisir lui-même son heure de début, est bien une régularisation sans aucun tiers. Le booléen jumeau `estSaisiParUnTiers` a été retiré avec le passage à l'identifiant : l'`Auteur` vient du jeton et l'opérateur du référentiel, et rien ne relie encore les deux — le comparer n'aurait plus produit qu'une réponse toujours vraie. Il reviendra avec le lot « utilisateur connecté ».

### La présence, base de la paie

Le client décrit son besoin comme « une pointeuse à laquelle on rajoute une option OF », et il a corrigé explicitement l'équipe sur ce point : les heures de présence courent de l'arrivée dans la société au départ, **jamais du premier au dernier élément travaillé**.

`JourneeDeTravail` porte donc son propre journal, d'`ARRIVEE`, `PAUSE`, `REPRISE` et `DEPART`. Ses bornes sont l'arrivée et le départ, **pas le jour calendaire** : aucun fuseau horaire n'entre dans le domaine, et une équipe de nuit ou un retour en soirée ouvre simplement une seconde journée. Elle expose deux mesures que les écrans de paie choisiront :

- `amplitude()` — de l'arrivée au départ, pauses comprises ;
- `fenetres()` — les périodes de présence effective, pauses exclues.

**La pause et le départ sont des faits de l'opérateur, écrits une seule fois.** Ils ne sont jamais recopiés dans le journal des éléments. C'est ce qui donne au client son bouton de pause unique et son bouton d'arrêt de fin de journée — « ne mettez qu'un bouton, pas trois » — sans jamais N clics pour N tâches, et sans qu'aucun code de diffusion n'ait à maintenir la cohérence de N journaux.

Le **bouton GLM** du client — le temps de présence sans élément rattaché — n'est pas un élément fictif : c'est le résidu de la présence moins le temps affecté, calculé à la lecture. La présence est comptée dès l'identification, comme le client l'a lui-même conclu (« il a pas besoin de bouton GLM, c'est juste qu'il est là »).

### Le temps effectif, croisement des deux journaux

`SuiviDAtelier.activites()` produit des intervalles **bruts** : ils ignorent la présence. `TempsDAtelierService.tempsEffectif` les ramène aux fenêtres de présence de l'opérateur, en intersectant chaque intervalle avec les fenêtres de **la journée où il a commencé**. Une seule règle, trois effets :

- une pause de midi **scinde** le travail en deux ;
- un départ **tronque** ce que l'opérateur a oublié d'arrêter — un `FIN` manquant ne produit plus un intervalle infini ;
- une régularisation de départ **corrige d'un coup tous les éléments** de la journée, là où une pause dupliquée par élément aurait demandé autant de corrections que d'éléments, et n'aurait jamais rattrapé un début inséré après coup.

Borner l'intervalle à sa journée est aussi ce qui empêche un travail jamais arrêté de courir jusqu'au lendemain : l'opérateur reclique sur l'élément à son retour, ce qui est exactement le geste que le client décrit. Un début qui ne tombe dans aucune journée connue est **rendu intact** : c'est la présence qui manque, et le domaine ne masque pas l'anomalie derrière un temps amputé.

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

Une pièce ratée se refait, sur le même élément et au même tarif, mais comptée à part. `EtatDActivite` distingue `EN_COURS` et `EN_NON_CONFORMITE`, et la catégorie d'un intervalle se lit **sur l'état atteint**, jamais sur le type d'événement : reprendre du bon travail après une non conformité se pointe comme un début. À la clôture, on sait « combien de temps on a passé à faire du bon travail et combien à refaire ».

### Les trois actes de correction

| Situation      | Acte         | Effet                                                |
| -------------- | ------------ | ---------------------------------------------------- |
| Saisie oubliée | `regularise` | insertion d'un événement daté dans le passé          |
| Saisie en trop | `annule`     | marquage de l'événement fautif, qui reste au journal |
| Saisie fausse  | `corrige`    | annulation **et** insertion, en un seul acte         |

Ces trois actes existent sur les deux agrégats : une heure d'arrivée fausse se corrige comme un début de travail faux.

Un journal ne se réécrit pas : l'événement erroné reste, porteur d'une `Annulation` qui trace qui a corrigé, quand et pourquoi. Le repli écarte les annulés, puis déroule l'automate.

`corrige` n'est pas la composition des deux autres, et c'est le point non évident : annuler un début puis insérer sa version corrigée ferait passer le journal par une séquence à fin orpheline, que l'automate refuserait à raison. La correction construit la séquence finale et ne la valide qu'une fois. L'automate protège donc la cohérence même sous correction.

**La clôture ne fige rien pour le gestionnaire.** Elle ferme le pointage aux opérateurs ; régularisation, annulation et correction restent admises, et la clôture elle-même se déplace ou s'annule. Le seul invariant qui subsiste est de cohérence, pas de permission : aucun événement daté après la clôture.

### Le temps réparti

Deux mesures coexisteront, qui ne s'additionnent pas de la même façon :

- **temps effectif** — la durée réelle passée sur un élément, telle que la produit `TempsDAtelierService`. Deux postes pendant 1 h font 2 h effectives.
- **temps réparti** — la même heure d'opérateur divisée par le **nombre de postes de travail** qu'il occupait simultanément, tous éléments confondus. Il ne servira qu'au coût de revient.

Le diviseur est bien le nombre de postes, et non le nombre d'activités ou d'éléments : le client énonce la règle deux fois de suite — coût horaire de chaque machine active non divisé, taux horaire de l'opérateur divisé par le nombre de machines qu'il utilise. Un opérateur sur trois éléments avec une seule machine n'est donc pas divisé.

**Le journal n'enregistre que l'effectif.** Le réparti traverse les agrégats — un nouveau pointage sur un second élément change la part déjà attribuée sur le premier —, il ne peut donc être qu'une fonction de projection, calculée à la lecture. `TempsDAtelierService` produit pour cela des intervalles complets, et pas seulement l'état courant dont l'écran a besoin aujourd'hui : c'est la couture sur laquelle les tableaux de bord se brancheront.

### Frontière avec elementdefabrication

`elementdefabrication` étant annoté `@BusinessContext`, l'atelier ne l'importe jamais. Il déclare sa propre identité, `ElementEngage`, dont le nom et le type sont **copiés à l'engagement** : c'est ce qu'affiche l'écran d'atelier, et un élément renommé plus tard ne doit pas réécrire l'histoire de l'atelier. Le port `ElementsEngageables` porte cette frontière.

**Mettre un élément en atelier est un geste métier explicite du back-office**, distinct de sa création : tout ce qui est créé n'est pas forcément à faire, et c'est cet acte qui fait apparaître l'élément sur l'écran des opérateurs. C'est aussi ce qui donne un sens à l'invariant « aucun événement antérieur à l'engagement ».

L'écran des opérateurs veut tous les éléments actifs d'un coup, sans rien qui défile et sans notion de date : la période de `SuiviDAtelierCriteria` est donc **facultative**, et ne sert qu'aux écrans de back-office.

Les quatre couches existent désormais, et les deux agrégats sont persistés en PostgreSQL, dans le schéma de l'entreprise courante. Chacun occupe deux tables : la ligne de l'agrégat et son journal. Le journal restant la seule source de vérité, l'état n'est jamais stocké _comme état_ — mais `etat`, `debut` et `fin` sont écrits en **colonnes de projection**, dérivées du journal à chaque écriture et jamais relues pour reconstruire l'agrégat. Sans elles, filtrer l'écran d'atelier sur les états ou retrouver la journée contenant un instant obligerait à ramener toute l'entreprise en mémoire.

Le modèle est relationnel plutôt qu'un journal sérialisé en `jsonb`, parce que les projections à venir — coût de revient, paie, synthèses — filtrent et groupent sur des attributs d'**événement** à travers tous les agrégats : un index les sert directement, là où un document devrait être désérialisé en entier pour être presque tout jeté. Les index `(operateur, date_de_survenue)` et `(poste, date_de_survenue)` sont posés dès maintenant à cette fin, et un contexte lecteur n'aura qu'à poser dessus une entité en lecture seule, comme `atelier` le fait déjà sur `element_de_fabrication`.

`ElementsEngageables` lit la table `element_de_fabrication` par une entité en lecture seule propre à l'atelier, sans jamais importer le contexte voisin. `OperateursConnus`, `PostesConnus` et `Habilitations` font de même sur `operateur`, `poste_de_travail` et `operateur_poste`.

L'API est décrite par OpenAPI (`/swagger-ui.html`) et par [atelier-api.md](atelier-api.md), qui porte ce que la spec ne peut pas dire.

### Points ouverts

1. **Régulariser après une dé-habilitation est refusé.** L'habilitation étant vérifiée sur les trois écritures du journal, un gestionnaire ne peut plus rattraper une saisie oubliée sur un poste dont l'opérateur a été retiré depuis. Le cas est assumé pour ce lot — il ferme la porte au contournement —, mais il laisserait un trou dans la paie s'il se produisait : à rouvrir si le client le rencontre.
2. **Quelle mesure alimente la paie ?** L'amplitude arrivée → départ, ou la somme des fenêtres de présence, pause de midi déduite ? Le client dit « les heures où il arrive à la société, il pointe et il part », mais pointe aussi sa pause déjeuner. Les deux mesures sont exposées, le choix reste à faire avec l'assistante.
3. **Le coût de revient monétaire.** Le taux horaire de l'opérateur et le coût horaire du poste sont désormais copiés sur l'événement du journal au moment de la saisie, sur le même patron que la nature de l'opération — ni recalculés ni relus depuis le référentiel après coup. Ce qui reste un lot à part entière : le **calcul** lui-même (temps réparti valorisé, coût de revient par élément ou par période), envisagé comme un futur bounded context séparé qui lira le journal d'`atelier` par port en lecture seule, plutôt que d'être mélangé au pointage. À reprendre en même temps que l'objection de Nicolas sur la division du taux humain, restée sans conclusion en réunion.
4. **Le bouton de pause global n'a jamais été validé de première main.** Il ne vient que de la réunion d'équipe. Dans la réunion client, la pause est décrite au singulier, sur un seul élément. Le modèle retient le bouton global — à reconfirmer, c'est lui qui structure l'écran principal.
5. **Le GLM comme résidu** (présence moins temps affecté) plutôt que comme élément fictif : cohérent avec ce que le client conclut, mais l'écran devra le rendre visible d'une façon ou d'une autre, puisqu'il tient au bouton.
6. **Le cycle de vie de l'élément lui-même.** La clôture existe côté atelier, sur le suivi. Reste à trancher si l'élément de fabrication porte en propre un statut, ou si son activité se lit entièrement par la présence ou l'absence d'un suivi non clôturé.
7. **Aucune garde d'unicité en base** sur « un seul suivi non clôturé par élément » ni « une seule journée ouverte par opérateur », contrairement à ce que `elementdefabrication` fait pour la `Reference`. Les deux règles vivent dans les services, mais une contrainte partielle transformerait en 500 deux états que le domaine admet aujourd'hui : rouvrir la clôture d'un suivi dont l'élément a été réengagé depuis, ou annuler le `DEPART` d'une journée dont l'opérateur est déjà revenu. À trancher côté domaine avant de poser la contrainte.
8. **L'écriture du journal rapproche par identifiant**, ce qui coûte une lecture indexée de la collection à chaque pointage. Si un journal devenait assez long pour que cette lecture pèse, la sortie est un upsert natif gardé (`on conflict (id) do update ... where ... is distinct from ...`), qui épargne à PostgreSQL toute version de tuple sur les lignes inchangées — au prix d'une scission permanente entre lecture JPA et écriture JDBC.

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
3. **Montants.** Coût horaire du poste et taux horaire de l'opérateur existent sur les deux agrégats (facultatifs, strictement positifs), et `atelier` les copie désormais sur chaque événement du journal au moment de la saisie, sur le même patron que la nature de l'opération ; seul le calcul du coût de revient lui-même reste à faire, lot suivant.
4. **Utilisateur connecté.** Tranché sur le principe, dans [strategie/authentification-pointage.md](strategie/authentification-pointage.md) : le pupitre porte une **identité d'appareil** et aucune session humaine, l'opérateur est **identifié** au geste — par un code, qui désigne sans prouver, ou par une signature, qui prouve. L'`Auteur` du jeton cessant dès lors de désigner une personne, c'est la **qualité de l'identification** portée par l'événement qui vaudra pour la paie, et c'est elle qui rouvrira `estSaisiParUnTiers`. Restent ouverts le régime du code — ouvert à tous ou réservé à l'exception —, le matériel des pupitres, et la validation juridique de l'empreinte.

## feuilledetemps

Première **projection transverse** du projet : un contexte purement lecteur, qui ne possède aucune table et
recalcule tout à chaque appel. Il répond à une seule question — _qu'a fait cette personne cette semaine, jour par
jour_ — et c'est ce qui le sépare d'`atelier`.

### Pourquoi il n'est pas dans atelier

`atelier` s'interdit explicitement le calendrier : une `JourneeDeTravail` y est bornée par une arrivée et un départ,
jamais par une date, et aucun `ZoneId` ni `LocalDate` n'entre dans ce contexte. Une feuille de temps hebdomadaire
n'est faite que de ça. C'est ici, et nulle part avant, que minuit existe — et c'est minuit qui décide à quel jour
appartient une heure de travail. Une équipe de nuit compte sur deux jours ; l'atelier ne saurait pas le dire.

Le découpage était annoncé : les index `(operateur, date_de_survenue)` et `(poste, date_de_survenue)` d'
`evenement_d_atelier` sont commentés dans le changelog comme ne servant pas l'atelier lui-même, mais « les
projections transverses des contextes à venir ». `feuilledetemps` est le premier de ces consommateurs.

### La lecture passe par la base, jamais par un import

`atelier`, `operateur` et `postedetravail` étant annotés `@BusinessContext`, ce contexte déclare ses propres entités
JPA en lecture seule sur leurs tables — exactement ce que fait déjà `atelier` sur `element_de_fabrication`. Il
rejoue donc **sa propre** version du repli de présence.

Cette duplication est assumée : le partage passerait soit par un import interdit, soit par le shared kernel, qui est
en anglais et ne peut pas accueillir du vocabulaire d'atelier. Le filet qui tient les deux implémentations alignées
est le scénario Cucumber, qui pointe par l'API d'`atelier` et relit par celle de la feuille de temps — il échoue dès
que les deux contextes cessent de lire les mêmes colonnes.

### Ce que la feuille montre

Sept jours toujours, du lundi au dimanche de la semaine ISO demandée, vides compris : un trou obligerait le lecteur
à deviner s'il manque une journée ou si l'opérateur n'était pas là. L'année est celle des semaines ISO, qui diffère
de l'année civile à ses bornes — la semaine 1 de 2026 commence le 29 décembre 2025.

La semaine est toujours explicite, jamais « la semaine courante » : deux appels identiques rendent la même chose, et
aucune horloge n'entre dans ce contexte.

**Une plage encore ouverte ne dépasse pas son propre jour.** Sans départ pointé, rien ne dit que l'opérateur était
encore là le lendemain ; l'étaler jusqu'à la fin de la semaine affirmerait une présence que personne n'a saisie.
C'est la transposition de la règle qu'`atelier` applique déjà à un travail jamais arrêté.

### Points ouverts

1. **Le travail par élément reste à faire** (lot 2) : intervalles d'activité réduits aux fenêtres de présence,
   découpés par jour, avec l'élément, le poste et la nature. La décision est prise — un pointage dont le début ne
   tombe dans aucune journée de présence sera **écarté**, là où `TempsDAtelierService` le rend intact. Sans
   présence, aucun jour ne peut l'accueillir sans arbitraire ; l'anomalie reste visible sur
   `GET /api/atelier/suivis/{id}/temps-effectif`.
2. **Le fuseau horaire est fixé à `Europe/Paris`** par un adapter, sur le patron des préfixes d'éléments de
   fabrication. Il passe déjà par un port : le jour où une entreprise cliente vit ailleurs, seul l'adapter change.
3. **Aucune restriction sur qui lit la feuille de qui.** `USER` et `GESTIONNAIRE` lisent l'historique de n'importe
   quel opérateur, faute de lien entre un utilisateur authentifié et une fiche du référentiel. À rouvrir avec le lot
   « utilisateur connecté », qui ramènera aussi `estSaisiParUnTiers` côté atelier.
