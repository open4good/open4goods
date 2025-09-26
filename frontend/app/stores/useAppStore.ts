// https://pinia.vuejs.org/core-concepts/
import { defineStore } from 'pinia'
import { NODE_MODE_DEV } from '~/constants';

export const useAppStore = defineStore('app', {
  state: () => ({
    debugMode: process.env.NODE_ENV === NODE_MODE_DEV,
  }),
  getters: {},
  actions: {},
})
