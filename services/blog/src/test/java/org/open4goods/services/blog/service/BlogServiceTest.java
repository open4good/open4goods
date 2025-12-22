package org.open4goods.services.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.Localisable;
import org.open4goods.services.blog.config.BlogConfiguration;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    @Mock
    private XwikiFacadeService xwikiFacadeService;
    @Mock
    private BlogConfiguration config;
    @Mock
    private Localisable<String, String> baseUrl;

    @Test
    void healthShouldBeUpWhenLoading() throws InterruptedException {
        BlogService blogService = new BlogService(xwikiFacadeService, config, baseUrl);
        
        // Mock xwiki service to block so we can observe the "loading" state
        CountDownLatch latch = new CountDownLatch(1);
        when(xwikiFacadeService.getPages(anyString())).thenAnswer(invocation -> {
            latch.await();
            return null; 
        });

        // Trigger update in a separate thread
        CompletableFuture.runAsync(() -> {
        	try {
				blogService.refreshPosts();
			} catch (Exception e) {
				// ignore
			}
        });

        // Wait a small amount of time to allow the thread to start and reach the blocking call
        Thread.sleep(200);

        Health health = blogService.health();
        
        // Unblock
        latch.countDown();
        
        // Assertion
        assertThat(health.getDetails()).containsEntry("loading", true);
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void healthShouldBeDownWhenNotLoadingAndNoPosts() {
        BlogService blogService = new BlogService(xwikiFacadeService, config, baseUrl);
        
        // Initial state: loading=false, posts empty
        Health health = blogService.health();
        
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("posts_count", 0);
    }
}
