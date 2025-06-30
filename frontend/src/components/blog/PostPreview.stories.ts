import PostPreview from './PostPreview.vue'
import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta<typeof PostPreview> = {
  component: PostPreview,
  title: 'Blog/PostPreview'
}
export default meta

export const Default: StoryObj<typeof PostPreview> = {
  render: (args) => ({
    components: { PostPreview },
    setup() { return { args } },
    template: '<PostPreview :post="args.post" />'
  }),
  args: {
    post: { title: 'Hello', summary: 'Summary', author: 'Jane', category: ['news'] }
  }
}
