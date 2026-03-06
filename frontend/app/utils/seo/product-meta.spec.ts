import { describe, expect, it } from 'vitest'
import { buildProductMeta } from './product-meta'

const serpTemplates = {
  title: {
    withImpactFull:
      '{productName} : ImpactScore : {score}/20 | Meilleurs prix, manuels, et caractéristiques pour le {brandModel}',
    withImpactCompact:
      '{productName} : ImpactScore : {score}/20 | Meilleurs prix pour {brandModel}',
    withImpactMinimal: '{productName} : ImpactScore : {score}/20',
    withoutImpactFull:
      '{productName} | Meilleurs prix, manuels, et caractéristiques pour le {brandModel}',
    withoutImpactCompact: '{productName} | Meilleurs prix pour {brandModel}',
    withoutImpactMinimal: '{productName}',
  },
  description: {
    withImpact:
      'Comparez {productName} ({brandModel}) : score d\'impact {score}/20, meilleurs prix, manuels et caractéristiques sur Nudger.',
    withImpactVertical:
      'Comparez {productName} ({brandModel}) dans la catégorie {verticalTitle} : score d\'impact {score}/20, meilleurs prix, manuels et caractéristiques sur Nudger.',
    withoutImpact:
      'Comparez {productName} ({brandModel}) : meilleurs prix, manuels et caractéristiques sur Nudger.',
    withoutImpactVertical:
      'Comparez {productName} ({brandModel}) dans la catégorie {verticalTitle} : meilleurs prix, manuels et caractéristiques sur Nudger.',
  },
}

describe('buildProductMeta', () => {
  it('keeps the impact score in title when score exists', () => {
    const meta = buildProductMeta({
      productName: 'iPhone 16 Pro',
      brandModel: 'Apple iPhone 16 Pro',
      score: 16.4,
      titleTemplates: serpTemplates.title,
      descriptionTemplates: serpTemplates.description,
    })

    expect(meta.title).toContain('ImpactScore : 16.4/20')
    expect(meta.description).toContain('score d\'impact 16.4/20')
  })

  it('does not mention impact score when score is missing', () => {
    const meta = buildProductMeta({
      productName: 'iPhone 16 Pro',
      brandModel: 'Apple iPhone 16 Pro',
      score: null,
      titleTemplates: serpTemplates.title,
      descriptionTemplates: serpTemplates.description,
    })

    expect(meta.title).not.toContain('ImpactScore')
    expect(meta.description).not.toContain('score d\'impact')
  })

  it('drops manuals/features phrase first when title is too long', () => {
    const meta = buildProductMeta({
      productName: 'Aspirateur balai sans fil ultra puissant autonomie longue durée',
      brandModel:
        'Marque Excellence Modèle Premium Hyper Edition Max Performance',
      score: 14.8,
      maxTitleLength: 80,
      titleTemplates: serpTemplates.title,
      descriptionTemplates: serpTemplates.description,
    })

    expect(meta.title).toContain('ImpactScore')
    expect(meta.title).not.toContain('manuels, et caractéristiques')
  })

  it('respects max lengths for title and description', () => {
    const meta = buildProductMeta({
      productName:
        'Montre connectée sport et santé génération avancée avec fonctionnalités premium',
      brandModel: 'Sportech Ultra Perform X Series',
      score: 12.2,
      maxTitleLength: 60,
      maxDescriptionLength: 120,
      verticalTitle: 'montres connectées',
      titleTemplates: serpTemplates.title,
      descriptionTemplates: serpTemplates.description,
    })

    expect(meta.title.length).toBeLessThanOrEqual(60)
    expect(meta.description.length).toBeLessThanOrEqual(120)
  })
})
