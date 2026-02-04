export type JsonLdValue =
  | string
  | number
  | boolean
  | null
  | undefined
  | JsonLdValue[]
  | { [key: string]: JsonLdValue }

export interface ProductJsonLdBreadcrumb {
  title: string
  link?: string
}

export interface ProductJsonLdSiteInfo {
  url: string
  name: string
}

export interface ProductJsonLdInput {
  product: {
    gtin?: number | string | null
    slug?: string | null
    fullSlug?: string | null
    names?: {
      prettyName?: string | null
      singular?: string | null
      metaDescription?: string | null
      ogDescription?: string | null
      h1Title?: string | null
    } | null
    base?: {
      bestName?: string | null
      vertical?: string | null
      externalIds?: {
        mpn?: Set<string> | string[] | null
        sku?: Set<string> | string[] | null
      } | null
      coverImagePath?: string | null
    } | null
    identity?: {
      brand?: string | null
      model?: string | null
      bestName?: string | null
    } | null
    resources?: {
      coverImagePath?: string | null
      externalCover?: string | null
      images?: Array<{ url?: string | null; originalUrl?: string | null }> | null
    } | null
    offers?: {
      offersCount?: number | null
      offersByCondition?: Record<string, Array<{
        url?: string | null
        price?: number | null
        currency?: string | null
        condition?: string | null
        datasourceName?: string | null
      }>> | null
    } | null
    scores?: {
      ecoscore?: {
        absolute?: {
          value?: number | null
        } | null
      } | null
    } | null
    attributes?: {
      referentialAttributes?: Record<string, string> | null
      indexedAttributes?: Record<string, { numericValue?: number | null }> | null
    } | null
  }
  productTitle: string
  canonicalUrl: string
  locale: string
  breadcrumbs: ProductJsonLdBreadcrumb[]
  site: ProductJsonLdSiteInfo
  review?: { [key: string]: JsonLdValue } | null
  impactScoreOn20?: number | null
  imageUrls?: string[]
}

const isNonEmptyString = (value: unknown): value is string =>
  typeof value === 'string' && value.trim().length > 0

const normalizeString = (value: unknown): string | undefined =>
  isNonEmptyString(value) ? value.trim() : undefined

const normalizeSet = (value?: Set<string> | string[] | null): string[] => {
  if (!value) {
    return []
  }

  if (Array.isArray(value)) {
    return value.filter(isNonEmptyString)
  }

  return Array.from(value).filter(isNonEmptyString)
}

export const compactJsonLd = (value: JsonLdValue): JsonLdValue | undefined => {
  if (value === null || value === undefined) {
    return undefined
  }

  if (typeof value === 'string') {
    return value.trim().length ? value : undefined
  }

  if (Array.isArray(value)) {
    const entries = value
      .map(item => compactJsonLd(item))
      .filter((item): item is JsonLdValue => item !== undefined)
    return entries.length ? entries : undefined
  }

  if (typeof value === 'object') {
    const entries = Object.entries(value).reduce<Record<string, JsonLdValue>>(
      (acc, [key, entry]) => {
        const normalized = compactJsonLd(entry)
        if (normalized !== undefined) {
          acc[key] = normalized
        }
        return acc
      },
      {}
    )

    return Object.keys(entries).length ? entries : undefined
  }

  return value
}

const resolveAttributeValue = (
  referentialAttributes: Record<string, string>,
  indexedAttributes: Record<string, { numericValue?: number | null }>,
  keys: string[]
): string | number | undefined => {
  for (const key of keys) {
    const indexed = indexedAttributes[key]?.numericValue
    if (typeof indexed === 'number') {
      return indexed
    }

    const referential = referentialAttributes[key]
    if (isNonEmptyString(referential)) {
      return referential
    }
  }

  return undefined
}

const toAbsoluteUrl = (origin: string, value: string): string | undefined => {
  try {
    return new URL(value, origin).toString()
  } catch (error) {
    if (import.meta.dev) {
      console.warn('Failed to build absolute URL for product JSON-LD.', error)
    }
    return undefined
  }
}

const resolveImageUrls = (origin: string, input?: string[]): string[] => {
  if (!input?.length) {
    return []
  }

  const seen = new Set<string>()
  const resolved: string[] = []

  for (const candidate of input) {
    const normalized = normalizeString(candidate)
    if (!normalized) {
      continue
    }

    const absolute = toAbsoluteUrl(origin, normalized) ?? normalized
    if (seen.has(absolute)) {
      continue
    }

    seen.add(absolute)
    resolved.push(absolute)
  }

  return resolved
}

const buildOfferList = (
  offersByCondition: Record<string, Array<{
    url?: string | null
    price?: number | null
    currency?: string | null
    condition?: string | null
    datasourceName?: string | null
  }>>
): Array<Record<string, JsonLdValue>> => {
  const offers = Object.values(offersByCondition)
    .flat()
    .filter(offer => typeof offer?.price === 'number' && isNonEmptyString(offer?.url))
    .map(offer => ({
      '@type': 'Offer',
      url: normalizeString(offer.url),
      price: offer.price,
      priceCurrency: normalizeString(offer.currency),
      availability: 'https://schema.org/InStock',
      itemCondition:
        offer.condition === 'NEW'
          ? 'https://schema.org/NewCondition'
          : offer.condition === 'OCCASION'
            ? 'https://schema.org/UsedCondition'
            : undefined,
      seller: {
        '@type': 'Organization',
        name: normalizeString(offer.datasourceName),
      },
    }))

  return offers
}

export const buildProductJsonLdGraph = (
  input: ProductJsonLdInput
): Record<string, JsonLdValue> | null => {
  const { product, productTitle } = input
  const productId = `${input.canonicalUrl}#product`
  const breadcrumbId = `${input.canonicalUrl}#breadcrumb`
  const webpageId = `${input.canonicalUrl}#webpage`

  const referentialAttributes =
    product.attributes?.referentialAttributes ?? {}
  const indexedAttributes = product.attributes?.indexedAttributes ?? {}

  const gtinValue =
    product.base?.gtin ?? product.gtin ?? product.gtin?.toString() ?? undefined
  const gtin13 = gtinValue
    ? String(gtinValue).padStart(13, '0')
    : undefined

  const images = resolveImageUrls(input.site.url, input.imageUrls ?? [])

  const offersByCondition = product.offers?.offersByCondition ?? {}
  const offers = buildOfferList(offersByCondition)
  const offerPrices = offers
    .map(offer => offer.price)
    .filter((price): price is number => typeof price === 'number')

  const offerCurrency = offers.find(offer => isNonEmptyString(offer.priceCurrency))
    ?.priceCurrency

  const additionalProperty = compactJsonLd([
    input.impactScoreOn20 != null
      ? {
          '@type': 'PropertyValue',
          name: 'Nudger Impact Score',
          value: input.impactScoreOn20,
        }
      : undefined,
    {
      '@type': 'PropertyValue',
      name: 'Indice de réparabilité',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'REPAIRABILITY_INDEX',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Disponibilité pièces détachées',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'MINAVAILABILITYSPAREPARTSYEARS',
      ]),
      unitText: 'ans',
    },
    {
      '@type': 'PropertyValue',
      name: 'Mises à jour logicielles',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'MINAVAILABILITYSOFTWAREUPDATESYEARS',
      ]),
      unitText: 'ans',
    },
    {
      '@type': 'PropertyValue',
      name: 'Garantie',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'WARRANTY',
      ]),
      unitText: 'ans',
    },
    {
      '@type': 'PropertyValue',
      name: 'Taille écran',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'DIAGONALE_POUCES',
      ]),
      unitText: 'in',
    },
    {
      '@type': 'PropertyValue',
      name: 'Technologie d’affichage',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'DISPLAY_TECHNOLOGY',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Résolution',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        "RÉSOLUTION DE L'ÉCRAN",
        'RESOLUTION',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Fréquence',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'TAUX DE RAFRAÎCHISSEMENT NATIF',
        'FREQUENCY_RATE',
      ]),
      unitText: 'Hz',
    },
    {
      '@type': 'PropertyValue',
      name: 'Ports HDMI',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'HDMI_PORTS_QUANTITY',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Ports USB',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'QUANTITÉ DE PORTS USB 2.0',
        'QUANTITE DE PORTS USB 2.0',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Wi-Fi',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'WIFI',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Standards Wi-Fi',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'STANDARDS WIFI',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Bluetooth',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'BLUETOOTH',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Version Bluetooth',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'MODÈLE DU BLUETOOTH',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'OS / Plateforme',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        "SYSTÈME D'EXPLOITATION INSTALLÉ",
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Année de sortie',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'YEAR',
      ]),
    },
    {
      '@type': 'PropertyValue',
      name: 'Couleur',
      value: resolveAttributeValue(referentialAttributes, indexedAttributes, [
        'COULEUR GENERIQUE',
        'NOM DE LA COULEUR',
      ]),
    },
  ]) as JsonLdValue

  const energyDetails = compactJsonLd(
    [
      {
        key: 'CLASSE_ENERGY_SDR',
        label: 'Étiquette énergie (SDR)',
      },
      {
        key: 'CLASSE_ENERGY_HDR',
        label: 'Étiquette énergie (HDR)',
      },
    ]
      .map(({ key, label }) => {
        const category = referentialAttributes[key]
        if (!isNonEmptyString(category)) {
          return undefined
        }

        return {
          '@type': 'EnergyConsumptionDetails',
          name: label,
          energyEfficiencyScaleMin:
            'https://schema.org/EUEnergyEfficiencyCategoryG',
          energyEfficiencyScaleMax:
            'https://schema.org/EUEnergyEfficiencyCategoryA',
          hasEnergyEfficiencyCategory: `https://schema.org/EUEnergyEfficiencyCategory${category}`,
        }
      })
      .filter(Boolean)
  ) as JsonLdValue

  const webPageEntry = {
    '@type': 'WebPage',
    '@id': webpageId,
    url: input.canonicalUrl,
    name: productTitle,
    inLanguage: input.locale,
    isPartOf: {
      '@type': 'WebSite',
      '@id': `${input.site.url}#website`,
      url: input.site.url,
      name: input.site.name,
    },
    mainEntity: {
      '@id': productId,
    },
  }

  const breadcrumbEntry = {
    '@type': 'BreadcrumbList',
    '@id': breadcrumbId,
    itemListElement: input.breadcrumbs.map((crumb, index) => ({
      '@type': 'ListItem',
      position: index + 1,
      name: crumb.title,
      item: crumb.link
        ? toAbsoluteUrl(input.site.url, crumb.link)
        : undefined,
    })),
  }

  const brandName = normalizeString(product.identity?.brand)
  const weightValue = resolveAttributeValue(referentialAttributes, indexedAttributes, [
    'POIDS (SANS SUPPORT)',
    'POIDS (AVEC SUPPORT)',
  ])
  const widthValue = resolveAttributeValue(referentialAttributes, indexedAttributes, [
    'LARGEUR (SANS SUPPORT)',
  ])
  const heightValue = resolveAttributeValue(referentialAttributes, indexedAttributes, [
    'HAUTEUR (SANS SUPPORT)',
  ])
  const depthValue = resolveAttributeValue(referentialAttributes, indexedAttributes, [
    'PROFONDEUR (SANS SUPPORT)',
  ])

  const productEntry = {
    '@type': 'Product',
    '@id': productId,
    url: input.canonicalUrl,
    name: productTitle,
    description:
      normalizeString(product.names?.metaDescription) ??
      normalizeString(product.names?.ogDescription),
    image: images,
    category:
      normalizeString(product.names?.singular) ??
      normalizeString(product.base?.vertical),
    brand: brandName
      ? {
          '@type': 'Brand',
          name: brandName,
        }
      : undefined,
    gtin13,
    model: normalizeString(product.identity?.model),
    mpn: normalizeString(normalizeSet(product.base?.externalIds?.mpn)[0]),
    sku: normalizeString(normalizeSet(product.base?.externalIds?.sku)[0]),
    color: resolveAttributeValue(referentialAttributes, indexedAttributes, [
      'COULEUR GENERIQUE',
      'NOM DE LA COULEUR',
    ]),
    additionalProperty,
    weight: weightValue
      ? {
          '@type': 'QuantitativeValue',
          value: weightValue,
          unitCode: 'KGM',
        }
      : undefined,
    width: widthValue
      ? {
          '@type': 'QuantitativeValue',
          value: widthValue,
          unitCode: 'MMT',
        }
      : undefined,
    height: heightValue
      ? {
          '@type': 'QuantitativeValue',
          value: heightValue,
          unitCode: 'MMT',
        }
      : undefined,
    depth: depthValue
      ? {
          '@type': 'QuantitativeValue',
          value: depthValue,
          unitCode: 'MMT',
        }
      : undefined,
    hasEnergyConsumptionDetails: energyDetails,
    offers:
      offers.length > 0
        ? {
            '@type': 'AggregateOffer',
            priceCurrency: offerCurrency,
            offerCount: product.offers?.offersCount ?? offers.length,
            lowPrice:
              offerPrices.length > 0
                ? Math.min(...offerPrices)
                : undefined,
            highPrice:
              offerPrices.length > 0
                ? Math.max(...offerPrices)
                : undefined,
            offers,
          }
        : undefined,
    review: input.review ?? undefined,
  }

  const graph = compactJsonLd({
    '@context': 'https://schema.org',
    '@graph': [breadcrumbEntry, productEntry, webPageEntry],
  })

  return (graph as Record<string, JsonLdValue>) ?? null
}
