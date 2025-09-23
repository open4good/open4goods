<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';

const props = defineProps({
  videoSrc: {
    type: String,
    required: true,
  },
  posterUrl: {
    type: String,
    default: '~/assets/images/video-image-placeholder.png',
  },
});

const isVideoLoaded = ref(false);
const showPlaceholder = ref(true);
const heroVideoRef = ref<HTMLVideoElement | null>(null);
const intersectionObserver = ref<IntersectionObserver | null>(null);
const isLoadingError = ref(false);

const videoClasses = computed(() => ({
  'hero-video': true,
  loaded: isVideoLoaded.value,
}));

const loadingClasses = computed(() => ({
  'loading-indicator': true,
  hidden: isVideoLoaded.value,
}));

// Handles video loading and transitions
const onVideoLoaded = () => {
  isVideoLoaded.value = true;
  showPlaceholder.value = false;
};

// Handles loading errors
const onVideoError = () => {
  isLoadingError.value = true;
  showPlaceholder.value = true;
};

// Attempts to start autoplay
const attemptAutoplay = () => {
  if (!heroVideoRef.value) return;
  heroVideoRef.value.play().catch(() => {});
};

onMounted(() => {
  const videoElement = heroVideoRef.value;
  if (!videoElement) {
    showPlaceholder.value = true;
    return;
  }

  // Check if video is already loaded (cache case)
  if (videoElement.readyState >= 3) {
    onVideoLoaded();
    attemptAutoplay();
  }

  // Events to handle loading - using loadstart for faster response
  videoElement.addEventListener('loadstart', onVideoLoaded);
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
    videoElement.removeEventListener('loadstart', onVideoLoaded);
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
      preload="none"
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
$z-index-video: 1
$z-index-overlay: 2
$z-index-content: 3
$z-index-loading: 4

$transition-duration: 0.3s
$animation-duration-gradient: 4s
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
  z-index: $z-index-video
  background-image: url('~/assets/images/video-image-placeholder.png')
  background-size: cover
  background-position: center
  background-repeat: no-repeat
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