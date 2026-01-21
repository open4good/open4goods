import { computed, type ComputedRef } from 'vue'
import type { ComposerTranslation } from 'vue-i18n'
import { resolveLocalizedRoutePath } from '~~/shared/utils/localized-routes'

export interface CommunityLink {
  id: string
  label: string
  description?: string
  icon: string
  to?: string
  href?: string
  external?: boolean
}

export interface CommunitySection {
  id: string
  title: string
  description: string
  links: CommunityLink[]
}

export interface UseCommunityMenuResult {
  sections: ComputedRef<CommunitySection[]>
  activePaths: ComputedRef<string[]>
}

export const useCommunityMenu = (
  t: ComposerTranslation,
  currentLocale: ComputedRef<string>
): UseCommunityMenuResult => {
  const linkedinUrl = computed(() => String(t('siteIdentity.links.linkedin')))

  const sections = computed<CommunitySection[]>(() => {
    const feedbackLink: CommunityLink = {
      id: 'feedback',
      label: String(
        t('siteIdentity.menu.community.sections.connect.items.feedback.label')
      ),
      description: String(
        t(
          'siteIdentity.menu.community.sections.connect.items.feedback.description'
        )
      ),
      icon: 'mdi-message-draw',
      to: resolveLocalizedRoutePath('feedback', currentLocale.value),
    }

    const contactLink: CommunityLink = {
      id: 'contact',
      label: String(
        t('siteIdentity.menu.community.sections.connect.items.contact.label')
      ),
      description: String(
        t(
          'siteIdentity.menu.community.sections.connect.items.contact.description'
        )
      ),
      icon: 'mdi-email-outline',
      to: resolveLocalizedRoutePath('contact', currentLocale.value),
    }

    const followLink: CommunityLink = {
      id: 'follow',
      label: String(
        t('siteIdentity.menu.community.sections.connect.items.follow.label')
      ),
      description: String(
        t(
          'siteIdentity.menu.community.sections.connect.items.follow.description'
        )
      ),
      icon: 'mdi-linkedin',
      href: linkedinUrl.value,
      external: true,
    }

    const teamLink: CommunityLink = {
      id: 'team',
      label: String(
        t('siteIdentity.menu.community.sections.collaborate.items.team.label')
      ),
      description: String(
        t(
          'siteIdentity.menu.community.sections.collaborate.items.team.description'
        )
      ),
      icon: 'mdi-account-group-outline',
      to: resolveLocalizedRoutePath('team', currentLocale.value),
    }

    const partnersLink: CommunityLink = {
      id: 'partners',
      label: String(
        t(
          'siteIdentity.menu.community.sections.collaborate.items.partners.label'
        )
      ),
      description: String(
        t(
          'siteIdentity.menu.community.sections.collaborate.items.partners.description'
        )
      ),
      icon: 'mdi-handshake-outline',
      to: resolveLocalizedRoutePath('partners', currentLocale.value),
    }

    const openDataLink: CommunityLink = {
      id: 'opendata',
      label: String(
        t('siteIdentity.menu.community.sections.resources.items.opendata.label')
      ),
      description: String(
        t(
          'siteIdentity.menu.community.sections.resources.items.opendata.description'
        )
      ),
      icon: 'mdi-database-outline',
      to: resolveLocalizedRoutePath('opendata', currentLocale.value),
    }

    const openSourceLink: CommunityLink = {
      id: 'opensource',
      label: String(
        t(
          'siteIdentity.menu.community.sections.resources.items.opensource.label'
        )
      ),
      description: String(
        t(
          'siteIdentity.menu.community.sections.resources.items.opensource.description'
        )
      ),
      icon: 'mdi-source-branch',
      to: resolveLocalizedRoutePath('opensource', currentLocale.value),
    }

    return [
      {
        id: 'connect',
        title: String(t('siteIdentity.menu.community.sections.connect.title')),
        description: String(
          t('siteIdentity.menu.community.sections.connect.description')
        ),
        links: [feedbackLink, contactLink, followLink],
      },
      {
        id: 'collaborate',
        title: String(
          t('siteIdentity.menu.community.sections.collaborate.title')
        ),
        description: String(
          t('siteIdentity.menu.community.sections.collaborate.description')
        ),
        links: [teamLink, partnersLink],
      },
      {
        id: 'resources',
        title: String(
          t('siteIdentity.menu.community.sections.resources.title')
        ),
        description: String(
          t('siteIdentity.menu.community.sections.resources.description')
        ),
        links: [openDataLink, openSourceLink],
      },
    ]
  })

  const activePaths = computed(() =>
    sections.value
      .flatMap(section => section.links.map(link => link.to))
      .filter(
        (path): path is string => typeof path === 'string' && path.length > 0
      )
  )

  return { sections, activePaths }
}

export type { CommunityLink as CommunityMenuLink }
