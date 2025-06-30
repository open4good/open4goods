import Author from './Author.vue'
import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta<typeof Author> = {
  component: Author,
  title: 'Blog/Author'
}
export default meta

export const Default: StoryObj<typeof Author> = {
  args: { name: 'Jane Doe' }
}
