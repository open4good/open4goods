import type {
  ProductAttributeDto,
  ProductAttributeSourceDto,
  ProductClassifiedAttributeGroupDto,
  ProductDto,
  ProductIndexedAttributeDto,
  ProductSourcedAttributeDto,
} from '~~/shared/api-client'
import { applyAiReviewReferenceLinks } from './ai-review-references'

const normaliseSourcesSet = (
  sourcing: ProductAttributeSourceDto | null | undefined
): void => {
  if (!sourcing) {
    return
  }

  const { sources } = sourcing
  if (sources instanceof Set) {
    ;(
      sourcing as ProductAttributeSourceDto & {
        sources?:
          | ProductAttributeSourceDto['sources']
          | ProductSourcedAttributeDto[]
      }
    ).sources = Array.from(sources)
  }
}

const normaliseAttributeWithSourcing = (
  attribute:
    | (ProductAttributeDto | ProductIndexedAttributeDto | null | undefined)
    | undefined
): void => {
  if (!attribute) {
    return
  }

  normaliseSourcesSet(attribute.sourcing)
}

const normaliseAttributeCollection = (
  collection?: Array<
    ProductAttributeDto | ProductIndexedAttributeDto | null | undefined
  > | null
): void => {
  if (!collection) {
    return
  }

  collection.forEach(attribute => {
    normaliseAttributeWithSourcing(attribute ?? undefined)
  })
}

const normaliseClassifiedGroup = (
  group: ProductClassifiedAttributeGroupDto | null | undefined
): void => {
  if (!group) {
    return
  }

  normaliseAttributeCollection(group.attributes)
  normaliseAttributeCollection(group.features)
  normaliseAttributeCollection(group.unFeatures)
}

export const normaliseProductDto = <T extends ProductDto | null | undefined>(
  product: T
): T => {
  if (!product) {
    return product
  }

  if (product.aiReview?.review) {
    applyAiReviewReferenceLinks(product.aiReview.review)
  }

  const { attributes } = product

  if (attributes?.indexedAttributes) {
    Object.values(attributes.indexedAttributes).forEach(attribute => {
      normaliseAttributeWithSourcing(attribute ?? undefined)
    })
  }

  if (attributes?.classifiedAttributes) {
    attributes.classifiedAttributes.forEach(group => {
      normaliseClassifiedGroup(group)
    })
  }

  return product
}
