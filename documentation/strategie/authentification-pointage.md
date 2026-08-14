# Stratégie — authentification et identification au pupitre

Document de réflexion, pas de spécification. Il fixe la stratégie retenue pour reconnaître l'opérateur qui pointe, sécuriser le pupitre d'atelier et survivre à une coupure réseau. Le détail métier et sa justification par le verbatim client vivent dans [contexte-metier.md](../contexte-metier.md), l'isolation par entreprise dans [multitenancy.md](../multitenancy.md), les invariants du pointage dans le [CLAUDE.md du contexte `atelier`](../../src/main/java/com/glm/glmback/atelier/CLAUDE.md) — ne rien dupliquer ici.

Il répond au point ouvert « Utilisateur connecté » (`contexte-metier.md`, section `operateur`), resté non tranché depuis la livraison des référentiels.

## Le problème

Le pupitre est le seul écran que les opérateurs touchent, et il ne sait pas qui est devant lui. L'`Auteur` d'une saisie vient du jeton (`AuteurConnecte` lit le claim `preferred_username`), l'opérateur vient du **corps de la requête**, sous forme d'identifiant du référentiel, et **rien ne relie les deux**. C'est pour cette raison que `estSaisiParUnTiers` a été retiré de l'API plutôt que rendu faux : le comparer n'aurait produit qu'une réponse toujours vraie.

Le client a énoncé deux moyens de reconnaissance : **l'empreinte digitale**, et un **code** quand l'empreinte ne fonctionne plus. Il a par ailleurs dit ne pas vouloir dépendre d'une connexion cloud permanente, alors que le business plan impose le SaaS.

Le vocabulaire courant résume tout cela par « ajouter un login ». Il masque **trois questions distinctes**, qui n'appellent pas les mêmes réponses :

| Question                   | Ce qu'elle demande                                                | État                            |
| -------------------------- | ----------------------------------------------------------------- | ------------------------------- |
| **Authentifier le poste**  | prouver que la saisie vient d'un pupitre autorisé de l'entreprise | à construire                    |
| **Identifier l'opérateur** | dire quelle personne est devant l'écran                           | à construire                    |
| **Autoriser l'acte**       | vérifier que cette personne peut faire ce qu'elle demande         | **déjà fait**, ne pas y toucher |

La troisième est réglée : `Habilitations` refuse un pointage sur un poste où l'opérateur n'est pas déclaré, et les rôles se déclarent sur le service applicatif. Ce document ne traite que les deux premières.

## Deux fronts, deux régimes

**L'identification de l'opérateur ne doit pas passer par l'émission d'un jeton.** C'est l'argument central, et il était déjà au dossier : la _vérification_ d'un jeton est locale — signature contre le JWKS mis en cache —, c'est son _émission_ et son rafraîchissement qui exigent le cloud. Donner un compte Keycloak à chaque opérateur rendrait donc le pointage impossible dès la coupure, et ferait de chaque embauche un acte d'administration IAM.

L'application se scinde en deux fronts, qui n'ont ni le même public, ni le même régime d'accès.

|                               | Back-office                                                     | Pupitre                                         |
| ----------------------------- | --------------------------------------------------------------- | ----------------------------------------------- |
| Public                        | gestionnaires, administrateurs                                  | opérateurs                                      |
| Réseau                        | cloud                                                           | cloud                                           |
| Authentification              | OIDC nominative, Keycloak                                       | **identité d'appareil, aucune session humaine** |
| Identification de la personne | le jeton                                                        | code saisi, ou signature de l'opérateur         |
| Actes                         | lecture, rapports, paramétrage, engagement, clôture, correction | pointage seul                                   |
| Rôles                         | `GESTIONNAIRE`, `ADMIN`                                         | `USER`                                          |

La ligne « actes » recoupe exactement le partage `@Secured` déjà en place : les trois écritures de correction — régularisation, annulation, correction — sont `GESTIONNAIRE`, le pointage est `USER`. **La scission des fronts ne demande aucune règle d'autorisation nouvelle : elle rend structurelle celle qui existe.**

Le coût est assumé : deux applications à construire, versionner et déployer. La contrepartie est double. Une ergonomie de kiosque — gros boutons, gants, un seul écran, aucune navigation — que le back-office n'aurait jamais. Et la disparition, sur le poste d'atelier, de la session OIDC humaine, qui est la principale fragilité hors ligne d'une application web.

## Sécuriser un pupitre sans login

Le pupitre n'a pas d'authentification humaine sur sa page d'accueil. Il doit pourtant présenter un jeton valide pour appeler l'API, et sa page d'accueil affiche déjà des données de l'entreprise — éléments engagés, opérateurs. « Pas de login » ne veut pas dire « pas d'identité ».

### Le tunnel réseau n'est pas la réponse

Un VPN authentifie un **emplacement**, pas un appareil ni une personne : il ne remplace aucun jeton, il s'ajoute. Et il coûte deux fois. Un point de terminaison chez le client, donc la machine à administrer qu'on cherchait à éviter. Surtout, **un mode de panne supplémentaire sur le lien même qu'on veut rendre résilient** : un pupitre à l'arrêt parce que le tunnel est tombé alors qu'Internet fonctionnait est un recul, pas une protection.

Un VPC est encore moins pertinent : c'est notre réseau privé cloud, l'atelier du client n'y est pas, et l'y raccorder demanderait de la connectivité dédiée.

Reste utile, à coût nul et **en défense en profondeur seulement**, une liste d'adresses IP autorisées quand le client a une IP fixe. Jamais comme mécanisme d'authentification.

### Un compte d'appareil, enrôlé une fois

Le pupitre devient un **utilisateur Keycloak d'appareil** — `pupitre-atelier-1` —, porteur de `ROLE_USER` et de l'attribut `tenant`. Rien de neuf dans la chaîne d'autorisation : `TenantAuthorizationManager` et `TenantSchemas` traitent ce jeton comme n'importe quel autre.

Son enrôlement passe par le **device authorization grant** (RFC 8628, le schéma des téléviseurs connectés) : le pupitre affiche un code, un gestionnaire l'approuve depuis le back-office **avec son propre compte**, le pupitre reçoit un jeton de rafraîchissement `offline_access`. Ensuite, plus personne ne se connecte jamais sur ce poste.

Ce que le dispositif donne : aucun secret partagé, aucun mot de passe tapé en atelier, une identité **par poste** révocable en un clic, et un droit limité au pointage d'une seule entreprise.

Trois points de configuration à connaître, vérifiés sur le realm actuel :

- **aucun client n'a le device grant activé** aujourd'hui ; il faudra un client dédié. `web_app` ne convient pas : il est en implicit flow avec `webOrigins: ["*"]`, acceptable en développement, pas pour un poste permanent ;
- les sessions hors ligne sont réglées à **30 jours d'inactivité, sans durée de vie maximale** (`offlineSessionIdleTimeout: 2592000`, `offlineSessionMaxLifespanEnabled: false`) : un pupitre qui se synchronise une fois par mois garde son identité indéfiniment ;
- les claims `tenant` et `roles` viennent du **client scope `glmproject`**. Un client qui ne le porte pas produit un jeton sans `tenant`, et `TenantAuthorizationManager` répond 403 — c'est le piège de configuration à ne pas redécouvrir. `internal` en est l'illustration : il a bien `serviceAccountsEnabled`, mais ses scopes par défaut sont `web-origins, roles, profile, email`.

## Identifier n'est pas authentifier

C'est la distinction qui structure tout le reste, et elle simplifie plus qu'elle ne complique.

**Sur un pupitre déjà sécurisé, un code saisi sert à identifier, pas à authentifier.** Ce n'est pas un secret : c'est un désignateur — un matricule tapé au clavier numérique plutôt que choisi dans une liste de soixante noms. Ce qui sécurise l'acte est alors le poste, par son identité d'appareil, et le contrôle physique de l'atelier.

Il faut l'écrire, parce que la pente naturelle est de traiter tout code comme un mot de passe et de réintroduire plus tard ce que cette lecture fait tomber :

- **pas de hachage, pas de rotation, pas de verrouillage après N échecs**, pas de « comment déverrouiller sans réseau » — aucune de ces questions ne se pose sur un désignateur ;
- **aucun secret à embarquer sur l'appareil**, donc rien à casser en force brute. Un code à quatre chiffres protégé par un hachage stocké localement se casse instantanément ; un désignateur n'a rien à protéger. La faiblesse principale du mode dégradé disparaît avec la question ;
- pas de « désignation puis code », qui n'aurait de sens que si le code prouvait quelque chose ;
- **`Matricule` existe déjà** au référentiel `operateur`, facultatif et unique quand il est renseigné. C'est le code. Seul arbitrage résiduel : réutiliser le matricule de paie, ou porter un code de pupitre distinct pour qu'un changement de matricule ne change pas ce que les gens tapent au mur.

### Le revers, à dire au client

Le pointage par code repose sur le contrôle physique du lieu, pas sur la cryptographie : rien n'empêche un collègue de taper le code de Dupont. C'est le régime de toutes les pointeuses à badge, et il est acceptable en soi. Mais **si l'empreinte est déployée précisément pour que personne ne pointe à la place d'un autre, le repli par code rouvre exactement ce trou pour qui l'emprunte.**

D'où **deux régimes d'identification**, à distinguer sur l'événement plutôt qu'à confondre :

|                                        | Ce que le pupitre affirme                     | Vérifiable par le serveur                                    |
| -------------------------------------- | --------------------------------------------- | ------------------------------------------------------------ |
| **Identification déclarative** (code)  | « quelqu'un a désigné Dupont sur ce pupitre » | non — la confiance vient du poste et du lieu                 |
| **Identification prouvée** (signature) | « Dupont a signé ce pointage »                | oui, y compris sur un événement rejoué trois jours plus tard |

**Décision à prendre avec le client** : le code est-il ouvert à tous, ou réservé à l'exception — doigt non enrôlé, capteur en panne, refus de la biométrie — et tracé comme telle ? Recommandation : **réservé à l'exception**. Sans quoi le niveau d'assurance de l'ensemble est celui du maillon le plus faible, et l'empreinte devient décorative.

Conséquence de conception, et c'est un gain : **les deux régimes tiennent dans le même écran.** On tape son code ; selon ce que l'entreprise a souscrit, un second geste — le doigt — vient prouver. Le régime est un paramétrage du site, pas une application différente.

## Rendre le pointage vérifiable plutôt que le pupitre infalsifiable

Aucun jeton posé sur un poste d'atelier ne sera inviolable. La bonne question n'est donc pas « comment rendre le pupitre infalsifiable », mais **« comment rendre le pointage vérifiable »**.

En régime prouvé, chaque événement porte la **signature de la clé de l'opérateur**, vérifiable contre une clé publique enregistrée à l'enrôlement. Le jeton d'appareil ne sert plus qu'à _atteindre_ l'API ; ce qui rend l'événement digne de foi est la signature de la personne. Un jeton dérobé permet alors de poster du bruit, pas de fabriquer des heures au nom de quelqu'un.

En régime déclaratif, cette garantie n'existe pas : le jeton d'appareil et le contrôle du lieu sont tout ce qu'il y a. C'est précisément pourquoi les deux régimes doivent rester distinguables.

## L'empreinte digitale

### Vérification (1:1) plutôt qu'identification (1:N)

L'arbitrage technique que le client n'a pas formulé, et qui décide de tout le reste.

- **1:N — « je pose le doigt, ça me reconnaît ».** Exige un moteur de comparaison et une base de gabarits sur le pupitre, donc un lecteur à SDK propriétaire et une application kiosque native. GLM traite alors de la donnée biométrique, et ne dispose d'aucune preuve cryptographique : le pupitre affirme, rien ne le vérifie.
- **1:1 — « je tape mon code, je valide au doigt ».** Réalisable avec l'authentifieur de plateforme du terminal (WebAuthn : Windows Hello, Android). Le gabarit reste dans l'élément sécurisé de l'appareil, GLM ne le voit jamais, et le serveur reçoit une signature vérifiable.

**Recommandation : le 1:1.** Il coûte un geste de plus — celui que la section précédente avait de toute façon prévu — et il règle **trois problèmes distincts avec une seule brique** :

| Problème            | Ce que la vérification 1:1 apporte                                                    |
| ------------------- | ------------------------------------------------------------------------------------- |
| RGPD                | le gabarit ne quitte pas l'élément sécurisé : GLM ne traite aucune donnée biométrique |
| Hors ligne          | une signature se vérifie sans réseau, et aucun secret ne séjourne sur l'appareil      |
| Sécurité du pupitre | la preuve du pointage cesse de dépendre du secret de l'appareil                       |

Le 1:N reste la variante à assumer si le client exige le geste unique. Elle se paie en conformité, en enfermement matériel, et en perte de la preuve.

### Le risque juridique

La biométrie relève de l'**article 9 du RGPD**, données sensibles. Le règlement type CNIL de 2019 encadre les dispositifs biométriques de **contrôle d'accès** sur le lieu de travail ; la **gestion des horaires** n'est pas la même finalité, et la CNIL s'y est historiquement montrée défavorable.

Ce document ne tranche pas un point de droit. Il pose une exigence : **validation par un conseil et analyse d'impact avant tout engagement commercial sur l'empreinte**, et il note que l'architecture recommandée — gabarit hors de portée de GLM — est celle qui minimise l'exposition, jusqu'à rendre discutable la qualification de traitement biométrique de notre côté.

### Le code n'est pas seulement un repli technique

Un salarié qui refuse la biométrie doit disposer d'un moyen de plein droit. La demande du client — « si l'empreinte ne fonctionne plus » — et l'exigence réglementaire convergent sur le même dispositif, pour deux raisons différentes.

Mais **« moyen de plein droit » n'est pas « moyen équivalent »** : le code identifie quand l'empreinte authentifie. Le repli est une baisse du niveau d'assurance, pas une alternative de même valeur — c'est ce qui justifie de le réserver à l'exception et de le tracer sur l'événement.

Le moyen retenu devient une **donnée de paramétrage du site**, ce qui isole l'exposition RGPD aux seules entreprises qui la demandent, conformément au principe que GLM est une trame et non une spécification.

### Pas de session de pupitre

Le poste est partagé. Ré-identification à chaque acte, ou fenêtre très courte pour enchaîner deux gestes. Jamais de session ouverte qu'on oublie de refermer — c'est le mode de défaillance classique des pointeuses à écran.

## Conséquence : `Auteur` cesse de désigner une personne

Aujourd'hui `AuteurConnecte` pose le `preferred_username` du jeton sur chaque événement. Avec un compte d'appareil, tout ce que le pupitre pousse portera `pupitre-atelier-1`.

La personne identifiée **et la façon dont elle l'a été** — signature, code, désignée par un gestionnaire — doivent donc voyager avec la saisie. Ce n'est pas un complément de l'auteur : **c'est ce qui le remplace comme preuve**, pour la paie comme pour un litige.

C'est aussi ce qui rouvre `estSaisiParUnTiers`, mais sur une base neuve. Il ne s'agira plus de comparer deux identités que rien ne rapproche, mais de lire une **qualité d'identification** portée par l'événement lui-même.

## La continuité de service, par ordre de coût croissant

Le client ne veut pas dépendre du réseau ; le business plan impose le cloud. La réponse n'est pas une architecture, c'est un ordre — du moins coûteux au plus coûteux, et on ne descend d'un cran que si le cran précédent ne suffit pas.

**1. La redondance du lien.** Une 4G de secours sur le routeur supprime la cause au lieu de traiter le symptôme, couvre l'écrasante majorité des coupures, ne demande **aucune maintenance logicielle** et laisse le produit intégralement cloud. C'est la première réponse honnête à « le système le plus simple possible ».

**2. Le pupitre en offline-first.** Une application web installable, avec file d'attente locale et cache du référentiel. Aucune machine de plus chez le client, **mise à jour automatique** à la reconnexion, et — point contre-intuitif — **l'origine reste celle du cloud**, donc les identités de l'opérateur survivent à la coupure. Un serveur local qui servirait l'application depuis sa propre origine les _casserait_, puisqu'une identité WebAuthn est liée à l'origine qui l'a enregistrée.

**3. Le relais local — une option, jamais le socle.** À proposer quand un besoin précis le justifie : parc de nombreux pupitres, exigence de durabilité au-delà du navigateur, refus d'un identifiant détenu par un poste. Ses coûts doivent être énoncés sans être adoucis — une machine à patcher, à superviser et **à mettre à jour à distance**, dont personne ne regarde le disque.

## Ce qui casse hors ligne, et ce qui n'en dépend pas

La dégradation n'est pas uniforme, et c'est ce qui rend le problème tenable.

|                                    | Sans cache | Avec cache du référentiel | En ligne |
| ---------------------------------- | ---------- | ------------------------- | -------- |
| Identifier l'opérateur             | non        | oui                       | oui      |
| Présence — arrivée, pause, départ  | non        | **oui**                   | oui      |
| Pointer sur un élément déjà engagé | non        | oui                       | oui      |
| Mettre un élément à l'atelier      | non        | non                       | oui      |

**La paie tient à la colonne du milieu.** Le besoin central — « une pointeuse à laquelle on rajoute une option OF » — ne dépend d'aucun élément : une `JourneeDeTravail` n'a besoin que de l'existence de l'opérateur. Ce qui casse hors ligne est l'option, pas la pointeuse.

**La mise à l'atelier est le seul vrai trou, et aucun dispositif technique ne le comble.** Engager est un acte du back-office ; le domaine refuse tout événement antérieur à l'engagement, et le nom de l'élément est numéroté par le serveur. Déplacer l'engagement au bord reviendrait à y déplacer la numérotation, donc le domaine — et à faire du poste d'atelier une seconde instance de l'application, ce que toute cette stratégie évite.

**Réponse retenue : l'engagement par anticipation.** Le back-office met les éléments à l'atelier à l'avance, le cache du pupitre les porte déjà. C'est une contrainte d'organisation, pas de logiciel, et elle est cohérente avec ce que le client décrit : engager est un geste de préparation, pas une réaction à l'urgence.

**Filet de dernier recours**, si le cas se produit quand même : la présence continue de courir, le travail non rattachable tombe dans le résidu — le « bouton GLM », déjà défini comme la présence moins le temps affecté —, et le gestionnaire l'affecte après coup par `regularise` ou `corrige`. Ces trois actes existent exactement pour ça ; rien à ajouter au modèle.

## Le rejeu

- **Idempotence : l'identifiant de l'événement doit naître au pupitre.** Il naît aujourd'hui dans le domaine, côté serveur. C'est la conséquence la plus concrète de tout le dispositif, et la seule qui touche un choix existant.
- **L'horodatage bitemporel est exactement la couture qu'il faut**, et il est déjà là. Un pointage rejoué garde sa date de survenue au moment du geste et sa date d'enregistrement **au moment de la saisie au pupitre**. Le serveur ne réhorodate jamais : sans cette règle, chaque coupure produirait un flot de fausses régularisations, puisque `estUneRegularisation()` se lit sur l'écart entre les deux dates.
- **Le rejeu peut échouer pour des raisons métier**, et c'est le risque le plus sérieux : habilitation retirée entre-temps, suivi clôturé, saisie concurrente, ou date de survenue devenue future si l'horloge du poste a dérivé. Il faut une issue **côté serveur** — mise en quarantaine et reprise explicite par un gestionnaire —, jamais un rejet silencieux ni une file qui grossit sur une machine que personne ne regarde. Le rejeu respecte par ailleurs l'ordre par agrégat : l'automate d'état en dépend.
- **La file locale est le seul point de perte du dispositif**, et le pupitre en est le porteur : un poste remplacé emporte ses saisies non acquittées. La supervision n'est donc pas un confort — le serveur alerte quand un pupitre n'a plus rien poussé, et l'écran affiche sa désynchronisation. **Une coupure silencieuse de trois semaines est le vrai scénario de perte de données**, bien plus que la panne franche.
- **Décalage de versions** : entre un pupitre resté en arrière et un serveur qui a évolué, l'API de poussée doit être versionnée et tolérante.

## La tension avec le business plan

Elle est réelle et il vaut mieux la nommer : le client veut fonctionner sans le cloud, le modèle économique impose le cloud.

Règle proposée : **le hors-ligne dégrade, il ne remplace pas.** Pas de back-office local, pas de correction, pas de paramétrage, aucune fonctionnalité qui n'existerait qu'hors ligne. Retenir le cloud comme socle et le relais comme option rend cette limite **structurelle plutôt que contractuelle** — un pupitre qui ne sait rien interroger ne peut pas devenir un produit local, et c'est la meilleure garantie qu'on puisse donner au modèle économique.

## Ce que cette stratégie ferme, ce qu'elle rouvre

- **Ferme** le point ouvert « Utilisateur connecté » du contexte `operateur`.
- **Rouvre** `estSaisiParUnTiers`, sur la qualité d'identification et non sur une comparaison d'identités.
- **Touche** le point ouvert « les gestionnaires ne sont pas déclarés » : un gestionnaire devra exister au référentiel dès lors qu'il pourra désigner un opérateur au pupitre ou approuver l'enrôlement d'un poste.
- **Ne modifie ni** l'isolation par entreprise, **ni** les rôles, **ni** aucun invariant du contexte `atelier`.

## Points ouverts

1. **Le code est-il ouvert à tous, ou réservé à l'exception ?** C'est la décision qui fixe le niveau d'assurance de l'ensemble. Recommandation : réservé à l'exception, et tracé comme telle.
2. **Code de pupitre ou matricule de paie ?** Le `Matricule` existe déjà et suffit ; un code distinct évite qu'un changement de matricule change ce que les gens tapent au mur.
3. **Geste unique ou code puis doigt ?** Question à poser au client en lui exposant ce que le geste unique coûte : conformité, enfermement matériel, perte de la preuve.
4. **Matériel des pupitres.** Le choix de la vérification 1:1 suppose des terminaux dotés d'un authentifieur de plateforme. À qualifier avant tout engagement.
5. **L'engagement par anticipation est-il acceptable pour l'organisation du client ?** C'est la seule contrainte que cette stratégie fait porter au métier plutôt qu'au logiciel.
6. **Base légale, analyse d'impact, information des salariés** — à faire valider avant de vendre l'empreinte.
7. **Durée d'autonomie visée** hors ligne : elle dimensionne le cache, la file d'attente et le seuil d'alerte de supervision. Aucune valeur n'a été demandée au client.
8. **Procédure de remplacement d'un pupitre**, y compris le sort des saisies non acquittées d'un poste hors service, et la révocation de son identité.
