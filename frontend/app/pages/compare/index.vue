<template>
  <div class="compare-page">
    <PageHeader
      :title="heroTitle"
      :eyebrow="t('compare.hero.eyebrow')"
      layout="single-column"
      container="lg"
      background="image"
      background-image-asset-key="compareBackground"
      class="mb-8"
    >
      <template #subtitle>
        <p class="page-header__subtitle">{{ heroSubtitle }}</p>
        <p
          v-if="productCount"
          class="d-flex align-center justify-center gap-2 mt-2 text-medium-emphasis"
        >
          <v-icon
            icon="mdi-package-variant-closed"
            size="18"
            aria-hidden="true"
          />
          <span>{{ compareSummary }}</span>
        </p>
      </template>

      <template v-if="heroBackLink" #actions>
        <v-btn
          :to="heroBackLink"
          variant="text"
          prepend-icon="mdi-arrow-left"
          class="mb-4 compare-page__hero-back"
        >
          {{ heroBackLabel }}
        </v-btn>
      </template>
    </PageHeader>

    <v-alert
      v-if="loadError"
      type="error"
      variant="tonal"
      border="start"
      class="mb-6"
      role="alert"
    >
      {{ loadError }}
    </v-alert>

    <v-alert
      v-else-if="hasMixedVerticals"
      type="warning"
      variant="tonal"
      border="start"
      class="mb-6"
      role="alert"
    >
      {{ t('compare.alerts.verticalMismatch') }}
    </v-alert>

    <v-progress-linear
      v-if="loading"
      indeterminate
      color="primary"
      class="mb-10"
      :aria-label="t('compare.states.loading')"
    />

    <div v-else-if="!products.length" class="compare-page__empty">
      <v-icon
        icon="mdi-compare-horizontal"
        size="64"
        class="compare-page__empty-icon"
      />
      <h2 class="compare-page__empty-title">{{ t('compare.empty.title') }}</h2>
      <p class="compare-page__empty-text">
        {{ t('compare.empty.description') }}
      </p>
    </div>

    <div v-else class="compare-page__content">
      <section class="compare-section">
        <v-container fluid class="pa-0" max-width="xxl">
          <div class="compare-grid" role="table">
            <div class="compare-grid__media" role="row" aria-hidden="true">
              <div
                class="compare-grid__feature compare-grid__feature--media"
                role="presentation"
              >
                <span class="sr-only">{{
                  t('compare.a11y.featureColumn')
                }}</span>
              </div>
              <div class="compare-grid__products" role="presentation">
                <div
                  v-for="product in products"
                  :key="`media-${product.gtin}`"
                  class="compare-grid__product-media"
                >
                  <NuxtLink
                    v-if="productLink(product)"
                    :to="productLink(product)"
                    class="compare-grid__product-link"
                    :aria-label="
                      t('compare.a11y.viewProduct', { name: product.title })
                    "
                  >
                    <NuxtImg
                      v-if="product.coverImage"
                      :src="product.coverImage"
                      :alt="product.title"
                      width="180"
                      height="180"
                      format="webp"
                      class="compare-grid__product-image"
                    />
                    <div
                      v-else
                      class="compare-grid__product-placeholder"
                      aria-hidden="true"
                    >
                      {{ productInitials(product.title) }}
                    </div>
                  </NuxtLink>
                  <template v-else>
                    <NuxtImg
                      v-if="product.coverImage"
                      :src="product.coverImage"
                      :alt="product.title"
                      width="180"
                      height="180"
                      format="webp"
                      class="compare-grid__product-image"
                    />
                    <div
                      v-else
                      class="compare-grid__product-placeholder"
                      aria-hidden="true"
                    >
                      {{ productInitials(product.title) }}
                    </div>
                  </template>
                </div>
              </div>
            </div>
            <div class="compare-grid__header" role="row">
              <div
                class="compare-grid__feature compare-grid__feature--header"
                role="columnheader"
              >
                <span class="sr-only">{{
                  t('compare.a11y.featureColumn')
                }}</span>
              </div>
              <div class="compare-grid__products" role="rowgroup">
                <article
                  v-for="product in products"
                  :key="product.gtin"
                  class="compare-grid__product"
                  role="columnheader"
                >
                  <ImpactScore
                    v-if="product.impactScore !== null"
                    :score="product.impactScore"
                    :max="5"
                    size="medium"
                    class="compare-grid__product-impact"
                  />
                  <NuxtLink
                    v-if="productLink(product)"
                    :to="productLink(product)"
                    class="compare-grid__product-model compare-grid__product-model--link"
                    :aria-label="
                      t('compare.a11y.viewProduct', { name: product.title })
                    "
                  >
                    {{ productModelLabel(product) }}
                  </NuxtLink>
                  <p v-else class="compare-grid__product-model">
                    {{ productModelLabel(product) }}
                  </p>
                  <p class="compare-grid__product-brand">
                    {{ product.brand ?? '—' }}
                  </p>
                  <div
                    v-if="product.country"
                    class="compare-grid__product-country"
                  >
                    <v-tooltip :text="product.country.name" location="bottom">
                      <template #activator="{ props: tooltipProps }">
                        <span
                          class="compare-grid__product-country-flag"
                          v-bind="tooltipProps"
                        >
                          <NuxtImg
                            v-if="product.country.flag"
                            :src="product.country.flag"
                            :alt="product.country.name"
                            width="24"
                            height="16"
                            class="compare-grid__flag"
                          />
                          <span class="compare-grid__country-label">{{
                            product.country.name
                          }}</span>
                        </span>
                      </template>
                    </v-tooltip>
                  </div>
                  <v-btn
                    variant="text"
                    size="small"
                    color="error"
                    class="compare-grid__product-remove"
                    :aria-label="
                      t('compare.actions.remove', { name: product.title })
                    "
                    @click="handleRemove(product.gtin)"
                  >
                    {{ t('compare.actions.removeShort') }}
                  </v-btn>
                </article>
              </div>
            </div>

            <div
              v-for="row in textualRows"
              :key="row.key"
              class="compare-grid__row"
              role="row"
            >
              <div class="compare-grid__feature" role="rowheader">
                <v-icon
                  :icon="row.icon"
                  size="20"
                  class="compare-grid__feature-icon"
                />
                <span class="compare-grid__feature-label">{{ row.label }}</span>
              </div>
              <div class="compare-grid__values" role="cell">
                <div
                  v-for="(product, columnIndex) in products"
                  :key="`${row.key}-${product.gtin}`"
                  :class="[
                    'compare-grid__value',
                    { 'compare-grid__value--has-list': row.type === 'list' },
                  ]"
                >
                  <div v-if="product" class="compare-grid__value-mobile">
                    <ClientOnly>
                      <NuxtLink
                        v-if="productLink(product)"
                        :to="productLink(product)"
                        class="compare-grid__value-mobile-media compare-grid__value-mobile-media--link"
                        :aria-label="
                          t('compare.a11y.viewProduct', { name: product.title })
                        "
                      >
                        <NuxtImg
                          v-if="product.coverImage"
                          :src="product.coverImage"
                          :alt="product.title"
                          width="88"
                          height="88"
                          format="webp"
                          class="compare-grid__value-mobile-image"
                        />
                        <div
                          v-else
                          class="compare-grid__value-mobile-placeholder"
                          aria-hidden="true"
                        >
                          {{ productInitials(product.title) }}
                        </div>
                      </NuxtLink>
                      <div v-else class="compare-grid__value-mobile-media">
                        <NuxtImg
                          v-if="product.coverImage"
                          :src="product.coverImage"
                          :alt="product.title"
                          width="88"
                          height="88"
                          format="webp"
                          class="compare-grid__value-mobile-image"
                        />
                        <div
                          v-else
                          class="compare-grid__value-mobile-placeholder"
                          aria-hidden="true"
                        >
                          {{ productInitials(product.title) }}
                        </div>
                      </div>
                    </ClientOnly>
                    <div class="compare-grid__value-mobile-details">
                      <p class="compare-grid__value-mobile-brand">
                        {{ product.brand ?? '—' }}
                      </p>
                      <p class="compare-grid__value-mobile-model">
                        {{ productModelLabel(product) }}
                      </p>
                    </div>
                  </div>

                  <div class="compare-grid__value-content">
                    <template v-if="row.type === 'text'">
                      <!-- eslint-disable vue/no-v-html -->
                      <p
                        v-if="row.values[columnIndex]"
                        class="compare-grid__value-text"
                        v-html="row.values[columnIndex]"
                      />
                      <!-- eslint-enable vue/no-v-html -->
                      <p v-else class="compare-grid__value-text">
                        {{ t('compare.textual.empty') }}
                      </p>
                    </template>
                    <template v-else>
                      <!-- eslint-disable vue/no-v-html -->
                      <ul
                        v-if="hasListValues(row.values[columnIndex])"
                        class="compare-grid__list compare-grid__list--pros-cons"
                        role="list"
                      >
                        <li
                          v-for="item in getListValues(row.values[columnIndex])"
                          :key="item"
                          class="compare-grid__list-item"
                          role="listitem"
                          v-html="item"
                        />
                      </ul>
                      <!-- eslint-enable vue/no-v-html -->
                      <p v-else class="compare-grid__value-text">
                        {{ t('compare.textual.empty') }}
                      </p>
                    </template>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </v-container>
      </section>

      <section class="compare-section">
        <h2 class="compare-section__title">
          {{ t('compare.sections.pricing') }}
        </h2>
        <div class="compare-grid compare-grid--compact" role="table">
          <div
            v-for="row in priceRows"
            :key="row.key"
            class="compare-grid__row"
            role="row"
          >
            <div class="compare-grid__feature" role="rowheader">
              <v-icon
                :icon="row.icon"
                size="20"
                class="compare-grid__feature-icon"
              />
              <span class="compare-grid__feature-label">{{ row.label }}</span>
            </div>
            <div class="compare-grid__values" role="cell">
              <div
                v-for="(product, index) in products"
                :key="`${row.key}-${product.gtin}`"
                :class="[
                  'compare-grid__value',
                  {
                    'compare-grid__value--highlight': row.highlight.has(index),
                  },
                ]"
              >
                <div v-if="product" class="compare-grid__value-mobile">
                  <ClientOnly>
                    <NuxtLink
                      v-if="productLink(product)"
                      :to="productLink(product)"
                      class="compare-grid__value-mobile-media compare-grid__value-mobile-media--link"
                      :aria-label="
                        t('compare.a11y.viewProduct', { name: product.title })
                      "
                    >
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </NuxtLink>
                    <div v-else class="compare-grid__value-mobile-media">
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </div>
                  </ClientOnly>
                  <div class="compare-grid__value-mobile-details">
                    <p class="compare-grid__value-mobile-brand">
                      {{ product.brand ?? '—' }}
                    </p>
                    <p class="compare-grid__value-mobile-model">
                      {{ productModelLabel(product) }}
                    </p>
                  </div>
                </div>

                <div class="compare-grid__value-content">
                  <span v-if="row.highlight.has(index)" class="sr-only">{{
                    t('compare.a11y.bestValue')
                  }}</span>
                  <div class="compare-grid__value-inner">
                    <v-icon
                      v-if="row.highlight.has(index)"
                      :icon="highlightIcon"
                      class="compare-grid__value-badge"
                      size="20"
                      aria-hidden="true"
                    />
                    <!-- eslint-disable vue/no-v-html -->
                    <p
                      v-if="hasMeaningfulCellValue(row.values[index])"
                      class="compare-grid__value-text"
                      v-html="row.values[index]"
                    />
                    <!-- eslint-enable vue/no-v-html -->
                    <p v-else class="compare-grid__value-text">
                      {{ t('compare.textual.empty') }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="compare-section">
        <h2 class="compare-section__title">
          {{ t('compare.sections.ecological') }}
        </h2>
        <div class="compare-grid compare-grid--compact" role="table">
          <div
            v-for="row in ecologicalScoreRows"
            :key="row.key"
            class="compare-grid__row"
            role="row"
          >
            <div class="compare-grid__feature" role="rowheader">
              <v-icon
                :icon="row.icon"
                size="20"
                class="compare-grid__feature-icon"
              />
              <span class="compare-grid__feature-label">{{ row.label }}</span>
            </div>
            <div class="compare-grid__values" role="cell">
              <div
                v-for="(product, index) in products"
                :key="`${row.key}-${product.gtin}`"
                :class="[
                  'compare-grid__value',
                  {
                    'compare-grid__value--highlight': row.highlight.has(index),
                  },
                ]"
              >
                <div v-if="product" class="compare-grid__value-mobile">
                  <ClientOnly>
                    <NuxtLink
                      v-if="productLink(product)"
                      :to="productLink(product)"
                      class="compare-grid__value-mobile-media compare-grid__value-mobile-media--link"
                      :aria-label="
                        t('compare.a11y.viewProduct', { name: product.title })
                      "
                    >
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </NuxtLink>
                    <div v-else class="compare-grid__value-mobile-media">
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </div>
                  </ClientOnly>
                  <div class="compare-grid__value-mobile-details">
                    <p class="compare-grid__value-mobile-brand">
                      {{ product.brand ?? '—' }}
                    </p>
                    <p class="compare-grid__value-mobile-model">
                      {{ productModelLabel(product) }}
                    </p>
                  </div>
                </div>

                <div class="compare-grid__value-content">
                  <span v-if="row.highlight.has(index)" class="sr-only">{{
                    t('compare.a11y.bestValue')
                  }}</span>
                  <div class="compare-grid__value-inner">
                    <v-icon
                      v-if="row.highlight.has(index)"
                      :icon="highlightIcon"
                      class="compare-grid__value-badge"
                      size="20"
                      aria-hidden="true"
                    />
                    <!-- eslint-disable vue/no-v-html -->
                    <p
                      v-if="hasMeaningfulCellValue(row.values[index])"
                      class="compare-grid__value-text"
                      v-html="row.values[index]"
                    />
                    <!-- eslint-enable vue/no-v-html -->
                    <p v-else class="compare-grid__value-text">
                      {{ t('compare.textual.empty') }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-for="row in ecologicalAttributeRows"
            :key="row.key"
            class="compare-grid__row"
            role="row"
          >
            <div class="compare-grid__feature" role="rowheader">
              <v-icon
                :icon="row.icon"
                size="20"
                class="compare-grid__feature-icon"
              />
              <span class="compare-grid__feature-label">{{ row.label }}</span>
            </div>
            <div class="compare-grid__values" role="cell">
              <div
                v-for="(product, index) in products"
                :key="`${row.key}-${product.gtin}`"
                :class="[
                  'compare-grid__value',
                  {
                    'compare-grid__value--highlight': row.highlight.has(index),
                  },
                ]"
              >
                <div v-if="product" class="compare-grid__value-mobile">
                  <ClientOnly>
                    <NuxtLink
                      v-if="productLink(product)"
                      :to="productLink(product)"
                      class="compare-grid__value-mobile-media compare-grid__value-mobile-media--link"
                      :aria-label="
                        t('compare.a11y.viewProduct', { name: product.title })
                      "
                    >
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </NuxtLink>
                    <div v-else class="compare-grid__value-mobile-media">
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </div>
                  </ClientOnly>
                  <div class="compare-grid__value-mobile-details">
                    <p class="compare-grid__value-mobile-brand">
                      {{ product.brand ?? '—' }}
                    </p>
                    <p class="compare-grid__value-mobile-model">
                      {{ productModelLabel(product) }}
                    </p>
                  </div>
                </div>

                <div class="compare-grid__value-content">
                  <span v-if="row.highlight.has(index)" class="sr-only">{{
                    t('compare.a11y.bestValue')
                  }}</span>
                  <div class="compare-grid__value-inner">
                    <v-icon
                      v-if="row.highlight.has(index)"
                      :icon="highlightIcon"
                      class="compare-grid__value-badge"
                      size="20"
                      aria-hidden="true"
                    />
                    <!-- eslint-disable vue/no-v-html -->
                    <p
                      v-if="hasMeaningfulCellValue(row.values[index])"
                      class="compare-grid__value-text"
                      v-html="row.values[index]"
                    />
                    <!-- eslint-enable vue/no-v-html -->
                    <p v-else class="compare-grid__value-text">
                      {{ t('compare.textual.empty') }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="compare-section">
        <h2 class="compare-section__title">
          {{ t('compare.sections.technical') }}
        </h2>
        <div class="compare-grid compare-grid--compact" role="table">
          <div
            v-for="row in popularAttributeRows"
            :key="row.key"
            class="compare-grid__row"
            role="row"
          >
            <div class="compare-grid__feature" role="rowheader">
              <v-icon
                :icon="row.icon"
                size="20"
                class="compare-grid__feature-icon"
              />
              <span class="compare-grid__feature-label">{{ row.label }}</span>
            </div>
            <div class="compare-grid__values" role="cell">
              <div
                v-for="(product, index) in products"
                :key="`${row.key}-${product.gtin}`"
                :class="[
                  'compare-grid__value',
                  {
                    'compare-grid__value--highlight': row.highlight.has(index),
                  },
                ]"
              >
                <div v-if="product" class="compare-grid__value-mobile">
                  <ClientOnly>
                    <NuxtLink
                      v-if="productLink(product)"
                      :to="productLink(product)"
                      class="compare-grid__value-mobile-media compare-grid__value-mobile-media--link"
                      :aria-label="
                        t('compare.a11y.viewProduct', { name: product.title })
                      "
                    >
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </NuxtLink>
                    <div v-else class="compare-grid__value-mobile-media">
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </div>
                  </ClientOnly>
                  <div class="compare-grid__value-mobile-details">
                    <p class="compare-grid__value-mobile-brand">
                      {{ product.brand ?? '—' }}
                    </p>
                    <p class="compare-grid__value-mobile-model">
                      {{ productModelLabel(product) }}
                    </p>
                  </div>
                </div>

                <div class="compare-grid__value-content">
                  <span v-if="row.highlight.has(index)" class="sr-only">{{
                    t('compare.a11y.bestValue')
                  }}</span>
                  <div class="compare-grid__value-inner">
                    <v-icon
                      v-if="row.highlight.has(index)"
                      :icon="highlightIcon"
                      class="compare-grid__value-badge"
                      size="20"
                      aria-hidden="true"
                    />
                    <!-- eslint-disable vue/no-v-html -->
                    <p
                      v-if="hasMeaningfulCellValue(row.values[index])"
                      class="compare-grid__value-text"
                      v-html="row.values[index]"
                    />
                    <!-- eslint-enable vue/no-v-html -->
                    <p v-else class="compare-grid__value-text">
                      {{ t('compare.textual.empty') }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-for="row in indexedAttributeRows"
            :key="row.key"
            class="compare-grid__row"
            role="row"
          >
            <div class="compare-grid__feature" role="rowheader">
              <v-icon
                :icon="row.icon"
                size="20"
                class="compare-grid__feature-icon"
              />
              <span class="compare-grid__feature-label">{{ row.label }}</span>
            </div>
            <div class="compare-grid__values" role="cell">
              <div
                v-for="(product, index) in products"
                :key="`${row.key}-${product.gtin}`"
                :class="[
                  'compare-grid__value',
                  {
                    'compare-grid__value--highlight': row.highlight.has(index),
                  },
                ]"
              >
                <div v-if="product" class="compare-grid__value-mobile">
                  <ClientOnly>
                    <NuxtLink
                      v-if="productLink(product)"
                      :to="productLink(product)"
                      class="compare-grid__value-mobile-media compare-grid__value-mobile-media--link"
                      :aria-label="
                        t('compare.a11y.viewProduct', { name: product.title })
                      "
                    >
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </NuxtLink>
                    <div v-else class="compare-grid__value-mobile-media">
                      <NuxtImg
                        v-if="product.coverImage"
                        :src="product.coverImage"
                        :alt="product.title"
                        width="88"
                        height="88"
                        format="webp"
                        class="compare-grid__value-mobile-image"
                      />
                      <div
                        v-else
                        class="compare-grid__value-mobile-placeholder"
                        aria-hidden="true"
                      >
                        {{ productInitials(product.title) }}
                      </div>
                    </div>
                  </ClientOnly>
                  <div class="compare-grid__value-mobile-details">
                    <p class="compare-grid__value-mobile-brand">
                      {{ product.brand ?? '—' }}
                    </p>
                    <p class="compare-grid__value-mobile-model">
                      {{ productModelLabel(product) }}
                    </p>
                  </div>
                </div>

                <div class="compare-grid__value-content">
                  <span v-if="row.highlight.has(index)" class="sr-only">{{
                    t('compare.a11y.bestValue')
                  }}</span>
                  <div class="compare-grid__value-inner">
                    <v-icon
                      v-if="row.highlight.has(index)"
                      :icon="highlightIcon"
                      class="compare-grid__value-badge"
                      size="20"
                      aria-hidden="true"
                    />
                    <!-- eslint-disable vue/no-v-html -->
                    <p
                      v-if="hasMeaningfulCellValue(row.values[index])"
                      class="compare-grid__value-text"
                      v-html="row.values[index]"
                    />
                    <!-- eslint-enable vue/no-v-html -->
                    <p v-else class="compare-grid__value-text">
                      {{ t('compare.textual.empty') }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div
            v-for="group in classifiedAttributeGroups"
            :key="group.name"
            class="compare-grid__group"
          >
            <h3 class="compare-grid__group-title">{{ group.name }}</h3>
            <div
              v-for="row in group.rows"
              :key="row.key"
              class="compare-grid__row"
              role="row"
            >
              <div class="compare-grid__feature" role="rowheader">
                <v-icon
                  :icon="row.icon"
                  size="20"
                  class="compare-grid__feature-icon"
                />
                <span class="compare-grid__feature-label">{{ row.label }}</span>
              </div>
              <div class="compare-grid__values" role="cell">
                <div
                  v-for="(product, index) in products"
                  :key="`${row.key}-${product.gtin}`"
                  :class="[
                    'compare-grid__value',
                    {
                      'compare-grid__value--highlight':
                        row.highlight.has(index),
                    },
                  ]"
                >
                  <div v-if="product" class="compare-grid__value-mobile">
                    <ClientOnly>
                      <NuxtLink
                        v-if="productLink(product)"
                        :to="productLink(product)"
                        class="compare-grid__value-mobile-media compare-grid__value-mobile-media--link"
                        :aria-label="
                          t('compare.a11y.viewProduct', { name: product.title })
                        "
                      >
                        <NuxtImg
                          v-if="product.coverImage"
                          :src="product.coverImage"
                          :alt="product.title"
                          width="88"
                          height="88"
                          format="webp"
                          class="compare-grid__value-mobile-image"
                        />
                        <div
                          v-else
                          class="compare-grid__value-mobile-placeholder"
                          aria-hidden="true"
                        >
                          {{ productInitials(product.title) }}
                        </div>
                      </NuxtLink>
                      <div v-else class="compare-grid__value-mobile-media">
                        <NuxtImg
                          v-if="product.coverImage"
                          :src="product.coverImage"
                          :alt="product.title"
                          width="88"
                          height="88"
                          format="webp"
                          class="compare-grid__value-mobile-image"
                        />
                        <div
                          v-else
                          class="compare-grid__value-mobile-placeholder"
                          aria-hidden="true"
                        >
                          {{ productInitials(product.title) }}
                        </div>
                      </div>
                    </ClientOnly>
                    <div class="compare-grid__value-mobile-details">
                      <p class="compare-grid__value-mobile-brand">
                        {{ product.brand ?? '—' }}
                      </p>
                      <p class="compare-grid__value-mobile-model">
                        {{ productModelLabel(product) }}
                      </p>
                    </div>
                  </div>

                  <div class="compare-grid__value-content">
                    <span v-if="row.highlight.has(index)" class="sr-only">{{
                      t('compare.a11y.bestValue')
                    }}</span>
                    <div class="compare-grid__value-inner">
                      <v-icon
                        v-if="row.highlight.has(index)"
                        :icon="highlightIcon"
                        class="compare-grid__value-badge"
                        size="20"
                        aria-hidden="true"
                      />
                      <!-- eslint-disable vue/no-v-html -->
                      <p
                        v-if="hasMeaningfulCellValue(row.values[index])"
                        class="compare-grid__value-text"
                        v-html="row.values[index]"
                      />
                      <!-- eslint-enable vue/no-v-html -->
                      <p v-else class="compare-grid__value-text">
                        {{ t('compare.textual.empty') }}
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'

import PageHeader from '~/components/shared/header/PageHeader.vue'
import ImpactScore from '~/components/shared/ui/ImpactScore.vue'
import {
  createCompareService,
  type CompareProductEntry,
} from '~/services/compare/CompareService'
import { useProductCompareStore } from '~/stores/useProductCompareStore'
import { buildCompareHash, parseCompareHash } from '~/utils/_compare-url'
import { formatBestPrice, formatOffersCount } from '~/utils/_product-pricing'
import {
  formatAttributeValue,
  resolveAttributeRawValueByKey,
  type ResolvedProductAttribute,
} from '~/utils/_product-attributes'
import { resolveScoreNumericValue } from '~/utils/score-values'
import { usePluralizedTranslation } from '~/composables/usePluralizedTranslation'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'
import type {
  AttributeConfigDto,
  ProductAggregatedPriceDto,
  ProductDto,
  ProductScoreDto,
  VerticalConfigFullDto,
} from '~~/shared/api-client'

interface TextualRow {
  key: string
  icon: string
  label: string
  type: 'text' | 'list'
  values: Array<string | string[] | null>
}

interface AttributeRow {
  key: string
  icon: string
  label: string
  values: Array<string | null>
  highlight: Set<number>
  availability: boolean[]
}

interface ClassifiedGroup {
  name: string
  rows: AttributeRow[]
}

const service = createCompareService()
const compareStore = useProductCompareStore()

const { t, locale, n } = useI18n()
const { translatePlural } = usePluralizedTranslation()
const router = useRouter()
const route = useRoute()
// const heroId = useId()

const canonicalUrl = useCanonicalUrl()

useHead(() => ({
  link: canonicalUrl.value
    ? [
        {
          rel: 'canonical',
          href: canonicalUrl.value,
        },
      ]
    : [],
}))

useSeoMeta({
  ogUrl: () => canonicalUrl.value || undefined,
})

definePageMeta({
  name: 'compare',
  ssr: false,
})

const loading = ref(false)
const loadError = ref<string | null>(null)
const products = ref<CompareProductEntry[]>([])
const verticalConfig = ref<VerticalConfigFullDto | null>(null)
const requestedGtins = ref<string[]>([])

const comparePath = computed(() =>
  resolveLocalizedRoutePath('compare', locale.value)
)

const productCount = computed(() => products.value.length)

const compareSummary = computed(() => {
  if (!productCount.value) {
    return ''
  }

  return translatePlural(
    'category.products.compare.itemsCount',
    productCount.value,
    {
      count: productCount.value,
    }
  )
})

const rawVerticalTitle = computed(
  () => verticalConfig.value?.verticalHomeTitle?.trim() ?? ''
)

const normalizedVerticalTitle = computed(() => {
  if (!rawVerticalTitle.value) {
    return ''
  }

  try {
    return rawVerticalTitle.value.toLocaleLowerCase(locale.value ?? undefined)
  } catch {
    return rawVerticalTitle.value.toLowerCase()
  }
})

const heroTitle = computed(() => {
  if (normalizedVerticalTitle.value) {
    return t('compare.hero.title', {
      verticalTitle: normalizedVerticalTitle.value,
    })
  }

  return t('compare.title')
})

const heroSubtitle = computed(() => {
  if (normalizedVerticalTitle.value) {
    return t('compare.hero.subtitle', {
      verticalTitle: normalizedVerticalTitle.value,
    })
  }

  return t('compare.subtitle')
})

const heroBackLink = computed(() => {
  const raw = verticalConfig.value?.verticalHomeUrl?.trim()
  if (!raw) {
    return null
  }

  if (/^https?:\/\//iu.test(raw)) {
    return raw
  }

  return raw.startsWith('/') ? raw : `/${raw}`
})

const heroBackLabel = computed(() => {
  if (normalizedVerticalTitle.value) {
    return t('compare.hero.backToCategory', {
      verticalTitle: normalizedVerticalTitle.value,
    })
  }

  return t('compare.hero.backFallback')
})

const highlightIcon = 'mdi-crown'

const hasMixedVerticals = computed(() =>
  service.hasMixedVerticals(products.value)
)

const productEntries = computed(() => products.value)

const attributeConfigs = computed(
  () => verticalConfig.value?.attributesConfig?.configs ?? []
)

const attributeConfigMap = computed(() => {
  return attributeConfigs.value.reduce<Map<string, AttributeConfigDto>>(
    (map, config) => {
      if (config?.key) {
        map.set(config.key, config)
      }
      return map
    },
    new Map<string, AttributeConfigDto>()
  )
})

const popularAttributeConfigs = computed(
  () => verticalConfig.value?.popularAttributes ?? []
)

const scoringAttributeConfigs = computed(() =>
  attributeConfigs.value.filter(config => config.asScore && config.key)
)

const normalizeNumericValue = (value: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'boolean') {
    return value ? 1 : 0
  }

  if (typeof value === 'string') {
    const normalised = value.replace(/[^0-9,.-]/g, '').replace(',', '.')
    const parsed = Number.parseFloat(normalised)
    return Number.isFinite(parsed) ? parsed : null
  }

  return null
}

const computeHighlightSet = (
  values: Array<number | null>,
  preference: 'higher' | 'lower'
): Set<number> => {
  const entries = values
    .map((value, index) => ({ index, value }))
    .filter(
      (entry): entry is { index: number; value: number } => entry.value != null
    )

  if (!entries.length) {
    return new Set()
  }

  const targetValue =
    preference === 'higher'
      ? Math.max(...entries.map(entry => entry.value))
      : Math.min(...entries.map(entry => entry.value))

  return new Set(
    entries
      .filter(entry => entry.value === targetValue)
      .map(entry => entry.index)
  )
}

const hasMeaningfulCellValue = (value: unknown): boolean => {
  if (value == null) {
    return false
  }

  if (Array.isArray(value)) {
    return value.some(entry => hasMeaningfulCellValue(entry))
  }

  if (typeof value === 'string') {
    return value.trim().length > 0
  }

  if (typeof value === 'number') {
    return Number.isFinite(value)
  }

  if (typeof value === 'boolean') {
    return true
  }

  return true
}

const hasListValues = (value: TextualRow['values'][number]): boolean => {
  return Array.isArray(value) && value.length > 0
}

const getListValues = (value: TextualRow['values'][number]): string[] => {
  return Array.isArray(value) ? value : []
}

const computeAvailabilityFromValues = (values: unknown[]): boolean[] => {
  return values.map(value => hasMeaningfulCellValue(value))
}

const hasEnoughAvailability = (availability: boolean[]): boolean => {
  const missing = availability.filter(value => !value).length
  return missing < 2
}

const filterSparseAttributeRows = (rows: AttributeRow[]): AttributeRow[] => {
  return rows.filter(row => hasEnoughAvailability(row.availability))
}

const formatPrice = (
  offer: ProductAggregatedPriceDto | undefined,
  product: ProductDto
): string | null => {
  if (!offer) {
    return null
  }

  const price = offer.price
  const currency = offer.currency ?? product.offers?.bestPrice?.currency
  const shortPrice = offer.shortPrice?.trim()

  if (shortPrice) {
    if (currency && !/[^0-9.,\s-]/u.test(shortPrice)) {
      return `${shortPrice} ${currency}`.trim()
    }

    return shortPrice
  }

  if (price == null) {
    return null
  }

  if (currency) {
    try {
      return n(price, { style: 'currency', currency })
    } catch {
      return `${n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${currency}`.trim()
    }
  }

  return n(price, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const resolveScoreValue = (
  score: ProductScoreDto | null | undefined
): { display: string | null; numeric: number | null } => {
  const resolved = resolveScoreNumericValue(score)

  if (resolved) {
    const formatOptions =
      resolved.source === 'percent'
        ? { maximumFractionDigits: 0 }
        : resolved.source === 'relative' || resolved.source === 'legacyRelative'
          ? { maximumFractionDigits: 2 }
          : { maximumFractionDigits: 1 }

    return {
      display: n(resolved.value, formatOptions),
      numeric: resolved.value,
    }
  }

  return {
    display: score?.absoluteValue ?? score?.relativeValue ?? null,
    numeric: null,
  }
}

const syncHash = async (hash: string) => {
  loading.value = true
  loadError.value = null

  try {
    const parsedGtins = parseCompareHash(hash)
    requestedGtins.value = parsedGtins

    if (!parsedGtins.length) {
      products.value = []
      verticalConfig.value = null
      compareStore.clear()
      loading.value = false
      return
    }

    const loadedProducts = await service.loadProducts(parsedGtins)
    products.value = loadedProducts

    compareStore.clear()
    loadedProducts.forEach(entry => {
      compareStore.addProduct(entry.product)
    })

    const verticalId =
      loadedProducts.find(entry => entry.verticalId)?.verticalId ?? null
    verticalConfig.value = await service.loadVertical(verticalId)
  } catch (error) {
    console.error('Failed to load comparison', error)
    loadError.value = t('compare.errors.loadFailed')
    products.value = []
    verticalConfig.value = null
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await syncHash(route.hash)
})

watch(
  () => route.hash,
  async next => {
    await syncHash(next)
  }
)

const textualRows = computed<TextualRow[]>(() => {
  const rows: TextualRow[] = [
    {
      key: 'description',
      icon: 'mdi-text-box-outline',
      label: t('compare.textual.description'),
      type: 'text',
      values: productEntries.value.map(entry => entry.review.description),
    },
    {
      key: 'pros',
      icon: 'mdi-thumb-up-outline',
      label: t('compare.textual.pros'),
      type: 'list',
      values: productEntries.value.map(entry => entry.review.pros),
    },
    {
      key: 'cons',
      icon: 'mdi-thumb-down-outline',
      label: t('compare.textual.cons'),
      type: 'list',
      values: productEntries.value.map(entry => entry.review.cons),
    },
  ]

  return rows.filter(row =>
    hasEnoughAvailability(computeAvailabilityFromValues(row.values))
  )
})

const priceRows = computed<AttributeRow[]>(() => {
  const newPrices = productEntries.value.map(
    entry => entry.product.offers?.bestNewOffer?.price ?? null
  )
  const occasionPrices = productEntries.value.map(
    entry => entry.product.offers?.bestOccasionOffer?.price ?? null
  )
  const offersCounts = productEntries.value.map(
    entry => entry.product.offers?.offersCount ?? null
  )

  const rows: AttributeRow[] = [
    {
      key: 'new-price',
      icon: 'mdi-currency-eur',
      label: t('compare.pricing.newPrice'),
      values: productEntries.value.map(
        entry =>
          formatPrice(entry.product.offers?.bestNewOffer, entry.product) ??
          formatBestPrice(entry.product, t, n)
      ),
      highlight: computeHighlightSet(newPrices, 'lower'),
      availability: newPrices.map(value => value != null),
    },
    {
      key: 'occasion-price',
      icon: 'mdi-rotate-orbit',
      label: t('compare.pricing.occasionPrice'),
      values: productEntries.value.map(entry =>
        formatPrice(entry.product.offers?.bestOccasionOffer, entry.product)
      ),
      highlight: computeHighlightSet(occasionPrices, 'lower'),
      availability: occasionPrices.map(value => value != null),
    },
    {
      key: 'offers-count',
      icon: 'mdi-store-outline',
      label: t('compare.pricing.offersCount'),
      values: productEntries.value.map(entry =>
        formatOffersCount(entry.product, translatePlural)
      ),
      highlight: computeHighlightSet(offersCounts, 'higher'),
      availability: offersCounts.map(value => value != null),
    },
  ]

  return filterSparseAttributeRows(rows)
})

const ecologicalScoreRows = computed<AttributeRow[]>(() => {
  const ecoscoreData = productEntries.value.map(entry =>
    resolveScoreValue(entry.product.scores?.ecoscore ?? null)
  )
  const brandScoreData = productEntries.value.map(entry =>
    resolveScoreValue(
      entry.product.scores?.scores?.BRAND_SUSTAINABILITY ?? null
    )
  )
  const dataQualityData = productEntries.value.map(entry =>
    resolveScoreValue(entry.product.scores?.scores?.DATA_QUALITY ?? null)
  )

  const rows: AttributeRow[] = [
    {
      key: 'ecoscore',
      icon: 'mdi-leaf',
      label: t('compare.ecological.ecoscore'),
      values: ecoscoreData.map(entry => entry.display),
      highlight: computeHighlightSet(
        ecoscoreData.map(entry => entry.numeric),
        'higher'
      ),
      availability: ecoscoreData.map(entry => entry.display != null),
    },
    {
      key: 'brand-sustainability',
      icon: 'mdi-earth',
      label: t('compare.ecological.brandSustainability'),
      values: brandScoreData.map(entry => entry.display),
      highlight: computeHighlightSet(
        brandScoreData.map(entry => entry.numeric),
        'higher'
      ),
      availability: brandScoreData.map(entry => entry.display != null),
    },
    {
      key: 'data-quality',
      icon: 'mdi-shield-check-outline',
      label: t('compare.ecological.dataQuality'),
      values: dataQualityData.map(entry => entry.display),
      highlight: computeHighlightSet(
        dataQualityData.map(entry => entry.numeric),
        'higher'
      ),
      availability: dataQualityData.map(entry => entry.display != null),
    },
  ]

  return filterSparseAttributeRows(rows)
})

const resolveAttributeRow = (
  config: AttributeConfigDto | null,
  key: string,
  label?: string
): AttributeRow => {
  const icon = config?.icon ?? 'mdi-tune'
  const betterIs = config?.betterIs === 'LOWER' ? 'lower' : 'higher'

  const rawValues = productEntries.value.map(entry =>
    resolveAttributeRawValueByKey(entry.product, key)
  )
  const resolvedAttributes = rawValues.map<ResolvedProductAttribute>(raw => ({
    key,
    label: label ?? config?.name ?? key,
    rawValue: raw,
    unit: config?.unit,
    icon: config?.icon,
    suffix: config?.suffix ?? null,
  }))

  const formattedValues = resolvedAttributes.map(attribute =>
    formatAttributeValue(attribute, t, n)
  )
  const numericValues = rawValues.map(raw => normalizeNumericValue(raw))
  const availability = rawValues.map(raw => hasMeaningfulCellValue(raw))

  return {
    key,
    icon,
    label: label ?? config?.name ?? key,
    values: formattedValues,
    highlight: computeHighlightSet(numericValues, betterIs),
    availability,
  }
}

const ecologicalAttributeRows = computed<AttributeRow[]>(() => {
  const rows = scoringAttributeConfigs.value.map(config =>
    resolveAttributeRow(config, config.key ?? '', config.name)
  )

  return filterSparseAttributeRows(rows)
})

const popularAttributeRows = computed<AttributeRow[]>(() => {
  const rows = popularAttributeConfigs.value.map(config =>
    resolveAttributeRow(config, config.key ?? '', config.name)
  )

  return filterSparseAttributeRows(rows)
})

const indexedAttributeRows = computed<AttributeRow[]>(() => {
  const seenKeys = new Set<string>()
  const popularKeys = new Set(
    popularAttributeConfigs.value
      .map(config => config.key)
      .filter(Boolean) as string[]
  )

  productEntries.value.forEach(entry => {
    const indexed = entry.product.attributes?.indexedAttributes ?? {}
    Object.keys(indexed).forEach(key => {
      if (!popularKeys.has(key)) {
        seenKeys.add(key)
      }
    })
  })

  const orderedKeys = Array.from(seenKeys).sort((a, b) => {
    const indexA = attributeConfigs.value.findIndex(config => config.key === a)
    const indexB = attributeConfigs.value.findIndex(config => config.key === b)

    if (indexA === -1 && indexB === -1) {
      return a.localeCompare(b)
    }

    if (indexA === -1) {
      return 1
    }

    if (indexB === -1) {
      return -1
    }

    return indexA - indexB
  })

  const rows = orderedKeys.map(key =>
    resolveAttributeRow(
      attributeConfigMap.value.get(key) ?? null,
      key,
      attributeConfigMap.value.get(key)?.name ?? key
    )
  )

  return filterSparseAttributeRows(rows)
})

const classifiedAttributeGroups = computed<ClassifiedGroup[]>(() => {
  const groups = new Map<
    string,
    { name: string; rows: Map<string, AttributeRow> }
  >()

  productEntries.value.forEach((entry, index) => {
    const groupEntries = entry.product.attributes?.classifiedAttributes ?? []
    groupEntries.forEach(group => {
      const groupName =
        group.name ?? t('compare.sections.technicalGroupFallback')
      if (!groups.has(groupName)) {
        groups.set(groupName, { name: groupName, rows: new Map() })
      }

      const currentGroup = groups.get(groupName)!
      const attributes = group.attributes ?? []

      attributes.forEach(attribute => {
        const attributeName = attribute?.name ?? attribute?.value ?? '—'
        const key = `${groupName}:${attributeName}`
        if (!currentGroup.rows.has(key)) {
          const config =
            attributeConfigMap.value.get(attributeName ?? '') ?? null
          currentGroup.rows.set(key, {
            key,
            icon: config?.icon ?? 'mdi-tune',
            label: attributeName,
            values: Array(productCount.value).fill<string | null>(null),
            highlight: new Set(),
            availability: Array(productCount.value).fill(false),
          })
        }

        const row = currentGroup.rows.get(key)!
        const rawValue = attribute?.value ?? null
        const normalizedValue =
          typeof rawValue === 'string'
            ? rawValue.trim().length > 0
              ? rawValue.trim()
              : null
            : rawValue

        row.values[index] =
          normalizedValue == null ? null : String(normalizedValue)
        row.availability[index] = hasMeaningfulCellValue(normalizedValue)

        const numericValues = row.values.map(value =>
          normalizeNumericValue(value)
        )
        const preference =
          attributeConfigMap.value.get(attributeName ?? '')?.betterIs ===
          'LOWER'
            ? 'lower'
            : 'higher'
        row.highlight = computeHighlightSet(numericValues, preference)
      })
    })
  })

  return Array.from(groups.values())
    .map(group => ({
      name: group.name,
      rows: filterSparseAttributeRows(Array.from(group.rows.values())),
    }))
    .filter(group => group.rows.length > 0)
})

const handleRemove = (gtin: string) => {
  compareStore.removeById(gtin)
  const remaining = requestedGtins.value.filter(value => value !== gtin)
  const hash = buildCompareHash(remaining)

  router.replace(hash ? `${comparePath.value}${hash}` : comparePath.value)
}

const productLink = (entry: CompareProductEntry) => {
  return entry.product.fullSlug ?? entry.product.slug ?? null
}

const productModelLabel = (entry: CompareProductEntry) => {
  return entry.model ?? entry.title ?? '—'
}

const productInitials = (title: string) => {
  return title
    .split(/\s+/)
    .filter(Boolean)
    .map(part => part[0]?.toUpperCase())
    .slice(0, 2)
    .join('')
}
</script>

<style scoped lang="sass">
.compare-page
  --compare-grid-sticky-offset: var(--v-layout-top, 0px)
  display: flex
  flex-direction: column
  gap: 2.5rem
  padding-bottom: 3rem

.compare-page__hero
  position: relative
  overflow: hidden
  background: radial-gradient(
    circle at top left,
    rgba(var(--v-theme-hero-gradient-start), 0.75),
    rgba(var(--v-theme-hero-gradient-end), 0.9)
  )
  color: rgba(var(--v-theme-hero-overlay-strong), 0.95)

.compare-page__hero-surface
  position: relative
  isolation: isolate

.compare-page__hero-surface::after
  content: ''
  position: absolute
  inset: 0
  background: linear-gradient(135deg, rgba(var(--v-theme-hero-overlay-soft), 0.12), rgba(var(--v-theme-hero-overlay-soft), 0.04))
  z-index: 0

.compare-page__hero-container
  position: relative
  z-index: 1
  padding-block: clamp(3rem, 6vw, 5rem)

.compare-page__hero-content
  display: flex
  flex-direction: column
  align-items: center
  gap: 1.25rem
  text-align: center
  max-width: 720px
  margin: 0 auto

.compare-page__hero-back
  display: inline-flex
  align-items: center
  gap: 0.5rem
  padding: 0.4rem 0.95rem
  border-radius: 999px
  background: rgba(var(--v-theme-hero-overlay-soft), 0.18)
  color: rgba(var(--v-theme-text-neutral-strong), 0.96)
  font-weight: 600
  text-decoration: none
  transition: background 0.2s ease, transform 0.2s ease

.compare-page__hero-back:hover,
.compare-page__hero-back:focus-visible
  background: rgba(var(--v-theme-hero-overlay-soft), 0.26)
  transform: translateY(-1px)

.compare-page__hero-back:focus-visible
  outline: 2px solid rgba(var(--v-theme-hero-overlay-strong), 0.4)
  outline-offset: 3px

.compare-page__hero-meta
  display: inline-flex
  align-items: center
  gap: 0.5rem
  padding: 0.5rem 0.75rem
  border-radius: 999px
  background: rgba(var(--v-theme-hero-overlay-soft), 0.18)
  color: rgba(var(--v-theme-text-neutral-strong), 0.95)
  font-weight: 500

.compare-page__title
  font-size: clamp(2rem, 3vw, 2.75rem)
  font-weight: 700
  margin-bottom: 0.75rem
  color: rgba(var(--v-theme-text-neutral-strong), 1)

.compare-page__subtitle
  color: rgba(var(--v-theme-text-neutral-secondary), 0.95)
  max-width: 720px
  margin: 0 auto

.compare-page__content
  display: flex
  flex-direction: column
  gap: 3rem

.compare-page__empty
  display: flex
  flex-direction: column
  align-items: center
  text-align: center
  gap: 1rem
  padding-block: 4rem
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85)

.compare-page__empty-icon
  color: rgb(var(--v-theme-accent-supporting))

.compare-page__empty-title
  font-size: 1.5rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.compare-page__empty-text
  max-width: 480px

.compare-section
  display: flex
  flex-direction: column
  gap: 1.5rem

.compare-section__title
  font-size: clamp(1.5rem, 2.2vw, 2rem)
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.compare-grid
  position: relative
  overflow-x: auto
  background: rgb(var(--v-theme-surface-glass))
  border-radius: 20px
  padding: 0 1.5rem 1.5rem
  display: flex
  flex-direction: column
  gap: 1rem

.compare-grid__media
  display: grid
  grid-template-columns: minmax(180px, 220px) repeat(auto-fit, minmax(220px, 1fr))
  gap: 1rem
  padding-top: 1.5rem

.compare-grid__feature--media
  display: flex
  align-items: center
  justify-content: center

.compare-grid--compact
  padding-inline: 1rem

.compare-grid__header
  display: grid
  grid-template-columns: minmax(180px, 220px) repeat(auto-fit, minmax(220px, 1fr))
  gap: 1rem
  position: sticky
  top: var(--compare-grid-sticky-offset)
  z-index: 4
  background: rgba(var(--v-theme-surface-glass), 0.94)
  border: 1px solid rgba(var(--v-theme-border-primary-strong), 0.2)
  border-radius: 16px
  padding: 1rem
  box-shadow: 0 22px 48px -32px rgba(var(--v-theme-shadow-primary-600), 0.35)
  backdrop-filter: blur(16px)

.compare-grid__feature--header
  display: flex
  align-items: center
  justify-content: center

.compare-grid__products
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))
  gap: 1rem

.compare-grid__product
  display: flex
  flex-direction: column
  align-items: center
  gap: 0.5rem
  text-align: center

.compare-grid__product-media
  display: flex
  align-items: center
  justify-content: center
  width: 100%
  height: 180px
  background: rgb(var(--v-theme-surface-default))
  border-radius: 16px
  overflow: hidden

.compare-grid__product-link
  display: flex
  align-items: center
  justify-content: center
  width: 100%
  height: 100%
  border-radius: inherit
  text-decoration: none
  color: inherit
  transition: transform 0.2s ease, box-shadow 0.2s ease

.compare-grid__product-link:hover,
.compare-grid__product-link:focus-visible
  transform: scale(1.02)
  box-shadow: 0 12px 30px rgba(var(--v-theme-shadow-primary-600), 0.18)

.compare-grid__product-link:focus-visible
  outline: 2px solid rgba(var(--v-theme-accent-supporting), 0.6)
  outline-offset: 2px

.compare-grid__product-image
  object-fit: contain
  max-width: 100%
  max-height: 100%

.compare-grid__product-placeholder
  font-size: 2rem
  font-weight: 600
  color: rgba(var(--v-theme-text-neutral-soft), 0.75)

.compare-grid__product-impact
  margin-top: 0.25rem

.compare-grid__product-model
  font-weight: 600
  margin: 0

.compare-grid__product-model--link
  text-decoration: none
  color: rgb(var(--v-theme-text-neutral-strong))
  transition: color 0.2s ease

.compare-grid__product-model--link:hover,
.compare-grid__product-model--link:focus-visible
  color: rgb(var(--v-theme-accent-supporting))

.compare-grid__product-brand
  color: rgba(var(--v-theme-text-neutral-secondary), 0.85)
  margin: 0

.compare-grid__product-country
  display: flex
  justify-content: center

.compare-grid__product-country-flag
  display: inline-flex
  align-items: center
  gap: 0.5rem
  color: rgb(var(--v-theme-text-neutral-secondary))

.compare-grid__flag
  border-radius: 4px

.compare-grid__product-remove
  margin-top: 0.5rem

.compare-grid__row
  display: grid
  grid-template-columns: minmax(180px, 220px) repeat(auto-fit, minmax(220px, 1fr))
  gap: 1rem
  align-items: stretch

.compare-grid__group
  display: flex
  flex-direction: column
  gap: 0.75rem
  padding-top: 1rem
  border-top: 1px solid rgba(var(--v-theme-border-primary-strong), 0.4)

.compare-grid__group-title
  font-size: 1.1rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))
  margin: 0

.compare-grid__feature
  display: flex
  align-items: flex-start
  gap: 0.75rem
  font-weight: 600
  color: rgb(var(--v-theme-text-neutral-strong))

.compare-grid__feature-icon
  color: rgb(var(--v-theme-accent-supporting))

.compare-grid__values
  display: grid
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))
  gap: 1rem

.compare-grid__value
  background: rgb(var(--v-theme-surface-default))
  border-radius: 16px
  padding: 1rem 1.25rem
  min-height: 100%
  display: flex
  flex-direction: column
  align-items: stretch
  justify-content: flex-start
  gap: 1rem

.compare-grid__value-content
  display: flex
  flex-direction: column
  align-items: center
  justify-content: center
  text-align: center
  gap: 0.75rem
  width: 100%

.compare-grid__value-mobile
  display: none
  align-items: center
  gap: 0.75rem

.compare-grid__value-mobile-media
  display: flex
  align-items: center
  justify-content: center
  width: 72px
  height: 72px
  border-radius: 16px
  background: rgb(var(--v-theme-surface-default))
  overflow: hidden

.compare-grid__value-mobile-media--link
  text-decoration: none

.compare-grid__value-mobile-image
  width: 100%
  height: 100%
  object-fit: cover

.compare-grid__value-mobile-placeholder
  display: flex
  align-items: center
  justify-content: center
  width: 100%
  height: 100%
  border-radius: inherit
  background: rgba(var(--v-theme-surface-primary-080), 0.7)
  color: rgb(var(--v-theme-text-neutral-soft))
  font-weight: 600
  font-size: 1rem
  text-transform: uppercase

.compare-grid__value-mobile-details
  display: flex
  flex-direction: column
  gap: 0.25rem
  text-align: left

.compare-grid__value-mobile-brand
  margin: 0
  font-size: 0.85rem
  color: rgb(var(--v-theme-text-neutral-soft))

.compare-grid__value-mobile-model
  margin: 0
  font-weight: 600
  font-size: 1rem
  color: rgb(var(--v-theme-text-neutral-strong))

.compare-grid__value-inner
  display: inline-flex
  align-items: center
  justify-content: center
  gap: 0.5rem
  width: 100%

.compare-grid__value-badge
  color: rgb(var(--v-theme-accent-supporting))

.compare-grid__value--highlight
  border: 2px solid rgba(var(--v-theme-accent-supporting), 0.6)
  box-shadow: 0 0 0 2px rgba(var(--v-theme-accent-supporting), 0.2)

.compare-grid__value--has-list
  align-items: stretch
  text-align: left

.compare-grid__value--has-list .compare-grid__value-content
  align-items: stretch
  text-align: left

.compare-grid__value--has-list .compare-grid__list
  align-items: flex-start

.compare-grid__value-text
  margin: 0
  color: rgb(var(--v-theme-text-neutral-secondary))

.compare-grid__list
  list-style: none
  margin: 0
  padding: 0
  display: flex
  flex-direction: column
  gap: 0.5rem
  width: 100%

.compare-grid__list-item
  position: relative
  padding-inline-start: 1.5rem
  color: rgb(var(--v-theme-text-neutral-secondary))

.compare-grid__list-item::before
  content: ''
  position: absolute
  inset-inline-start: 0
  inset-block-start: 0.4rem
  width: 0.75rem
  height: 0.75rem
  border-radius: 50%
  background: rgba(var(--v-theme-accent-supporting), 0.7)

.compare-grid__values > .compare-grid__value
  min-height: 120px

.sr-only
  position: absolute
  width: 1px
  height: 1px
  padding: 0
  margin: -1px
  overflow: hidden
  clip: rect(0, 0, 0, 0)
  white-space: nowrap
  border: 0

@media (min-width: 960px)
  .compare-page__hero-content
    align-items: flex-start
    text-align: left

  .compare-page__hero-back
    align-self: flex-start

@media (max-width: 960px)
  .compare-grid__header
    display: none

  .compare-grid__row
    grid-template-columns: 1fr

  .compare-grid__values
    grid-template-columns: 1fr

  .compare-grid__value
    min-height: auto
    display: grid
    grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr)
    align-items: stretch
    gap: 1rem

  .compare-grid__value-mobile
    display: flex

  .compare-grid__value-content
    align-items: flex-start
    text-align: left

  .compare-grid__value-inner
    justify-content: flex-start

  .compare-grid__products
    grid-template-columns: 1fr
</style>
