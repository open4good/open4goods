import { blogService } from '~/services/blog.service'

/**
 * Test endpoint to debug blog data
 */
export default defineEventHandler(async _event => {
  try {
    // Get raw data from the external API
    const response = await blogService.getArticles()

    // Return both the processed data and some debug info
    return {
      success: true,
      timestamp: new Date().toISOString(),
      data: response,
      debug: {
        articlesCount: response.data?.length || 0,
        sampleArticle: response.data?.[0] || null,
        imageUrls:
          response.data?.map(article => ({
            title: article.title,
            image: article.image,
            hasImage: !!article.image,
            imageLength: article.image?.length || 0,
          })) || [],
      },
    }
  } catch (error) {
    console.error('Error in test endpoint:', error)

    return {
      success: false,
      error: error instanceof Error ? error.message : 'Unknown error',
      timestamp: new Date().toISOString(),
    }
  }
})
