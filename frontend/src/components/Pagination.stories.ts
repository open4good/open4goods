import Pagination from './Pagination.vue'
import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta<typeof Pagination> = {
  component: Pagination,
  title: 'Pagination'
}
export default meta

export const Default: StoryObj<typeof Pagination> = {
  render: (args) => ({
    components: { Pagination },
    setup() {
      return { args }
    },
    template: '<Pagination :meta="args.meta" />'
  }),
  args: {
    meta: { number: 0, size: 10, totalElements: 20, totalPages: 2 }
  }
}
