# Asynchronous Blog Loading Plan

The current `BlogService` refreshes all posts at startup using `@PostConstruct`. This blocks the application until XWiki calls complete. We want to make the initial refresh non‑blocking.

## Option 1 – Initial load in parallel
1. Replace the `@PostConstruct` annotation on `refreshPosts`.
2. Create an `@EventListener(ApplicationReadyEvent.class)` method that runs `refreshPosts()` in a new thread. Example:
   ```java
   @EventListener(ApplicationReadyEvent.class)
   public void loadPostsAsync() {
       CompletableFuture.runAsync(this::refreshPosts);
   }
   ```
3. Keep the existing `@Scheduled` annotation for periodic updates.
4. Update health checks to handle the case where posts are still loading (empty list is acceptable at startup).

## Option 2 – Load on demand
1. Remove startup refresh entirely.
2. When a controller calls `getPosts()` or `getPostsByUrl()`, check if the internal lists are empty and `loading` is `false`.
3. If so, trigger `refreshPosts()` asynchronously in a background thread while returning an empty result to the caller.
4. Optionally expose an administrative endpoint to force a refresh.

Both approaches avoid blocking application startup and allow blog content to become available once loaded.
