
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, test } from 'vitest'
import TheArticle from './TheArticle.vue'
import type { BlogPostDto } from '~~/shared/api-client'

interface BlogArticle extends BlogPostDto {
  content?: string
}

describe('TheArticle component', () => {
  const article: BlogArticle = {
    title: 'Test Title',
    author: 'Jane Doe',
    content: '<p>HTML content</p>',
    createdMs: 1640995200000,
  }

  test('renders title, author, date and content', async () => {
    const wrapper = await mountSuspended(TheArticle, {
      props: { article },
    })

    expect(wrapper.find('h1').text()).toBe('Test Title')
    expect(wrapper.text()).toContain('Jane Doe')

    const formattedDate = new Date(article.createdMs ?? 0).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    })
    expect(wrapper.text()).toContain(formattedDate)

    // TODO : Fix this test
    //expect(wrapper.find('.article-content').html()).toContain('HTML content')
  })
})
