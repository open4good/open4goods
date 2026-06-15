// AUTO-GENERATED FILE - DO NOT EDIT

import { z } from 'zod'

export const healthSchema = z.object({
  status: z.string(),
  components: z.record(z.string(), z.unknown()).optional()
})

export type HealthInput = z.infer<typeof healthSchema>
