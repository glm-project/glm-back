# Contexte métier

Décrit les bounded contexts métier du projet et leur rôle. Les règles de code (architecture, DDD, tests, conventions) sont dans `glm-back/CLAUDE.md`.

Toutes les données métier sont isolées par entreprise cliente : chaque entreprise a son propre schéma PostgreSQL, désigné par le claim `tenant` du token. Aucun agrégat ne porte donc d'identifiant d'entreprise — l'isolation est assurée par l'infrastructure, décrite dans [multitenancy.md](multitenancy.md).

## elementdefabrication

Gère les éléments de fabrication. Un `ElementDeFabrication` porte son `TypeDElementDeFabrication` — ordre de fabrication ou produit — comme une valeur : les deux ne diffèrent aujourd'hui que par ce type, par leur préfixe de nommage et par leur série de numérotation. Le nom est toujours produit par le domaine à la création, par numérotation automatique propre au type et à l'année — l'API ne le fournit jamais.

« Produit » est un terme volontairement générique : l'application s'adresse à plusieurs entreprises clientes, dont les métiers nomment différemment ce qu'elles fabriquent (des moules, chez le client de référence).

La `Fiche` porte ce que l'utilisateur peut renseigner et réviser. Ses deux champs sont **facultatifs** : un élément de fabrication se réduit légitimement à son seul numéro. La `Reference` est l'identifiant que l'entreprise donne elle-même à l'élément dans son propre système (numéro de moule, référence de plan) ; elle est **unique par entreprise** quand elle est renseignée, l'unicité étant portée par une contrainte du schéma du tenant. PostgreSQL considérant les `NULL` comme distincts, autant d'éléments que nécessaire peuvent rester sans référence, et deux entreprises peuvent employer la même.

La garde d'unicité vit dans `ElementsDeFabricationService`, qui lit le détenteur d'une référence par le port `ElementDeFabricationRepository.idPourReference` et lève `ReferenceDejaUtiliseeException` (409). La contrainte en base est le filet de dernier recours : deux créations strictement concurrentes de la même référence produiraient un 500 plutôt qu'un 409, cas assumé puisque la création est le fait du dirigeant ou de son assistante.

La création et la modification passent par des commandes d'action (`ElementDeFabricationToCreate`, `ElementDeFabricationToUpdate`) construites par l'adapter primaire et orchestrées par `ElementsDeFabricationService`, service de domaine pur qui porte les ports repository, compteur, préfixes et horloge.

Le repository et le compteur sont persistés en PostgreSQL, dans le schéma de l'entreprise courante : la numérotation repart donc de 1 pour chaque entreprise, et deux entreprises peuvent porter le même nom d'élément. Les préfixes restent en dur dans `InMemoryPrefixesDElementsDeFabrication`, donc communs à toutes les entreprises.

### Points ouverts

Confrontation du modèle aux réunions client du 01/08/2026 (`discussions/synthese-reunions-01-08-2026.md`). Ces points sont identifiés, non traités, et à trancher avant d'aller plus loin.

1. **Cycle de vie.** Le client décrit une clôture manuelle en back-office qui retire l'élément des écrans opérateurs (« tous les opérateurs ne verront que les OF qui sont ouverts, actifs ») et qui déclenche la synthèse de coût (« quand on boucle l'OF, là ça sort la synthèse »). Aucun statut ni transition `cloturer` n'existe aujourd'hui : seule la saisie des produits est développée.
2. **Relation produit ↔ ordre de fabrication.** Le client décrit un enchaînement (« ce moule neuf, une fois testé, s'il y a une opération à faire dessus, ça se transforme en OF ») mais ne demande jamais le lien, et l'imposer exclurait les modifications sur des produits antérieurs à l'application. Le jour où ce lien sera ajouté, il ne concernera que les ordres de fabrication : les deux types cesseront de ne différer que par leur valeur, ce qui rouvrira la question de scinder l'agrégat unique — aujourd'hui justifié précisément parce qu'ils ne diffèrent par aucun champ.
3. **Suppression.** Le client ne parle jamais de supprimer, seulement de clôturer. Dès que les temps seront saisis, la suppression d'un élément qui en porte devra être interdite : elle détruirait des heures de paie.
4. **Numérotation et préfixes.** Les préfixes sont figés pour toutes les entreprises, ce qui contredit la cible multi-clients. L'année et le reset annuel du format `PRD-2026-000001` n'ont par ailleurs aucune source client, alors que des ordres de fabrication durant plusieurs mois traversent les millésimes.
5. **Critère de lecture.** `ElementDeFabricationCriteria` ne filtre que par période de création et la liste est paginée. Aucun écran décrit par le client ne filtre ainsi : le seul critère cité est « actifs seulement », et l'écran opérateur veut tout voir d'un coup (« rien qui défile »). À revoir avec le cycle de vie.
