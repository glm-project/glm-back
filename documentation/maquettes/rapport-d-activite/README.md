# Maquettes du rapport d'activité de l'opérateur

Fichiers cibles validés le 12 octobre 2026 pour la feature glm-back#151. Ce sont des **maquettes**, pas la sortie
du code : elles fixent le contenu et la forme que les exports doivent atteindre. Données d'exemple : Martin Lefèvre
(identifiant `MLE`), semaine 38 et septembre 2026.

| Fichier                              | Ticket       | Contenu                                                                                  |
| ------------------------------------ | ------------ | ---------------------------------------------------------------------------------------- |
| `activite-MLE-S38-2026.xlsx`         | glm-back#153 | Excel d'une semaine : onglet Synthèse (`Jours`, `Elements`, `Natures`), onglet Pointages |
| `activite-MLE-2026-09.xlsx`          | glm-back#153 | Excel d'un mois, même structure                                                          |
| `activite-MLE-S38-2026-synthese.pdf` | glm-back#154 | PDF synthèse d'une semaine (une page)                                                    |
| `activite-MLE-2026-09-synthese.pdf`  | glm-back#154 | PDF synthèse d'un mois                                                                   |
| `activite-MLE-2026-09-detail.pdf`    | glm-back#154 | PDF détaillé d'un mois : la synthèse, puis le journal de tous les pointages              |
| `apercu-*.png`                       | —            | Aperçus affichés dans les tickets                                                        |

Les totaux concordent entre les fichiers d'une même période : somme des jours = somme des éléments = somme des natures
= somme des pointages = temps pointé (47,75 h pour la semaine 38, 164,08 h pour septembre). Une heure passée sur deux
postes en même temps compte sur chacun, comme dans la synthèse des heures (le 17 septembre).

Exemples présents : non-conformité (`NC`), fin automatique (le 18 septembre, close à son échéance), deux postes en
même temps (le 17 septembre), jours sans pointage, samedi travaillé (le 19 septembre). Une activité **en cours**
n'apparaît que dans la période courante : elle n'a pas d'exemple ici, le journal la montre sans fin ni durée avec la
mention « en cours ».

Le tableau par nature de travail n'apparaît que si la période compte au moins deux natures.

Écarts connus avec l'export final : le logo n'y est qu'un emplacement réservé, alors que l'export affiche le logo
déposé dans le paramétrage (case vide sans logo). Le nom de l'entreprise reste à fournir plus tard. L'Excel n'a pas
de logo.
