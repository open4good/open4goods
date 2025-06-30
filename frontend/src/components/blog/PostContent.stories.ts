import PostContent from './PostContent.vue'
import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta<typeof PostContent> = {
  component: PostContent,
  title: 'Blog/PostContent'
}
export default meta

export const Default: StoryObj<typeof PostContent> = {
  render: () => ({ components: { PostContent }, template: '<PostContent body="<p>Body</p>" />' })
}
