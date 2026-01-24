# Documentation des Styles et Cartes (FR)

Ce document explique comment utiliser les styles de cartes dans le projet Open4Goods.

## Standardisation : NudgerCard

Nous utilisons un composant unique pour toutes les cartes (simples ou interactives) : **`NudgerCard`**.

**Chemin** : `frontend/app/components/shared/cards/NudgerCard.vue`

### Cas 1 : Carte Simple (Layout / Wrapper)
Pour entourer du contenu visuel ou textuel sans interaction particulière (pas de hover, pas de clic).

```vue
<NudgerCard
  :flat-corners="[]"       
  :accent-corners="[]"     
  base-radius="24px"       
  padding="0"              
  :border="false"          
  :shadow="false"          
  :hoverable="false"       
  background="transparent" 
>
  <!-- Votre contenu (image, texte...) -->
</NudgerCard>
```

### Cas 2 : Carte Interactive (Call to Action, Sélection)
Pour les éléments cliquables avec feedback visuel.

```vue
<NudgerCard
  :accent-corners="['bottom-right']" 
  accent-radius="40px"
  :shadow="true"
  :hoverable="true"
  padding="md"
>
  <!-- Contenu interactif -->
</NudgerCard>
```

### Props Principales
- `baseRadius`: Rayon par défaut des coins (ex: '30px').
- `accentCorners`: Liste des coins à accentuer (ex: `['top-left']`).
- `flatCorners`: Liste des coins à "aplatir" (rayon 0).
- `shadow`: Active l'ombre portée (true/false).
- `hoverable`: Active l'animation au survol (true/false).
- `border`: Ajoute une bordure (true/false).
