<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';

const props = defineProps({
  videoSrc: {
    type: String,
    required: true,
  },
  posterUrl: {
    type: String,
    default:
      'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIwIiBoZWlnaHQ9IjI0MCIgdmlld0JveD0iMCAwIDMyMCAyNDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIzMjAiIGhlaWdodD0iMjQwIiBmaWxsPSJ1cmwoI3BhaW50MF9saW5lYXJfMF8xKSIvPgo8ZGVmcz4KPGxpbmVhckdyYWRpZW50IGlkPSJwYWludDBfbGluZWFyXzBfMSIgeDE9IjAiIHkxPSIwIiB4Mj0iMzIwIiB5Mj0iMjQwIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+CjxzdG9wIHN0b3AtY29sb3I9IiM2NjdlZWEiLz4KPHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjNzY0YmEyIi8+CjwvbGluZWFyR3JhZGllbnQ+CjwvZGVmcz4KPC9zdmc+Cg==',
  },
});

const isVideoLoaded = ref(false);
const showPlaceholder = ref(true);
const showLoading = ref(true);
const heroVideoRef = ref<HTMLVideoElement | null>(null);
const intersectionObserver = ref<IntersectionObserver | null>(null);
const isLoadingError = ref(false);

const videoClasses = computed(() => ({
  'hero-video': true,
  loaded: isVideoLoaded.value,
}));

const loadingClasses = computed(() => ({
  'loading-indicator': true,
  hidden: !showLoading.value,
}));

// Handles video loading and transitions
const onVideoLoaded = () => {
  console.log('✅ Video loaded and ready to play.');
  isVideoLoaded.value = true;
  showLoading.value = false;
  // Use a small delay for a smooth transition
  setTimeout(() => {
    showPlaceholder.value = false;
  }, 300);
};

// Handles loading errors
const onVideoError = (e: Event) => {
  console.error('❌ Video loading error:', e);
  isLoadingError.value = true;
  showLoading.value = false;
  showPlaceholder.value = true; // Ensures placeholder remains visible
  // Display error message or reload button can be added here
};

// Attempts to start autoplay
const attemptAutoplay = () => {
  if (!heroVideoRef.value) return;
  const playPromise = heroVideoRef.value.play();
  if (playPromise !== undefined) {
    playPromise
      .then(() => {
        console.log('🎬 Autoplay successful.');
      })
      .catch((error: Error) => {
        console.log('🚫 Autoplay blocked by browser:', error);
        // If autoplay is blocked, we can leave the placeholder
        // or add a "play" button
      });
  }
};

onMounted(() => {
  const videoElement = heroVideoRef.value;
  if (!videoElement) {
    console.warn('🎥 Video element not found. Using CSS fallback.');
    showPlaceholder.value = true;
    showLoading.value = false;
    return;
  }

  // Check if video is already loaded (cache case)
  if (videoElement.readyState >= 3) {
    console.log('🎥 Video already cached, immediate loading.');
    onVideoLoaded();
    attemptAutoplay();
  }

  // Events to handle loading
  videoElement.addEventListener('loadeddata', onVideoLoaded);
  videoElement.addEventListener('canplaythrough', onVideoLoaded);
  videoElement.addEventListener('canplay', attemptAutoplay);
  videoElement.addEventListener('error', onVideoError);

  // Intersection Observer to optimize playback
  if ('IntersectionObserver' in window) {
    intersectionObserver.value = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            if (videoElement.paused) {
              videoElement.play().catch(() => {});
            }
          } else {
            if (!videoElement.paused) {
              videoElement.pause();
            }
          }
        });
      },
      { threshold: 0.5 }
    );
    intersectionObserver.value.observe(videoElement);
  }
});

onBeforeUnmount(() => {
  // Cleanup events and observer
  const videoElement = heroVideoRef.value;
  if (videoElement) {
    videoElement.removeEventListener('loadeddata', onVideoLoaded);
    videoElement.removeEventListener('canplaythrough', onVideoLoaded);
    videoElement.removeEventListener('canplay', attemptAutoplay);
    videoElement.removeEventListener('error', onVideoError);
  }
  if (intersectionObserver.value) {
    intersectionObserver.value.disconnect();
  }
});
</script>

<template>
  <section class="hero-section">
    <The-hero-title />
    <div :class="loadingClasses">
      <div class="spinner"></div>
    </div>

    <div v-if="showPlaceholder || isLoadingError" class="video-placeholder"></div>

    <video
      v-if="!isLoadingError"
      ref="heroVideoRef"
      :class="videoClasses"
      playsinline
      muted
      loop
      autoplay
      preload="metadata"
      :poster="props.posterUrl"
    >
      <source :src="props.videoSrc" type="video/mp4" />
      <p>Your browser does not support HTML5 video playback. Consider updating for a modern experience.</p>
    </video>

    <div class="hero-overlay"></div>
  </section>
</template>

<style lang="sass" scoped>
// SASS Variables
$hero-min-height: 600px
$z-index-placeholder: 1
$z-index-video: 2
$z-index-overlay: 3
$z-index-content: 4
$z-index-loading: 5

$transition-duration: 0.8s
$animation-duration-gradient: 8s
$animation-duration-fadeup: 1.2s

$max-content-width: 1200px
$subtitle-max-width: 600px

// Mixins
@mixin position-absolute-fullsize
  position: absolute
  top: 0
  left: 0
  width: 100%
  height: 100%

@mixin center-absolute
  position: absolute
  top: 50%
  left: 50%
  transform: translate(-50%, -50%)

// Animations
@keyframes gradientShift
  0%
    background-position: 0% 50%
  50%
    background-position: 100% 50%
  100%
    background-position: 0% 50%

@keyframes fadeInUp
  0%
    opacity: 0
    transform: translateY(30px)
  100%
    opacity: 1
    transform: translateY(0)

@keyframes spin
  0%
    transform: rotate(0deg)
  100%
    transform: rotate(360deg)

// HERO SECTION - MAIN CONTAINER
.hero-section
  position: relative
  width: 100%
  height: 100vh
  min-height: $hero-min-height
  overflow: hidden
  display: flex
  align-items: center
  justify-content: center
  background: #1a1a1a
  border-radius: 20px

// VIDEO PLACEHOLDER
.video-placeholder
  @include position-absolute-fullsize
  z-index: $z-index-placeholder
  background: linear-gradient(45deg, #667eea 0%, #764ba2 25%, #f093fb 50%, #f5576c 75%, #4facfe 100%)
  background-size: 400% 400%
  animation: gradientShift $animation-duration-gradient ease infinite
  opacity: 1
  transition: opacity $transition-duration ease-in-out

// ACTUAL VIDEO ELEMENT
.hero-video
  @include position-absolute-fullsize
  object-fit: cover
  z-index: $z-index-video
  opacity: 0
  transition: opacity $transition-duration ease-in-out

  &.loaded
    opacity: 1

// OVERLAY
.hero-overlay
  @include position-absolute-fullsize
  background: linear-gradient(70deg, rgba(0, 0, 0, 0.60) 2.67%, rgba(146, 146, 146, 0.00) 78.72%)
  z-index: $z-index-overlay

// HERO TEXT CONTENT
.hero-content
  position: relative
  z-index: $z-index-content
  text-align: center
  color: white
  max-width: $max-content-width
  padding: 0 2rem
  animation: fadeInUp $animation-duration-fadeup ease-out

.hero-title
  font-size: clamp(2.5rem, 8vw, 6rem)
  font-weight: 700
  margin-bottom: 1.5rem
  background: linear-gradient(135deg, #fff 0%, #e2e8f0 100%)
  background-clip: text
  -webkit-background-clip: text
  -webkit-text-fill-color: transparent
  text-shadow: 0 0 30px rgba(255, 255, 255, 0.3)

.hero-subtitle
  font-size: clamp(1.25rem, 3vw, 1.8rem)
  margin-bottom: 2.5rem
  opacity: 0.95
  font-weight: 300
  max-width: $subtitle-max-width
  margin-left: auto
  margin-right: auto

// CALL-TO-ACTION BUTTON
.hero-cta
  a
    display: inline-block
    padding: 1rem 2.5rem
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)
    color: white
    text-decoration: none
    border-radius: 50px
    font-weight: 600
    font-size: 1.1rem
    transition: all 0.3s ease
    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3)
    user-select: none

    &:hover
      transform: translateY(-2px)
      box-shadow: 0 15px 40px rgba(102, 126, 234, 0.4)

    &:active
      transform: translateY(0)

// LOADING INDICATOR
.loading-indicator
  @include center-absolute
  z-index: $z-index-loading
  opacity: 1
  transition: opacity 0.5s ease
  pointer-events: none

  &.hidden
    opacity: 0

.spinner
  width: 40px
  height: 40px
  border: 3px solid rgba(255, 255, 255, 0.3)
  border-top: 3px solid white
  border-radius: 50%
  animation: spin 1s linear infinite

// RESPONSIVE DESIGN
@media (max-width: 1024px)
  .hero-section
    height: 80vh

@media (max-width: 768px)
  .hero-section
    height: 70vh
    min-height: 500px

  .hero-content
    padding: 0 1rem

  .hero-overlay
    background: linear-gradient(135deg, rgba(0, 0, 0, 0.6) 0%, rgba(0, 0, 0, 0.3) 100%)

@media (max-width: 480px)
  .hero-subtitle
    font-size: 1.1rem

  .hero-cta a
    padding: 0.8rem 2rem
    font-size: 1rem

// USER PREFERENCES
@media (prefers-reduced-motion: reduce)
  .video-placeholder
    animation: none

  .hero-content
    animation: none

  *
    transition-duration: 0.1s !important
</style>