# Contexte métier

Décrit les bounded contexts métier du projet et leur rôle. Les règles de code (architecture, DDD, tests, conventions) sont dans `glm-back/CLAUDE.md`.

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

**Une régularisation se reconnaît à l'écart entre ces deux dates, pas à l'identité de l'auteur.** Les deux faits sont distincts et exposés séparément : `estUneRegularisation()` dit que la saisie est différée, `estSaisiParUnTiers()` dit qu'un gestionnaire a saisi pour un opérateur. L'option de pointage en retard, qui laisse l'opérateur saisir lui-même son heure de début, est bien une régularisation sans aucun tiers.

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

`CleDActivite` est le couple (opérateur, `Optional<PosteDeTravail>`). Le `PosteDeTravail` est ce que l'opérateur engage en pointant : une machine chez le client de référence, un établi, un four, une salle ailleurs.

- Il est **facultatif** : une entreprise sans parc machine laisse l'`Optional` vide et retrouve une activité unique par opérateur.
- C'est **lui**, et non la nature de l'opération, qui identifie l'activité. L'érosionniste qui met deux pièces du même élément sur deux machines mène ainsi deux activités indépendantes — cas que le client détaille explicitement, et qu'une clé fondée sur la fonction refuserait.

La `NatureDOperation` (fraisage, tournage, érosion, dessin) est **recopiée du profil de l'opérateur** au moment de la saisie, par le port `FonctionsDesOperateurs`, comme le nom de l'élément est recopié à l'engagement : un profil modifié plus tard ne réécrit pas l'histoire de l'atelier. Elle ne porte **aucun invariant** — elle n'est qu'un axe d'agrégation pour la synthèse par catégorie de travail. Le client ne demande aucun blocage, et un pointage refusé en atelier coûterait plus cher qu'une ligne de synthèse mal rangée. Elle est facultative comme le poste.

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

À ce jour, seul le `domain/` existe : `application/`, `infrastructure/primary/` et `infrastructure/secondary/` restent à écrire.

### Points ouverts

1. **Quelle mesure alimente la paie ?** L'amplitude arrivée → départ, ou la somme des fenêtres de présence, pause de midi déduite ? Le client dit « les heures où il arrive à la société, il pointe et il part », mais pointe aussi sa pause déjeuner. Les deux mesures sont exposées, le choix reste à faire avec l'assistante.
2. **Le coût de revient monétaire.** Le taux horaire de l'opérateur et le coût horaire du poste n'existent nulle part : aucune source de données, donc aucun port. Le lot suivant. À reprendre en même temps que l'objection de Nicolas sur la division du taux humain, restée sans conclusion en réunion.
3. **Le bouton de pause global n'a jamais été validé de première main.** Il ne vient que de la réunion d'équipe. Dans la réunion client, la pause est décrite au singulier, sur un seul élément. Le modèle retient le bouton global — à reconfirmer, c'est lui qui structure l'écran principal.
4. **Le GLM comme résidu** (présence moins temps affecté) plutôt que comme élément fictif : cohérent avec ce que le client conclut, mais l'écran devra le rendre visible d'une façon ou d'une autre, puisqu'il tient au bouton.
5. **Le cycle de vie de l'élément lui-même.** La clôture existe côté atelier, sur le suivi. Reste à trancher si l'élément de fabrication porte en propre un statut, ou si son activité se lit entièrement par la présence ou l'absence d'un suivi non clôturé.
