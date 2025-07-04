import BarChart from './BarChart.vue'
import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta<typeof BarChart> = {
  component: BarChart,
  title: 'Charts/BarChart'
}
export default meta

export const Basic: StoryObj<typeof BarChart> = {
  render: () => ({
    components: { BarChart },
    setup() {
      return { data: { labels: ['A', 'B'], values: [4, 2] } }
    },
    template: '<BarChart :data="data" />'
  })
}
