package org.open4goods.services.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Calendar;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.open4goods.model.Localisable;
import org.open4goods.services.blog.config.BlogConfiguration;
import org.open4goods.services.blog.model.BlogPost;
import org.open4goods.xwiki.XWikiServiceConfiguration;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BlogServiceTest.Config.class)
class BlogServiceTest {

    @Configuration
    @Import(BlogService.class)
    static class Config {
        @Bean
        BlogConfiguration blogConfiguration() {
            return new BlogConfiguration();
        }

        @Bean
        Localisable<String, String> baseUrl() {
            return new Localisable<>();
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(XWikiServiceConfiguration.ONE_HOUR_LOCAL_CACHE_NAME);
        }
    }

    @MockBean
    XwikiFacadeService xwikiFacadeService;

    @Autowired
    BlogService blogService;

    @Test
    void updateIndexDoesNotFetchFullPosts() {
        Pages pages = new Pages();
        PageSummary ps = new PageSummary();
        ps.setName("slug");
        ps.setTitle("Title");
        pages.getPageSummaries().add(ps);
        when(xwikiFacadeService.getPages("Blog")).thenReturn(pages);

        blogService.updatePostIndex();

        verify(xwikiFacadeService).getPages("Blog");
        verify(xwikiFacadeService, never()).getFullPage(anyString(), anyString());
        assertThat(blogService.getPostIndex()).containsEntry("slug", "Title");
    }

    @Test
    void getPostUsesCache() {
        FullPage full = new FullPage();
        Page page = new Page();
        page.setHidden(false);
        page.setId("xwiki:Blog.slug");
        page.setModified(Calendar.getInstance());
        full.setWikiPage(page);
        full.setProperties(new HashMap<>());
        full.getProperties().put("title", "Title");
        when(xwikiFacadeService.getFullPage("Blog", "slug")).thenReturn(full);

        BlogPost first = blogService.getPost("slug");
        BlogPost second = blogService.getPost("slug");

        verify(xwikiFacadeService, times(1)).getFullPage("Blog", "slug");
        assertThat(first.getTitle()).isEqualTo("Title");
        assertThat(second).isSameAs(first);
    }
}
