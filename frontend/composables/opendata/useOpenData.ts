import { OpendataApi } from '~/src/api'

const api = new OpendataApi()

export const useOpenData = () => {
  return useAsyncData('open-data', () => api.getOpendataMeta())
}
