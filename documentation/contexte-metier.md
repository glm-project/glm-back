# Contexte métier

Décrit les bounded contexts métier du projet et leur rôle. Les règles de code (architecture, DDD, tests, conventions) sont dans `glm-back/CLAUDE.md`.

## elementdefabrication

Gère les éléments de fabrication. Un `ElementDeFabrication` porte son `TypeDElementDeFabrication` — ordre de fabrication ou produit — comme une valeur : les deux ne diffèrent aujourd'hui que par ce type, par leur préfixe de nommage et par leur série de numérotation. Le nom est toujours produit par le domaine à la création, par numérotation automatique propre au type et à l'année — l'API ne le fournit jamais.

La création et la modification passent par des commandes d'action (`ElementDeFabricationToCreate`, `ElementDeFabricationToUpdate`) construites par l'adapter primaire et orchestrées par `ElementsDeFabricationService`, service de domaine pur qui porte les ports repository, compteur, préfixes et horloge.
