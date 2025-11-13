import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'fr.nudger.app',
  appName: 'Nudger',
  webDir: '.output/public',
  bundledWebRuntime: false,
  server: {
    androidScheme: 'https',
  },
  ios: {
    contentInset: 'automatic',
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 0,
      androidScaleType: 'CENTER_CROP',
      splashFullScreen: true,
    },
  },
}

export default config
