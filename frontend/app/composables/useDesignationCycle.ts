import { computed, ref, unref, type MaybeRef } from 'vue'

/**
 * Provide a deterministic cycling helper for product designation variants.
 */
export const useDesignationCycle = (
  designations: MaybeRef<string[] | null | undefined>
) => {
  const index = ref(0)
  const designationList = computed(() => unref(designations) ?? [])

  const nextDesignation = () => {
    const currentList = designationList.value
    if (currentList.length === 0) {
      return ''
    }
    const value = currentList[index.value % currentList.length]
    index.value = (index.value + 1) % currentList.length
    return value
  }

  return {
    designations: designationList,
    nextDesignation,
    designationIndex: index,
  }
}
