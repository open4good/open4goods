import { getLatestWeeklyDigest } from '../../../utils/metriks'

export default defineEventHandler(async () => getLatestWeeklyDigest())
