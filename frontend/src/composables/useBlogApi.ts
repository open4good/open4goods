import { BlogApi, Configuration } from '@/api'
import { useRuntimeConfig } from '#imports'

export function useBlogApi() {
  const config = useRuntimeConfig()
  return new BlogApi(new Configuration({ basePath: config.public.siteUrl }))
}
