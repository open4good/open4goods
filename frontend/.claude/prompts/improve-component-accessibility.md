# Améliorer l'accessibilité a11y d'un composant Vue

Vous allez analyser et améliorer l'accessibilité (a11y) d'un composant Vue existant en suivant les standards WCAG 2.1 et les bonnes pratiques Vuetify.

## Objectif

Rendre le composant accessible à tous les utilisateurs, notamment ceux utilisant des technologies d'assistance (lecteurs d'écran, navigation au clavier, etc.).

## Étapes d'analyse et d'amélioration

### 1. Analyse du composant existant

**ÉTAPE 1 : Audit d'accessibilité**
- Lire le composant sélectionné et analyser sa structure
- Identifier les éléments interactifs (boutons, liens, formulaires)
- Vérifier la hiérarchie des titres (h1, h2, etc.)
- Examiner l'utilisation des couleurs et contrastes
- Analyser la navigation au clavier
- Vérifier les images et médias

### 2. Points de contrôle WCAG 2.1

**Niveau A (Critique)**
- [ ] **Contenu non-textuel** : Alt text pour les images
- [ ] **Média temporel** : Sous-titres, transcriptions
- [ ] **Adaptable** : Structure sémantique correcte
- [ ] **Distinguable** : Contraste minimum 4.5:1 (3:1 pour texte large)
- [ ] **Accessible au clavier** : Tous les éléments interactifs
- [ ] **Convulsions** : Pas de contenu clignotant
- [ ] **Navigable** : Liens descriptifs, focus visible
- [ ] **Compatible** : Markup valide, noms accessibles

**Niveau AA (Recommandé)**
- [ ] **Distinguable** : Contraste amélioré 7:1 (4.5:1 pour texte large)
- [ ] **Navigable** : Titres de page, ordre de focus logique
- [ ] **Saisie** : Labels et instructions clairs
- [ ] **Compatible** : Changements de statut annoncés

### 3. Implémentation Vuetify a11y

**Composants Vuetify avec accessibilité intégrée**
```vue
<!-- Boutons -->
<v-btn 
  :aria-label="buttonLabel"
  :disabled="isDisabled"
  @click="handleClick"
>
  <v-icon 
    :icon="iconName" 
    :aria-hidden="hasText"
  />
  {{ buttonText }}
</v-btn>

<!-- Navigation -->
<nav aria-label="Navigation principale">
  <v-list>
    <v-list-item 
      v-for="item in menuItems"
      :key="item.id"
      :to="item.path"
      :aria-current="$route.path === item.path ? 'page' : undefined"
    >
      {{ item.title }}
    </v-list-item>
  </v-list>
</nav>

<!-- Formulaires -->
<v-form @submit.prevent="handleSubmit">
  <v-text-field
    v-model="email"
    :label="$t('email')"
    :error-messages="emailErrors"
    :aria-describedby="emailErrors.length ? 'email-errors' : undefined"
    required
    type="email"
  />
  <div 
    v-if="emailErrors.length" 
    id="email-errors" 
    class="sr-only"
  >
    {{ emailErrors.join(', ') }}
  </div>
</v-form>

<!-- Images -->
<v-img 
  :src="imageSrc"
  :alt="imageAlt"
  :aria-describedby="hasCaption ? 'img-caption' : undefined"
/>
<p v-if="hasCaption" id="img-caption" class="text-caption">
  {{ imageCaption }}
</p>

<!-- Alertes et statuts -->
<v-alert 
  :type="alertType"
  :aria-live="isImportant ? 'assertive' : 'polite'"
  role="alert"
>
  {{ alertMessage }}
</v-alert>
```

### 4. Attributs ARIA essentiels

**Navigation et structure**
- `role`: Définit le rôle sémantique (`button`, `navigation`, `main`, etc.)
- `aria-label`: Nom accessible quand le texte visible n'est pas suffisant
- `aria-labelledby`: Référence à un élément qui labellise
- `aria-describedby`: Référence à une description supplémentaire

**États et propriétés**
- `aria-current`: Page/étape actuelle (`page`, `step`, `true`)
- `aria-expanded`: État d'un élément collapsible
- `aria-hidden`: Cache aux technologies d'assistance
- `aria-live`: Annonce les changements (`polite`, `assertive`)
- `aria-pressed`: État d'un bouton toggle

**Formulaires**
- `aria-required`: Champ obligatoire
- `aria-invalid`: Champ en erreur
- `aria-describedby`: Description d'aide ou d'erreur

### 5. Navigation au clavier

**Support des raccourcis standards**
- `Tab` / `Shift+Tab` : Navigation séquentielle
- `Enter` / `Space` : Activation des boutons
- `Escape` : Fermeture des modales/menus
- Flèches : Navigation dans les listes/menus

```vue
<template>
  <div 
    @keydown.enter="handleActivation"
    @keydown.space.prevent="handleActivation"
    @keydown.escape="handleClose"
    :tabindex="isInteractive ? 0 : -1"
  >
    <!-- Contenu -->
  </div>
</template>
```

### 6. Gestion du focus

**Focus management**
```vue
<script setup lang="ts">
import { ref, nextTick } from 'vue'

const focusTarget = ref<HTMLElement>()

const handleModalOpen = async () => {
  // Ouvrir la modale
  await nextTick()
  focusTarget.value?.focus()
}

const handleModalClose = () => {
  // Fermer et restaurer le focus
  previousFocusElement?.focus()
}
</script>

<template>
  <v-dialog v-model="isOpen" @after-enter="handleModalOpen">
    <v-card>
      <v-card-title>
        <h2 ref="focusTarget" tabindex="-1">Titre de la modale</h2>
      </v-card-title>
      <!-- Contenu -->
    </v-card>
  </v-dialog>
</template>
```

### 7. Couleurs et contrastes

**Vérification des contrastes**
- Texte normal : ratio minimum 4.5:1
- Texte large (18pt+ ou 14pt+ gras) : ratio minimum 3:1
- Éléments d'interface : ratio minimum 3:1

**Ne pas utiliser uniquement la couleur**
```vue
<!-- ❌ Mauvais : seule la couleur indique l'état -->
<span :class="{ 'text-red': hasError }">{{ message }}</span>

<!-- ✅ Bon : couleur + icône + texte -->
<div class="d-flex align-center">
  <v-icon 
    v-if="hasError" 
    icon="mdi-alert-circle" 
    color="error"
    :aria-hidden="true"
  />
  <span :class="{ 'text-error': hasError }">
    {{ hasError ? 'Erreur: ' : '' }}{{ message }}
  </span>
</div>
```

### 8. Responsive et zoom

**Support du zoom jusqu'à 200%**
```vue
<template>
  <!-- Utiliser les unités relatives et les classes Vuetify -->
  <v-container class="pa-4">
    <v-row>
      <v-col cols="12" sm="6" md="4">
        <!-- Contenu adaptatif -->
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
/* Éviter les tailles fixes */
.custom-element {
  min-height: 44px; /* Taille minimale tactile */
  padding: 0.75rem; /* Unités relatives */
}
</style>
```

### 9. Internationalisation (i18n)

**Attributs de langue**
```vue
<template>
  <div :lang="$i18n.locale">
    <h1>{{ $t('page.title') }}</h1>
    
    <!-- Texte dans une autre langue -->
    <blockquote lang="en">
      "The only way to do great work is to love what you do."
    </blockquote>
  </div>
</template>
```

### 10. Tests d'accessibilité

**Outils de test recommandés**
- **axe-core** : Intégration avec Vitest
- **Lighthouse** : Audit automatique
- **NVDA/JAWS** : Test avec lecteurs d'écran
- **Navigation clavier** : Test manuel

```typescript
// tests/accessibility.spec.ts
import { mount } from '@vue/test-utils'
import { axe, toHaveNoViolations } from 'jest-axe'
import Component from '@/components/Component.vue'

expect.extend(toHaveNoViolations)

describe('Accessibility tests', () => {
  it('should not have accessibility violations', async () => {
    const wrapper = mount(Component)
    const results = await axe(wrapper.element)
    expect(results).toHaveNoViolations()
  })
})
```

## Checklist finale

**Structure et sémantique**
- [ ] Hiérarchie des titres correcte (h1 > h2 > h3...)
- [ ] Landmarks ARIA (`main`, `nav`, `aside`, etc.)
- [ ] Listes structurées avec `ul`/`ol`/`li`
- [ ] Texte alternatif pour toutes les images informatives

**Navigation et interaction**
- [ ] Tous les éléments interactifs accessibles au clavier
- [ ] Ordre de tabulation logique
- [ ] Focus visible sur tous les éléments
- [ ] Pas de piège à clavier

**Formulaires**
- [ ] Labels associés à tous les champs
- [ ] Messages d'erreur descriptifs et liés
- [ ] Instructions claires
- [ ] Validation accessible

**Couleurs et contraste**
- [ ] Contraste suffisant (4.5:1 minimum)
- [ ] Information pas uniquement transmise par la couleur
- [ ] Support du mode sombre/clair

**Contenu dynamique**
- [ ] Changements d'état annoncés (`aria-live`)
- [ ] Messages d'erreur/succès accessibles
- [ ] Modales et overlays correctement gérés

**Mobile et responsive**
- [ ] Taille minimale tactile 44x44px
- [ ] Support du zoom 200%
- [ ] Navigation tactile accessible

## Dynamic Arguments
$ARGUMENTS

---

**IMPORTANT**: Priorisez les améliorations par impact utilisateur et implémentez de manière progressive. Testez chaque modification avec les outils d'accessibilité disponibles.