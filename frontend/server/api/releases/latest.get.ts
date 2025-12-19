import { getLatestRelease } from '../../utils/releases'

export default defineEventHandler(async () => getLatestRelease())
