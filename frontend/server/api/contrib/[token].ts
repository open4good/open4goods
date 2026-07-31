import { defineEventHandler } from 'h3'

import { resolveAffiliationRedirect } from '../../utils/affiliation-redirect'

export default defineEventHandler(event => resolveAffiliationRedirect(event))
