import PieChart from './PieChart.vue'
import type { Meta, StoryObj } from '@storybook/vue3'

const meta: Meta<typeof PieChart> = {
  component: PieChart,
  title: 'Charts/PieChart'
}
export default meta

export const Basic: StoryObj<typeof PieChart> = {
  render: () => ({
    components: { PieChart },
    setup() {
      return { data: { labels: ['A', 'B'], values: [10, 5] } }
    },
    template: '<PieChart :data="data" />'
  })
}
