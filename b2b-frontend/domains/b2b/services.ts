export interface ServiceDescriptor {
  id: string
  slug: string
  icon: string
  category: 'product' | 'utility'
  status: 'live' | 'coming-soon'
  credits: number
  featured: boolean
  order: number
  docSlug?: string
  playgroundPath?: string
}

export const SERVICES: ServiceDescriptor[] = [
  {
    id: 'product.price',
    slug: 'price',
    icon: 'mdi-tag-outline',
    category: 'product',
    status: 'live',
    credits: 5,
    featured: true,
    order: 1,
    docSlug: 'products/price',
    playgroundPath: '/docs/products/price/playground'
  },
  {
    id: 'product.identity',
    slug: 'identity',
    icon: 'mdi-identifier',
    category: 'product',
    status: 'coming-soon',
    credits: 1,
    featured: false,
    order: 2
  },
  {
    id: 'product.attributes',
    slug: 'attributes',
    icon: 'mdi-format-list-bulleted',
    category: 'product',
    status: 'coming-soon',
    credits: 4,
    featured: true,
    order: 3
  },
  {
    id: 'product.images',
    slug: 'images',
    icon: 'mdi-image-multiple-outline',
    category: 'product',
    status: 'coming-soon',
    credits: 3,
    featured: false,
    order: 4
  },
  {
    id: 'product.documents',
    slug: 'documents',
    icon: 'mdi-file-document-multiple-outline',
    category: 'product',
    status: 'coming-soon',
    credits: 3,
    featured: false,
    order: 5
  },
  {
    id: 'product.price-history',
    slug: 'price-history',
    icon: 'mdi-chart-line',
    category: 'product',
    status: 'coming-soon',
    credits: 8,
    featured: true,
    order: 6
  },
  {
    id: 'product.impact',
    slug: 'impact',
    icon: 'mdi-leaf-circle-outline',
    category: 'product',
    status: 'coming-soon',
    credits: 15,
    featured: true,
    order: 7
  },
  {
    id: 'product.energy',
    slug: 'energy',
    icon: 'mdi-lightning-bolt-outline',
    category: 'product',
    status: 'coming-soon',
    credits: 10,
    featured: true,
    order: 8
  },
  {
    id: 'product.taxonomy',
    slug: 'taxonomy',
    icon: 'mdi-sitemap-outline',
    category: 'product',
    status: 'coming-soon',
    credits: 15,
    featured: false,
    order: 9
  },
  {
    id: 'product.alternatives',
    slug: 'alternatives',
    icon: 'mdi-swap-horizontal-circle-outline',
    category: 'product',
    status: 'coming-soon',
    credits: 20,
    featured: false,
    order: 10
  },
  {
    id: 'barcode.render',
    slug: 'barcode-render',
    icon: 'mdi-barcode',
    category: 'utility',
    status: 'live',
    credits: 1,
    featured: true,
    order: 12,
    docSlug: 'barcodes/render'
  },
  {
    id: 'barcode.check',
    slug: 'barcode-check',
    icon: 'mdi-barcode-scan',
    category: 'utility',
    status: 'live',
    credits: 0,
    featured: false,
    order: 13,
    docSlug: 'barcodes/check'
  }
]

export function allServices(): ServiceDescriptor[] {
  return [...SERVICES].sort((a, b) => a.order - b.order)
}

export function featuredServices(): ServiceDescriptor[] {
  return allServices().filter(s => s.featured)
}

export function getServiceBySlug(slug: string): ServiceDescriptor | undefined {
  return SERVICES.find(s => s.slug === slug)
}
