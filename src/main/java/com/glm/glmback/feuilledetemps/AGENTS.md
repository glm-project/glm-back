# Bounded context `feuilledetemps`

Les règles communes sont dans [glm-back/AGENTS.md](../../../../../../../AGENTS.md), les raisons métier dans
[documentation/contexte-metier.md](../../../../../../../documentation/contexte-metier.md).

## Responsabilité et vocabulaire

Ramener les activités interprétées par atelier au calendrier de l'entreprise : un opérateur, une semaine ISO
explicite, sept jours du lundi au dimanche, vides compris. Ce contexte est purement lecteur et ne possède aucune table.

`FeuilleDeTemps` porte l'opérateur résolu, la semaine et ses `JourDeLaSemaine`. Chaque jour porte des
`IntervalleDActivite` : affectation (`Activite`), bornes de portion (`Plage`) et identité, état et bornes de
l'activité entière (`ActiviteLue`). `ActiviteInterpretee` est la projection reçue du port et évaluée à la lecture.

Atelier possède le pointage, sa correction et l'interprétation. La feuille ne rejoue aucun journal, ne calcule
aucune durée ni valorisation et ne porte aucune présence ni temps présumé. Le référentiel possède l'opérateur ;
la feuille expose les identifiants de poste et d'élément sans lire leurs libellés.

## Invariants

- `ActivitesDeLOperateur` lit `activite_d_atelier` par une entité propre `@Immutable`, jointe au suivi pour son élément.
  Aucun import des contextes `atelier`, `operateur` ou `postedetravail`, tous annotés `@BusinessContext`.
- La sélection porte sur le recouvrement de la semaine par le début et la fin réelle ou l'échéance. Aucune borne
  basse fixe du début : une régularisation peut établir plus de 13 h, voire plus d'une semaine.
- `FeuillesDeTempsService` relève `Clock.now()` une seule fois et transmet cet instant à toutes les activités.
  La transaction en lecture seule ne garantit pas un instantané commun face aux écritures concurrentes.
- Les états sont `TERMINEE`, `TERMINEE_AUTOMATIQUEMENT`, `EN_COURS`, `A_RESOUDRE`. Une fin réelle est conservée,
  même au-delà de l'échéance. Sans elle, l'échéance atteinte termine automatiquement l'activité à cette borne.
  Une activité à résoudre reste sans fin ; l'échéance ne résout pas le conflit.
- Chaque portion conserve l'identité stable, l'état et les bornes de l'activité entière. La fin n'existe que pour
  les deux états terminés. Le découpage aux minuits locaux et aux limites de semaine ne déplace jamais ces bornes.
- Une activité en cours rend une indication sans fin sur chaque jour atteint à l'instant de lecture, dans la semaine.
  Son début entier permet de lire « en cours depuis dimanche » sur lundi, sans durée à compter.
- Une activité à résoudre indique actuellement son jour de début ; son étendue sera complétée dans la tranche
  consacrée à la borne de conflit.
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
