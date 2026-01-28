import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import HomePhotoInvitation from './HomePhotoInvitation.vue'

const messages: Record<string, string> = {
  'home.photoInvitation.title': "Et si c'était vous ?",
  'home.photoInvitation.ariaLabel': 'Proposer votre photo pour Nudger',
  'home.photoInvitation.contact.subject': 'Proposition de photo pour Nudger',
  'home.photoInvitation.contact.message': 'Bonjour, je propose une photo.',
  'home.photoInvitation.imageAlt': 'Portrait souriant invitant à participer',
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => messages[key] ?? key,
  }),
}))

vi.mock('#i18n', () => ({
  useLocalePath: () => (name: string) => (name === 'contact' ? '/contact' : ''),
}))

const createStub = (tag: string, className = '') =>
  defineComponent({
    name: `${tag}-stub`,
    setup(_, { slots, attrs }) {
      return () =>
        h(
          tag,
          { class: [className, attrs.class], ...attrs },
          slots.default ? slots.default() : []
        )
    },
  })

const NuxtLinkStub = defineComponent({
  name: 'NuxtLinkStub',
  props: {
    to: { type: [String, Object], default: undefined },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        'a',
        {
          ...attrs,
          class: ['nuxt-link-stub', attrs.class],
          'data-to':
            typeof props.to === 'string'
              ? props.to
              : props.to
                ? JSON.stringify(props.to)
                : undefined,
        },
        slots.default?.()
      )
  },
})

describe('HomePhotoInvitation', () => {
  it('renders the invitation title and contact link', async () => {
    const wrapper = await mountSuspended(HomePhotoInvitation, {
      props: {
        imageSrc: '/images/example.png',
      },
      global: {
        stubs: {
          NuxtLink: NuxtLinkStub,
          VCard: createStub('div'),
          VImg: createStub('img'),
        },
      },
    })

    expect(wrapper.text()).toContain(messages['home.photoInvitation.title'])

    const link = wrapper.find('.nuxt-link-stub')
    const linkTarget = JSON.parse(link.attributes('data-to')) as {
      path: string
      query: { subject: string; message: string }
    }

    expect(linkTarget).toEqual({
      path: '/contact',
      query: {
        subject: messages['home.photoInvitation.contact.subject'],
        message: messages['home.photoInvitation.contact.message'],
      },
    })
  })
})
