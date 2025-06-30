import Card from './Card.vue'
import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta<typeof Card> = {
  component: Card,
  title: 'Card'
}
export default meta

export const Default: StoryObj<typeof Card> = {
  render: () => ({ components: { Card }, template: '<Card>Example</Card>' })
}
