import { getReleaseNotes } from '../../utils/releases'

export default defineEventHandler(async () => getReleaseNotes())
