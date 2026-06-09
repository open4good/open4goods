import { listMetrikProviders } from '../../utils/metriks'

export default defineEventHandler(async () => listMetrikProviders())
